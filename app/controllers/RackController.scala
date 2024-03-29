package controllers

import models.Component.{HiddenFields,ComponentType}
import models.db.TrackerCollection
import models.initialContents.InitialContents
import models.project.JiraProject
import models._
import org.broadinstitute.LIMStales.sampleRacks.MatchFound
import play.api.data.Form
import play.api.libs.Files
import play.api.libs.json.JsObject
import play.api.mvc.{MultipartFormData, Action}
import utils.MessageHandler
import utils.MessageHandler.FlashingKeys
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

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
	def htmlForCreate(id: String) = views.html.rackCreateForm(_: Form[Rack], id)

	// Play html view to create stack of racks
	def htmlForCreateStack = views.html.rackCreateStackForm(_: Form[Rack])

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
	def addRack(id: String) = Action { request => add(id) }

	/**
	 * Request to find a rack, identified by an ID.  We find the rack (or return an error if it can’t be found) and
	 * then go to the page to view/update the rack.
	 * @param id rack ID
	 * @return responds to request with html form to view/update rack
	 */
	def findRackByID(id: String) = Action.async { request => find(id,request) }

	/**
	 * Go create a rack.  If all goes well the plate is created in the DB and the user is redirected home.
	 * @param id rack ID
	 * @return responds to request with message and form with errors or home page
	 */
	def createRackFromForm(id: String) = Action.async { implicit request => create(id, request) }

	/**
	 * Get a form filled in with specified ID
	 * @param id id to set in form
	 * @return rack form filled in with id
	 */
	private def makeIdForm(id: String) = form.fill(Rack(id,
		None, None, List.empty, None, None, ContainerDivisions.Division.DIM8x12))

	/**
	 * Create html to be sent back to get parameters to make a stack.  First create the form filled in with IDs and
	 * then output the html based on the created form.
	 * @param id ids for stack
	 * @param completionStr completion status to set as global message in form
	 * @return html to create stack of racks
	 */
	def makeStackHtml(id: String, completionStr: String) = {
		val idForm = makeIdForm(id)
		val formWithStatus = MessageHandler.formGlobalError(idForm, completionStr)
		htmlForCreateStack(formWithStatus)
	}

	/**
	 * Request to add a stack of racks
	 * @return response with completion status of rack insertions
	 */
	def addRackStack() = Action.async { request => addStack(request) }

	/**
	 * Map of match found to description of match for sample tubes
	 */
	private lazy val bspSampleLegend = Map(
		MatchFound.Match ->"Rack and well location match",
		MatchFound.NotWell -> "Match of rack only (well locations differ)",
		MatchFound.NotRack -> "Rack and well location both do not match",
		MatchFound.NotFound -> "Tube not found in BSP samples"
	)

	/**
	 * Map of match found to description of match for sample tubes to receive antibodies
	 */
	private lazy val bspAntibodyLegend = Map(
		MatchFound.Match ->"Rack and well location match",
		MatchFound.NotWell -> "Rack and antibody match but well does not",
		MatchFound.NotRack -> "Antibody matches but rack and well location both do not match",
		MatchFound.NotFound -> "Tube not found in BSP samples or antibody does not match"
	)

	/**
	 * Do BSP report for a Rack.  We show how the scan done of the rack being used compares with what BSP reported
	 * about the rack.
	 * @param id barcode for rack
	 * @return responds to request with report comparing BSP rack(s) and input rack
	 */
	def doBSPReport(id: String) = Action.async { implicit request =>
		Application.findRequestUsingID(id,request,List(ComponentType.Rack))((cType,json,request) => {
			Rack.getBSPmatch(id,
				(matches, foundRack) => {
					// Get suitable legend
					val isAntibody = foundRack.list.exists(_.contents.exists(_.antiBody.isDefined))
					val legend = if (isAntibody) bspAntibodyLegend else bspSampleLegend
					// Get layout type and corresponding data to know dimensions of rack
					val layout = (json \ ContainerDivisions.divisionKey).as[String]
					val layoutType = ContainerDivisions.Division.withName(layout)
					val layoutData = ContainerDivisions.divisionDimensions(layoutType)
					// Show the results
					Ok(views.html.rackScanForm("Rack Scan")(foundRack.issue, id, matches,
						layoutData.rows, layoutData.columns, legend))
				},
				(err) => {
					val result = Redirect(routes.RackController.findRackByID(id))
					FlashingKeys.setFlashingValue(result, FlashingKeys.Status, err)
				}
			)
		})
	}

	//@TODO Better error correction
	private def insertScan(rackScan: RackScan) =
		RackScan.insertOrReplace(rackScan).map((_) =>
			Map.empty[Option[String], String])

	/**
	 * Go update a rack.  If all goes well the rack is updated in the DB and the user is redirected to
	 * to the home page.  Before the update takes place we check if a scan file, with barcodes and positiions,
	 * has been specified.  If a rack scan file is there we record the contents.
	 * @return responds to request with message and form with errors or home page
	 */
	def updateRackFromForm(id: String) = Action.async { implicit request =>
		update(id, request,
			// Before the update to the DB occurs process any rack scan file that's there
			// If there are problems we return an error string
			preUpdate = (data) => {
				processRackScan(request.body.asMultipartFormData) match {
					case Some(racks) =>
						val rackList = racks.list
						if (rackList.isEmpty)
							Future.successful(Map(Some(Rack.rackScanKey) -> "Scan file contents invalid"))
						else if (rackList.size != 1)
							Future.successful(Map(Some(Rack.rackScanKey) ->
								"Scan file for multiple racks.  It must be for a single rack"))
						else if (rackList.head.barcode != data.id)
							Future.successful(Map(Some(Rack.rackScanKey) ->
								("Scan file is for the wrong rack: " + racks.list.head.barcode)))
						else {
							val rackEntry = rackList.head
							val rackEntryWithoutNA =
								rackEntry.copy(
									// Take out any N/A barcodes
									contents = rackEntry.contents.flatMap(
										(c) => if (c.barcode != "N/A") Some(c) else None)
								)
							data.initialContent match {
								// BSP tubes - make sure project is set and then enter scan results in DB
								case Some(InitialContents.ContentType.BSPtubes) =>
									if (data.project.isEmpty)
										Future.successful(Map(Some(Component.formKey + "." + Component.projectKey) ->
											"Project must be set before recording scan file for BSP samples"))
									else
										insertScan(rackEntryWithoutNA)
								// Antibody tubes - make sure all entries are tubes and then enter scan results in DB
								case _ =>
									val ids = rackEntryWithoutNA.contents.map((rackTube) => rackTube.barcode)
									RackScan.findTubes(ids).flatMap {
										case (tubes, notTubes, _) if notTubes.nonEmpty =>
											Future.successful(Map(None ->
												s"Entries not tubes: ${notTubes.map(_.id).mkString(",")}"))
										// If some tubes not registered then register them now
										case (tubes, _, notFound) if notFound.nonEmpty =>
											// Make list of tubes not registered
											val multiTube =
												Tube(id = notFound.mkString(" "),
													description = Some(s"Tube registered from scan of rack $id"),
													project = data.project, tags = List.empty,
													locationID = None, initialContent = None)
													.toComponentList
											val tubeList = multiTube.makeList
											// Try to register these tubes
											TrackerCollection.insertComponents(tubeList).flatMap {
												// If some registrations failed then report it
												case (ok: List[String], nok: List[String]) if nok.nonEmpty =>
													def plural(x: Int) = if (x == 1) "" else "s"
													val summary =
														s"${ok.size} component${plural(ok.size)} succcessfully inserted, " +
														s"${nok.size} insertion${plural(nok.size)} failed" +
														":\n" + nok.mkString(";\n")
													Future.successful(Map(None ->
														s"Attempt to register tubes failed: $summary"))
												// We registered all the tubes - now go insert the scan
												case _ => insertScan(rackEntryWithoutNA)
											}
										// All tubes found and they're tubes - just insert the scan
										case (tubes, _, _) =>
											insertScan(rackEntryWithoutNA)
									}
							}
						}
					case _ => Future.successful(Map.empty[Option[String], String])
				}
			})
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
			JiraProject.makeRackScanList(file.ref.file.getCanonicalPath)
		}
}
