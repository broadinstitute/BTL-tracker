package models

import formats.CustomFormats._
import mappings.CustomMappings._
import models.ContainerDivisions.Division
import models.ContainerDivisions.Division._
import models.Transfer.Slice
import models.db.{TrackerCollection, TransferCollection}
import models.initialContents.InitialContents.ContentType
import play.api.data.{Form, Mapping}
import play.api.data.Forms._
import play.api.libs.json._
import utils.{No, Yes, YesOrNo}
import Transfer._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.bson.{BSON, BSONArray, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONHandler, BSONInteger, BSONString, Macros}

/**
 * Transfers are between components
 * Created by nnovod on 11/27/14.
 */

/**
 * Transfer data.  Note: Must have complete path for Quad types below to avoid bug from macro Json.format which says it
 * can't find matching apply and unapply methods if type simply specified as Quad.Quad
 * See https://github.com/playframework/playframework/issues/1469
 * @param from ID we're transferring from
 * @param to ID we're transferring to
 * @param fromQuad optional quadrant transfer is coming from
 * @param toQuad optional quadrant transfer is going to
 * @param project optional project transfer is associated with
 * @param slice optional slice to transfer
 * @param cherries wells cherry picked (indicies to wells going across first) - valid if and only if slice is set to CP
 * @param free anything goes transfer (well -> target well(s)).  Wells by index - valid iff slice set to FREE
 * @param isTubeToMany true if transferring tube to a multi-welled container
 * @param isSampleOnly true if transferring from a plate to a tube and only want to transfer wells with samples
 */
case class Transfer(from: String, to: String,
					fromQuad: Option[Transfer.Quad.Quad], toQuad: Option[Transfer.Quad.Quad], project: Option[String],
					slice: Option[Transfer.Slice.Slice], cherries: Option[List[Int]],
					free: Option[Transfer.FreeList], isTubeToMany: Boolean, isSampleOnly: Boolean) {

	import models.Transfer.Quad._
	import models.Transfer.Slice._
	/**
	 * Make a description of the transfer, including quadrant descriptions.
	 * @return description of transfer, including quadrant information
	 */
	def quadDesc: String = {
		def qDesc(id: String, quad: Option[Quad], slice: Option[Slice]) = {
			val sliceStr = slice.map((s) => {
				val head = s match {
					case Slice.CP => "cherry picked wells"
					case Slice.FREE => "many to many wells"
					case _ => s"slice $s"
				}
				s"$head of "
			}).getOrElse("")
			sliceStr + quad.map((q) => s"$q of $id").getOrElse(id)
		}
		val (fromSlice, toSlice) = if (isTubeToMany) (None, slice) else (slice, None)
		val samples = if (isSampleOnly) "of samples only " else ""
		"transfer " + samples + "from " + qDesc(from, fromQuad, fromSlice) + " to " + qDesc(to, toQuad, toSlice)
	}

	/**
	 * Get source and target components of transfer
	 * @return components found or error message
	 */
	def getComponents() : Future[YesOrNo[(Component, Component)]] = {
		// Get transfer source and target components
		TrackerCollection.findIds(List(from, to)).map((contents) => {
			val findFromC = contents.find(_.id == from)
			val findToC = contents.find(_.id == to)
			// Check out that we were able to retrieve both items
			(findFromC, findToC) match {
				// Got them both
				case (Some(fromC), Some(toC)) => Yes((fromC, toC))
				// Cases where one or more of the components can't be found
				case (Some(_), None) => No(s"Component $to not found")
				case (None, Some(_)) => No(s"Component $from not found")
				case _ => No(s"Components $from and $to not found")
			}
		})

	}

	/**
	 * Type for inserting a transfer into the DB - if there's an error a string is returned
	 */
	private type DoInsert = (Transfer) => Future[Option[String]]

	/**
	 * Do simple insert of transfer object
	 * @param t transfer to insert
	 * @return optional error
	 */
	private def doInsert(t: Transfer) = TransferCollection.insert(t)

	/**
	 * Insert the transfer into the DB.  This is often not a simple single insert into the DB because of
	 * "subcomponents".  Some components (in reality just non-BSP racks) are really just holders for subcomponents
	 * (e.g., tubes in racks).  Transfers involving subcomponents must be broken down into multiple transfers to/from
	 * the subcomponents.  That's what we do here.
	 * @param execInsert callback to do insert of individual transfer
	 * @return (number of inserts done, error)
	 */
	def insert(execInsert: DoInsert = doInsert): Future[(Int, Option[String])] = {
		// First get transfer components
		getComponents().flatMap {
			case No(err) => Future.successful(0, Some(err))
			// Go do transfer
			case Yes((fromC, toC)) => transferComponents(fromC, toC, execInsert)
		}
	}

	/**
	 * Insert the transfer into the DB if components have already been found.
	 * This is often not a simple single insert into the DB because of "subcomponents".  Some components
	 * (in reality just non-BSP racks) are really just holders for subcomponents (e.g., tubes in racks).
	 * Transfers involving subcomponents must be broken down into multiple transfers to/from the subcomponents.
	 * @param fromC transfer source component
	 * @param toC transfer destination component
	 * @param execInsert callback to do insert of individual transfer
	 * @return (number of inserts done, error)
	 */
	def transferComponents(fromC: Component, toC: Component, execInsert: DoInsert = doInsert) = {
		/*
		 * Get method to get fetch subcomponents
		 * @param c component to get subcomponents for
		 * @return method to fetch subcomponents
		 */
		def getSubFetcher(c: Component) =
			c match {
				case sc: SubComponents => sc.getSubFetcher
				case _ => None
			}

		/*
		 * From and to components contain subcomponents.  Must become many subcomponent to subcomponent transfers.
		 * @param wellMapping mapping of transfers: each entry is a source well to one or more destination wells
		 * @param fromSC function to get subcomponents of transfer source
		 * @param toSC function to get subcomponents of transfer target
		 * @return results of DB inserts
		 */
		def subToSub(wellMapping: Map[String, List[String]],
					 fromSC: SubComponents#SubFetcher, toSC: SubComponents#SubFetcher)() = {
			// Start up futures to get position to subcomponent mappings
			val scList = List(fromSC(), toSC())
			// Make them a single future with a list of results
			Future.sequence(scList).map {
				// Should always be two entries back - subcomponents of from and to components
				case fromWells :: toWells :: _ =>
					// Gather errors - if any then report them and finish
					val errs = List(fromWells, toWells).flatMap(_.getNoOption)
					if (errs.nonEmpty)
						List(No(errs.mkString("; ")))
					else {
						// Get maps of subcomponentWell->subcomponent
						val fromWellsMap = fromWells.getYes
						val toWellsMap = toWells.getYes
						// Now we do all the inserts for subcomponent->subcomponent transfers
						// We base it on the from->to mappings (can be multiple well destinations
						// for one source well)
						wellMapping.flatMap {
							case (sourceWell, destWells) =>
								// Look for source well subcomponent
								fromWellsMap.get(sourceWell) match {
									// Found source well subcomponent
									case Some(fromSub) =>
										// flatMap to get transfers to destination well(s) found
										destWells.flatMap((dest) => {
											// Get destination well subcomponent
											toWellsMap.get(dest) match {
												// Found destination well subcomponent
												case Some(toSub) =>
													// Set insert started
													Some(Yes(
														Transfer(from = fromSub, to = toSub,
															fromQuad = None, toQuad = None,
															project = this.project, slice = None,
															cherries = None, free = None, isTubeToMany = false,
															isSampleOnly = false)))
												// Didn't find destination well subcomponent
												case None => None
											}
										})
									// Didn't find source well subcomponent - nothing to transfer
									case None => List.empty
								}
						}.toList
					}
				// Should never get here
				case _ => List(No("Error fetching subcomponents"))
			}
		}

		/*
		 * From a component with subcomponents to one without subcomponents.
		 * If an undivided destination component then subcomponent to undividedComponent transfers,
		 * otherwise subcomponent to many divided component well transfers using isTubeToMany.
		 * @param wellMapping mapping of transfers: each entry is a source well to one or more destination wells
		 * @param fromSC function to get subcomponents of transfer source
		 * @param toC transfer target component
		 * @return results of DB inserts
		 */
		def fromSub(wellMapping: Map[String, List[String]], fromSC: SubComponents#SubFetcher, toC: Component)() = {
			fromSC().map {
				case No(err) =>
					List(No(s"Error finding subcomponents of $from: $err"))
				case Yes(wells) =>
					// See if divided destination
					(getLayout(toC) match {
						// Make subcomponent to divided component well transfers
						// We base it on the from->to mappings
						case Some(div) =>
							wellMapping.flatMap {
								case (sourceWell, destWells) =>
									// Look for source well subcomponent
									wells.get(sourceWell) match {
										// Found source well subcomponent
										case Some(fromSub) =>
											Some(Yes(Transfer.fromTubeTransfer(
												tube = fromSub, plate = toC.id,
												wells = destWells, div = div, proj = project)
											))
										// Didn't find source well subcomponent - nothing to transfer
										case None => None
									}
							}
						// Make subcomponent to undivided component transfers
						// We base it on the from->to mappings which for undivided destination is
						// just wells pointing to themselves
						case None =>
							wellMapping.keys.flatMap((sourceWell) => {
								wells.get(sourceWell) match {
									case Some(fromSub) =>
										Some(Yes(Transfer(from = fromSub, to = to,
											fromQuad = None, toQuad = None, project = project,
											slice = None, cherries = None, free = None,
											isTubeToMany = false, isSampleOnly = false)))
									case None => None
								}
							})
					}).toList
			}
		}

		/*
		 * To a component with subcomponents from one without subcomponents
		 * If from a divided component then transfers of individual wells from the divided component to individual
		 * subcomponents.  If from an undivided component that becomes many undivided to subcomponent transfers.
		 * @param wellMapping mapping of transfers: each entry is a source well to one or more destination wells
		 * @param toSC function to get subcomponents of transfer target
		 * @param fromC component being transferred from
		 * @return results of DB inserts
		 */
		def toSub(wellMapping: Map[String, List[String]], toSC: SubComponents#SubFetcher, fromC: Component)() = {
			toSC().map {
				case No(err) =>
					List(No(s"Error finding subcomponents of $to: $err"))
				case Yes(toWells) =>
					// See if divided source
					getLayout(fromC) match {
						case Some(div) =>
							// Make divided component to subcomponent transfers
							// We base it on the from->to mappings making single well to subcomponent transfers
							wellMapping.keys.flatMap(
								(fromWell) => {
									// Get wells to transfer to
									val targetWells = wellMapping(fromWell)
									// Map those to wanted transfers (well from divided source to sub component)
									targetWells.flatMap((toSub) => {
										toWells.get(toSub) match {
											case Some(subC) =>
												Some(Yes(Transfer.toTubeTransfer(tube = subC, plate = from,
													wells = List(fromWell), div = div, proj = project)))
											case None => None
										}
									})
								}).toList
						case None =>
							// Make undivided to subcomponent transfer
							// We base it on the from->to mappings which for undivided source should be
							// single well pointing to destinations
							wellMapping.get(TransferContents.oneWell) match {
								case Some(targets) =>
									targets.flatMap(
										(targetWell) => {
											// Make tube-to-tube (undivided source to sub component) transfer
											toWells.get(targetWell) match {
												case Some(toSub) =>
													Some(Yes(Transfer(from = from, to = toSub,
														fromQuad = None, toQuad = None, project = project,
														slice = None, cherries = None, free = None,
														isTubeToMany = false, isSampleOnly = false)))
												case None => None
											}
										})
								case None =>
									List(No(s"Can not transfer from undivided $from to rack $to"))
							}
					}
			}
		}

		/*
		 * Simple transfer when neither source nor target have subcomponents.
		 * @return DB insert result
		 */
		def noSubs() = Future.successful(List(Yes(this)))

		/*
		 * Get function to create transfers.  Function returned is dependent on whether the source and/or target of the
		 * transfer has subcomponents.
		 * @param fromC transfer source component
		 * @param toC transfer target component
		 * @return function to create Transfers needed to complete transfer
		 */
		def getTransfers(fromC: Component, toC: Component) = {
			// Get well mapping of transfer between components
			// Each source well goes to one or more destination wells
			val wellMapping = getWellMapping(soFar = Map.empty[String, List[String]],
				fromComponent = fromC, toComponent = toC, getSameMapping = true)(
				makeOut = (_, _, found) => found)
			// Check out if source and/or target have subcomponents that can be fetched
			(getSubFetcher(fromC), getSubFetcher(toC)) match {
				// From and to components with subcomponents
				case (Some(fromSC), Some(toSC)) =>
					(subToSub(wellMapping, fromSC, toSC) _, isAdditionLegit _)
				// From a component with subcomponents to one without subcomponents
				case (Some(fromSC), None) =>
					(fromSub(wellMapping, fromSC, toC) _, isAdditionLegit _)
				// To a component with subcomponents from one without subcomponents
				case (None, Some(toSC)) =>
					(toSub(wellMapping, toSC, fromC) _, isAdditionLegit _)
				// Neither component has subcomponents - do simple transfer
				case _ => (noSubs _, isComponentAdditionLegit(_: Transfer, fromC, toC))
			}
		}

		/*
		 * Complete the inserts.  We turn the list of futures into a single future that returns a summary of
		 * the inserts done.
		 * @param inserts pending inserts
		 * @return (# inserts done, errors)
		 */
		def completeInserts(inserts: List[Future[Option[String]]]) = {
			// Convert list of futures for inserts into a one future list of inserts
			Future.sequence(inserts).map((done) => {
				// Get all errors found (flatten only gets back Options defined)
				val errs = done.flatten
				// Return what we've done
				(done.size - errs.size, if (errs.isEmpty) None else Some(errs.mkString("; ")))
			})
		}

		// Get proper method to create transfers and method to do verification that transfer legit
		val (getTrans, verify) = getTransfers(fromC, toC)
		// Go get transfers we need to do and process results
		getTrans().flatMap((trans) => {
			// Get errors
			val errs = trans.flatMap(_.getNoOption)
			// If errors collecting transfers then exit now with errors
			if (errs.nonEmpty)
				Future.successful(0, Some(errs.mkString("; ")))
			else {
				// Retrieve transfers
				val transfers = trans.flatMap(_.getYesOption)
				// Check if anything illegal
				val legit = transfers.map(verify)
				// Gather legality results and if all ok complete inserts
				Future.sequence(legit)
					.flatMap((isLegit) => {
						val legitErrs = isLegit.flatten
						if (legitErrs.nonEmpty)
							Future.successful(0, Some(legitErrs.mkString("; ")))
						else {
							val inserts = transfers.map(execInsert)
							completeInserts(inserts)
						}
					})
			}
		})
	}

	/**
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
	 * cherry picking or free, is a subset of a quadrant (for components that have quadrants) or an entire component
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
	 * @param makeOut callback to return result - called with (soFar, component, input->output wells picked)
	 * @param getSameMapping return mapping of wells if transfer of entire components with same division
	 * @tparam T type of parameter tracking results
	 * @return result of makeOut callback or original input (soFar) if complete component move and getSameMapping false
	 */
	def getWellMapping[T](soFar: T, fromComponent: Component, toComponent: Component, getSameMapping: Boolean)
						 (makeOut: (T, Division.Division, Map[String, List[String]]) => T): T = {
		// Make a one-to-one map into generic one-to-many
		def makeOneToMany(in: Map[String, String]) =
			in.map{
				case (key, value) => key -> List(value)
			}
		// Get destination component to look at divisions, unless we're coming from a tube to a divided compoment
		val divComponent = if (isTubeToMany) toComponent else fromComponent
		val trans =
			(fromQuad, toQuad, slice, cherries, free) match {
				// From a quadrant to an entire component - should be 384-well component to non-quadrant component
				case (Some(fromQ), None, None, _, _) =>
					Some(DIM16x24, makeOneToMany(TransferWells.qFrom384(fromQ)))
				// To a quadrant from an entire component - should be non-quadrant component to 384-well component
				// Make sure we set component division to dimensions of destination if coming from tube
				case (None, Some(toQ), None, _, _) =>
					getLayout(divComponent).map((_, makeOneToMany(TransferWells.qTo384(toQ))))
				// Slice of quadrant to an entire component - should be 384-well component to 96-well component
				case (Some(fromQ), None, Some(qSlice), cher, _) =>
					Some(DIM16x24,
						makeOneToMany(TransferWells.slice384to96wells(quad = fromQ, slice = qSlice, cherries = cher)))
				// Quadrant to quadrant - must be 384-well component to 384-well component
				case (Some(fromQ), Some(toQ), None, _, _) =>
					Some(DIM16x24, makeOneToMany(TransferWells.q384to384map(fromQ, toQ)))
				// Quadrant to quadrant with slice - must be 384-well component to 384-well component
				case (Some(fromQ), Some(toQ), Some(qSlice), cher, _) =>
					Some(DIM16x24,
						makeOneToMany(TransferWells.slice384to384wells(
							fromQ = fromQ, toQ = toQ, slice = qSlice, cherries = cher)
						))
				// Slice of non-quadrant component to quadrant - Must be 96-well component or tube to 384-well component
				case (None, Some(toQ), Some(qSlice), cher, _) =>
					getLayout(divComponent).map((_,
						makeOneToMany(TransferWells.slice96to384wells(quad = toQ, slice = qSlice, cherries = cher))
					))
				// Slice without quadrant - see if free for all
				case (None, None, Some(qSlice), cher, fr) =>
					fr match {
						// Free for all
						case Some(freeList) =>
							// Make a map of named wells from a free list of indexed wells
							def makeEntry(in: Transfer.FreeList,
										  inToWell: (Int) => String, outToWell: (Int) => String) = {
								in.map {
									case (inWell, outList) =>
										inToWell(inWell) -> outList.map(outToWell)
								}.toMap
							}
							(getLayout(fromComponent), getLayout(toComponent)) match {
								case (Some(DIM8x12), Some(DIM8x12)) =>
									Some(DIM8x12, makeEntry(freeList,
										TransferWells.make96WellStrFromIndex,
										TransferWells.make96WellStrFromIndex
									))
								case (Some(DIM8x12), Some(DIM16x24)) =>
									Some(DIM8x12, makeEntry(freeList,
										TransferWells.make96WellStrFromIndex,
										TransferWells.make384WellStrFromIndex
									))
								case (Some(DIM16x24), Some(DIM8x12)) =>
									Some(DIM16x24, makeEntry(freeList,
										TransferWells.make384WellStrFromIndex,
										TransferWells.make96WellStrFromIndex
									))
								case (Some(DIM16x24), Some(DIM16x24)) =>
									Some(DIM16x24, makeEntry(freeList,
										TransferWells.make384WellStrFromIndex,
										TransferWells.make384WellStrFromIndex
									))
								case _ =>
									None
							}
						// Either a 96-well component (non-quadrant transfer)
						// or a straight cherry picked 384-well component (no quadrants involved)
						// or a tube to a divided component
						case None =>
							getLayout(divComponent) match {
								case Some(DIM8x12) =>
									Some(DIM8x12,
										makeOneToMany(TransferWells.slice96to96wells(slice = qSlice, cherries = cher)))
								case Some(DIM16x24) =>
									Some(DIM16x24,
										makeOneToMany(
											TransferWells.slice384to384wells(slice = qSlice, cherries = cher)
										)
									)
								case _ => None
							}
					}
				// Transfer to entire divided component either from/to a tube or between same size components
				case (None, None, None, _, _) if isTubeToMany || getSameMapping =>
					getLayout(divComponent) match {
						case Some(DIM8x12) => Some(DIM8x12, makeOneToMany(TransferWells.entire96to96wells))
						case Some(DIM16x24) => Some(DIM16x24, makeOneToMany(TransferWells.entire384to384wells))
						case _ => None
					}
				case _ =>
					None
			}
		// Callback if anything found to get new results
		trans match {
			case Some(tr) =>
				// Make into list of wells transferred to
				val wells =
					if (isTubeToMany)
						Map(TransferContents.oneWell -> tr._2.values.flatten.toList)
					else
						tr._2
				makeOut(soFar, tr._1, wells)
			case None => soFar
		}
	}

}

/**
 * Junior class to Transfer to get initial data for transfer.  This is just used in forms.
 * @param from ID we're transferring from
 * @param to ID we're transferring to
 * @param project optional project transfer is associated with
 */
case class TransferStart(from: String, to: String, project: Option[String]) {
	def toTransferForm = TransferForm(toTransfer, dataMandatory = false,
		isQuadToQuad = false, isQuadToTube = false, isTubeToQuad = false, canBeSampleOnly = false)
	def toTransfer = Transfer(from, to, None, None, project, None, None, None, isTubeToMany = false, isSampleOnly = false)
}

/**
 * Transfer with UI parameters
 * @param dataMandatory true if quadrant information must be specified (non-quadrant component to/from quadrant component)
 * @param isQuadToQuad true if quadrant transfers must be quad to quad (both source and destination have quadrants)
 * @param isQuadToTube true if transfer from a source with quadrants to a non-divided component
 * @param isTubeToQuad true if slice transfer must have to quadrant
 * @param canBeSampleOnly true if plate to tube transfer where only wells with samples can be transferred
 */
case class TransferForm(transfer: Transfer, dataMandatory: Boolean, isQuadToQuad: Boolean,
						isQuadToTube: Boolean, isTubeToQuad: Boolean, canBeSampleOnly: Boolean)

// Companion object
object Transfer {
	/**
	 * Get a list of enum string values sorted by the order of enum values
	 * @param enums enumeration to be sorted
	 * @tparam E enumeration type
	 * @return list of enum string values, sorted by enumeration value, of all enum values
	 */
	private def enumSortedList[E <: Enumeration](enums: E) =
		enums.values.toList.sortWith(_ < _).map(_.toString)

	// Quadrant enumeration
	object Quad extends Enumeration {
		type Quad = Value
		val Q1 = Value("1st quadrant")
		val Q2 = Value("2nd quadrant")
		val Q3 = Value("3rd quadrant")
		val Q4 = Value("4th quadrant")
		/**
		 * Get a short string description for a quadrant
		 * @param q quadrant
		 * @return short description of quadrant
		 */
		def shortStr(q: Quad): String =
			q match {
				case Q1 => "Q1"
				case Q2 => "Q2"
				case Q3 => "Q3"
				case Q4 => "Q4"
			}
	}

	// String values for dropdown lists etc.
	val quadVals: List[String] = enumSortedList(Quad)

	// Slice enumeration
	object Slice extends Enumeration {
		type Slice = Value
		val S1 = Value("1-3 x A-H")
		val S2 = Value("4-6 x A-H")
		val S3 = Value("7-9 x A-H")
		val S4 = Value("10-12 x A-H")
		val S5 = Value("1-6 x A-H")
		val S6 = Value("7-12 x A-H")
		val CP = Value("Cherry Pick Wells")
		val FREE = Value("File Input")
	}

	// String values for dropdown lists etc.
	val sliceVals: List[String] = enumSortedList(Slice).filterNot(_ == Slice.FREE.toString)

	// Keys for form
	val fromKey = "from"
	val toKey = "to"
	val projectKey = "project"

	// Form to create TransferStart objects
	val startForm = Form(
		mapping(
			fromKey -> nonEmptyText,
			toKey -> nonEmptyText,
			projectKey -> optional(text)
		)(TransferStart.apply)(TransferStart.unapply))
	// Formatter for going to/from and validating Json
	implicit val transferStartFormat: Format[TransferStart] = Json.format[TransferStart]

	// Element in free-for-all list (source well -> destination well(s))
	type FreeElement = (Int, List[Int])
	// Free-for-all list
	type FreeList = List[FreeElement]

	// JSON formatting of free list
	implicit object FreeFormat extends Format[FreeList] {
		/**
		 * Convert JSON to free list
		 * @param json json containing free list
		 * @return free list of sourceWell->destinationWells
		 */
		def reads(json: JsValue) : JsResult[FreeList] = {
			val noSource = -1 // Index to flag there's no source
			// Get free list of (sourceWell -> listDestinationWells)
			val out =
				json match {
					case JsArray(eles) =>
						// Go through entries in list
						eles.map {
							case o: JsObject =>
								// Should only be item in object - sourceWell -> arrayOfDestinationWells
								o.fields.headOption match {
									case Some((key: String, jVal)) =>
										jVal match {
											case JsArray(toEles) =>
												// Convert destinatino wells to a list of integers
												val eleList = toEles.flatMap {
													case i: JsNumber => Some(i.value.toInt)
													case _ => None
												}.toList
												// Point source well to destination wells
												key.toInt -> eleList
											case _ => key.toInt -> List.empty[Int]
										}
									case _ => noSource -> List.empty[Int]
								}
							case _ => noSource -> List.empty[Int]
						}
					case _ => List.empty[FreeElement]
				}
			// Return list filtering out those with no source or destination wells
			JsSuccess(
				out.filter {
					case (key, entries) => key != noSource && entries.nonEmpty
				}.toList)
		}

		/**
		 * Convert a free list to JSON
		 * @param freeList free list
		 * @return
		 */
		def writes(freeList: FreeList): JsValue = {
			// Get array of source->destination(s)
			val jsAry =
				freeList.map {
					case (key, toVals) =>
						JsObject(Seq(key.toString -> JsArray(toVals.map((i) => JsNumber(i)))))
				}
			JsArray(jsAry)
		}
	}

	// Keys for form
	val fromQuadKey = "fromQuad"
	val toQuadKey = "toQuad"
	val sliceKey = "slice"
	val cherriesKey = "cherries"
	val freeKey = "free"
	val isTubeToManyKey = "isTubeToMany"
	val isSampleOnlyKey = "isSampleOnly"

	// Mapping to create/read Transfer objects
	val transferMapping: Mapping[Transfer] = mapping(
		fromKey -> nonEmptyText,
		toKey -> nonEmptyText,
		fromQuadKey -> optional(enum(Quad)),
		toQuadKey -> optional(enum(Quad)),
		projectKey -> optional(text),
		sliceKey -> optional(enum(Slice)),
		cherriesKey -> optional(list(number)),
		freeKey -> ignored(None: Option[FreeList]),
		isTubeToManyKey -> boolean,
		isSampleOnlyKey -> boolean
	)(Transfer.apply)(Transfer.unapply)

	// Keys for form
	val transferKey = "transfer"
	val mandatoryKey = "dataMandatory"
	val isQuadToQuadKey = "isQuadToQuad"
	val isQuadToTubeKey = "isQuadToTube"
	val isTubeToQuadKey = "isTubeToQuad"
	val canBeSampleOnlyKey = "canBeSampleOnly"

	// Mapping to create/read Transfer form
	val transferFormMapping: Mapping[TransferForm] = mapping(
		transferKey -> transferMapping,
		mandatoryKey -> boolean,
		isQuadToQuadKey -> boolean,
		isQuadToTubeKey -> boolean,
		isTubeToQuadKey -> boolean,
		canBeSampleOnlyKey -> boolean
	)(TransferForm.apply)(TransferForm.unapply)

	// Form used for transferring - it includes verification to see if proper set of quadrants are set.  If a
	// transfer is being done between two components with quadrants (i.e., 384-well component to 384-well component)
	// isQuadToQuad is true and then if neither the entire component is being transferred nor is there cherry-picking
	// then both quadrants must be specified.  If a transfer between a source with quadrants and a non-divided target
	// then isQuadToTube is true and a source quadrant must be set if slicing.
	val form = Form(transferFormMapping
		verifying ("Quadrant(s) must be specified", f =>
		(!f.isQuadToQuad && !f.isQuadToTube && !f.isTubeToQuad) ||
			((f.transfer.slice.isEmpty || f.transfer.slice.get == Slice.CP) &&
				f.transfer.fromQuad.isEmpty && f.transfer.toQuad.isEmpty) ||
			(f.isQuadToQuad && f.transfer.fromQuad.isDefined && f.transfer.toQuad.isDefined) ||
			(f.isQuadToTube && f.transfer.fromQuad.isDefined) ||
			(f.isTubeToQuad && f.transfer.toQuad.isDefined)
	))

	// Same form but without verification - this is used to get data from form in case verification failed with regular
	// form with verification
	val formWithoutVerify = Form(transferFormMapping)

	// Form with everything setup to do cherry picking
	val formForCherryPicking = Form(transferMapping)

	// Formatter for going to/from and validating Json
	// Supply our custom enum Reader and Writer for content type enum
	implicit val quadFormat: Format[Quad.Quad] = enumFormat(Quad)
	implicit val sliceFormat: Format[Slice.Slice] = enumFormat(Slice)
	implicit val transferFormat: Format[Transfer] = Json.format[Transfer]
	implicit val transferFormFormat: Format[TransferForm] = Json.format[TransferForm]

	/**
	 * Get a set of wells as indicies for cherry picking
	 * @param div plate division type
	 * @param wells wells in plate tube is going to
	 * @return
	 */
	private def getCherryIndicies(div: ContainerDivisions.Division.Division, wells: List[String]) =
		wells.map((pos) => {
			div match {
				case ContainerDivisions.Division.DIM8x12 => TransferWells.make96IdxFromWellStr(pos)
				case ContainerDivisions.Division.DIM16x24 => TransferWells.make384IdxFromWellStr(pos)
			}
		})


	/**
	 * Create a transfer from a tube to a set of wells on a plate.  Note: can't be apply because Json.format macro gets
	 * confused when there are multiple applys.
	 * @param tube tube source for transfer
	 * @param plate destination plate for transfer
	 * @param wells wells we are transferring tube into
	 * @param div division type for plate
	 * @param proj optional project to associate with transfer
	 * @return transfer setup for tube to destination wells
	 */
	def fromTubeTransfer(tube: String, plate: String, wells: List[String],
						 div: ContainerDivisions.Division.Division, proj: Option[String]): Transfer = {
		// Get wells as indicies
		val wellIdxs = getCherryIndicies(div, wells)
		// Create transfer from tube to wells in plate
		Transfer(from = tube, to = plate, fromQuad = None, toQuad = None, project = proj,
			slice = Some(Transfer.Slice.CP), cherries = Some(wellIdxs), free = None,
			isTubeToMany = true, isSampleOnly = false)
	}

	/**
	 * Create a transfer to a tube from wells on a plate.
	 * @param tube tube destination for transfer
	 * @param plate plate source for transfer
	 * @param wells wells we are transferring from plate
	 * @param div division type for plate
	 * @param proj optional project to associate with transfer
	 * @return transfer setup for source wells on plate to tube
	 */
	def toTubeTransfer(tube: String, plate: String, wells: List[String],
					   div: ContainerDivisions.Division.Division, proj: Option[String]): Transfer = {
		// Get wells as indicies
		val wellIdxs = getCherryIndicies(div, wells)
		// Create transfer to tube from wells in plate
		Transfer(from = plate, to = tube, fromQuad = None, toQuad = None, project = proj,
			slice = Some(Slice.CP), cherries = Some(wellIdxs), free = None,
			isTubeToMany = false, isSampleOnly = false)
	}

	/**
	 * Is addition of this transfer legitimate
	 * @return nothing if all ok otherwise error message
	 */
	private def isAdditionLegit(transfer: Transfer): Future[Option[String]] = {
		transfer.getComponents.flatMap {
			case Yes((from, to)) => isComponentAdditionLegit(transfer, from, to)
			case No(err) => Future.successful(Some(err))
		}
	}

	/**
	 * Is adding a tranfer legit.  It must not create a cyclic graph and if a project is specified it must be
	 * in one or more of the component(s) leading into (and including) the graph's component.  Note, this checks
	 * only the top components of the transfer, not the subcomponents.
	 * @param transfer transfer to check out
	 * @param fromC source component of transfer
	 * @param toC destination component of transfer
	 * @return nothing if all fine or error message
	 */
	private def isComponentAdditionLegit(transfer: Transfer,
										 fromC: Component, toC: Component): Future[Option[String]] = {
		(fromC, toC) match {
			// Transfers into BSP rack not allowed
			case (_, toC: Rack)
				if toC.initialContent.contains(ContentType.BSPtubes) =>
				Future.successful(Some(s"Transfer into BSP rack not allowed (rack ${transfer.to})"))
			// Transfers into sample plate not allowed
			case (_, toC: Plate)
				if toC.initialContent.contains(ContentType.SamplePlate) ||
					toC.initialContent.contains(ContentType.AnonymousSamplePlate) =>
				Future.successful(Some(s"Transfer into sample plate not allowed (plate ${transfer.to})"))
			case _ =>
				if (transfer.from == transfer.to)
					Future.successful(Some(s"Transfer to self (${transfer.from}) not allowed"))
				else
					TransferHistory.makeSourceGraph(transfer.from).map {
						(graph) =>
							(
								TransferHistory.isGraphAdditionCyclicErr(tran = transfer, graph = graph),
								TransferHistory.isGraphAdditionProjectErr(tran = transfer, from = fromC, graph = graph)
							) match {
								case (None, None) => None
								case (Some(err0), Some(err1)) => Some(err0 + "; " + err1)
								case (s: Some[String], _) => s
								case (_, s: Some[String]) => s
								case _ => Some(s"Transfer from ${transfer.from} to ${transfer.to} not legit")
							}
					}
		}
	}

	/**
	 * Get counts for a list of transfers. Returns counts:
	 * # whole components transferred, # quadrants,
	 * # cherry picks, # cherry picked wells, # frees, # free input wells, # free output wells
	 * @param transfers transfer list
	 * @return tuple of counts
	 */
	def getTransferCounts(transfers: List[Transfer]): (Int, Int, Int, Int, Int, Int, Int, Int, Int) = {
		transfers.foldLeft((0, 0, 0, 0, 0, 0, 0, 0 ,0)) {
			case ((whole, quads, slices, sliceWells, chers, cherWells, frees, inWells, outWells), next) =>
				next.slice match {
					case Some(Slice.FREE) if next.free.isDefined =>
						val freeList = next.free.get
						(whole, quads, slices, sliceWells, chers, cherWells, frees + 1, inWells + freeList.size,
							outWells + freeList.foldLeft(0) {
								case (soFar, (_, outs)) =>
									soFar + outs.size
							}
						)
					case Some(Slice.CP) if next.cherries.isDefined =>
						val cherries = next.cherries.get
						(whole, quads, slices, sliceWells, chers + 1, cherWells + cherries.size, frees, inWells, outWells)
					case Some(Slice.S1) | Some(Slice.S2) | Some(Slice.S3) | Some(Slice.S4) =>
						(whole, quads, slices + 1, sliceWells + 48, chers, cherWells, frees, inWells, outWells)
					case Some(Slice.S5) | Some(Slice.S6) =>
						(whole, quads, slices + 1, sliceWells + 24, chers, cherWells, frees, inWells, outWells)
					case _ =>
						if (next.fromQuad.isDefined || next.toQuad.isDefined)
							(whole, quads + 1, slices, sliceWells, chers, cherWells, frees, inWells, outWells)
						else
							(whole + 1, quads, slices, sliceWells, chers, cherWells, frees, inWells, outWells)
				}
		}
	}

	/**
	 * BSON reader/writers for Enums - will signal if errors - handlers should handle that
	 */
	implicit object BSONQuadHandler extends BSONHandler[BSONString, Quad.Quad] {
		def read(doc: BSONString): Quad.Quad = Quad.withName(doc.value)
		def write(quad: Quad.Quad): BSONString = BSON.write(quad.toString)
	}
	implicit object BSONSliceHandler extends BSONHandler[BSONString, Slice.Slice] {
		def read(doc: BSONString):Slice.Slice = Slice.withName(doc.value)
		def write(slice: Slice.Slice): BSONString = BSON.write(slice.toString)
	}

	/**
	 * BSON handler for transfer free list used to map well to one or more wells: List[(Int, List[Int])]
	 */
	implicit object BSONFreeHandler extends BSONHandler[BSONArray, Transfer.FreeList] {
		/**
		 * Convert BSON to free list
		 * @param doc bson containing free list
		 * @return free list of sourceWell->destinationWells
		 */
		def read(doc: BSONArray): Transfer.FreeList =
			doc.values.map {
				case d: BSONDocument =>
					// Get one item in document (src -> destination(s)
					val (key, value) = d.elements.head
					value match {
						case arr : BSONArray =>
							// Convert to srcWellInt -> list of destinationWells
							val intArray =
								arr.values.map {
									case n: BSONInteger =>
										n.value
								}.toList
							key.toInt -> intArray
					}
			}.toList

		/**
		 * Convert a free list to BSON
		 * @param slice free list
		 * @return BSON array with elements containing srcWell->destinationWells
		 */
		def write(slice: Transfer.FreeList): BSONArray =
			BSONArray(
				slice.map {
					case (key, value) =>
						BSONDocument(key.toString -> BSONArray(value.map(BSONInteger)))
				}
			)
	}

	/**
	 * Handler to convert between Transfer and BSON
	 */
	implicit val transferBSON:
		BSONDocumentReader[Transfer] with BSONDocumentWriter[Transfer] with BSONHandler[BSONDocument, Transfer] =
		Macros.handler[Transfer]
}

/**
 * Transfer with time added
 * @param from ID we're transferring from
 * @param to ID we're transferring to
 * @param fromQuad optional quadrant transfer is coming from
 * @param toQuad optional quadrant transfer is going to
 * @param slice optional slice to transfer
 * @param time time transfer was created
 */
class TransferWithTime(override val from: String, override val to: String,
					   override val fromQuad: Option[Transfer.Quad.Quad],
					   override val toQuad: Option[Transfer.Quad.Quad],
					   override val project: Option[String], override val slice: Option[Transfer.Slice.Slice],
					   override val cherries: Option[List[Int]], override val free: Option[List[(Int, List[Int])]],
					   override val isTubeToMany: Boolean, override val isSampleOnly: Boolean,
					   val time: Long)
	extends Transfer(from, to, fromQuad, toQuad, project, slice, cherries, free, isTubeToMany, isSampleOnly)

/**
 * Companion object
 */
object TransferWithTime {
	/**
	 * Create a transfer with time
	 * @param transfer transfer done
	 * @param time time transfer was done
	 * @return object with transfer data and time of transfer
	 */
	def apply(transfer: Transfer, time: Long) : TransferWithTime =
		new TransferWithTime(transfer.from, transfer.to, transfer.fromQuad, transfer.toQuad,
			transfer.project, transfer.slice, transfer.cherries, transfer.free, transfer.isTubeToMany,
			transfer.isSampleOnly, time)
}
