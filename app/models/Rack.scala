package models

/**
 * Created on 1/28/15.
 * This the MVC model for a rack.
 */
import mappings.CustomMappings._
import models.ContainerDivisions.Division
import models.initialContents.InitialContents
import InitialContents.ContentType
import models.project.JiraProject
import org.broadinstitute.LIMStales.sampleRacks.MatchFound
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json,Format}
import Component._
import play.api.mvc.{AnyContent, Request}

/**
 * Rack - it's a container (can have initial contents) and can have a location (e.g., a freezer) and
 * can have stuff transferred out of it.  Normally a rack has tubes that are subcomponents but for
 * efficiency reasons, if all the tubes of a rack have the same contents and are always used together
 * then the rack itself becomes the container where everything that happens to it happens to all the tubes
 * on the rack.
 *
 * @param id rack barcode
 * @param description optional description of rack
 * @param project optional project ID
 * @param tags name/value pairs to associate with the rack
 * @param locationID Optional ID of Component where this rack is located
 * @param initialContent Optional initial contents
 * @param layout tube layout on rack
 */
case class Rack(override val id: String,override val description: Option[String],override val project: Option[String],
                override val tags: List[ComponentTag],
                override val locationID: Option[String], override val initialContent: Option[ContentType.ContentType],
                override val layout: Division.Division)
	extends Component with Location with Container with Transferrable with JiraProject with ContainerDivisions
	with ComponentCanBeList[Rack] {
	override val component = Rack.componentType
	override val validLocations = Rack.validLocations
	override val validTransfers = Rack.validTransfers
	override val validContents = Rack.validContents

	/**
	 * Check if what's set in request is valid - specifically we check if the project set contains the rack.
 	 * @param request HTTP request (has hidden field with project set before update)
	 * @return Future of map of fields to errors - empty if no errors found
	 */
	override protected def isValid(request: Request[AnyContent]) = isProjectValid(getHiddenField(request,_.project))

	/**
	 * Make a new component that includes the ability to make a list of individual components from the ID list.
	 */
	def toComponentList =
		new Rack(id, description, project, tags, locationID, initialContent, layout) with ComponentList[Rack] {
			/**
			 * Make a copy of this component with a new ID
			 * @param newId new ID to set in component copy
			 * @return component that's a copy of this one except with a new ID
			 */
			def idCopy(newId: String) = this.copy(id = newId)
		}
}

object Rack extends ComponentObject[Rack](ComponentType.Rack) {
	/**
	 * Form mapping for a rack.  Note that component contents must be referred to as componentData.fieldName in forms
	 * since clean inheritance doesn't appear to be possible.  That's why the (un)applyWithComponent is needed
	 * (see componentMap for more details).
	 */
	private def applyWithComponent(c: Component,l: Option[String],
	                               con: Option[ContentType.ContentType],layout: Division.Division) =
		Rack(c.id,c.description,c.project,c.tags,l,con,layout)
	private def unapplyWithComponent(r: Rack) = Some(r.getComponent,r.locationID,r.initialContent,r.layout)

	val validLocations = List(ComponentType.Freezer)
	val validTransfers = List(ComponentType.Rack,ComponentType.Plate,ComponentType.Tube)
	val validContents = InitialContents.ContentType.rackTubes

	/**
	 * Keys to be used in forms (in views and elsewhere) for rack specific fields
 	 */
	val rackScanKey = "rackScan"

	/**
	 * Form to use in views for rack
	 */
	override val form = Form(
		mapping(
			Component.formKey -> Component.componentMap,
			Location.locationKey -> optional(text),
			Container.contentKey -> optional(enum(ContentType)),
			ContainerDivisions.divisionKey -> enum(Division)
		)(applyWithComponent)(unapplyWithComponent))

	/**
	 * Formatter for going to/from and validating Json
	 */
	override implicit val formatter: Format[Rack] = formatWithComponent(Json.format[Rack])

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

	import org.broadinstitute.LIMStales.sampleRacks.{BSPScan, SSFIssueList, BSPTube}

	/**
	 * Get matches for BSP rack.
	 * @param id id (barcode) for rack
	 * @param found callback if matches found for BSP rack
	 * @param notFound callback if matches not found for BSP rack
	 * @tparam R type returned by callback (and thus us)
	 * @return callback return type
	 */
	def getBSPmatch[R](id: String, found: (RackScan#MatchByPos[BSPTube], SSFIssueList[BSPScan]) => R,
					   notFound: (String) => R) = {
		// Find the projects with scan of the rack
		val (rack, err) = RackScan.findRackSync(id)
		if (rack.isEmpty) {
			// Looks like scan of rack never done
			notFound(makeErrMsg("Scan File entry", id, err))
		} else {
			// Get list of projects containing original BSP scan of rack
			val (bspRacks, bspErr) = JiraProject.getBspIssueCollection(id)
			// Get first scanned rack (should only be one there)
			val foundRack = rack.head
			if (bspRacks.isEmpty || foundRack.contents.isEmpty) {
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
					/*
					 * Determine how a rack scan's tube matches up with the same (equivalent 2d barcode) tube found in
					 * a bsp scan.
					 * @param scannedRackBarcode barcode of the rack scanned
					 * @param bspRackBarcode barcode of bsp rack found with matching tube
					 * @param scannedRackTube rack scan's tube's contents
					 * @param bspTube bsp rack's tube's contents
					 * @param foundInPos bsp tube found in our rackrack/position
					 *                   set if and only if rack tube not in same position as bsp tube found
					 * @return
					 */
					def findMatch(scannedRackBarcode: String, bspRackBarcode: String,
								  scannedRackTube: RackTube, bspTube: BSPTube, foundInPos: Option[Option[BSPTube]]) =
						foundInPos match {
							case Some(foundTube) =>
								val isAbDifferent = foundTube match {
									case Some(foundBsp) => foundBsp.antiBody != bspTube.antiBody
									case None => bspTube.antiBody.isDefined
								}
								if (isAbDifferent) MatchFound.NotFound
								else if (scannedRackBarcode != bspRackBarcode) MatchFound.NotRack
								else MatchFound.NotWell
							case None => MatchFound.Match
						}
					// Match rack scan with all bsp rack scans for project
					// (note heads are ok - should only be one rack scan and only one project for bsp rack(s))
					val bspProject = bspRacks.head
					val bspScans = bspProject.list
					val matches = foundRack.matchContent(bspScans, Some(findMatch(_, _, _, _, _)))
					found(matches, bspProject)
				}
			}
		}
	}
}

