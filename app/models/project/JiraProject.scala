package models.project

import models.{ComponentList, Component}
import models.Component._
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
	 * @param hiddenProject Hidden field (normally from HTTP request) with project set before update
	 * @return Future of map of form fields to errors - empty if no errors found
	 */
	protected def isProjectValid(hiddenProject: Option[String]) : Future[Map[Option[String], String]] = {
		// If no project set or project hasn't changed then just return saying all is fine
		if (project.isEmpty || hiddenProject == project)
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
	 * Check if project is associated with component.  If there is no association found then an error is returned.
	 * @param id id of component
	 * @param project id of project
	 * @return map of errors (form field -> error); empty map if project is associated with component
	 */
	private def checkProject(id: String, project: String) : Future[Option[(Option[String], String)]] = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		Future {
			// Component description (type id)
			val componentID = component.toString + " " + id
			// Key to project in form
			val projectFormKey = formKey + "." + projectKey
			// Go get contents for issue
			val (entries, err) = component match {
				case ComponentType.Rack => JiraProject.getBspIssueCollection(id)
				case ComponentType.Plate => JiraProject.getPlateIssueCollection(id)
				case _ => (List.empty[SSFIssueList[_]], None)
			}
			if (entries.isEmpty) {
				// Nothing returned - set global error (if DB error) or project specific error if no projects found
				if (err.isDefined) {
					Some(None -> ("Error looking for " + component.toString + " in projects database: " + err.get))
				} else {
					Some(Some(projectFormKey) -> ("No projects found for " + componentID))
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
}

object JiraProject {
	/**
	 * Make a SSFList from a file containing scan (set of position/barcodes) of a rack's contents.
	 * @param file rack scan results
	 * @return list with rack scan contents
	 */
	def makeRackScanList(file: String) = SSFList(file, RackScan)

	//@TODO GET RID OF insertRackIssueCollection and getRackIssueCollection - use RackScan instead
	// Then copy over (without project) the rack scans in the Jira DB
	/**
	 * Put rack scan results into the database.
	 * @param racks list with rack scan contents
	 * @param project project (SSF ticket) to associate with rack scan
	 */
	def insertRackIssueCollection(racks: SSFList[RackScan], project: String) =
		BtllimsRackOpers.insertRacks(SSFIssueList(project, List.empty, None, racks.list))

	/**
	 * Get the projects associated with a rack, along with the results of scans done of the racks.  For each rack scan
	 * a list of the tubes that are part of the rack is included.
 	 * @param id rack id
	 * @return lists of projects found along with associated rack scans (optional error message returned as well)
	 */
	def getRackIssueCollection(id: String) =
		getIssueCollection[RackScan](() => BtllimsRackOpers.retrieveOneRack(id).toList)

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
