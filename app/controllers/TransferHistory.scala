package controllers

import models._
import models.Transfer.Quad._
import models.initialContents.InitialContents.ContentType
import play.api.libs.json.JsObject
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.BSONFormats._

import scala.concurrent.Future
import scalax.collection.edge.LkDiEdge
import scalax.collection.Graph
import scalax.collection.edge.Implicits._


/**
 * @author Nathaniel Novod
 *         Date: 3/2/15
 *         Time: 12:02 PM
 */
object TransferHistory extends Controller with MongoController {

	/**
	 * Future to get list of component that are source/destination of transfers to/from a specified component
 	 * @param id component id
	 * @param directionKey transfer key to indicate to or from
	 * @return list of components that were target or source of transfers to specified component
	 */
	private def getTransferIDs(id: String, directionKey: String) = {
		val cursor = TransferController.transferCollection.find(BSONDocument(directionKey -> id)).cursor[BSONDocument]
		cursor.collect[List]()
	}

	/**
	 * Future to get list of components that were directly transferred into a component id
	 * @param id component ID
	 * @return list of components that were directly transferred to the input component id
	 */
	private def getSourceIDs(id: String) = getTransferIDs(id, Transfer.toKey)

	/**
	 * Future to get list of components that were directly transferred from a component id
	 * @param id component ID
	 * @return list of components that were directly transferred to from the input component id
	 */
	private def getTargetIDs(id: String) = getTransferIDs(id, Transfer.fromKey)

	/**
	 * Get source of transfer
	 * @param transfer transfer to look at
	 * @return source of transfer
	 */
	private def getSourceTransferID(transfer: Transfer) = transfer.from

	/**
	 * Get target of transfer
	 * @param transfer transfer to look at
	 * @return target of transfer
	 */
	private def getTargetTransferID(transfer: Transfer) = transfer.to

	/**
	 * Partial function to make a graph of sources
	 */
	private val sourceGraphParams = makeDirectionalGraph(_: String, getSourceIDs, getSourceTransferID)

	/**
	 * Partial function to make a graph of targets
	 */
	private val targetGraphParams = makeDirectionalGraph(_: String, getTargetIDs, getTargetTransferID)

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
	 * Future to get two lists: one of components directly transferred to or from a component and one for the associated
	 * transfers that took place.
	 * @param componentID we want to know what was transferred to or from this id
	 * @param getIDs callback to get sources or targets of tranfers to/from specified component
	 * @return list of component objects that are sources or targets for the specified component and list of transfers
	 */
	private def getHistory(componentID: String, getIDs: (String) => Future[List[BSONDocument]]) = {
		// First get list of components as BSON documents (note flatmap to avoid future of future)
		val previousComponents = getIDs(componentID).flatMap(getComponents)
		// Now convert BSON returned to component objects (first map for future, next to convert bson list)
		previousComponents.map {
			case ((components, transfers)) =>
				History(components.map(getComponentObject), transfers.map(getTransferObject))
		}
	}

	/**
	 * Nasty recursive fellow to keep going back through transfers, starting at a particular component, to get
	 * all the transfers that led to or from the component in question.
	 * @param componentID component to find all transfers to or from
	 * @param getIDs callback to get sources or targets of tranfers to/from specified component
	 * @param getTransferID callback to get id from transfer
	 * @param historyToList callback, given history found, that converts that history into a list of wanted type
	 * @tparam T type of list to be returned
	 * @return future to calculate wanted list from transfers to or from (direct or indirect) specified component
	 */
	private def makeTransferList[T](componentID: String, getIDs: (String) => Future[List[BSONDocument]],
									getTransferID: (Transfer) => String,
									historyToList: (History) => List[T]): Future[List[T]] = {
		getHistory(componentID, getIDs).flatMap((history) =>
			if (history.transfers.isEmpty) Future.successful(List.empty[T])
			else {
				val historyList = historyToList(history)
				val fromSet = history.transfers.map(getTransferID).toSet
				// Go recurse to work on components leading into this one, folding the new component transfers
				// into what we've found so far
				Future.fold(futures = fromSet.map((from) =>
					makeTransferList(from, getIDs, getTransferID, historyToList)))(zero = historyList)(op = _ ++ _)
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
		TransferHistory.makeSourceGraph(data.from).map {
			(graph) => isGraphAdditionCyclic(data.to, graph)
		}
	}

	/**
	 * Will adding this node to the graph make it cyclic?
	 * @param addition id of node to be added
	 * @param graph graph that the node will be added to
	 * @return true if node is already in graph and graph will thus become cyclic if node is added
	 */
	def isGraphAdditionCyclic(addition: String, graph: Graph[Component, LkDiEdge]) =
		graph.nodes.exists((n) => n.id == addition)

	/**
	 * Get list of all projects referenced in a graph
 	 * @param graph graph to be searched
	 * @return components that have a project set
	 */
	def getGraphProjects(graph: Graph[Component, LkDiEdge]) =
		graph.nodes.filter((n) => n.project.isDefined).map(_.value.asInstanceOf[Component].project.get)

	/**
	 * Make a graph from the transfers (direct or indirect) to or from a component.
	 * @param componentID id of target component
	 * @param getIDs callback to get sources or targets of tranfers to/from specified component
	 * @param getTransferID callback to get id from transfer
	 * @return future with a graph of transfers
	 */
	private def makeDirectionalGraph(componentID: String, getIDs: (String) => Future[List[BSONDocument]],
									 getTransferID: (Transfer) => String) = {
		val edges = makeTransferList(componentID, getIDs, getTransferID,
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

	/**
	 * Make a graph from the transfers (direct or indirect) to a component.
	 * @param componentID id of target component
	 * @return future with a graph of transfers
	 */
	def makeSourceGraph(componentID: String) = sourceGraphParams(componentID)

	/**
	 * Make a graph from the transfers (direct or indirect) from a component.
	 * @param componentID id of source component
	 * @return future with a graph of transfers
	 */
	def makeTargetGraph(componentID: String) = targetGraphParams(componentID)

	/**
	 * Make a graph of the transfers, direct or indirect, that are sources and targets of a component.
	 * @param componentID id of component
	 * @return future with a graph of transfers
	 */
	def makeBidirectionalGraph(componentID: String) = {
		val source = makeSourceGraph(componentID)
		val target = makeTargetGraph(componentID)
		source.flatMap((sourceGraph) => target.map((targetGraph) => sourceGraph ++ targetGraph))
	}

	// Some imports needed for making dot output
	import scalax.collection.io.dot._
	import scalax.collection.edge.LkDiEdge

	/**
	 * Get the component for a node in the graph - just some ugly casting
	 * @param node graph node
	 * @return Component node points to
	 */
	private def getNodeComponent(node: Graph[Component,LkDiEdge]#NodeT) = node.value.asInstanceOf[Component]

	/**
	 * Make a dot format representation of a graph (can be components sources, targets or both)
	 * @param componentID id of final destination or target of graph
	 * @param makeGraph callback to make wanted graph for component
	 * @return dot output for graph
	 */
	private def makeDot(componentID: String, makeGraph: (String) => Future[Graph[Component, LkDiEdge]]) = {
		makeGraph(componentID).map((graph) => {
			// Make root of dot graph
			val root = DotRootGraph(directed = true, id = Some(Id(componentID)))

			// Get representation for node (Component) in Graph
			def getNodeId(node: Graph[Component,LkDiEdge]#NodeT) = {
				val component = getNodeComponent(node)
				component match {
					// If there's initial content include it in identifier
					case c: Container if c.initialContent.isDefined && c.initialContent.get != ContentType.NoContents =>
						component.id + s" (${c.initialContent.get.toString})"
					// Otherwise, if no initial contents but a project, include project
					case _ if component.project.isDefined => component.id + s" (${component.project.get.toString})"
					// Otherwise just identify the node by its id
					case _ => component.id
				}
			}

			// Handler to display edge
			def edgeHandler(innerEdge: Graph[Component,LkDiEdge]#EdgeT) =
				innerEdge.edge match {
					case LkDiEdge(source, target, edgeLabel) =>
						// Make the edge format
						def makeEdgeStmt(label: String) = DotEdgeStmt(NodeId(getNodeId(source)),
							NodeId(getNodeId(target)), List(DotAttr(Id("label"), Id(label))))
						// Make edge label: If a quad transfer then quad we're going to or from, otherwise nothing
						edgeLabel match {
							case TransferEdge(Some(fromQ), _, _) => Some(root, makeEdgeStmt(s"from ${fromQ.toString}"))
							case TransferEdge(_, Some(toQ), _) => Some(root, makeEdgeStmt(s"to ${toQ.toString}"))
							case _ =>  Some(root, DotEdgeStmt(NodeId(getNodeId(source)), NodeId(getNodeId(target))))
						}
				}

			// Get empty Dot graph for component
			def emptyGraph(id: String) = s"digraph $id {\n$id;\n}"

			// Go get the Dot output (note IDE gives error on toDot reference but it compiles without any problem)
			if (graph.isEmpty) emptyGraph(componentID)
			else graph.toDot(dotRoot = root, edgeTransformer = edgeHandler)
		})
	}

	/**
	 * Make a dot format representation of a graph of components that are sources (directly or indirectly) for the
	 * specified component.
	 * @param componentID id of target component
	 * @return dot output for graph
	 */
	def makeSourceDot(componentID: String) = makeDot(componentID, makeSourceGraph)

	/**
	 * Make a dot form representation of a graph of components that are targets (directly or indirectly) of the
	 * specified component.
	 * @param componentID id of source component
	 * @return dot output for graph
	 */
	def makeTargetDot(componentID: String) = makeDot(componentID, makeTargetGraph)

	/**
	 * Make a dot form representation of a graph of components that are sources or targets (directly or indirectly) of
	 * the specified component.
	 * @param componentID id of source component
	 * @return dot output for graph
	 */
	def makeBidirectionalDot(componentID: String) = makeDot(componentID, makeBidirectionalGraph)

	/**
	 * Get set of components in graph leading into and out of specified component.
	 * @param componentID component ID get set around
	 * @return components that lead, directly or indirectly, into or out of specified component
	 */
	def getAssociatedNodes(componentID: String) =
		makeBidirectionalGraph(componentID).map(_.nodes.map(getNodeComponent))
}
