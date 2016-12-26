package models
import models.Component.ComponentType
import models.ContainerDivisions.Division
import models.db.{TrackerCollection, TransferCollection}
import models.Transfer.Slice._
import org.broadinstitute.spreadsheets.{CellSheet, HeaderSheet}
import org.broadinstitute.spreadsheets.Utils._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.{No, Yes, YesOrNo}

import scala.annotation.tailrec
import scala.concurrent.Future

/**
 * Do transfers via a csv file containing multiple anywhere-to-anywhere transfers.
 * Created by nnovod on 12/14/16.
 */
object TransferFile {
	//@TODO Make project a global to transfer - to be applied to all transfers and inserted components
	//@TODO Ever want to handle racks?
	//@TODO Check for cyclical graphs
	// File headers
	private val sourcePlateHdr = "Source Plate Barcode"
	private val destPlateHdr = "Destination Plate Barcode"
	private val sourceWellHdr = "Source Well"
	private val destWellHdr = "Destination Well"
	private val sourceTypeHdr = "Source Component Type"
	private val destTypeHdr = "Destination Component Type"
	private val volHdr = "Transfer Volume"

	// File component types
	private val plate96Type = "96-well plate"
	private val plate384Type = "384-well plate"
	private val tubeType = "tube"
	private val componentTypes = List(plate96Type, plate384Type, tubeType)
	// Map of file component types to real types
	private val componentTypesMap =
		Map(plate96Type -> ComponentType.Plate, plate384Type -> ComponentType.Plate, tubeType -> ComponentType.Tube)
	// Map of file component types to real divisions
	private val componentDivisions =
		Map(plate96Type -> Some(Division.DIM8x12), plate384Type -> Some(Division.DIM16x24), tubeType -> None)

	/**
	 * Store individual transfer information from file
	 * @param source source component
	 * @param dest destination component
	 * @param vol volume of transfer
	 */
	private case class TransferInfo(source: ComponentInfo, dest: ComponentInfo, vol: Option[String])

	/**
	 * Store information about source/targe of transfer from file
	 * @param id component ID
	 * @param cType type of component
	 * @param div division of component
	 * @param well well of component
	 */
	private case class ComponentInfo(id: String, cType: Option[ComponentType.ComponentType],
									 div: Option[Division.Division], well: String)

	/**
	 * Go parse contents of file containing transfers and insert the transfers found
	 * @param file file name
	 * @return transfers inserted or error message
	 */
	def insertTransferFile(file: String): Future[YesOrNo[List[Transfer]]] = {
		/*
		 * Open file that has transfers - can be either a csv file or excel spreadsheet
		 * @param file file name
		 * @return sheet with header row first followed by rows with content
		 */
		def getFile = {
			val sheetToVals = (sh: CellSheet) => new HeaderSheet(sh)
			if (isSpreadSheet(file)) getSheetData(file, 0, sheetToVals)
			else getCSVFileData(file, sheetToVals)
		}

		val sheet = getFile
		// Get iterator to go through entries
		val transferList = (new sheet.RowValueIter).toList
		insertTransfers(transferList)
	}

	// Transfers parsed, map of component(s) found in DB, map of components to be inserted into DB
	private type TranData = (List[TransferInfo], Map[String, Component], Map[String, Component])
	// Transfers and components parsed
	private type CheckData = (List[TransferInfo], Map[String, List[ComponentInfo]])
	// Info from parse (list of maps with header->value)
	private type InputData = List[Map[String, String]]

	/**
	 * Initial parse of transfers found.  We check for some obvious errors (missing IDs, invalid component types or
	 * inconsisten types) and return the results.
	 * @param transfers transfers found in input (list of maps with header->value)
	 * @return transfers found or error message
	 */
	private def checkTransferParse(transfers: InputData): YesOrNo[CheckData] = {
		// Fold all the transfers into an error message or the transfer info
		val (transList, errs) = transfers.foldLeft(List.empty[TransferInfo], Set.empty[String]) {
			case ((platesSoFar, errsSoFar), row) =>
				def checkIfEmpty(opt: Option[String]) =
					if (opt.isEmpty || opt.get.trim.isEmpty) None else Some(opt.get.trim)
				def checkIfEmptyUC(opt: Option[String]) =
					checkIfEmpty(opt) match {
						case Some(str) => Some(str.toUpperCase)
						case _ => None
					}
				def makeTypeErr(c: String, cType: String) =
					s"$c has invalid type: $cType.  Must be one of ${componentTypes.mkString(",")}"
				// Get info from file
				val (src, dst, sWell, dWell, sType, dType, vol) =
					(row.get(sourcePlateHdr), row.get(destPlateHdr),
						row.get(sourceWellHdr), row.get(destWellHdr),
						row.get(sourceTypeHdr), row.get(destTypeHdr), row.get(volHdr))
				val source = checkIfEmpty(src)
				val dest = checkIfEmpty(dst)
				val sTypeDef = checkIfEmpty(sType)
				val dTypeDef = checkIfEmpty(dType)
				// Error if plate headers missing or component type is invalid
				if (source.isEmpty || dest.isEmpty)
					(platesSoFar, errsSoFar + s"$sourcePlateHdr and $destPlateHdr must be specified")
				else if (sTypeDef.isDefined && !componentTypes.contains(sTypeDef.get))
					(platesSoFar, errsSoFar + makeTypeErr(source.get, sTypeDef.get))
				else if (dTypeDef.isDefined && !componentTypes.contains(dTypeDef.get))
					(platesSoFar, errsSoFar + makeTypeErr(dest.get, dTypeDef.get))
				else {
					// Get valid information into useful structures
					val srcComponent =
						ComponentInfo(
							id = source.get, cType = sTypeDef.map(componentTypesMap(_)),
							div = sTypeDef.flatMap(componentDivisions(_)),
							well = checkIfEmptyUC(sWell).getOrElse("")
						)
					val destComponent =
						ComponentInfo(
							id = dest.get, cType = dTypeDef.map(componentTypesMap(_)),
							div = dTypeDef.flatMap(componentDivisions(_)),
							well = checkIfEmptyUC(dWell).getOrElse("")
						)
					val trans =
						TransferInfo(source = srcComponent, dest = destComponent, vol = vol)
					(trans :: platesSoFar, errsSoFar)
				}
		}
		// Make a map grouped by componentID
		val componentMap =
			transList
				.flatMap((t) => List(t.source, t.dest))
				.groupBy(_.id)
		// Check if component has more than one type
		val errs1 = componentMap.foldLeft(errs) {
			case (errsSoFar, (component, entries)) =>
				// Get different types (type and division) specified for component (should only be one)
				val types = entries.flatMap((e) =>
					(e.cType, e.div) match {
						case (Some(t), Some(d)) => Some(s"$t ($d)")
						case (Some(t), _) => Some(s"$t")
						case _ => None
					}
				).toSet
				// If two or more then trouble - multiple types specified for same component
				if (types.size >= 2)
					errsSoFar + s"Component $component has multiple types: ${types.mkString(", ")}"
				else
					errsSoFar
		}
		// Exit saying yes or no
		if (errs1.nonEmpty)
			No(errs1.mkString("; "))
		else
			Yes((transList, componentMap))
	}

	/**
	 * Get a components division type
	 * @param c component
	 * @return optional division type
	 */
	private def getDiv(c: Component) =
		c match {
			case divisions: ContainerDivisions =>
				Some(divisions.layout)
			case _ => None
		}

	/**
	 * Check if types in input agree with what's already in the DB.
	 * @param components components found in DB
	 * @param componentMap map of component ID to component info in input
	 * @return if problems then error message
	 */
	private def checkTransferTypes(components: List[Component], componentMap: Map[String, List[ComponentInfo]]) = {
		// Go through components returned from DB to see if types and divisions for agree with what was requested
		components.flatMap((component) =>
			// Get component requested
			componentMap.get(component.id) match {
				case Some(entries) =>
					entries.find(_.cType.isDefined) match {
						// If type defined in input see if it's same as what's in DB
						case Some(entry) =>
							val entryType = entry.cType.get
							if (entryType != component.component)
								Some(
									s"${component.id} found with type ${component.component}.  $entryType expected"
								)
							else {
								// Check if division of component in DB same as what was set in requests
								val cDiv = getDiv(component)
								val entryDiv = entries.flatMap(_.div).headOption
								if (cDiv == entryDiv)
									None
								else {
									val foundDiv =
										if (cDiv.isDefined) cDiv.get.toString else "no"
									val wantedDiv =
										if (entryDiv.isDefined) entryDiv.get.toString else "no"
									Some(
										s"${component.id} found with $foundDiv division, $wantedDiv division expected"
									)
								}
							}
						case None => None
					}
				case None => None
			}
		)
	}

	/**
	 * Check if wells specified are valid for components
	 * @param allComponents all components (ones found in DB and ones we need to create)
	 * @param transList list of transfers requested.
	 * @return error message if problems.
	 */
	private def chkWells(allComponents: Map[String, Component], transList: List[TransferInfo]) = {
		/*
		 * Check that well specification is valid
		 * @param cID component ID
		 * @param cWell well specification
		 * @return optional error message
		 */
		def chkWell(cID: String, cWell: String) = {
			// Get component - should always be there due to earlier checks
			val c = allComponents(cID)
			getDiv(c) match {
				// See if well valid for component's type of division
				case Some(div) =>
					cWell match {
						// Parse well - if invalid format it will fail regular expression parse
						case TransferWells.wellRegexp(row, col) =>
							div match {
								// Check if ok for 96-well component
								case Division.DIM8x12 =>
									if (row.charAt(0) > 'H' || col.toInt > 12)
										Some(s"Invalid well specified for 96-well $cID: $cWell")
									else
										None
								// Check if ok for 384-well component
								case Division.DIM16x24 =>
									if (row.charAt(0) > 'P' || col.toInt > 24)
										Some(s"Invalid well specified for 384-well $cID: $cWell")
									else
										None
								case _ => Some(s"Unknown division for $cID")
							}
						case _ =>
							if (cWell.nonEmpty)
								Some(s"Invalid well specified for $cID: $cWell")
							else
								Some(s"No well specified for $cID")
					}
				// If undivided we ignore well
				case _ => None
			}

		}

		// Get well errors
		transList.flatMap(
			(tInfo) => {
				val (sID, dID) = (tInfo.source.id, tInfo.dest.id)
				val (sWell, dWell) = (tInfo.source.well, tInfo.dest.well)
				List(chkWell(sID, sWell), chkWell(dID, dWell))
			}
		).flatten
	}

	/**
	 * Check that transfers are legit.
	 * @param transfers transfers found in input (list of maps with header->value)
	 * @return if ok then list of transfers, list of components in DB, list of components not in DB
	 */
	private def checkTransfers(transfers: InputData) : Future[YesOrNo[TranData]] = {
		/*
		 * Make a component object from the input info
		 * @param wantedComponent info for component to be created
		 * @return component created based on input info
		 */
		def makeComponent(wantedComponent: ComponentInfo) = {
			val desc = "Automatically registered during transfer"
			//@TODO set project?
			wantedComponent.cType.get match {
				case ComponentType.Plate =>
					Plate(id = wantedComponent.id,
						description = Some(desc),
						project = None,
						tags = List.empty,
						locationID = None,
						initialContent = None,
						layout = wantedComponent.div.get)
				case ComponentType.Tube =>
					Tube(id = wantedComponent.id,
						description = Some(desc),
						project = None,
						tags = List.empty,
						locationID = None,
						initialContent = None
					)
			}
		}

		// Start by checking if input information looks good
		checkTransferParse(transfers) match {
			case no: No => Future.successful(no)
			// Now go on to get components in DB and check if input and components in DB agree
			case Yes((transList, componentMap)) =>
				// Now go find which components already there
				val ids = componentMap.keys.toList
				TrackerCollection.findIds(ids)
					.map((components) => {
						// See if types in DB and input agree
						val typeErrs = checkTransferTypes(components, componentMap)
						if (typeErrs.nonEmpty)
							No(typeErrs.mkString("; "))
						else {
							// Get list of ids found
							val componentIds = components.map(_.id)
							// See if there were any ids not found
							val notFound = ids.diff(componentIds)
							// Check that type specified for all components that must be added
							val noTypes = notFound.flatMap((cID) => {
								componentMap.get(cID) match {
									case Some(entries) =>
										if (entries.flatMap(_.cType).headOption.isEmpty)
											Some(s"Type must be specified for unregistered $cID")
										else
											None
									case _ => None
								}
							})
							if (noTypes.nonEmpty)
								No(noTypes.mkString("; "))
							else {
								// Get map of components found
								val componentFoundMap = components.map((c) => c.id -> c).toMap
								// Make map of components not found
								val componentNotFoundMap: Map[String, Component] =
									componentMap.flatMap {
										case (cID, cInfo) =>
											componentFoundMap.get(cID) match {
												case None =>
													// Know type there since we checked previously
													val wantedComponent = cInfo.find(_.cType.isDefined).get
													val componentToUse = makeComponent(wantedComponent)
													Some(cID -> componentToUse)
												case _ => None
											}
									}
								// Get map for all components
								val allComponents = componentFoundMap ++ componentNotFoundMap
								val wellErrs = chkWells(allComponents, transList)
								// Exit with errors or what's been found
								if (wellErrs.nonEmpty)
									No(wellErrs.mkString("; "))
								else {
									Yes(transList, componentFoundMap, componentNotFoundMap)
								}
							}
						}
					})
		}
	}

	/**
	 * Values for match types found for wells in list (quadrant or entire plate)
	 */
	private object MatchType extends Enumeration {
		type MatchType = Value
		val M_Q1 = Value
		val M_Q2 = Value
		val M_Q3 = Value
		val M_Q4 = Value
		val M_Whole = Value
	}
	import MatchType._
	import Transfer.Quad._
	// Map of quadrant match type to actual quadrant (must have been quadrant match)
	private val matchToQ =
		Map(M_Q1 -> Q1, M_Q2 -> Q2, M_Q3 -> Q3, M_Q4 -> Q4)
	// Map of quadrant match type to list of indicies to wells in match
	private val matchQuadWellList =
		List(M_Q1 -> TransferWells.quadIdxWells(Q1),
			M_Q2 -> TransferWells.quadIdxWells(Q2),
			M_Q3 -> TransferWells.quadIdxWells(Q3),
			M_Q4 -> TransferWells.quadIdxWells(Q4)
		)
	// Indicies of entire 96-well palte
	private val whole96 = (0 to 95).toList
	// Map of non-quadrant match types to list of indicies for match
	private val match96list = List(M_Whole -> whole96)

	/**
	 * Make Transfer objects for all the transfers requested.
	 * @param wellTrans transfers wanted (inID, outID) -> list of (inWell, list of output wells)
	 * @param tranComponents components in transfers
	 * @return transfer objects to be inserted
	 */
	private def makeTransfer(wellTrans: Map[(String, String), List[(Int, List[Int])]],
							 tranComponents: Map[String, Component]) : Iterable[Transfer] = {
		// Go through transfers wanted
		wellTrans.map {
			case ((src, dest), wellList) =>
				import MatchType._
				/*
				 * Go through input/output for transfer to see if it matches requested lists.  We look for all
				 * matches at once to only need to go through well lists once.  A list is considered to be a match
				 * if both input and output match exactly one of the entries in the input/output list.
				 * @param inMatches matches we're looking for in input
				 * @param outMatches matches we're looking for in output
				 * @return (is input and output the same?, matches found
				 */
				def isMatch(inMatches: List[(MatchType, List[Int])],
							outMatches: List[(MatchType, List[Int])]) : (Boolean, Option[(MatchType, MatchType)]) = {
					/*
					 * Recursive fellow to do real work
					 * @param left what's left to match
					 * @param isSame input and output same so far?
					 * @param inLeft what's left that can be matched in input
					 * @param outLeft what's left that can be matched in output
					 * @return
					 */
					@tailrec
					def chkMatch(left: Transfer.FreeList, isSame: Boolean, inLeft: List[(MatchType, List[Int])],
								 outLeft: List[(MatchType, List[Int])]) : (Boolean, Option[(MatchType, MatchType)]) = {
						if (left.isEmpty) {
							/*
							 * See if we completed match - if only one left in match list and it's empty
							 * then we've found a match.
							 * @param matchLeft match lists left
							 * @return if a match found we return it
							 */
							def oneLeft(matchLeft: List[(MatchType, List[Int])]) =
								if (matchLeft.size == 1 && matchLeft.head._2.isEmpty)
									Some(matchLeft.head._1)
								else
									None

							// Return if input and output the same and if match found for both input and output
							(oneLeft(inLeft), oneLeft(outLeft)) match {
								case (Some(inMatch), Some(outMatch)) =>
									(isSame, Some(inMatch, outMatch))
								case _ => (isSame, None)
							}
						} else if (!isSame && (inLeft.isEmpty || outLeft.isEmpty)) {
							// Already doesn't match so leave now with the sad news
							(false, None)
						} else {
							// Check if matches continue
							val (nextSrc, nextDst) = left.head
							// If multiple destination wells then we can't match
							if (nextDst.size != 1)
								(false, None)
							else {
								/*
								 * Check if next well found matches next well expected in match lists.
								 * @param wanted well we want to match
								 * @param matchLeft matches we're still looking for
								 * @return list of matches well matched (updated to next well to match)
								 */
								def leftList(wanted: Int, matchLeft: List[(MatchType, List[Int])]) =
									matchLeft.foldLeft(List.empty[(MatchType, List[Int])])((soFar, next) => {
										if (next._2.isEmpty || wanted != next._2.head)
											soFar
										else
											(next._1, next._2.tail) :: soFar
									})
								// See if input and output remains the same
								val stillSame = isSame && left.head._1 == left.head._2.head
								// Go check if we're still matching any of lists
								chkMatch(left.tail, stillSame,
									leftList(nextSrc, inLeft), leftList(nextDst.head, outLeft))
							}
						}
					}
					// Start up the check
					chkMatch(left = wellList, isSame = true, inLeft = inMatches, outLeft = outMatches)
				}
				// A few simple methods to create wanted transfers
				//@TODO set project?
				def plainTran =
					Transfer(from = src, to = dest, fromQuad = None, toQuad = None, project = None,
						slice = None, cherries = None, free = None,
						isTubeToMany = false, isSampleOnly = false)
				def cherryTran(cher: List[Int], tToMany: Boolean) =
					Transfer(from = src, to = dest, fromQuad = None, toQuad = None, project = None,
						slice = Some(CP), cherries = Some(cher), free = None,
						isTubeToMany = tToMany, isSampleOnly = false)
				def cherryTranSource =
					cherryTran(cher = wellList.map(_._1), tToMany = false)
				def freeTran =
					Transfer(from = src, to = dest, fromQuad = None, toQuad = None, project = None,
						slice = Some(FREE), cherries = None, free = Some(wellList),
						isTubeToMany = false, isSampleOnly = false)
				def qTran(sQ: Option[Quad], dQ: Option[Quad]) =
					Transfer(from = src, to = dest, fromQuad = sQ, toQuad = dQ, project = None,
						slice = None, cherries = None, free = None,
						isTubeToMany = false, isSampleOnly = false)

				// Get source and destination components
				val srcC = tranComponents(src)
				val destC = tranComponents(dest)
				// Match based on component types/divisions
				(getDiv(srcC), getDiv(destC)) match {
					// 96 to 96 well plates
					case (Some(Division.DIM8x12), Some(Division.DIM8x12)) =>
						isMatch(List.empty, List.empty) match {
							case (true, _) =>
								if (wellList.size == 96 || wellList.isEmpty)
									plainTran // Plate to Plate (96)
								else
									cherryTranSource // Cherry pick plate (96)
							case _ =>
								//"Plate free for all (96)"
								freeTran
						}
					// 384 to 384 well plates
					case (Some(Division.DIM16x24), Some(Division.DIM16x24)) =>
						isMatch(matchQuadWellList, matchQuadWellList) match {
							case (_, Some((sQ, dQ))) =>
								qTran(Some(matchToQ(sQ)), Some(matchToQ(dQ))) //s"From $sQ to $dQ"
							case (true, _) =>
								if (wellList.size == 384 || wellList.isEmpty)
									plainTran // Plate to Plate (384)
								else
									cherryTranSource // Cherry pick plate (384)
							case _ =>
								//"Plate free for all (384)"
								freeTran
						}
					// 96 to 384 well plates
					case (Some(Division.DIM8x12), Some(Division.DIM16x24)) =>
						isMatch(match96list, matchQuadWellList) match {
							case (_, Some((srcMatch, dQ)))
								if srcMatch == M_Whole =>
								// s"To $dQ"
								qTran(None, Some(matchToQ(dQ)))
							case _ =>
								// "Plate free for all 96 -> 384"
								freeTran
						}
					// 384 to 96 well plates
					case (Some(Division.DIM16x24), Some(Division.DIM8x12)) =>
						isMatch(matchQuadWellList, match96list) match {
							case (_, Some((sQ, dstMatch)))
								if dstMatch == M_Whole =>
								// s"From $sQ"
								qTran(Some(matchToQ(sQ)), None)
							case _ =>
								// "Plate free for all 96 -> 384"
								freeTran
						}
					// "Between tubes"
					case (None, None) =>
						plainTran
					// "From Tube"
					case (None, _) =>
						cherryTran(cher = wellList.flatMap(_._2), tToMany = true)
					// "To Tube"
					case (_, None) =>
						cherryTranSource
					// Should never get here - just make it a free-for-all transfers
					case _ =>
						freeTran
				}
		}
	}

	/**
	 * Check and insert transfers.
	 * @param transfers transfers found in input (list of maps with header->value)
	 * @return Error message or list of transfers done
	 */
	def insertTransfers(transfers: InputData) : Future[YesOrNo[List[Transfer]]] = {
		// Check if transfers look legit
		checkTransfers(transfers).flatMap {
			case no: No => Future.successful(no)
			// Onward to insert transfers
			case Yes((transList, componentFoundMap, componentNotFoundMap)) =>
				// Put in components not in DB yet
				TrackerCollection.insertComponents(componentNotFoundMap.values.toList)
					.flatMap {
						case (_, fail) =>
							// If inserts failed bail out
							if (fail.nonEmpty)
								Future.successful(No(s"Failed inserts: ${fail.mkString(";")}"))
							else {
								// Get transfers grouped by same source/destination
								val srcToDst = transList.groupBy((tranInfo) => {
									(tranInfo.source.id, tranInfo.dest.id)
								})
								// Make one map of all the compoments (both originally in DB and not)
								val allComponents = componentFoundMap ++ componentNotFoundMap
								// Make free-for-all transfer lists
								val wellTrans =
									srcToDst.map {
										case ((srcID, destID), trans) =>
											/*
											 * Create a well index based on the compoment division
											 * @param cID component ID
											 * @param well well name
											 * @return index for well
											 */
											def getWellIdx(cID: String, well: String) = {
												// Get component and get well index based on component division
												val c = allComponents(cID)
												getDiv(c) match {
													// 384-well component
													case Some(Division.DIM16x24) =>
														TransferWells.make384IdxFromWellStr(well)
													// 96-well component
													case Some(Division.DIM8x12) =>
														TransferWells.make96IdxFromWellStr(well)
													// Undivided component
													case _ => 0
												}
											}
											// Make well list for transfers between the source and destination
											val wellList =
												trans
													.map((tInfo) => // Get well->well lists
														getWellIdx(srcID, tInfo.source.well) ->
															getWellIdx(destID, tInfo.dest.well)
													)
													.groupBy(_._1) // Group by source wells
													.toList // Make them into a list
													.map { // And set source well -> destination wells
														case (in, out) =>
															in -> out.map(_._2).distinct.sorted
													}
											// Make list sorted so the can be compared for matching later
											(srcID, destID) -> wellList.sortBy(_._1)
									}
								// Make transfer objects for what we've found
								val transfers = makeTransfer(wellTrans, allComponents).toList
								// Go startup transfers
								val futTransfers = transfers.map(TransferCollection.insert)
								// Make list of futures into one future with a list of results
								Future.sequence(futTransfers).map(_ => Yes(transfers))
							}
					}
		}
	}
}