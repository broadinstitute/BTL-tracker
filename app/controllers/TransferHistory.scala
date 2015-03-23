package controllers

import models._
import models.Transfer.Quad._
import play.api.libs.json.JsObject
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.BSONFormats._

import scala.concurrent.Future
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
		// Get the time of the transfer
		val time = bson.getAs[BSONObjectID]("_id") match {
			case Some(id) => id.time
			case _ => 0
		}
		// Get json since model conversions are setup to do json reads/writes
		val json = BSONFormats.BSONDocumentFormat.writes(bson).as[JsObject]
		import Transfer.transferFormat
		val transfer = json.as[Transfer]
		// Add time to transfer info
		TransferWithTime(transfer, time)
	}

	/**
	 * Transfer history found.
	 * @param components components that were transferred from
	 * @param transfers transfers that took place from components
	 */
	private case class History(components: List[Component], transfers: List[TransferWithTime])

	/**
	 * Future to get two lists: one of components directly transferred into a component and one for the associated
	 * transfers that took place.
	 * @param componentID we want to know what was transferred into this id
	 * @return list of component objects that are immediate sources for the specified component and list of transfers
	 */
	private def getHistory(componentID: String) = {
		// First get list of components as BSON documents (note flatmap to avoid future of future)
		val previousComponents = getPreviousIDs(componentID).flatMap(getComponents)
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
				val historyList = historyToList(history)
				val fromSet = history.transfers.map(_.from).toSet
				// Go recurse to work on components leading into this one, folding the new component transfers
				// into what we've found so far
				Future.fold(futures = fromSet.map((from) =>
					makeTransferList(from, historyToList)))(zero = historyList)(op = _ ++ _)
			}
		)
	}

	/**
	 * An edge for a transfer graph
	 * @param fromQuad optional source quad of component transfer is coming from
	 * @param toQuad optional destination quad of component transfer is going to
	 * @param time when transfer was done
	 */
	case class TransferEdge(fromQuad: Option[Quad], toQuad: Option[Quad], time: Long)

	/**
	 * Would the addition of this transfer lead to a cyclic graph?  Get what leads into the "from" part of the transfer
	 * and see if it contains the "to" part of the transfer.
	 * @param data transfer to be added
	 * @return future true if graph would become cyclic with addition of the transfer
	 */
	def isAdditionCyclic(data: Transfer) = {
		TransferHistory.makeGraph(data.from).map {
			(graph) => graph.nodes.filter((n) => n.id == data.to).nonEmpty
		}
	}

	/**
	 * Make a graph from the transfers (direct or indirect) into a component.
	 * @param componentID id of target component
	 * @return future with a graph of transfers
	 */
	def makeGraph(componentID: String) = {
		val edges = makeTransferList(componentID,
			historyToList = (history) => {
				// Find component in history's component list (note this list should be very small)
				def findComponent(id: String) = {
					history.components.find(_.id == id) match {
						case Some(c) => c
						case None => throw new Exception(s"Missing Component: $id")
					}
				}
				// Map all the transfers into graph edges
				history.transfers.map((t) =>
					(findComponent(t.from) ~+#> findComponent(t.to))(TransferEdge(t.fromQuad, t.toQuad, t.time)))
			}
		)
		// When future with list of edges returns make it into a graph
		edges.map((e) => Graph(e: _*))
	}

	import scalax.collection.io.dot._
	import scalax.collection.edge.LkDiEdge
	import scalax.collection.Graph
//	import implicits._
	def makeDot(componentID: String) = {
		makeGraph(componentID).map((graph) => {
			val root = DotRootGraph(directed = true, id = Some(Id(s"Graph for $componentID")))
			def edgeHandler(innerEdge: Graph[Component,LkDiEdge]#EdgeT) =
				innerEdge.edge match {
					case LkDiEdge(source, target, edgeLabel) => {
						def makeEdgeStmt(label: String) = DotEdgeStmt(NodeId(source.asInstanceOf[Component].id),
							NodeId(target.asInstanceOf[Component].id),
							List(DotAttr(Id("label"), Id(label))))
						edgeLabel match {
							case TransferEdge(Some(fromQ), _, _) => Some(root, makeEdgeStmt(fromQ.toString))
							case TransferEdge(_, Some(toQ), _) => Some(root, makeEdgeStmt(toQ.toString))
							case _ =>  Some(root, DotEdgeStmt(NodeId(source.asInstanceOf[Component].id),
								NodeId(target.asInstanceOf[Component].id)))
						}
					}}
			graph.toDot(root, edgeHandler)
		})
	}
}
