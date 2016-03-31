package models

import formats.CustomFormats._
import mappings.CustomMappings._
import models.ContainerDivisions.Division
import models.ContainerDivisions.Division._
import models.db.{TransferCollection, TrackerCollection}
import models.initialContents.InitialContents.ContentType
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
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


	def insert = {
		TrackerCollection.findIds(List(from, to)).flatMap((components) => {
			def isSubEligible(c: Component) =
				c match {
					case ct: Container
						if ct.initialContent.isDefined && ct.initialContent.get == ContentType.BSPtubes => false
					case _ => true
				}

			def getSubFetcher(c: Component) =
				c match {
					case sc: SubComponents => sc.getSubFetcher
					case _ => None
				}

			def doInsert(t: Transfer) = {
				TransferCollection.insert(t).map {
					(lastError) => (1, None)
				}.recover {
					case err => (0, Some(err.getLocalizedMessage))
				}
			}

			// Get objects from bson
			val contents = ComponentFromJson.bsonToComponents(components)
			val findFromC = contents.find(_.id == from)
			val findToC = contents.find(_.id == to)
			(findFromC, findToC) match {
				case (Some(fromC), Some(toC)) =>
					if (isSubEligible(fromC) && isSubEligible(toC)) {
						val wellMapping = getWellMapping(soFar = Map.empty[String, List[String]],
							fromComponent = fromC, toComponent = toC, getSameMapping = true)(
							makeOut = (_, _, found) => found)
						(getSubFetcher(fromC), getSubFetcher(toC)) match {
							// From and to a rack must become many tube to tube transfers
							case (Some(fromSC), Some(toSC)) =>
								val scList = List(fromSC(), toSC())
								Future.sequence(scList).flatMap {
									case fromWells :: toWells :: _ =>
										val errs = List(fromWells._2, toWells._2).flatMap((s) => s)
										if (errs.nonEmpty)
											Future.successful(0, Some(errs.mkString("; ")))
										else {
											val fromWellsMap = fromWells._1
											val toWellsMap = toWells._1
											val inserts = wellMapping.flatMap {
												case (sourceWell, destWells) =>
													fromWellsMap.get(sourceWell) match {
														case Some(fromSubC) =>
															destWells.flatMap((dest) => {
																toWellsMap.get(dest) match {
																	case Some(toSubC) =>
																		Some(doInsert(
																			Transfer(from = fromSubC, to = toSubC,
																				fromQuad = None, toQuad = None,
																				project = this.project, slice = None,
																				cherries = None, isTubeToMany = false)))
																	case None => None
																}
															})
														case None => List.empty
													}
											}
											Future.sequence(inserts.toList).map((done) => {
												val totalInserts = done.foldLeft(0)(_ + _._1)
												val errs = done.flatMap(_._2)
												(totalInserts, if (errs.isEmpty) None else Some(errs.mkString("; ")))
											})
										}
								}
							// From a rack - if to an undivided component then lots of tube-to-tube transfers,
							// otherwise lots of tube-to-many wells on a plate transfers using isTubeToMany
							case (Some(fromSC), None) =>
								fromSC().map {
									case ((_, Some(err))) =>
										(0, Some(s"Error finding subcomponents of $from"))
									case ((wells, _)) =>
										(0, None)
								}
							// To a rack - if not from a tube then we can't do it since we don't support transferring
							// individual wells from a plate to a tube - from a tube become many tube-to-tube transfers
							case (None, Some(toSC)) =>
								if (isTubeToMany)
									toSC().map {
										case ((_, Some(err))) =>
											(0, Some(s"Error finding subcomponents of $to"))
										case ((toWells, _)) =>
											val fromWells = Map(TransferContents.oneWell -> from)
											(0, None)
									}
								else
									Future.successful((0, Some(s"Error: expected $from to be a tube")))
							case _ => doInsert(this)
						}
					}
					else
						doInsert(this)

				case (Some(_), None) => Future.successful(0, Some(s"Component $to not found"))
				case (None, Some(_)) => Future.successful(0, Some(s"Component $from not found"))
				case _ => Future.successful(0, Some(s"Components $from and $to not found"))
			}
		})

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
		/*
		 * Get a components layout if it is divided
		 * @param c component
		 * @return layout of component if one is found
		 */
		def getLayout(c: Component) =
			c match {
				case cd: ContainerDivisions => Some(cd.layout)
				case _ => None
			}

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
				// Transfer to entire divided component either from a tube or between same size components
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
				// Make into list of wells transferred to - single well unless from tube
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
