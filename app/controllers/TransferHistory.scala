package controllers

import models.Component.ComponentType
import models.ContainerDivisions.Division
import models.ContainerDivisions.Division.Division
import models.Transfer.Quad._
import models._
import models.initialContents.InitialContents.ContentType
import models.initialContents.InitialContents.ContentType.ContentType
import org.broadinstitute.LIMStales.sampleRacks.{BSPScan, SSFIssueList}
import play.api.libs.json.JsObject
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.bson.BSONDocument
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.BSONFormats._

import scala.concurrent.Future
import scalax.collection.edge.LkDiEdge
import scalax.collection.immutable.Graph
import scalax.collection.edge.Implicits._


/**
 * @author Nathaniel Novod
 *         Date: 3/2/15
 *         Time: 12:02 PM
 */
object TransferHistory extends Controller with MongoController {

	/**
	 * Future to get list of components that were directly transferred into a component id
	 * @param id component ID
	 * @return list of components that were directly transferred to the input component id
	 */
	private def getPreviousIDs(id: String) = {
		val cursor = TransferController.transferCollection.find(BSONDocument(Transfer.toKey -> id)).cursor[BSONDocument]
		cursor.collect[List]()
	}

	/**
	 * Future to retrieve list of source components, given list of transfers
	 * @param transfers list of transfers
	 * @return list of components corresponding to sources in transfers and input list of transfers
	 */
	private def getComponents(transfers: List[BSONDocument]) = {
		// flatMap used to get rid of any from ids that come back as None (shouldn't be any)
		val idList = transfers.flatMap((x) =>
			(x.getAs[String](Transfer.fromKey), x.getAs[String](Transfer.toKey)) match {
				case (Some(f), Some(t)) => List(f, t)
				case (Some(f), None) => List(f)
				case (None, Some(t)) => List(t)
				case (None, None) => List.empty
			}
		)
		// Go find all the components that were in the from list
		val cursor = ComponentController.trackerCollection.find(BSONDocument(
			Component.idKey -> BSONDocument("$in" -> idList))).cursor[BSONDocument]
		cursor.collect[List]().map((_, transfers))
	}

	/**
	 * Get component object from bson.
	 * @param bson input bson
	 * @return component object
	 */
	private def getComponentObject(bson: BSONDocument) = {
		// Get json since model conversions are setup to do json reads/writes
		val json = BSONFormats.BSONDocumentFormat.writes(bson).as[JsObject]
		TransferController.getComponent(json)
	}

	/**
	 * Get transfer object from bson.
	 * @param bson input bson
	 * @return transfer object
	 */
	private def getTransferObject(bson: BSONDocument) = {
		// Get json since model conversions are setup to do json reads/writes
		val json = BSONFormats.BSONDocumentFormat.writes(bson).as[JsObject]
		import Transfer.transferFormat
		json.as[Transfer]
	}

	/**
	 * Transfer history found.
	 * @param components components that were transferred from
	 * @param transfers transfers that took place from components
	 */
	private case class History(components: List[Component], transfers: List[Transfer])

	/**
	 * Future to get component objects for the components transferred into a single component as well as transfer
	 * objects that show how transfer took place.
	 * @param componentID we want to know what was transferred into this id
	 * @return list of component objects that are immediate sources for the specified component and list of transfers
	 */
	private def getHistory(componentID: String) = {
		// First get list of components as BSON documents (note flatmap to avoid future of future)
		val previousComponents =
			getPreviousIDs(componentID).flatMap(getComponents)
		// Now convert BSON returned to component objects (first map for future, next to convert bson list)
		previousComponents.map {
			case ((components, transfers)) =>
				History(components.map(getComponentObject), transfers.map(getTransferObject))
		}
	}

	/**
	 * Nasty recursive fellow to keep going back through transfers, starting at a particular component, to get
	 * all the transfers that led to the component in question.
	 * @param componentID component to find all transfers into
	 * @param historyToList callback, given history found, that converts that history into a list of wanted type
	 * @tparam T type of list to be returned
	 * @return future to calculate wanted list from transfers into (direct or indirect) specified component
	 */
	private def makeTransferList[T](componentID: String, historyToList: (History) => List[T]): Future[List[T]] = {
		getHistory(componentID).flatMap((history) =>
			if (history.transfers.isEmpty) Future.successful(List.empty[T])
			else {
				val toList = historyToList(history)
				val fromSet = history.transfers.map(_.from).toSet
				// Go recurse to work on components leading into this one, folding the new component transfers
				// into what we've found so far (latest)
				Future.fold(futures = fromSet.map((f) => makeTransferList(f, historyToList)))(zero = toList)(op = _ ++ _)
			}
		)
	}

	/**
	 * An edge for a transfer graph
	 * @param fromQuad optional source quad of component transfer is coming from
	 * @param toQuad optional destination quad of component transfer is going to
	 */
	case class TransferEdge(fromQuad: Option[Quad], toQuad: Option[Quad])

	/**
	 * Make a graph from the transfers (direct or indirect) into a component.
	 * @param componentID id of target component
	 * @return future with a graph of transfers
	 */
	def makeGraph(componentID: String) = {
		val edges = makeTransferList(componentID, (history) => {
			// Find component in history's component list (note this list should be very small)
			def findComponent(id: String) = {
				history.components.find(_.id == id) match {
					case Some(c) => c
					case None => throw new Exception(s"Missing Component: $id")
				}
			}
			// Map all the transfers into graph edges
			history.transfers.map((t) =>
				(findComponent(t.from) ~+#> findComponent(t.to))(TransferEdge(t.fromQuad, t.toQuad)))
		})
		// When future with list of edges returns make it into a graph
		edges.map((e) => Graph(e: _*))
	}

	case class Contents(bsp: Option[SSFIssueList[BSPScan]], mid: Option[ContentType], quad: Option[Quad])

	// Need this to have label on edge be converted to a TransferLabel
	import scalax.collection.edge.LBase._
	object TransferLabel extends LEdgeImplicits[TransferEdge]
	import TransferLabel._

	def getContents(componentID: String) = {
		makeGraph(componentID).map((graph) => {

			def mergePlateContents(p: Plate, c: Contents, l: Option[TransferEdge]) =
				Contents(c.bsp, p.initialContent, if (l.isDefined) l.get.fromQuad  else None)
			def mergeTubeContents(t: Tube, c: Contents, l: Option[TransferEdge]) =
				Contents(c.bsp, c.mid, c.quad)
			def mergeRackContents(r: Rack, c: Contents, l: Option[TransferEdge]) =
				Contents(Some(SSFIssueList("SSF-1",
					List.empty[String], None, List.empty[BSPScan])), c.mid, c.quad)

			def findContents(c: graph.NodeT,
							 contents: Contents = Contents(None, None, None)) : Contents = {
				val inputs = c.incoming
				inputs.foldLeft(contents)((soFar, next) => {
					val edge = next.edge

					edge match {
						case LkDiEdge(input, _, label) =>
							val belowContents = findContents(input, contents)
							input.component match {
								case ComponentType.Plate =>
									mergePlateContents(input.value.asInstanceOf[Plate], belowContents, Some(label))
								case ComponentType.Tube =>
									mergeTubeContents(input.value.asInstanceOf[Tube], belowContents, Some(label))
								case ComponentType.Rack =>
									mergeRackContents(input.value.asInstanceOf[Rack], belowContents, Some(label))
							}
					}
				})
			}

			graph.nodes.find((p) => p.id == componentID) match {
				case None => Contents(None, None, None)
				// ** Need to take into account this node in contents **
				// ** May need to have list (one per quad) of contents **
				case Some(node) => findContents(node)
			}
		})
	}
}
