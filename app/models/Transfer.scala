package models

import formats.CustomFormats._
import mappings.CustomMappings._
import models.ContainerDivisions.Division
import models.ContainerDivisions.Division._
import models.db.{TransferCollection, TrackerCollection}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import utils.{Yes, No}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

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
 * @param cherries wells cherry picked (indicies to wells going across first)
 * @param isTubeToMany true if transferring tube to a multi-welled container
 */
case class Transfer(from: String, to: String,
					fromQuad: Option[Transfer.Quad.Quad], toQuad: Option[Transfer.Quad.Quad], project: Option[String],
					slice: Option[Transfer.Slice.Slice], cherries: Option[List[Int]], isTubeToMany: Boolean) {

	import models.Transfer.Quad._
	import models.Transfer.Slice._
	/**
	 * Make a description of the transfer, including quadrant descriptions.
	 * @return description of transfer, including quadrant information
	 */
	def quadDesc = {
		def qDesc(id: String, quad: Option[Quad], slice: Option[Slice]) = {
			val sliceStr = slice.map((s) => {
				val head = if (s != CP) s"slice $s" else "cherry picked wells"
				s"$head of "
			}).getOrElse("")
			sliceStr + quad.map((q) => s"$q of $id").getOrElse(id)
		}
		val (fromSlice, toSlice) = if (isTubeToMany) (None, slice) else (slice, None)
		"transfer from " + qDesc(from, fromQuad, fromSlice) + " to " + qDesc(to, toQuad, toSlice)
	}


	/**
	 * Do simple insert of transfer object
	 * @param t transfer to insert
	 * @return optional error
	 */
	private def doInsert(t: Transfer) = TransferCollection.insert(t)

	/**
	 * Type for inserting a transfer into the DB - if there's an error a string is returned
	 */
	private type DoInsert = (Transfer) => Future[Option[String]]

	/**
	 * Insert the transfer into the DB.  This is often not a simple single insert into the DB because of
	 * "subcomponents".  Some components (in reality just non-BSP racks) are really just holders for subcomponents
	 * (e.g., tubes in racks).  Transfers involving subcomponents must be broken down into multiple transfers to/from
	 * the subcomponents.  That's what we do here.
	 * @param execInsert callback to do insert of individual transfer
	 * @return (number of inserts done, error)
	 */
	def insert(execInsert: DoInsert = doInsert) = {
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
		 * @param doInsert callback to do individual DB inserts
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
					val errs = List(fromWells.getNoOption, toWells.getNoOption).flatMap((s) => s)
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
															cherries = None, isTubeToMany = false)))
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
		 * @param doInsert callback to do individual DB inserts
		 * @return results of DB inserts
		 */
		def fromSub(wellMapping: Map[String, List[String]], fromSC: SubComponents#SubFetcher, toC: Component)() = {
			fromSC().map {
				case No(err) =>
					List(No(s"Error finding subcomponents of $from"))
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
											slice = None, cherries = None, isTubeToMany = false)))
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
		 * @param doInsert callback to do individual DB inserts
		 * @return results of DB inserts
		 */
		def toSub(wellMapping: Map[String, List[String]], toSC: SubComponents#SubFetcher, fromC: Component)() = {
			toSC().map {
				case No(err) =>
					List(No(s"Error finding subcomponents of $to"))
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
														slice = None, cherries = None, isTubeToMany = false)))
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
		 * @param doInsert callback to DB insert
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
					subToSub(wellMapping, fromSC, toSC) _
				// From a component with subcomponents to one without subcomponents
				case (Some(fromSC), None) =>
					fromSub(wellMapping, fromSC, toC) _
				// To a component with subcomponents from one without subcomponents
				case (None, Some(toSC)) =>
					toSub(wellMapping, toSC, fromC) _
				// Neither component has subcomponents - do simple transfer
				case _ => noSubs _
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
				// Get all errors found (flatMap only gets back Options defined)
				val errs = done.flatMap((err) => err)
				// Return what we've done
				(done.size - errs.size, if (errs.isEmpty) None else Some(errs.mkString("; ")))
			})
		}

		// First get transfer source and target components
		TrackerCollection.findIds(List(from, to)).flatMap((components) => {
			// Get objects from bson
			val contents = ComponentFromJson.bsonToComponents(components)
			val findFromC = contents.find(_.id == from)
			val findToC = contents.find(_.id == to)
			// Check out that we were able to retrieve both items
			(findFromC, findToC) match {
				// Got them both
				case (Some(fromC), Some(toC)) =>
					// Get proper method to create transfers
					val getTrans = getTransfers(fromC, toC)
					// Go get transfers we need to do and process results
					getTrans().flatMap((trans) => {
						// Get errors
						val errs = trans.flatMap(_.getNoOption)
						// If errors collecting transfers then exit now with errors
						if (errs.nonEmpty) Future.successful(0, Some(errs.mkString("; ")))
						// Otherwise go complete the inserts
						else
							completeInserts(trans.map(_.getYes).map(doInsert))
					})
				// Cases where one or more of the components can't be found
				case (Some(_), None) => Future.successful(0, Some(s"Component $to not found"))
				case (None, Some(_)) => Future.successful(0, Some(s"Component $from not found"))
				case _ => Future.successful(0, Some(s"Components $from and $to not found"))
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
	 * @param makeOut callback to return result - called with (soFar, component, input->output wells picked)
	 * @param getSameMapping return mapping of wells if transfer of entire components with same division
	 * @tparam T type of parameter tracking results
	 * @return result of makeOut callback or original input (soFar) if complete component move and getSameMapping false
	 */
	def getWellMapping[T](soFar: T, fromComponent: Component, toComponent: Component, getSameMapping: Boolean)
						 (makeOut: (T, Division.Division, Map[String, List[String]]) => T) = {
		// Get destination component to look at divisions, unless we're coming from a tube to a divided compoment
		val divComponent = if (isTubeToMany) toComponent else fromComponent
		val trans =
			(fromQuad, toQuad, slice, cherries) match {
				// From a quadrant to an entire component - should be 384-well component to non-quadrant component
				case (Some(fromQ), None, None, _) => Some(DIM16x24, TransferWells.qFrom384(fromQ))
				// To a quadrant from an entire component - should be non-quadrant component to 384-well component
				case (None, Some(toQ), None, _) => Some(DIM8x12, TransferWells.qTo384(toQ))
				// Slice of quadrant to an entire component - should be 384-well component to 96-well component
				case (Some(fromQ), None, Some(qSlice), cher) =>
					Some(DIM16x24, TransferWells.slice384to96wells(quad = fromQ, slice = qSlice, cherries = cher))
				// Quadrant to quadrant - must be 384-well component to 384-well component
				case (Some(fromQ), Some(toQ), None, _) =>
					Some(DIM16x24, TransferWells.q384to384map(fromQ, toQ))
				// Quadrant to quadrant with slice - must be 384-well component to 384-well component
				case (Some(fromQ), Some(toQ), Some(qSlice), cher) =>
					Some(DIM16x24, TransferWells.slice384to384wells(fromQ = fromQ, toQ = toQ,
						slice = qSlice, cherries = cher))
				// Slice of non-quadrant component to quadrant - Must be 96-well component to 384-well component
				case (None, Some(toQ), Some(qSlice), cher) =>
					Some(DIM8x12, TransferWells.slice96to384wells(quad = toQ, slice = qSlice, cherries = cher))
				// Either a 96-well component (non-quadrant transfer)
				// or a straight cherry picked 384-well component (no quadrants involved)
				// or a tube to a divided component
				case (None, None, Some(qSlice), cher) =>
					getLayout(divComponent) match {
						case Some(DIM8x12) =>
							Some(DIM8x12, TransferWells.slice96to96wells(slice = qSlice, cherries = cher))
						case Some(DIM16x24) =>
							Some(DIM16x24, TransferWells.slice384to384wells(slice = qSlice, cherries = cher))
						case _ => None
					}
				// Transfer to entire divided component either from/to a tube or between same size components
				case (None, None, None, _) if isTubeToMany || getSameMapping =>
					getLayout(divComponent) match {
						case Some(DIM8x12) => Some(DIM8x12, TransferWells.entire96to96wells)
						case Some(DIM16x24) => Some(DIM16x24, TransferWells.entire384to384wells)
						case _ => None
					}
				case _ =>
					None
			}
		// Callback if anything found to get new results
		trans match {
			case Some(tr) =>
				// Make into list of wells transferred to - only single destination wells unless from tube
				val wells =
					if (isTubeToMany)
						Map(TransferContents.oneWell -> tr._2.values.toList)
					else
						tr._2.map { case (k, v) => k -> List(v) }
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
		isQuadToQuad = false, isQuadToTube = false, isTubeToQuad = false)
	def toTransfer = Transfer(from, to, None, None, project, None, None, isTubeToMany = false)
}

/**
 * Transfer with UI parameters
 * @param dataMandatory true if quadrant information must be specified (non-quadrant component to/from quadrant component)
 * @param isQuadToQuad true if quadrant transfers must be quad to quad (both source and destination have quadrants)
 * @param isQuadToTube true if transfer from a source with quadrants to a non-divided component
 * @param isTubeToQuad true if slice transfer must have to quadrant
 */
case class TransferForm(transfer: Transfer, dataMandatory: Boolean, isQuadToQuad: Boolean,
						isQuadToTube: Boolean, isTubeToQuad: Boolean)

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
	}

	// String values for dropdown lists etc.
	val quadVals = enumSortedList(Quad)

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
	}

	// String values for dropdown lists etc.
	val sliceVals = enumSortedList(Slice)

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
	implicit val transferStartFormat = Json.format[TransferStart]

	// Keys for form
	val fromQuadKey = "fromQuad"
	val toQuadKey = "toQuad"
	val sliceKey = "slice"
	val cherriesKey = "cherries"
	val isTubeToManyKey = "isTubeToMany"

	// Mapping to create/read Transfer objects
	val transferMapping = mapping(
		fromKey -> nonEmptyText,
		toKey -> nonEmptyText,
		fromQuadKey -> optional(enum(Quad)),
		toQuadKey -> optional(enum(Quad)),
		projectKey -> optional(text),
		sliceKey -> optional(enum(Slice)),
		cherriesKey -> optional(list(number)),
		isTubeToManyKey -> boolean
	)(Transfer.apply)(Transfer.unapply)

	// Keys for form
	val transferKey = "transfer"
	val mandatoryKey = "dataMandatory"
	val isQuadToQuadKey = "isQuadToQuad"
	val isQuadToTubeKey = "isQuadToTube"
	val isTubeToQuadKey = "isTubeToQuad"

	// Mapping to create/read Transfer form
	val transferFormMapping = mapping(
		transferKey -> transferMapping,
		mandatoryKey -> boolean,
		isQuadToQuadKey -> boolean,
		isQuadToTubeKey -> boolean,
		isTubeToQuadKey -> boolean
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
	implicit val transferFormat = Json.format[Transfer]
	implicit val transferFormFormat = Json.format[TransferForm]

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
			slice = Some(Transfer.Slice.CP), cherries = Some(wellIdxs), isTubeToMany = true)
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
			slice = Some(Transfer.Slice.CP), cherries = Some(wellIdxs), isTubeToMany = false)
	}
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
					   override val cherries: Option[List[Int]],
					   override val isTubeToMany: Boolean,
					   val time: Long)
	extends Transfer(from, to, fromQuad, toQuad, project, slice, cherries, isTubeToMany)

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
			transfer.project, transfer.slice, transfer.cherries, transfer.isTubeToMany, time)
}
