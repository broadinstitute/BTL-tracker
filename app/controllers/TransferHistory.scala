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
			 * Set initial contents, before transfers, of a component.
			 * @param c Component
			 * @return initial contents found
			 */
			def beginningContents(c: Component) =
				ComponentContents(c,Contents(getBspContents(c),getInitialContent(c)))

			/**
			 * If a quadrant transfer then map input wells to output wells.  If quadrant of 384-well plate being mapped
			 * to a 96-well plate then get contents of quadrant being used in 384-well plate and set it to wells that
			 * it will occupy on the 96-well plate.  If a 96-well plate is headed to a quadrant of a 384-well plate then
			 * set the wells of the 96-well plate input to be the wells they will be in the 384-well plate quadrant.
			 * @param in contents for input to transfer
			 * @param transfer transfer to be done from input
			 * @return contents mapped to output wells (quadrant of input or entire input mapped to quadrant of output)
			 */
			def takeQuadrants(in: ComponentContents, transfer: TransferEdge) = {
				/**
				 * Do the mapping of a quadrant between original input wells to wells it will go to in the destination.
				 * @param in input component contents
				 * @param layout layout we should be going to or from
				 * @param wellMap map of well locations in input to well locations in output
				 * @return input component contents mapped to destination wells
				 */
				def mapQuadrantWells(in: ComponentContents, layout: ContainerDivisions.Division.Division,
									 wellMap: Map[String, String]) = {
					in.component match {
						case cd:ContainerDivisions if cd.layout == layout =>
							// Get new MID mapping taking wells from input that can go to output
							val newMid = in.contents.mid.map((midContents) => {
								val newMidContents = midContents.contents.flatMap{
									case ((well, mid)) if wellMap.get(well).isDefined =>
										List(wellMap(well) -> mid)
									case _ => List.empty
								}
								MolBarcodeContents(newMidContents)
							})
							// Get new BSP mapping taking tubes from rack that can go to output
							val newBsp = in.contents.bsp.map((rackInfo) => {
								val newList = rackInfo.contents.flatMap{
									case ((well, bsp)) if wellMap.get(well).isDefined =>
										List(wellMap(well) -> bsp)
									case _ => List.empty
								}
								RackContentsInfo(jiraIssue = rackInfo.jiraIssue,
									jiraComponents = rackInfo.jiraComponents,
									jiraSummary = rackInfo.jiraSummary,contents = newList)
							})
							// Create the new input contents
							ComponentContents(in.component,Contents(newBsp, newMid))
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

//*********************************************************************************************************************

			/**
			 * Contents of a merged well
			 * @param project project ID
			 * @param mids molecular IDs set with specific well being used
			 * @param tube bsp tube in well
			 */
			case class MergedWell(project: String, mids: Set[MolBarcodeWell], tube: String)

			/**
			 * Contents found from merge
			 * @param projects map of project IDs to summary found for that issue
			 * @param mids map of mid types to quadrants of mids used
			 * @param tubes map of tube 2D barcodes to bsp tube contents
			 * @param contents set of contents in this container
			 * @param wells if contents are in wells then well-by-well contents
			 * @param errors list of errors found along the way
			 */
			case class MergeContents(projects: Map[String, Option[String]],
									 mids: Map[ContentType, Set[Quad]],
									 tubes: Map[String, BSPTube],
									 contents: Set[MergedWell],
									 wells: Option[Map[String, MergedWell]], errors: List[String])

			// 1. Set projects to include new project
			// 2. Set mids to include new mid
			// 3. Set tubes to include new entries via map merge
			// 4. Set wells to include MergedWells via map merge with mids and/or tubes
			// Alternative:
			// Have Map of entries (indexed by sample barcode and MIDs) to entry information and list of wells -
			//     - make special hash and eq functions for these based on tube barcode and MID names
			//     - before changing well contents take it out of list of wells and if last well remove entry
			//     from map
			// Have Map of wells to entries - this is just used to combine per well stuff quicker

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
			 * @param contents map by contents with well locations
			 * @param wells map contents by well location
			 * @param errs list of errors encountered
			 */
			case class MergeTotalContents(contents: Map[MergeResult, Set[String]], wells: Map[String, Set[MergeResult]],
										  errs: List[String])

			/**
			 * Get bsp sample information - if a rack we go look up if there's bsp information associated with the component.
			 * @param c Component to find bsp sample information for
			 * @return optional match, by well, of bsp sample information found
			 */
			def getBspContent(c: Component) =
				c match {
					case rack: Rack => RackController.getBSPmatch(rack.id,
						found = (matches, ssfIssue) => {
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
							(bspMatches, List.empty[String])
						},
						notFound = (err) => (Map.empty[String, MergeResult], List(err)))
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
			 * @param c component
			 * @return results with contents found
			 */
			def getComponentContent(c: Component) = {
				val bsps = getBspContent(c)
				val mids = getMidContent(c)
				if (bsps._1.isEmpty) mids._1
				else if (mids._1.isEmpty) bsps._1
				else
					bsps._1 ++ mids._1.map {
						case (well, res) => bsps._1.get(well) match {
							case Some(bsp) => well -> MergeResult(bsp.bsp, res.mid)
							case _ => well -> res
						}
					}
			}

			// When merging/folding for each well if there are MIDs that are not attached yet (MergeResult with
			// bsp set to None) then attach them to all the samples (and get rid of MergeResult with bsp set to None).
			def mergeResults(inResults: MergeTotalContents, outResults: MergeTotalContents) = {

			}

//*********************************************************************************************************************

			/**
			 * Merge together the contents of two components.
			 * @param in "from" component of transfer (input to other component)
			 * @param out "to" component of transfer (output of transfer)
			 * @param transfer transfer that took place between components
			 * @return output component contents now including materials transferred from input
			 */
			def mergeContents(in: ComponentContents, out: ComponentContents, transfer: TransferEdge) = {
				val inWithQuads = takeQuadrants(in, transfer)
				val inContents = inWithQuads.contents
				val outContents = out.contents
				// @TODO Need to use TruGrade quadrant plates to keep MID names matched with well name?  Have method to
				// get quadrants from mids?  Need to make merges into Map[String,Set] to get multiple inputs but
				// don't duplicate if getting same mids or bsp for same plate

				// If there's something in the input then use it, otherwise leave the output as is
				ComponentContents(out.component,
					Contents(inContents.bsp.fold(ifEmpty = outContents.bsp)(Some(_)),
						inContents.mid.fold(ifEmpty = outContents.mid)(Some(_))))
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
