package models.project

import models.Component
import models.Component._
import org.broadinstitute.LIMStales.mongo.{BtllimsPlatesCollection,BtllimsBSPsCollection,BtllimsRacksCollection}
import org.broadinstitute.LIMStales.sampleRacks.{SamplePlate,SSFIssueList,BSPScan,RackScan}
import play.api.mvc.{AnyContent,Request}

import scala.concurrent.Future

/**
 * @author Nathaniel Novod
 *         Date: 2/12/15
 *         Time: 4:27 PM
 */
trait JiraProject {
	// Must be associated with a component
	this: Component =>
	/**
	 * Check if what's set in request is valid - specifically we check if the project set contains the rack or plate.
	 * @param request HTTP request (has hidden field with project set before update)
	 * @return Future of map of fields to errors - empty if no errors found
	 */
	protected def isProjectValid(request: Request[AnyContent]) : Future[Map[Option[String], String]] = {
		// If no project set or project hasn't changed then just return saying all is fine
		if (project.isEmpty || getHiddenField(request,_.project) == project)
			Future.successful(Map.empty)
		else {
			import play.api.libs.concurrent.Execution.Implicits.defaultContext
			Future {
				val componentID = component.toString + " " + id
				// Go get contents for issue
				val (entries, err) = component match {
					case ComponentType.Rack => JiraProject.getBSPRackIssueCollection(id)
					case ComponentType.Plate => JiraProject.getPlateIssueCollection(id)
					case _ => (List.empty[SSFIssueList[_]], None)
				}
				if (entries.isEmpty) {
					// Nothing returned - set global error (from DB or simply not found)
					if (err.isDefined) {
						Map(None -> ("Error looking for " + component.toString + " in projects database: " + err.get))
					} else {
						Map(Some(formKey + "." + projectKey) -> ("No projects found for " + componentID))
					}
				} else {
					// Entry returned - if it's for our project then we're happy - otherwise return error
					if (entries.exists(_.issue == project.get))
						Map.empty
					else
						Map(Some(formKey + "." + projectKey) ->
							(componentID + " is not included in specified project.  " +
								component.toString + " found in project" +
								(if (entries.size != 1) "s " else " ") + entries.map(_.issue).mkString(",") + "."))
				}
			}
		}
	}
}

object JiraProject {
	/**
	 * Get the projects associated with a rack, along with the results of scans done of the racks.  For each rack scan
	 * a list of the tubes that are part of the rack is included.
 	 * @param id rack id
	 * @return lists of projects found along with associated rack scans (optional error message returned as well)
	 */
	def getRackIssueCollection(id: String) =
		getIssueCollection[RackScan](() => BtllimsRacksCollection.retrieveOneRack(id).toList)

	/**
	 * Get the projects associated with a rack, along with the results of BSP scans done of racks.  For each rack
	 * scan a list of the tubes that are part of the rack is included.
	 * @param id component id
	 * @return list of projects found along with associate BSP rack scans (optional error message returned as well)
	 */
	def getBSPRackIssueCollection(id: String) =
		getIssueCollection[BSPScan](() => BtllimsBSPsCollection.retrieveRack(id).toList)

	/**
	 * Get the projects associated with a plate.
	 * @param id component id
	 * @return list of projects found along with associated Plate information (optional error message returned as well)
	 */
	def getPlateIssueCollection(id: String) =
		getIssueCollection[SamplePlate](() => BtllimsPlatesCollection.retrievePlate(id).toList)

	/**
	 * Get a list of entries from the DB
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
