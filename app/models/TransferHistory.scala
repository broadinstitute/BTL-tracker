package models

/**
 * Model for creating and manipulating transfer history graphs.
 * Created by nnovod on 4/23/15.
 */

import models.Component.ComponentType
import models.Transfer.Slice.Slice
import models.Transfer.Quad._
import models.db.{TrackerCollection, TransferCollection}
import play.api.libs.json.JsObject
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats
import play.mvc.Call
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

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
	  * Get source of transfer
	  *
	  * @param transfer transfer to look at
	  * @return source of transfer
	  */
	private def getSourceTransferID(transfer: Transfer) = transfer.from

	/**
	  * Get target of transfer
	  *
	  * @param transfer transfer to look at
	  * @return target of transfer
	  */
	private def getTargetTransferID(transfer: Transfer) = transfer.to

	/**
	  * Partial function to make a graph of sources
	  */
	private val sourceGraphParams = makeDirectionalGraph(_: String,
		getIDs = TransferCollection.getSourceIDs, getTransferID = getSourceTransferID)

	/**
	  * Partial function to make a graph of targets
	  */
	private val targetGraphParams = makeDirectionalGraph(_: String,
		getIDs = TransferCollection.getTargetIDs, getTransferID = getTargetTransferID)

	/**
	  * Future to retrieve list of source components, given list of transfers
	  *
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
		TrackerCollection.findIds(idList).map((_, transfers))
	}

	/**
	  * Get component object from bson.
	  *
	  * @param bson input bson
	  * @return component object
	  */
	private def getComponentObject(bson: BSONDocument) = {
		// Get json since model conversions are setup to do json reads/writes
		val json = BSONFormats.BSONDocumentFormat.writes(bson).as[JsObject]
		// Do conversion to model component object
		ComponentFromJson.getComponent(json)
	}

	/**
	  * Get transfer object from bson.
	  *
	  * @param bson input bson
	  * @return transfer object
	  */
	def getTransferObject(bson: BSONDocument) = {
		// Get the time of the transfer
		val time = bson.getAs[BSONObjectID]("_id") match {
			case Some(id) => id.time
			case _ => 0
		}
		// Get json since Transfer conversions are setup to do json reads/writes
		val json = BSONFormats.BSONDocumentFormat.writes(bson).as[JsObject]
		import Transfer.transferFormat
		val transfer = json.as[Transfer]
		// Add time to transfer info
		TransferWithTime(transfer = transfer, time = time)
	}

	/**
	  * Transfer history found.
	  *
	  * @param components components that were transferred from
	  * @param transfers transfers that took place from components
	  */
	private case class History(components: List[Component], transfers: List[TransferWithTime])

	/**
	  * Future to get two lists: one of components directly transferred to or from a component and one for the associated
	  * transfers that took place.
	  *
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
				History(components = components.map(getComponentObject), transfers = transfers.map(getTransferObject))
		}
	}

	/**
	  * Nasty recursive fellow to keep going back through transfers, starting at a particular component, to get
	  * all the transfers that led to or from the component in question.
	  *
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
					makeTransferList(componentID = from, getIDs = getIDs, getTransferID = getTransferID,
						historyToList = historyToList)))(zero = historyList)(op = _ ++ _)
			}
		)
	}

	/**
	  * An edge for a transfer graph
	  *
	  * @param fromQuad optional source quad of component transfer is coming from
	  * @param toQuad optional destination quad of component transfer is going to
	  * @param cherries optional list of indicies to cherry picked wells
	  * @param isTubeToMany transfer is from one well to many (e.g., tube to plate)
	  * @param time when transfer was done
	  */
	case class TransferEdge(fromQuad: Option[Quad], toQuad: Option[Quad], slice: Option[Slice],
							cherries: Option[List[Int]], isTubeToMany: Boolean, time: Long)

	/**
	  * Would the addition of this transfer lead to a cyclic graph?  Get what leads into the "from" part of the transfer
	  * and see if it contains the "to" part of the transfer.
	  *
	  * @param data transfer to be added
	  * @return future true if graph would become cyclic with addition of the transfer
	  */
	def isAdditionCyclic(data: Transfer) = {
		TransferHistory.makeSourceGraph(data.from).map {
			(graph) => isGraphAdditionCyclic(addition = data.to, graph = graph)
		}
	}

	/**
	  * Will adding this node to the graph make it cyclic?
	  *
	  * @param addition id of node to be added
	  * @param graph graph that the node will be added to
	  * @return true if node is already in graph and graph will thus become cyclic if node is added
	  */
	def isGraphAdditionCyclic(addition: String, graph: Graph[Component, LkDiEdge]) =
		graph.nodes.exists((n) => n.id == addition)

	/**
	  * Get list of all projects referenced in a graph
	  *
	  * @param graph graph to be searched
	  * @return components that have a project set
	  */
	def getGraphProjects(graph: Graph[Component, LkDiEdge]) =
		graph.nodes.filter((n) => n.project.isDefined).map(_.value.project.get)

	/**
	  * Make a graph from the transfers (direct or indirect) to or from a component.  If there are no transfers to/from
	  * component then the graph returned has just the component itself and no edges.
	  *
	  * @param componentID id of target component
	  * @param getIDs callback to get sources or targets of tranfers to/from specified component
	  * @param getTransferID callback to get id from transfer
	  * @return future with a graph of transfers
	  */
	private def makeDirectionalGraph(componentID: String, getIDs: (String) => Future[List[BSONDocument]],
									 getTransferID: (Transfer) => String) = {
		val edges = makeTransferList(componentID = componentID, getIDs = getIDs, getTransferID = getTransferID,
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
					(findComponent(t.from) ~+#>
						findComponent(t.to))(TransferEdge(fromQuad = t.fromQuad, toQuad = t.toQuad,
						slice = t.slice, cherries = t.cherries,
						isTubeToMany = t.isTubeToMany, time = t.time)))
			}
		)
		// When future with list of edges returns make it into a graph
		edges.flatMap((e) => {
			// If empty then make graph with just node of component (or nothing if ID can't be found)
			if (e.isEmpty) {
				TrackerCollection.findID[JsObject](componentID).map{
					case Some(json) => Graph[Component, LkDiEdge](ComponentFromJson.getComponent(json))
					case None => Graph[Component, LkDiEdge]()
				}
			} else {
				Future.successful(Graph(e: _*))
			}
		})
	}

	/**
	  * Make a graph from the transfers (direct or indirect) to a component.
	  *
	  * @param componentID id of target component
	  * @return future with a graph of transfers
	  */
	def makeSourceGraph(componentID: String) = sourceGraphParams(componentID)

	/**
	  * Make a graph from the transfers (direct or indirect) from a component.
	  *
	  * @param componentID id of source component
	  * @return future with a graph of transfers
	  */
	def makeTargetGraph(componentID: String) = targetGraphParams(componentID)

	/**
	  * Make a graph of the transfers, direct or indirect, that are sources and targets of a component.
	  *
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
	  *
	  * @param node graph node
	  * @return Component node points to
	  */
	private def getNodeComponent(node: Graph[Component,LkDiEdge]#NodeT) = node.value

	/**
	  * Make a dot format representation of a graph (can be components sources, targets or both)
	  *
	  * @param componentID id of final destination or target of graph
	  * @param accessPath callback to get call to link to find for component
	  * @param transferDisplay callback to get call to display transfer
	  * @param makeGraph callback to make wanted graph for component
	  * @return dot output for graph
	  */
	private def makeDot(componentID: String, accessPath: (String) => Call, transferDisplay: (String, String) => Call,
						makeGraph: (String) => Future[Graph[Component, LkDiEdge]]) : Future[String] = {
		makeGraph(componentID).map((graph) => {
			// Set root information for dot graph: graph is directed and graph ID is based on component ID
			val root = DotRootGraph(directed = true, id = Some(Id(componentID)))

			/*
			 * Setup node from graph for dot rendition.  Node id is simply component ID and label has additional
			 * description
			 * @param innerNode node to transform
			 * @return node representation.
			 */
			def nodeHandler(innerNode: Graph[Component, LkDiEdge]#NodeT): Option[(DotGraph, DotNodeStmt)] = {
				val attrs = Seq(DotAttr(Id("label"), Id(getNodeLabel(innerNode))),
					DotAttr(Id("href"), Id(getLabelURL(innerNode))),
					DotAttr(Id("shape"), Id(getShape(innerNode))))
				val attrsFinal = getStyle(innerNode) match {
					case Some(styleStr) => attrs :+ DotAttr(Id("style"), Id(styleStr))
					case None => attrs
				}
				Some((root, DotNodeStmt(NodeId(getNodeId(innerNode)), attrsFinal)))
			}

			/*
			 * Setup node label - the label is the component ID with additional description.
			 * @param node to make label for
			 * @return node label.
			 */
			def getNodeLabel(node: Graph[Component, LkDiEdge]#NodeT) = {
				val component = getNodeComponent(node)
				val id = component.id
				// Get id and description up to 23 characters - if longer truncate it and add ...
				val intro = component.description match {
					case Some(d) if d.trim.length > 23 => " " + d.trim.substring(0, 20) + "..."
					case Some(d) if d.trim.length > 0 => " " + d.trim
					case _ => ""
				}
				// Get initial contents and project
				val ic = component match {
					case c: Container if c.initialContent.isDefined =>
						Some(c.initialContent.get.toString)
					case _ => None
				}
				val ip = (ic, component.project) match {
					case (Some(i), Some(p)) => s" ($p $i)"
					case (Some(i), None) => s" ($i)"
					case (None, Some(p)) => s" ($p)"
					case _ => ""
				}
				// Put it all together for the resultant string
				s"$id$intro$ip"
			}


			// Get URL for a node
			def getLabelURL(node: Graph[Component, LkDiEdge]#NodeT) =
				accessPath(getNodeComponent(node).id).url()

			// Get URL for a transfer
			def getTransferURL(source: Graph[Component, LkDiEdge]#NodeT, target: Graph[Component, LkDiEdge]#NodeT) =
				transferDisplay(getNodeId(source), getNodeId(target)).url()

			// Get representation for node (Component) in Graph
			def getNodeId(node: Graph[Component,LkDiEdge]#NodeT) = {
				val component = getNodeComponent(node)
				component.id
			}

			// Get shape for node (Component) in Graph
			def getShape(node: Graph[Component,LkDiEdge]#NodeT) = {
				val component = getNodeComponent(node)
				component.component match {
					case ComponentType.Rack => "box3d"
					case ComponentType.Tube => "box" // rounded box looked better vs. "ellipse"
					case _ => "box"
				}
			}

			// Get style for node (Component) in Graph
			def getStyle(node: Graph[Component,LkDiEdge]#NodeT) = {
				val component = getNodeComponent(node)
				component.component match {
					case ComponentType.Tube => Some("rounded")
					case _ => None
				}
			}

			// Handler to display edge
			def edgeHandler(innerEdge: Graph[Component,LkDiEdge]#EdgeT) =
				innerEdge.edge match {
					case LkDiEdge(source, target, edgeLabel) =>
						// Make the edge format
						def makeEdgeStmt(label: String) = DotEdgeStmt(node_1Id = NodeId(getNodeId(source)),
							node_2Id = NodeId(getNodeId(target)), attrList = List(DotAttr(Id("label"), Id(label)),
								DotAttr(Id("href"), Id(getTransferURL(source, target)))))
						def makeQuadSliceStmt(slice: Option[Slice]) =
							slice.map((s) => s" (${s.toString})").getOrElse("")
						// Make edge label: If a quad/slice transfer then quad/slice we're going to or from
						edgeLabel match {
							case TransferEdge(Some(fromQ), Some(toQ), qSlice, _, _, _) if fromQ != toQ =>
								Some(root,
									makeEdgeStmt(s"from ${fromQ.toString} to ${toQ.toString}${makeQuadSliceStmt(qSlice)}"))
							case TransferEdge(Some(fromQ), _, qSlice, _, _, _) =>
								Some(root, makeEdgeStmt(s"from ${fromQ.toString}${makeQuadSliceStmt(qSlice)}"))
							case TransferEdge(_, Some(toQ), qSlice, _, _, _) =>
								Some(root, makeEdgeStmt(s"to ${toQ.toString}${makeQuadSliceStmt(qSlice)}"))
							case TransferEdge(_, _, Some(slice), _, _, _) =>
								Some(root, makeEdgeStmt(slice.toString))
							case _ =>
								Some(root, DotEdgeStmt(node_1Id = NodeId(getNodeId(source)),
									node_2Id = NodeId(getNodeId(target))))
						}
				}

			// Go get the Dot output
			if (graph.edges.isEmpty) {
				// If no edges then get id for component's graph node, that should only be one there, and make a dot
				// representation with just that node
				val (id, label, href, shape, style) = graph.nodes.lastOption match {
					case Some(last) => (getNodeId(last), getNodeLabel(last), getLabelURL(last),
						getShape(last), getStyle(last))
					case None => (componentID, componentID, accessPath(componentID).url(), "box", None)
				}
				val idStr = "\"" + id + "\""
				val labelStr = "label = \"" + label + "\""
				val hrefStr = "href = \"" + href + "\""
				val shapeStr = "shape = \"" + shape + "\""
				val styleStr = style.map(", style = \"" + _ + "\"").getOrElse("")
				s"digraph $idStr {\n$idStr [$labelStr, $hrefStr, $shapeStr$styleStr]\n}"
			} else {
				// (note IDE gives error on toDot reference but it compiles without any problem)
				// import scalax.collection.io.dot._
				graph.toDot(dotRoot = root, edgeTransformer = edgeHandler, cNodeTransformer = Some(nodeHandler))
			}
		})
	}

	/**
	  * Make a dot format representation of a graph of components that are sources (directly or indirectly) for the
	  * specified component.
	  *
	  * @param componentID id of target component
	  * @param accessPath callback to get call to link to find for component
	  * @param transferDisplay callback to get call to display transfer
	  * @return dot output for graph
	  */
	def makeSourceDot(componentID: String, accessPath: (String) => Call, transferDisplay: (String, String) => Call) =
		makeDot(componentID = componentID, accessPath = accessPath, transferDisplay = transferDisplay,
			makeGraph = makeSourceGraph)

	/**
	  * Make a dot form representation of a graph of components that are targets (directly or indirectly) of the
	  * specified component.
	  *
	  * @param componentID id of source component
	  * @param accessPath callback to get call to link to find for component
	  * @param transferDisplay callback to get call to display transfer
	  * @return dot output for graph
	  */
	def makeTargetDot(componentID: String, accessPath: (String) => Call, transferDisplay: (String, String) => Call) =
		makeDot(componentID = componentID, accessPath = accessPath, transferDisplay = transferDisplay,
			makeGraph = makeTargetGraph)

	/**
	  * Make a dot form representation of a graph of components that are sources or targets (directly or indirectly) of
	  * the specified component.
	  *
	  * @param componentID id of source component
	  * @param accessPath callback to get call to link to find for component
	  * @param transferDisplay callback to get call to display transfer
	  * @return dot output for graph
	  */
	def makeBidirectionalDot(componentID: String, accessPath: (String) => Call,
							 transferDisplay: (String, String) => Call) =
		makeDot(componentID = componentID, accessPath = accessPath, transferDisplay = transferDisplay,
			makeGraph = makeBidirectionalGraph)

	/**
	  * Get set of components in graph leading into and out of specified component.
	  *
	  * @param componentID component ID get set around
	  * @return components that lead, directly or indirectly, into or out of specified component
	  */
	def getAssociatedNodes(componentID: String) =
		makeBidirectionalGraph(componentID).map(_.nodes.map(getNodeComponent))
}
