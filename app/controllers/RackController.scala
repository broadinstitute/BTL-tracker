package controllers

import models.Component.{HiddenFields,ComponentType}
import models.{Component,ContainerDivisions,Rack}
import org.broadinstitute.LIMStales.mongo.BtllimsRacksCollection
import org.broadinstitute.LIMStales.sampleRacks.{BSPTube, SSFIssueList, SSFList, RackScan}
import play.api.data.Form
import play.api.libs.Files
import play.api.libs.json.JsObject
import play.api.mvc.{MultipartFormData, Action}
import Errors.FlashingKeys
import models.project.JiraProject

/**
 * Rack of other containers.
 * Created by nnovod on 1/28/15.
 */
object RackController extends ComponentController[Rack] {
	// Play form for rack
	val form = Rack.form
	// Play formatter for Json verification/conversion
	implicit val formatter = Rack.formatter

	// Play html view to create rack
	def htmlForCreate = views.html.rackCreateForm(_: Form[Rack])

	// Play html view to update rack
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]) =
		views.html.rackUpdateForm(_: Form[Rack], id, hiddenFields)

	// Component type
	val componentType = ComponentType.Rack

	// Way to make component from Json
	def componentFromJson(json: JsObject) = json.as[Rack]

	/**
	 * Request to add a rack - we simply put up the form to get the parameters to create the rack.
	 * @return responds to request with html form to get plate input
	 */
	def addRack() = Action { implicit request => add }

	/**
	 * Request to find a rack, identified by an ID.  We find the rack (or return an error if it can’t be found) and
	 * then go to the page to view/update the rack.
	 * @param id rack ID
	 * @return responds to request with html form to view/update rack
	 */
	def findRackByID(id: String) = Action.async { implicit request => find(id,request) }

	/**
	 * Go create a rack.  If all goes well the plate is created in the DB and the user is redirected to
	 * to view/update the newly created rack.
	 * @return responds to request with html form to view/update added rack
	 */
	def createRackFromForm() = Action.async { implicit request => create(request)}

	/**
	 * Make an error message for when we can't find something in the DB
	 * @param whatNotFound item we couldn't find
	 * @param id item id we looked for
	 * @param err error returned from database
	 * @return nice text with all the error parts put together
	 */
	private def makeErrMsg(whatNotFound: String, id: String, err: Option[String]) = {
		val m = whatNotFound + " not found for " + id
		val dbErr =
			err match {
				case Some(errMsg) => ": " + errMsg
				case None => ""
			}
		m + dbErr
	}

	/**
	 * Do BSP report for a Rack.  We show how the scan done of the rack being used compares with what BSP reported
	 * about the rack.
	 * @param id barcode for rack
	 * @return responds to request with report comparing BSP rack(s) and input rack
	 */
	def doBSPReport(id: String) = Action.async { implicit request =>
		Application.findRequestUsingID(id,request,List(ComponentType.Rack))((cType,json,request) => {
			getBSPmatch(id,
				(matches, foundRack) => {
					// Get layout type and corresponding data to now dimensions of rack
					val layout = (json \ Rack.layoutKey).as[String]
					val layoutType = ContainerDivisions.Division.withName(layout)
					val layoutData = ContainerDivisions.divisionDimensions(layoutType)
					// Show the results
					Ok(views.html.rackScanForm("Rack Scan")(foundRack.issue, id, matches,
						layoutData.rows, layoutData.columns))

				},
				(err) => {
					val result = Redirect(routes.RackController.findRackByID(id))
					FlashingKeys.setFlashingValue(result, FlashingKeys.Status, err)
				}
			)
		})
	}

	/**
	 * Go update a rack.  If all goes well the rack is updated in the DB and the user is redirected to
	 * to view/update the newly updated rack.
	 * @return responds to request with updated form to view/update rack
	 */
	def updateRackFromForm(id: String) = Action.async { implicit request =>
		update(request, id,
			// Before the update to the DB occurs process any rack scan file that's there
			// If there are problems we return an error string
			preUpdate = (data) => {
				processRackScan(request.body.asMultipartFormData) match {
					case Some(racks) if racks.list.isEmpty =>
						Map(Some(Rack.rackScanKey) -> "Scan file contents invalid")
					case Some(racks) if data.project.isEmpty =>
						Map(Some(Component.formKey + "." + Component.projectKey) ->
							"Project must be set before recording scan file")
					case Some(racks) =>
						if (racks.list.exists((r) => r.barcode == data.id)) {
							// Racks file has at least one entry for wanted rack - let's put it into the DB
							BtllimsRacksCollection.insertRacks(
								SSFIssueList(data.project.get, List.empty, None, racks.list))
							Map.empty[Option[String], String]
						} else {
							// Racks file has entries but not wanted rack - get what it is and report error
							val rackFound = racks.list.headOption match {
								case Some(r) => " (" + r.barcode + ")"
								case None => ""
							}
							Map(Some(Rack.rackScanKey) -> ("Scan file is for the wrong rack" + rackFound))
						}
					case _ => Map.empty[Option[String], String]
				}
			})
	}

	/**
	 * Get matches for BSP rack.
 	 * @param id id for rack
	 * @param found callback if matches found for BSP rack
	 * @param notFound callback if matches not found for BSP rack
	 * @tparam R type returned by callback (and thus us)
	 * @return callback return type
	 */
	def getBSPmatch[R](id: String, found: (RackScan#MatchByPos[BSPTube], SSFIssueList[RackScan]) => R,
					   notFound: (String) => R) = {
		// Find the projects with scan of the rack
		val (rack, err) = JiraProject.getRackIssueCollection(id)
		if (rack.isEmpty) {
			// Looks like scan of rack never done
			notFound(makeErrMsg("Scan File entry", id, err))
		} else {
			// Get list of projects containing original BSP scan of rack
			val (bspRacks, bspErr) = JiraProject.getBSPRackIssueCollection(id)
			// Get first of list of scanned racks (should only be one there)
			val foundRack = rack.head
			if (bspRacks.isEmpty || foundRack.list.isEmpty) {
				// Looks like BSP results never entered
				notFound(makeErrMsg("BSP Rack entry", id, bspErr))
			} else {
				// If an empty rack then a bad BSP report
				val isEmptyRack =
					bspRacks.forall((issue) => issue.list.forall(_.contents.isEmpty))
				if (isEmptyRack) {
					notFound("BSP rack has no recorded contents." +
						"  Check if the Jira BSP attachment is missing fields, such as tube barcodes.")
				} else {
					// Get how scan matches up using first project from each list (should only be one in each list)
					val matches = foundRack.list.head.matchContent(bspRacks.head.list)
					found(matches, foundRack)
				}
			}
		}
	}

	/**
	 * Process an updated rack scan file.
 	 * @param d if it exists, multipart form data that contains temporary file that has been update
	 */
	private def processRackScan(d: Option[MultipartFormData[Files.TemporaryFile]]) =
		for {
			data <- d
			file <- data.file(Rack.rackScanKey)
		} yield {
			SSFList(file.ref.file.getCanonicalPath, RackScan)
		}
}
