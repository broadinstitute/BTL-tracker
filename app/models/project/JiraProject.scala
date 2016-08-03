package models.project

import models.initialContents.InitialContents
import models.initialContents.InitialContents.ContentType
import models._
import models.Component._
import org.broadinstitute.LIMStales.sampleRacks.RackScan
import org.broadinstitute.LIMStales.sampleRacks._
import JiraDBs._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Module to look at associations between a component and a jira project (set as component's project).
 * @author Nathaniel Novod
 *         Date: 2/12/15
 *         Time: 4:27 PM
 */

trait JiraProject {
	// Must be associated with a component
	this: Component =>

	/**
	 * Check if what's set in request is valid.  Specifically we check if the project set contains the rack(s) or
	 * plate(s).
	 * @param hidden Hidden field (normally from HTTP request) with project set before update
	 * @return Future of map of form fields to errors - empty if no errors found
	 */
	protected def isProjectValid(hidden: Option[HiddenFields]) : Future[Map[Option[String], String]] = {
		// Get project and initialContents before request
		val (proj, initContent) = hidden match {
			case Some(h) => (h.project, h.contentType)
			case _ => (None, None)
		}
		// Check if we're changing to sample initial content
		val isChangingToBSP =
			this match {
				case c: Container => c.initialContent != initContent &&
					(c.initialContent.contains(ContentType.BSPtubes) || c.initialContent.contains(ContentType.SamplePlate))
				case _ => false
			}
		// If no project set or neither project nor content type changed then we're all set
		if (project.isEmpty || (proj == project && !isChangingToBSP))
			Future.successful(Map.empty)
		else {
			val proj = project.get
			this match {
				case cl: ComponentList[_] =>
					val components = cl.makeList
					val projChecks = for (c <- components) yield checkProject(c.id, proj)
					Future.fold(projChecks)(Map.empty[Option[String], String]){
						case (soFar, Some((key, value))) =>
							soFar.get(key) match {
								case Some(valSet) => soFar + (key -> (valSet + ";\n" + value))
								case _ => soFar + (key -> value)
							}
						case (soFar, None) => soFar
					}
				case c => checkProject(c.id, proj).map {
					case Some((key, value)) => Map(key -> value)
					case _ => Map.empty
				}
			}
		}
	}

	/**
	 * Check if project is associated with component if it's a BSP rack or sample plate.
	 * If there is no association found then an error is returned.
	 * @param id id of component
	 * @param project id of project
	 * @return map of errors (form field -> error); empty map if project is associated with component
	 */
	private def checkProject(id: String, project: String) : Future[Option[(Option[String], String)]] = {

		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		def findEntry[T](getList: (String) => (List[SSFIssueList[T]], Option[String]),
						 errStr: (String, String) => String) = {
			Future {
				// Component description (type id)
				val componentID = component.toString + " " + id
				// Key to project in form
				val projectFormKey = formKey + "." + projectKey
				// Go get contents for issue
				val (entries, err) = getList(id)
				if (entries.isEmpty) {
					// Nothing returned - set global error (if DB error) or project specific error if no projects found
					if (err.isDefined) {
						Some(None -> ("Error looking for " + component.toString + " in projects database: " + err.get))
					} else {
						Some(Some(projectFormKey) -> errStr(componentID, project))
					}
				} else {
					// Entry returned - if it's for our project then we're happy - otherwise return error
					if (entries.exists(_.issue == project))
						None
					else
						Some(Some(projectFormKey) -> (componentID + " is not included in specified project.  " +
							component.toString + " found in project" +
							(if (entries.size != 1) "s " else " ") + entries.map(_.issue).mkString(",") + "."))
				}
			}
		}

		this match {
			case r : Rack if r.initialContent.contains(InitialContents.ContentType.BSPtubes) =>
				findEntry[BSPScan](JiraProject.getBspIssueCollection,
					(componentID, _) => "No projects found for " + componentID)
			case p: Plate if p.initialContent.contains(InitialContents.ContentType.SamplePlate) =>
				findEntry[SampleMapEntry]((_) => JiraProject.getSampleMapCollection(project),
					(_, project) => "No sample map found for " + project)
			case _ => Future.successful(None)
		}
	}
}

object JiraProject {
	/**
	 * Make a SSFList from a file containing scan (set of position/barcodes) of a rack's contents.
	 * @param file rack scan results
	 * @return list with rack scan contents
	 */
	def makeRackScanList(file: String) = SSFList(file, RackScan)

	/**
	 * Make a SSFList from a spreadsheet containing bsp info about a rack's contents.
	 * @param file bsp spreadsheet
	 * @return list with bsp scan contents
	 */
	def makeBspScanList(file: String) = SSFList(file, BSPScan)

	/**
	 * Put bsp scan results into the database.
	 * @param racks list with rack contents
	 * @param project project (SSF ticket) to associate with bsp scan
	 */
	def insertBspIssueCollection(racks: SSFList[BSPScan], project: String) =
		BtllimsBspOpers.insertBSPs(SSFIssueList(project, List.empty, None, racks.list))

	/**
	 * Get the projects associated with a rack, along with the results of BSP scans done of racks.  For each rack
	 * scan a list of the tubes that are part of the rack is included.
	 * @param id component id
	 * @return list of projects found along with associate BSP rack scans (optional error message returned as well)
	 */
	def getBspIssueCollection(id: String) =
		getIssueCollection[BSPScan](() => BtllimsBspOpers.retrieveRack(id).toList)

	/**
	 * Get the projects associated with a plate.
	 * @param id component id
	 * @return list of projects found along with associated Plate information (optional error message returned as well)
	 */
	def getPlateIssueCollection(id: String) =
		getIssueCollection[SamplePlate](() => BtllimsPlateOpers.retrievePlate(id).toList)

	/**
	 * Get the sample map for a project id (Jira ticket #)
	 * @param id project ID
	 * @return list of sample maps along with associated project information (optional error message returned as well)
	 */
	def getSampleMapCollection(id: String) =
		getIssueCollection[SampleMapEntry](() => BtllimsSampleMapOpers.findSSF(id).toList)

	/**
	 * Get a list of entries
	 * @param getList callback to get list
	 * @tparam R type of items in list
	 * @return list found in DB and optional error message
	 */
	private def getIssueCollection[R](getList: () => List[SSFIssueList[R]]): (List[SSFIssueList[R]], Option[String]) =
		try {
			(getList(), None)
		} catch {
			case e: Exception => (List.empty[SSFIssueList[R]], Some(e.getLocalizedMessage))
		}
}
