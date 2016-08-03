package models

import models.ContainerDivisions.Division
import models.Transfer.Quad._
import models.Transfer.Slice._
import models.initialContents.InitialContents
import models.TransferHistory.TransferEdge
import InitialContents.ContentType
import models.project.JiraProject

import scala.concurrent.Future

import scalax.collection.Graph
import scalax.collection.edge.LkDiEdge

/**
 * TransferContent combines all the contents of components leading into a specified component.  First a graph is made
 * to know what inputs there are and then the graph is traversed to combine their contents.
 * Created by nnovod on 3/18/15.
 */
object TransferContents {

	/**
	 * Sample entry for merged results.
	 * @param project jira ticket ID
	 * @param projectDescription jira summary
	 * @param sampleTube tube barcode (if rack), otherwise plate barcode
	 * @param gssrSample GSSR barcode
	 * @param collabSample collaborator sample ID
	 * @param origCollabID original collaborator sample ID
	 * @param individual collaborator participant
	 * @param sampleID original sample ID
	 * @param antibody antibody requested for sample
	 * @param pos position in rack for tube
	 */
	case class MergeSample(project: String, projectDescription: Option[String], sampleTube: String,
						   gssrSample: Option[String], collabSample: Option[String], origCollabID: Option[String],
						   individual: Option[String], sampleID: Option[String], antibody: Option[String], pos: String) {
		// Override equals and hash to make it simpler
		override def equals(arg: Any) = {
			arg match {
				case MergeSample(p, _, s, _, _, _, _, _, _, ps) => p == project && s == sampleTube && ps == pos
				case _ => false
			}
		}
		override def hashCode = (project.hashCode * 31 * 31) + (sampleTube.hashCode * 31) + pos.hashCode
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
	 * Single sample and associated MIDs and antibodies.
	 * @param bsp sample information
	 * @param mid MIDs associated with sample
	 * @param antibody antibodies associated with sample
	 */
	case class MergeResult(bsp: Option[MergeSample], mid: Set[MergeMid], antibody: Set[String])

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
	 * @return future returning contents of component
	 */
	def getContents(componentID: String) : Future[Option[TransferContents.MergeTotalContents]] = {
		val emptyResult = Map.empty[String, MergeResult]
		// First make a graph of the transfers leading into the component.  That gives us a reasonable way to rummage
		// through all the transfers that have been done.  We map that graph into our component's contents
		TransferHistory.makeSourceGraph(componentID).map(f = (graph) => {
			/*
			 * Get bsp sample information - if a rack we go look up if there's bsp information associated with the
			 * component.
			 * @param c Component to find bsp sample information for
			 * @return optional match, by well, of bsp sample information found
			 */
			def getBspContent(c: Component) =
				c match {
					case rack: Rack if rack.initialContent.contains(ContentType.BSPtubes) =>
						Rack.getBSPmatch(id = rack.id,
							found = (matches, ssfIssue) => {
								// Get results of bsp matching into a map of wells to sample info
								val bspMatches = matches.flatMap {
									case ((well, (matchFound, Some(tube)))) =>
										Some(well -> MergeResult(
											bsp = Some(MergeSample(
												project = ssfIssue.issue, projectDescription = ssfIssue.summary,
												sampleTube = tube.barcode, gssrSample = tube.gssrBarcode,
												collabSample = tube.collaboratorSample,
												origCollabID = tube.collaboratorSample,
												individual = tube.collaboratorParticipant, sampleID = tube.sampleID,
												antibody = tube.antiBody, pos = tube.pos
											)),
											mid = Set.empty, antibody = Set.empty))
									case _ => None
								}
								// Return our sample map and that no errors found
								(bspMatches, List.empty[String])
							},
							// BSP info not found for rack - return empty map and error
							notFound = (err) => (emptyResult, List(err)))
					case plate: Plate
						if plate.initialContent.contains(ContentType.SamplePlate) =>
						plate.project match {
							case Some(project) =>
								val (samples, err) = JiraProject.getSampleMapCollection(project)
								samples.find(_.issue == project) match {
									case Some(projectSamples) =>
										val result = projectSamples.list.map(
											(sample) => {
												sample.pos -> MergeResult(
													bsp = Some(MergeSample(
														project = project,
														projectDescription = projectSamples.summary,
														sampleTube = plate.id, gssrSample = None,
														collabSample = Some(sample.sampleID),
														origCollabID = Some(sample.origSampleID),
														individual = None, sampleID = Some(sample.sampleID),
														antibody = None, pos = sample.pos
													)),
													mid = Set.empty, antibody = Set.empty)
											}
										)
										(result.toMap, List.empty[String])
									case None =>
										val errList = err match {
											case Some(err) => List(err)
											case None =>
												List(s"Unable to find sample map for ${plate.id} (project $project)")
										}
										(emptyResult, errList)
								}
							case None => (emptyResult, List(s"No project set for sample plate ${plate.id}"))
						}
					// Not a rack or sample plate - nothing to return
					case _ => (emptyResult, List.empty[String])
				}

			/*
			 * Get initial contents of container.
			 * @param c Component to get initial contents
			 * @return optional initial contents found
			 */
			def getInitialContent(c: Component) =
				c match {
					// @TODO Clean this up - make async and maybe put more smarts in RackScan
					case rack: Rack if rack.initialContent.isEmpty || rack.initialContent.get != ContentType.BSPtubes =>
						RackScan.findRackSync(rack.id) match {
							case (_, Some(err)) => (Map.empty[String, MergeResult], List(err))
							// We recurse (indirectly) to get the contents of each tube.  All pretty ugly and for now
							// there can't be multiple contents with samples in these tubes either
							case (racks, _) =>
								// Get the rack's tubes
								val rackTubes = racks.flatMap((r) => if (r.barcode == rack.id) r.contents else List.empty)
								// Get contents of all the tubes
								RackScan.getTubeContentsSync(rackTubes.map(_.barcode)) match {
									case (_, Some(err)) => (Map.empty[String, MergeResult], List(err))
									case (tubes, _) =>
										// Make map of barcodes to position
										val tubeMap = rackTubes.map((t) => t.barcode -> t.pos).toMap
										// Merge together the contents found into a result - someday we should get return
										// the TotalContents (which is what getTubeContents has originally) and return
										// that instead but for now there's no case where a tube is in a rack and has
										// multiple samples so we'll live with this for now
										val tubeResults = tubes.flatMap {
											case (tube, contents) if contents.nonEmpty =>
												// Get back tube's rack position for pos->content mapping
												tubeMap.get(tube.id) match {
													case Some(pos) =>
														if (contents.size == 1)
															Some(pos -> contents.head, None)
														else {
															val mids = contents.flatMap(_.mid)
															val abs = contents.flatMap(_.antibody)
															val bsps = contents.flatMap(_.bsp)
															val err =
																if (bsps.nonEmpty)
																	Some(s"Samples not supported in rack tube (${tube.id} in ${rack.id}")
																else
																	None
															Some(pos ->
																MergeResult(bsp = None, mid = mids, antibody = abs), err)
														}
													case _ => None
												}
											case _ => None
										}
										val errs = tubeResults.flatMap(_._2)
										val tubeContents = tubeResults.map(_._1).toMap
										(tubeContents, errs)
								}
						}
					case container: Container =>
						container.initialContent match {
							case Some(ic) if ContentType.isMolBarcode(ic) =>
								val mids = InitialContents.contents(ic).contents.map {
									case (well, mbw) =>
										well -> MergeResult(bsp = None,
											mid = Set(MergeMid(sequence = mbw.getSeq, name = mbw.getName,
												isNextera = mbw.isNextera)), antibody = Set.empty)
								}
								(mids, List.empty[String])
							// Antibody can only be initial content of an undivided container
							case Some(ic) if ContentType.isAntibody(ic) =>
								(Map(oneWell -> MergeResult(bsp = None, mid = Set.empty,
									antibody = Set(ic.toString))), List.empty[String])
							case _ => (Map.empty[String, MergeResult], List.empty[String])
						}
					case _ => (Map.empty[String, MergeResult], List.empty[String])
				}

			/*
			 * Get initial contents for a component - contents can include bsp input (for a rack) or MIDs (for a plate)
			 * or ABs (for a tube)
			 * @param component component
			 * @return results with contents found
			 */
			def getComponentContent(component: Component) = {
				// Make map with single MergeResult into a set of MergeResults
				def mapWithSet(in: Map[String, MergeResult]) = in.map { case (k, v) => k -> Set(v) }

				// Get bsp and initial content and then merge the results together
				val bsps = getBspContent(component)
				val midsAndAbs = getInitialContent(component)
				val errs = bsps._2 ++ midsAndAbs._2
				if (bsps._1.isEmpty) MergeTotalContents(component = component, wells = mapWithSet(midsAndAbs._1),
					errs = errs)
				else if (midsAndAbs._1.isEmpty) MergeTotalContents(component = component, wells = mapWithSet(bsps._1),
					errs = errs)
				else {
					// Merge together bsp and initial contents maps
					val wellMap = bsps._1 ++ midsAndAbs._1.map {
						case (well, res) => bsps._1.get(well) match {
							case Some(bsp) => well -> MergeResult(bsp = bsp.bsp, mid = res.mid, antibody = res.antibody)
							case _ => well -> res
						}
					}
					MergeTotalContents(component = component, wells = mapWithSet(wellMap), errs = errs)
				}
			}

			/*
			 * If a quadrant and/or slice transfer then map input wells to output wells.  Only 384-well components have
			 * quadrants so any transfers to/from a quadrant must be to/from a 384-well component.  A slice, other than
			 * cherry picking, is a subset of a quadrant (for components that have quadrants) or an entire component
			 * (for non-quadrant components, such as 96-well components, in which case the entire component can be
			 * thought of as a single quadrant).
			 * This method, taking in account the quadrants/slices wanted determines where the selected input wells
			 * will wind up in the output component.
			 * @param in contents for input to transfer
			 * @param out contents for output being transferred into
			 * @param transfer transfer to be done from input
			 * @return contents mapped to output wells (quadrant/slice/entire input mapped to quadrant/slice of output)
			 */
			def takeQuadrant(in: MergeTotalContents, out: MergeTotalContents, transfer: TransferEdge) =
				getNextWellMapping(soFar = in, fromComponent = in.component, toComponent = out.component,
					fromQuad = transfer.fromQuad, toQuad = transfer.toQuad, quadSlice = transfer.slice,
					cherries = transfer.cherries, isTubeToMany = transfer.isTubeToMany,
					isSampleOnly = transfer.isSampleOnly, getSameMapping = false) {
					/*
				     * Do the mapping of a quadrant between original input wells to wells it will go to in destination.
				     * @param in input component contents
				     * @param layout layout we should be coming from if a divided component
				     * @param wellMap map of well locations in input to well locations in output
				     * @return input component contents mapped to destination wells
				     */
						(in: MergeTotalContents, layout: ContainerDivisions.Division.Division,
						wellMap: Map[String, List[String]]) =>
							{
								// See where to look if divided component
								val divComponent = if (transfer.isTubeToMany) out.component else in.component
								getLayout(divComponent) match {
									case Some(foundLayout) if foundLayout == layout =>
										// See which wells we want and make new mapping for them
										val newWells =
											in.wells.flatMap {
												case (well, contents) if wellMap.get(well).isDefined =>
													wellMap(well).map(_ -> contents)
												case _ => List.empty
											}
										// Create the new input contents
										MergeTotalContents(component = in.component, wells = newWells, errs = in.errs)
									// Input not a divided component - take single input
									case _ => in
								}
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
				// if full plate only going to one quadrant of output)
				val inWithQuads = takeQuadrant(in = input, out = output, transfer = transfer).wells
				// If going to a component without divisions (e.g., a tube) put all of input into one "well"
				val inContent =
					if (output.component.isInstanceOf[ContainerDivisions]) inWithQuads
					else {
						// If we only want wells with samples (to avoid MIDs that are unattached) then eliminate
						// any results for wells in input that don't include a sample
						val inputs =
							if (transfer.isSampleOnly) {
								inWithQuads.map {
									case (well, contents) =>
										if (contents.exists(_.bsp.isDefined))
											well -> contents
										else
											well -> Set.empty[MergeResult]
								}
							} else {
								inWithQuads
							}
						Map(oneWell -> getAsOneResult(inputs))
					}
				val outContents = output.wells
				//@TODO Need to use TruGrade quadrant plates to keep MID names matched with well name?
				// Merge input into output - combine maps to get entries that might be in one but not the other and
				// then within map combine what's in the same well.
				val newResults = inContent ++ outContents.map {
					case (well, results) => (inContent.get(well), outContents.get(well)) match {
						// Something in input and output for well - merge them together
						case (Some(inResults), Some(outResults)) =>
							// Combine input and output sets (will eliminate duplicates)
							val allResults = inResults ++ outResults
							val mergedResults = mergeWellResults(allResults)
							well -> mergedResults
						// Nothing in well of either input or output so nothing to merge
						case _ => well -> results
					}
				}
				// Finally return total contents with input merged into output
				MergeTotalContents(component = output.component, wells = newResults, errs = input.errs ++ output.errs)
			}

			/*
			 * Find the contents for a component that is a node in a graph recording the transfers that lead
			 * into the component.
			 * @param output component node for which we want, taking into account transfers, the contents
			 * @return contents of component, including both initial contents and what was transferred in
			 */
			def findContents(output: Graph[Component, LkDiEdge]#NodeT): MergeTotalContents = {
				// Find all components directly transferred in to output, sorting them by when transfer occurred
				val inputs = output.incoming.toList.sortWith((edge1, edge2) => edge1.label.time < edge2.label.time)
				// Fold all the incoming components into output
				inputs.foldLeft(getComponentContent(output.value))((soFar, next) => {
					val edge = next.edge
					edge match {
						case LkDiEdge(input, _, label) =>
							// We recurse to look for transfers into the input (output for recursion)
							val inputContents = findContents(input)
							mergeResults(input = inputContents, output = soFar, transfer = label)
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
	 * Merge together all the contents going into a single well.  In particular, if there any unattached MIDs or
	 * antibodies, then they should now be attached to any samples in the well.
	 * @param results contents going into well
	 * @return merged contents going into well
	 */
	private def mergeWellResults(results: Set[MergeResult]) = {
		// Separate those with and without initial values
		val initialsVsSamples = results.groupBy(_.bsp.isDefined)
		val initials = initialsVsSamples.getOrElse(key = false, default = Set.empty)
		val samples = initialsVsSamples.getOrElse(key = true, default = Set.empty)
		// Merge together lists attaching MIDs/ABs not yet associated with samples
		if (initials.isEmpty || samples.isEmpty) results
		else samples.map((sample) => MergeResult(bsp = sample.bsp, mid = sample.mid ++ initials.flatMap(_.mid),
			antibody = sample.antibody ++ initials.flatMap(_.antibody)))
	}

	/**
	 * Merge all the wells' results into a single result.  First we put all the wells results into a single set and
	 * then within that set we attach any unattached mids to the samples.  This may lead to results that look a bit
	 * strange merging multiple wells into a tube.  In particular if MIDs are unattached in one well from a plate
	 * transferred into a tube then the unattached MIDs get attached to all the samples in other wells from the
	 * plate when this plate is put into the tube.  Correct?  In theory, yes.
	 * @param in map of wells to result sets
	 * @return single result set combining all the wells' result sets
	 */
	private def getAsOneResult(in: Map[String, Set[MergeResult]]) =
		mergeWellResults(in.foldLeft(Set.empty[MergeResult])((soFar, next) => soFar ++ next._2))

	/*
	 * Get a components layout if it is divided
	 * @param c component
	 * @return layout of component if one is found
	 */
	private def getLayout(c: Component) =
		c match {
			case cd: ContainerDivisions => Some(cd.layout)
			case _ => None
		}

	/**
	 * If a quadrant and/or slice transfer then map input wells to output wells.  Only 384-well components have
	 * quadrants so any transfers to/from a quadrant must be to/from a 384-well component.  A slice, other than
	 * cherry picking, is a subset of a quadrant (for components that have quadrants) or an entire component
	 * (for non-quadrant components, such as 96-well components, in which case the entire component can be
	 * thought of as a single quadrant).
	 * This method, taking in account the quadrants/slices wanted determines where the selected input wells
	 * will wind up in the output component.
	 * If the input or output is not a divided component (e.g., a tube) and the other side of the transfer is
	 * divided then the "wells" for the non-divided component will simply match up with the divided component's wells.
	 * For example a transfer from wells A01 and D12 from a plate to a tube will be set as A01->A01, D12->D12.
	 * Similarly, a transfer from a tube to wells A01 and D12 on a plate will get the results A01->A01, D12->D12
	 * (this should only happen if isTubeToMany is set).
	 * @param soFar initial object with results so far to add to to return results of merging in new wells
	 * @param fromComponent component being transferred from
	 * @param toComponent component being transferred to
	 * @param fromQuad quadrant transfer is coming from
	 * @param toQuad quadrant transfer is going to
	 * @param quadSlice slice of quadrant being transferred
	 * @param cherries cherry picked wells being transferred
	 * @param makeOut callback to return result - called with (soFar, component, input->output wells picked)
	 * @param isTubeToMany input is a tube being transferred to one or more wells in a multi-well component
	 * @param isSampleOnly only transfer wells containing samples
	 * @param getSameMapping return mapping of wells if transfer of entire components with same division
	 * @tparam T type of parameter tracking results
	 * @return result of makeOut callback or original input (soFar) if complete component move and getSameMapping false
	 */
	def getNextWellMapping[T](soFar: T, fromComponent: Component, toComponent: Component,
							  fromQuad: Option[Quad], toQuad: Option[Quad],
							  quadSlice: Option[Slice], cherries: Option[List[Int]],
							  isTubeToMany: Boolean, isSampleOnly: Boolean, getSameMapping: Boolean)
							 (makeOut: (T, Division.Division, Map[String, List[String]]) => T) =
		Transfer(from = fromComponent.id, to = toComponent.id, fromQuad = fromQuad, toQuad = toQuad,
			project = None, slice = quadSlice, cherries = cherries,
			isTubeToMany = isTubeToMany, isSampleOnly = isSampleOnly)
			.getWellMapping(soFar = soFar, fromComponent = fromComponent, toComponent = toComponent,
				getSameMapping = getSameMapping)(makeOut = makeOut)

}
