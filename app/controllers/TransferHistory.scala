package controllers

import models._
import models.Transfer.Quad._
import models.initialContents.InitialContents
import InitialContents.ContentType._

import models.ContainerDivisions.Division._
import models.initialContents.MolecularBarcodes.{MolBarcodeWell, MolBarcodeContents}
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
	 * Future to get two lists: one of components directly transferred into a component and one for the associated
	 * transfers that took place.
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
	 */
	case class TransferEdge(fromQuad: Option[Quad], toQuad: Option[Quad])

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
					(findComponent(t.from) ~+#> findComponent(t.to))(TransferEdge(t.fromQuad, t.toQuad)))
			}
		)
		// When future with list of edges returns make it into a graph
		edges.map((e) => Graph(e: _*))
	}

	/**
	 * Contents of a component.
 	 * @param component Component object (e.g., plate, rack, tube)
	 * @param contents Contents common across the entire plate
	 */
	case class ComponentContents(component: Component, contents: Contents)

	/**
	 * Contents of a rack
 	 * @param jiraIssue Jira issue
	 * @param jiraComponents Jira components
	 * @param jiraSummary Jira summary
	 * @param contents
	 */
	case class RackContentsInfo(jiraIssue: String, jiraComponents: List[String], jiraSummary: Option[String],
								contents: RackScan#MatchByPos[BSPTube])

	/**
	 * Contents of a component - bsp samples and molecular ids.
	 * @param bsp map, by well, of samples
	 * @param mid molecular ID set being used
	 */
	case class Contents(bsp: Option[RackContentsInfo], mid: Option[InitialContents.ContentsMap[MolBarcodeWell]])

	/**
	 * Get bsp sample information - if a rack we go look up if there's bsp information associated with the component.
 	 * @param c Component to find bsp sample information for
	 * @return optional match, by well, of bsp sample information found
	 */
	private def getBspContents(c: Component) =
		c match {
			case rack: Rack => RackController.getBSPmatch(rack.id,
				found = (matches, ssfIssue) => Some(RackContentsInfo(ssfIssue.issue, ssfIssue.components,
					ssfIssue.summary, matches)),
				notFound = (err) => None)
			case _ => None
		}

	/**
	 * Get initial contents of container.
 	 * @param c Component to get initial contents
	 * @return optional initial contents found
	 */
	private def getInitialContent(c: Component) =
		c match {
			case container: Container =>
				container.initialContent match {
					case Some(ic) if ic != NoContents => Some(InitialContents.contents(ic))
					case _ => None
				}
			case _ => None
		}

	// Need this to have label on edge be converted to a TransferEdge - making an implicit to be used for edge
	import scalax.collection.edge.LBase._
	object TransferLabel extends LEdgeImplicits[TransferEdge]
	import TransferLabel._

	/**
	 * Get the ultimate contents of a component.  The ultimate includes both what's initially in the component as well
	 * as everything transferred into it.  First we make a graph of the transfers that lead into the component and then
	 * we find the contents by travelling up the graph.
	 * @param componentID ID for component we want to know the contents of
	 * @return future to get contents of component
	 */
	def getContents(componentID: String) = {
		// First make a graph of the transfers leading into the component.  That gives us a reasonable way to rummage
		// through all the transfers that have been done.  We map that graph into our component's contents
		makeGraph(componentID).map((graph) => {
			/**
			 * BSP entry for merged results.
			 * @param project jira ticket ID
			 * @param projectDescription jira summary
			 * @param sampleTube bsp tube barcode
			 * @param gssrSample bsp GSSR barcode
			 * @param collabSample bsp collaborator sample ID
			 * @param individual bsp collaborator participant
			 * @param library bsp collaborator library ID
			 */
			case class MergeBsp(project: String,projectDescription: Option[String],sampleTube: String,
								gssrSample: Option[String],collabSample: Option[String],individual: Option[String],
								library: Option[String]) {
				// Override equals and hash to make it simpler
				override def equals(arg: Any) = {
					arg match {
						case MergeBsp(p,_,s,_,_,_,_) => p == project && s == sampleTube
						case _ => false
					}
				}
				override def hashCode = (project.hashCode * 31) + sampleTube.hashCode
			}

			/**
			 * Molecular barcodes for merged results.
			 * @param sequence barcode sequence for EZPASS (e.g., may have "-" between multiple barodes)
			 * @param name name of barcode
			 */
			case class MergeMid(sequence: String, name: String) {
				// Override equals and hash to make them simpler
				override def equals(arg: Any) = {
					arg match {
						case MergeMid(s, _) => s == sequence
						case _ => false
					}
				}
				override def hashCode = sequence.hashCode
			}

			/**
			 * Single sample and associated MIDs.
			 * @param bsp sample information
			 * @param mid MIDs associated with sample
			 */
			case class MergeResult(bsp: Option[MergeBsp], mid: Set[MergeMid])

			/**
			 * Object used to keep track of all contents being merged together.
			 * @param component component these contents have been merged into
			 * @param wells map contents by well location
			 * @param errs list of errors encountered
			 */
			case class MergeTotalContents(component: Component,
										  wells: Map[String, Set[MergeResult]], errs: List[String])

			/**
			 * Get bsp sample information - if a rack we go look up if there's bsp information associated with the component.
			 * @param c Component to find bsp sample information for
			 * @return optional match, by well, of bsp sample information found
			 */
			def getBspContent(c: Component) =
				c match {
					case rack: Rack => RackController.getBSPmatch(rack.id,
						found = (matches, ssfIssue) => {
							// Get results of bsp matching into a map of wells to sample info
							val bspMatches = matches.flatMap {
								case ((well, (matchFound, Some(tube)))) =>
									List(well -> MergeResult(
										Some(MergeBsp(project = ssfIssue.issue, projectDescription = ssfIssue.summary,
											sampleTube = tube.barcode, gssrSample = tube.gssrBarcode,
											collabSample = tube.collaboratorSample,
											individual = tube.collaboratorParticipant, library = tube.sampleID)),
										Set.empty))
								case _ => List.empty
							}
							// Return our sample map and that no errors found
							(bspMatches, List.empty[String])
						},
						// BSP info not found for rack - return empty map and error
						notFound = (err) => (Map.empty[String, MergeResult], List(err)))
					// Not a rack - nothing to return
					case _ => (Map.empty[String, MergeResult], List.empty[String])
				}

			/**
			 * Get initial contents of container.
			 * @param c Component to get initial contents
			 * @return optional initial contents found
			 */
			def getMidContent(c: Component) =
				c match {
					case container: Container =>
						container.initialContent match {
							case Some(ic) if ic != NoContents => {
								val mids = InitialContents.contents(ic).contents.map{
									case (well, mbw) =>
										well -> MergeResult(None, Set(MergeMid(mbw.getSeq, mbw.getName)))
								}
								(mids, List.empty[String])
							}
							case _ => (Map.empty[String, MergeResult], List.empty[String])
						}
					case _ => (Map.empty[String, MergeResult], List.empty[String])
				}

			/**
			 * Get initial contents for a component - contents can include bsp input (for a rack) or MIDs (for a plate)
			 * @param component component
			 * @return results with contents found
			 */
			def getComponentContent(component: Component) = {
				// Make map with single MergeResult into a set of MergeResults
				def mapWithSet(in: Map[String, MergeResult]) = in.map{case (k, v) => k -> Set(v)}

				// Get bsp and mid content and then merge the results together
				val bsps = getBspContent(component)
				val mids = getMidContent(component)
				if (bsps._1.isEmpty) MergeTotalContents(component, mapWithSet(mids._1), bsps._2 ++ mids._2)
				else if (mids._1.isEmpty) MergeTotalContents(component, mapWithSet(bsps._1), bsps._2 ++ mids._2)
				else {
					// Merge together bsp and mid maps
					val wellMap = bsps._1 ++ mids._1.map {
						case (well, res) => bsps._1.get(well) match {
							case Some(bsp) => well -> MergeResult(bsp.bsp, res.mid)
							case _ => well -> res
						}
					}
					MergeTotalContents(component, mapWithSet(wellMap), bsps._2 ++ mids._2)
				}
			}

			/**
			 * If a quadrant transfer then map input wells to output wells.  If quadrant of 384-well plate being mapped
			 * to a 96-well plate then get contents of quadrant being used in 384-well plate and set it to wells that
			 * it will occupy on the 96-well plate.  If a 96-well plate is headed to a quadrant of a 384-well plate then
			 * set the wells of the 96-well plate input to be the wells they will be in the 384-well plate quadrant.
			 * @param in contents for input to transfer
			 * @param transfer transfer to be done from input
			 * @return contents mapped to output wells (quadrant of input or entire input mapped to quadrant of output)
			 */
			def takeQuadrant(in: MergeTotalContents, transfer: TransferEdge) = {
				/**
				 * Do the mapping of a quadrant between original input wells to wells it will go to in the destination.
				 * @param in input component contents
				 * @param layout layout we should be going to or from
				 * @param wellMap map of well locations in input to well locations in output
				 * @return input component contents mapped to destination wells
				 */
				def mapQuadrantWells(in: MergeTotalContents, layout: ContainerDivisions.Division.Division,
									 wellMap: Map[String, String]) = {
					in.component match {
						case cd:ContainerDivisions if cd.layout == layout =>
							val newWells = in.wells.flatMap{
								case (well, contents) if wellMap.get(well).isDefined =>
									List(wellMap(well) -> contents)
								case _ => List.empty
							}
							// Create the new input contents
							MergeTotalContents(in.component, newWells, in.errs)
						case _ => in
					}
				}

				// Go do mapping based on type of transfer
				(transfer.fromQuad, transfer.toQuad) match {
					case (Some(from), None) => mapQuadrantWells(in, DIM16x24, Transfer.qFrom384(from))
					case (None, Some(to)) => mapQuadrantWells(in, DIM8x12, Transfer.qTo384(to))
					case (Some(from), Some(to))  => in
					case _ => in
				}
			}

			// When merging/folding for each well if there are MIDs that are not attached yet (MergeResult with
			// bsp set to None) then attach them to all the samples (and get rid of MergeResult with bsp set to None).
			def mergeResults(input: MergeTotalContents, output: MergeTotalContents, transfer: TransferEdge) = {
				val inWithQuads = takeQuadrant(input, transfer)
				val inContents = inWithQuads.wells
				val outContents = output.wells
				// @TODO Need to use TruGrade quadrant plates to keep MID names matched with well name?
				val newResults = inContents ++ outContents.map {
					case (well, results) => (inContents.get(well), outContents.get(well)) match {
						case (Some(inResults), Some(outResults)) =>
							// Combine input and output sets (will eliminate duplicates)
							val allResults = inResults ++ outResults
							// Separate those with and without MIDs
							val midsVsSamples = allResults.groupBy(_.bsp.isDefined)
							val mids = midsVsSamples(false)
							val samples = midsVsSamples(true)
							// Merge together lists attaching MIDs not yet associated with samples
							val mergedResults =
								if (mids.size == 0 || samples.size == 0) allResults
								else {
									samples.map((sample) =>
										MergeResult(sample.bsp, sample.mid ++ (mids.flatMap(_.mid)))
									)
								}
							well -> mergedResults
						case _ => well -> results
					}
				}
				MergeTotalContents(output.component, newResults, input.errs ++ output.errs)
			}

			/**
			 * Find the contents for a component that is a node in a graph recording the transfers that lead
			 * into the component.
			 * @param output component node for which we want, taking into account transfers, the contents
			 * @return contents of component, including both initial contents and what was transferred in
			 */
			def findContents(output: graph.NodeT) : MergeTotalContents = {
				// Find all components directly transferred in
				val inputs = output.incoming
				// Fold all the incoming components into our contents
				inputs.foldLeft(getComponentContent(output))((soFar, next) => {
					val edge = next.edge
					edge match {
						case LkDiEdge(input, _, label) =>
							// We recurse to keep looking for previous transfers
							// Note: IDE thinks input isn't a graph.NodeT but compiler is perfectly happy
							val inputContents = findContents(input)
							mergeResults(inputContents, soFar, label)
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
