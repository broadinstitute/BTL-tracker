package controllers

import models.{Container, Rack, Component, Transfer}
import models.Transfer.Quad._

import models.initialContents.InitialContents.ContentType.ContentType
import org.broadinstitute.LIMStales.sampleRacks.{BSPTube, RackScan}
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
				Future.fold(futures = fromSet.map((f) =>
					makeTransferList(f, historyToList)))(zero = toList)(op = _ ++ _)
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

	/**
	 * Contents of a component.
 	 * @param component Component object (e.g., plate, rack, tube)
	 * @param all Contents common across the entire plate
	 * @param byQuad Contents specific to quadrants of plate
	 */
	case class ComponentContents(component: Component, all: Contents, byQuad: Map[Quad, Contents])

	/**
	 * Contents of a component - bsp samples and molecular ids.
	 * @param bsp map, by well, of samples
	 * @param mid molecular ID set being used
	 */
	case class Contents(bsp: Option[RackScan#MatchByPos[BSPTube]], mid: Option[ContentType])

	/**
	 * Get bsp sample information - if a rack we go look up if there's bsp information associated with the component.
 	 * @param c Component to find bsp sample information for
	 * @return optional match, by well, of bsp sample information found
	 */
	def getBspContents(c: Component) =
		c match {
			case rack: Rack => RackController.getBSPmatch(rack.id,(matches, _) => Some(matches),(err) => None)
			case _ => None
		}

	/**
	 * Get initial contents of container.
 	 * @param c Component to get initial contents
	 * @return optional initial contents found
	 */
	def getInitialContent(c: Component) =
		c match {
			case container: Container => container.initialContent
			case _ => None
		}

	/**
	 * Set initial contents, before transfers, of a component.
	 * @param c Component
	 * @return initial contents found
	 */
	def beginningContents(c: Component) =
		ComponentContents(c,Contents(getBspContents(c),getInitialContent(c)),Map.empty)

	// Need this to have label on edge be converted to a TransferEdge - making an implicit to be used for edge
	import scalax.collection.edge.LBase._
	object TransferLabel extends LEdgeImplicits[TransferEdge]
	import TransferLabel._

	/**
	 * Get the ultimate contents of a component.  The ultimate includes both what's initially in the component as well
	 * as everything transferred into it.
	 * @param componentID ID for component we want to know the contents of
	 * @return future to get contents of component
	 */
	def getContents(componentID: String) = {
		// First make a graph of the transfers leading into the component.  That gives us a reasonable way to rummage
		// through all the transfers that have been done.  We map that graph into our component's contents
		makeGraph(componentID).map((graph) => {
			/**
			 * Merge together the contents of two components.
			 * @param in "from" component of transfer (input to other component)
			 * @param out "to" component of transfer (output of transfer)
			 * @param transfer transfer that took place between components
			 * @return output component contents now including materials transferred from input
			 */
			def mergeContents(in: ComponentContents, out: ComponentContents, transfer: TransferEdge) = {
				// @TODO Work out quadrants
				val inAll = in.all
				val outAll = out.all
				ComponentContents(out.component,
					Contents(inAll.bsp.fold(outAll.bsp)(Some(_)), inAll.mid.fold(outAll.mid)(Some(_))), Map.empty)
			}

			/**
			 * Find the contents for a component that is a node in a graph recording the transfers that lead
			 * into the component.
 			 * @param output component node for which we want, taking into account transfers, the contents
			 * @return contents of component, including both initial contents and what was transferred in
			 */
			def findContents(output: graph.NodeT) : ComponentContents = {
				// Find all components directly transferred in
				val inputs = output.incoming
				// Fold all the incoming components into our contents
				inputs.foldLeft(beginningContents(output))((soFar, next) => {
					val edge = next.edge
					edge match {
						case LkDiEdge(input, _, label) =>
							// We recurse to keep looking for previous transfers
							// Note: IDE thinks input isn't a graph.NodeT but compiler is perfectly happy
							val inputContents = findContents(input)
							mergeContents(inputContents, soFar, label)
						case _ => soFar
					}
				})
			}

			// Find our component in the graph and then find its contents
			graph.nodes.find((p) => p.id == componentID) match {
				case None => None
				case Some(node) => Some(findContents(node))
			}
		})
	}
}
