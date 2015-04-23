package models

import models.ContainerDivisions.Division._
import models.initialContents.InitialContents
import models.TransferHistory.TransferEdge

import scalax.collection.edge.LkDiEdge

/**
 * TransferContent combines all the contents of components leading into a specified component.  First a graph is made
 * to know what inputs there are and then the graph is traversed to combine that contents.
 * Created by nnovod on 3/18/15.
 */
object TransferContents {

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
	 * @param isNextera is it a Nextera barcode
	 */
	case class MergeMid(sequence: String, name: String, isNextera: Boolean) {
		// Override equals and hash to make them simpler
		override def equals(arg: Any) = {
			arg match {
				case MergeMid(s, _, isNext) => s == sequence && isNext == isNextera
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

	// Need this to have label on edge be converted to a TransferEdge - making an implicit to be used for edge
	import scalax.collection.edge.LBase._
	object TransferLabel extends LEdgeImplicits[TransferEdge]
	import TransferLabel._
	import play.api.libs.concurrent.Execution.Implicits.defaultContext

	/**
	 * "Well" name for a single well component (e.g., a tube)
	 */
	val oneWell = "TOTAL_CONTENTS"

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
		TransferHistory.makeSourceGraph(componentID).map((graph) => {
			/*
			 * Get bsp sample information - if a rack we go look up if there's bsp information associated with the component.
			 * @param c Component to find bsp sample information for
			 * @return optional match, by well, of bsp sample information found
			 */
			def getBspContent(c: Component) =
				c match {
					case rack: Rack => Rack.getBSPmatch(rack.id,
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

			/*
			 * Get initial contents of container.
			 * @param c Component to get initial contents
			 * @return optional initial contents found
			 */
			def getMidContent(c: Component) =
				c match {
					case container: Container =>
						container.initialContent match {
							case Some(ic) =>
								val mids = InitialContents.contents(ic).contents.map{
									case (well, mbw) =>
										well -> MergeResult(None, Set(MergeMid(mbw.getSeq, mbw.getName, mbw.isNextera)))
								}
								(mids, List.empty[String])
							case _ => (Map.empty[String, MergeResult], List.empty[String])
						}
					case _ => (Map.empty[String, MergeResult], List.empty[String])
				}

			/*
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
				val errs = bsps._2 ++ mids._2
				if (bsps._1.isEmpty) MergeTotalContents(component, mapWithSet(mids._1), errs)
				else if (mids._1.isEmpty) MergeTotalContents(component, mapWithSet(bsps._1), errs)
				else {
					// Merge together bsp and mid maps
					val wellMap = bsps._1 ++ mids._1.map {
						case (well, res) => bsps._1.get(well) match {
							case Some(bsp) => well -> MergeResult(bsp.bsp, res.mid)
							case _ => well -> res
						}
					}
					MergeTotalContents(component, mapWithSet(wellMap), errs)
				}
			}

			/*
			 * If a quadrant and/or slice transfer then map input wells to output wells.  Only 384-well components have
			 * quadrants so any transfers to/from a quadrant must be to/from a 384-well component.  A slice is a subset
			 * of a quadrant (for components that have quadrants) or an entire component (for non-quadrant components,
			 * such as 96-well components, in which case the entire component can be thought of as a single quadrant).
			 * This method, taking in account the quadrants/slices wanted determines where the selected input wells
			 * will wind up in the output component.
			 * @param in contents for input to transfer
			 * @param transfer transfer to be done from input
			 * @return contents mapped to output wells (quadrant of input or entire input mapped to quadrant of output)
			 */
			def takeQuadrant(in: MergeTotalContents, transfer: TransferEdge) = {
				/*
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
				(transfer.fromQuad, transfer.toQuad, transfer.slice) match {
					// From a quadrant to an entire component - should be 384-well component to non-quadrant component
					case (Some(from), None, None) => mapQuadrantWells(in, DIM16x24, Transfer.qFrom384(from))
					// To a quadrant from an entire component - should be 96-well component to 384-well component
					case (None, Some(to), None) => mapQuadrantWells(in, DIM8x12, Transfer.qTo384(to))
					// Slice of quadrant to an entire component - should be 384-well component to non-quadrant component
					case (Some(from), None, Some(slice)) =>
						mapQuadrantWells(in, DIM16x24, Transfer.slice384to96map(from, slice))
					// Quadrant to quadrant - must be 384-well component to 384-well component
					case (Some(from), Some(to), None) =>
						mapQuadrantWells(in, DIM16x24, Transfer.q384to384map(from, to))
					// Slice of quadrant to slice of quadrant - must be 384-well component to 384-well component
					case (Some(from), Some(to), Some(slice)) =>
						mapQuadrantWells(in, DIM16x24, Transfer.slice384to384map(from, to, slice))
					// Slice of non-quadrant component to quadrant - Must be 96-well component to 384-well component
					case (None, Some(to), Some(slice)) =>
						mapQuadrantWells(in, DIM8x12, Transfer.slice96to384map(to, slice))
					// Slice of non-quadrant component to non-quadrant component
					// must be 96-well component to 96-well component
					case (None, None, Some(slice)) =>
						mapQuadrantWells(in, DIM8x12, Transfer.slice96to96map(slice))
					case _ => in
				}
			}

			/*
			 * Merge input into output.  When merging/folding for each well if there are MIDs that are not attached
			 * yet (MergeResult with bsp set to None) then attach them to all the samples (and get rid of MergeResult
			 * with bsp set to None).
			 * @param input input being merged into output
			 * @param output output being merged into
			 * @param transfer transfer that was done from input to output
			 * @return new merge result with input merged into output
			 */
			def mergeResults(input: MergeTotalContents, output: MergeTotalContents, transfer: TransferEdge) = {
				// Make sure we only get quadrants transferred (either one quadrant of input or reassign of wells
				// if full plate only going to one quadrant of output
				val inWithQuads = takeQuadrant(input, transfer).wells
				// If going to a component without divisions (e.g., a tube) put all of input into one "well"
				val inContent =
					if (output.component.isInstanceOf[ContainerDivisions]) inWithQuads
					else Map(oneWell -> getAsOneResult(inWithQuads))
				val outContents = output.wells
				// @TODO Need to use TruGrade quadrant plates to keep MID names matched with well name?
				// Merge input into output - combine maps to get entries that might be in one but not the other and
				// then within map combine what's in the same well
				val newResults = inContent ++ outContents.map {
					case (well, results) => (inContent.get(well), outContents.get(well)) match {
						case (Some(inResults), Some(outResults)) =>
							// Combine input and output sets (will eliminate duplicates)
							val allResults = inResults ++ outResults
							// Separate those with and without MIDs
							val midsVsSamples = allResults.groupBy(_.bsp.isDefined)
							val mids = midsVsSamples.getOrElse(false, Set.empty)
							val samples = midsVsSamples.getOrElse(true, Set.empty)
							// Merge together lists attaching MIDs not yet associated with samples
							val mergedResults =
								if (mids.size == 0 || samples.size == 0) allResults
								else samples.map((sample) =>
									MergeResult(sample.bsp, sample.mid ++ mids.flatMap(_.mid)))
							well -> mergedResults
						case _ => well -> results
					}
				}
				// Finally return total contents with input merged into output
				MergeTotalContents(output.component, newResults, input.errs ++ output.errs)
			}

			/*
			 * Find the contents for a component that is a node in a graph recording the transfers that lead
			 * into the component.
			 * @param output component node for which we want, taking into account transfers, the contents
			 * @return contents of component, including both initial contents and what was transferred in
			 */
			def findContents(output: graph.NodeT) : MergeTotalContents = {
				// Find all components directly transferred in to output, sorting them by when transfer occurred
				val inputs = output.incoming.toList.sortWith((edge1, edge2) => edge1.label.time < edge2.label.time)
				// Fold all the incoming components into output
				inputs.foldLeft(getComponentContent(output))((soFar, next) => {
					val edge = next.edge
					edge match {
						case LkDiEdge(input, _, label) =>
							// We recurse to look for transfers into the input (output for recursion)
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

	/**
	 * Merge all the wells' results into a single result
	 * @param in map of wells to result sets
	 * @return single result set combining all the wells' result sets
	 */
	def getAsOneResult(in: Map[String, Set[MergeResult]]) =
		in.foldLeft(Set.empty[MergeResult]) ((soFar, next) => soFar ++ next._2)
}
