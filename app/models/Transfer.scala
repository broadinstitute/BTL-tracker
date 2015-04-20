package models

import formats.CustomFormats._
import mappings.CustomMappings._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._

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
 */
case class Transfer(from: String, to: String,
					fromQuad: Option[Transfer.Quad.Quad], toQuad: Option[Transfer.Quad.Quad], project: Option[String],
					slice: Option[Transfer.Slice.Slice])

/**
 * Junior class to Transfer to get initial data for transfer.  This is just used in forms.
 * @param from ID we're transferring from
 * @param to ID we're transferring to
 * @param project optional project transfer is associated with
 */
case class TransferStart(from: String, to: String, project: Option[String]) {
	def toTransferForm = TransferForm(toTransfer, dataMandatory = false, isQuadToQuad = false, isQuadToTube = false)
	def toTransfer = Transfer(from, to, None, None, project, None)
}

/**
 * Transfer with UI parameters
 * @param dataMandatory true if quadrant information must be specified (non-quadrant component to/from quadrant component)
 * @param isQuadToQuad true if quadrant transfers must be quad to quad (both source and destination have quadrants)
 * @param isQuadToTube true if transfer from a source with quadrants to a non-divided component
 */
case class TransferForm(transfer: Transfer, dataMandatory: Boolean, isQuadToQuad: Boolean, isQuadToTube: Boolean)

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

	import Quad._
	/**
	 * Make a well mapping going between a quadrant of a 384 well plate and a 96 well plate.
	 * @param qtr which quarter to move to/from
	 * @param toTuple callback to make a tuple of source well -> target well
	 * @return map going between a quadrant of a 384 well plate and a 96 well plate
	 */
	private def qToQ(qtr: Quad, toTuple: (Char, Int, (Int, Int)) => (String, String)) = {
		// Get relative x/y coordinates within each group of 2x2 wells to quadrants
		val q = qtr match {
			case Q1 => (0, 0)
			case Q2 => (0, 1)
			case Q3 => (1, 0)
			case Q4 => (1, 1)
		}
		// Make map
		(for {x <- 'A' to 'H'
			  y <- 1 to 12}
			yield toTuple(x, y, q)).toMap
	}

	/**
	 * Make string for well within quadrant in 384 well plate
	 * @param x row coordinate of matching well in 96-well plate
	 * @param y column coordinate of matching well in 96-well plate
	 * @param q relative coordinates to wanted well with each 2x2 set of wells to wanted quadrant
	 * @return well description (letter and 2-digit number - e.g., A01)
	 */
	private def well384(x: Char, y: Int, q: (Int, Int)) =
		f"${((x - 'A') * 2) + 'A' + q._1}%c${((y - 1) * 2) + 1 + q._2}%02d"

	/**
	 * Make string for well in 384 well plate
	 * @param x row coordinate of well
	 * @param y column coordinate of well
	 * @return well description (letter and 2-digit number - e.g., A01)
	 */
	private def well96(x: Char, y: Int) = f"$x$y%02d"

	/**
	 * Make map of 96-well plate to quadrant of 384-well plate
	 * @param qtr quadrant of 384-well plate
	 * @return map of 96-well plate wells to target quadrant wells in 384-well plate
	 */
	private def q96to384(qtr: Quad) = qToQ(qtr, (x, y, q) => well96(x, y) -> well384(x, y, q))

	/**
	 * Make map of 384-well plate quadrant to 96-well plate
	 * @param qtr quadrant of 384-well plate
	 * @return map of quadrant of 384-well plate wells to target 96-well plate wells
	 */
	private def q384to96(qtr: Quad) = qToQ(qtr, (x, y, q) => well384(x, y, q) -> well96(x, y))

	// Well mappings between 96-well plate and 384-well plate quadrants - can be used to see what well quadrant
	// transfers come from and are going to
	val qTo384 = Map(Q1 -> q96to384(Q1), Q2 -> q96to384(Q2), Q3 -> q96to384(Q3), Q4 -> q96to384(Q4))
	val qFrom384 = Map(Q1 -> q384to96(Q1), Q2 -> q384to96(Q2), Q3 -> q384to96(Q3), Q4 -> q384to96(Q4))
	// Make 384 to 384 map of maps: (fromQ, toQ) -> (originalWells -> destinationWells)
	val q384to384map =
		(for {
			qFrom <- Quad.values.toIterable
			qTo <- Quad.values.toIterable
		} yield {
				val from = qFrom384(qFrom)
				val to = qTo384(qTo)
				// Going from original source well to final destination well - 96 in middle to calculate quadrant well
				(qFrom, qTo) -> from.map {
					case (k, v) => k -> to(v)
				}
			}).toMap

	// Slice enumeration
	object Slice extends Enumeration {
		type Slice = Value
		val S1 = Value("1-3 x A-H")
		val S2 = Value("4-6 x A-H")
		val S3 = Value("7-9 x A-H")
		val S4 = Value("10-12 x A-H")
		val S5 = Value("1-6 x A-H")
		val S6 = Value("7-12 x A-H")
		val S7 = Value("1-12 x A-H")
	}

	// String values for dropdown lists etc.
	val sliceVals = enumSortedList(Slice)

	import Slice._

	/**
	 * Make a list of wells included in a wanted slice of a 96-well plate.
	 * @param slice slice wanted
	 * @return list containing only wells from wanted slice
	 */
	private def slice96(slice: Slice) = {
		def slice24(y: Int) = (y, 'A', 3, 8)
		def slice48(y: Int) = (y, 'A', 6, 8)
		val s = slice match {
			case S1 => slice24(1)
			case S2 => slice24(4)
			case S3 => slice24(7)
			case S4 => slice24(10)
			case S5 => slice48(1)
			case S6 => slice48(7)
			case S7 => (1, 'A', 12, 8)
		}
		val xRange = s._2 until (s._2 + s._4).toChar
		val yRange = s._1 until (s._1 + s._3)
		List.tabulate[String](xRange.size * yRange.size)(i => {
			well96(xRange(i/yRange.size), yRange(i%yRange.size))
		})
	}

	/**
	 * Make self well map
	 * @param wellList list of wells to make a map of to point to themselves
	 */
	private def makeSelfWellMap(wellList: List[String]) = wellList.map((w) => w -> w).toMap

	/**
	 * Get a map of wells for a slice of a 96-well plate going into a 96-well plate
	 * @param slice slice of plate wanted
	 * @return map of wells to well including only wells from slice
	 */
	private def slice96to96(slice: Slice) = {
		makeSelfWellMap(slice96(slice))
	}

	/**
	 * Get a map of wells for a slice of a 96-well plate going into a 384-well plate
	 * @param quad quadrant of plate to take slice to
	 * @param slice slice of quadrant wanted
	 * @return map of source 96-plate wells to 384-plate wells
	 */
	private def slice96to384(quad: Quad, slice: Slice) = {
		// Get wells of quadrant wanted (slice of 96 well quadrant)
		val sliceWells = slice96(slice)
		// Get map of wells from slice in quadrant
		qTo384(quad).filter {
			case (w, _) => sliceWells.contains(w)
		}
	}

	/**
	 * Get a map of wells for a slice of a 384-well plate going into a 96-well plate
	 * @param quad quadrant of plate to take slice from
	 * @param slice slice of quadrant wanted
	 * @return map of source 384-plate wells to 96-plate wells
	 */
	private def slice384to96(quad: Quad, slice: Slice) = {
		// Get wells of quadrant wanted (slice of 96 well quadrant)
		val sliceWells = slice96(slice)
		// Get map of wells from slice in quadrant
		qFrom384(quad).filter {
			case (_, w) => sliceWells.contains(w)
		}
	}

	/**
	 * For quadrant slices, creates a map of (quadrant,slice) -> (originalWell -> targetWell)
	 * @param slicer callback to get a mapping of original wells to target wells
	 * @return map, keyed by quadrant and slice, to values that are maps of slice's original wells to target wells
	 */
	private def getQuadSliceMap(slicer: (Quad.Quad, Slice.Slice) => Map[String, String]) =
		(for {
			s <- Slice.values.toIterable
			q <- Quad.values.toIterable
		} yield {
				(q, s) -> slicer(q, s)
		}).toMap

	// Make map of maps: (quadrant,slice) -> (originalWells -> destinationWells)
	val slice96to384map = getQuadSliceMap(slice96to384)
	val slice384to96map = getQuadSliceMap(slice384to96)
	// Make 384 to 384 map of maps: (fromQ, toQ, slice) -> (originalWells -> destinationWells)
	val slice384to384map =
		(for {
			s <- Slice.values.toIterable
			qFrom <- Quad.values.toIterable
			qTo <- Quad.values.toIterable
		} yield {
			val from = slice384to96map(qFrom, s)
			val to = slice96to384map(qTo, s)
			// Going from original source well to final destination well - 96 in middle to calculate quadrant/slice
			(qFrom, qTo, s) -> from.map {
				case (k, v) => k -> to(v)
			}
		}).toMap
	// Make 96 to 96 map of maps: slice -> (originalWells -> destinationWells) - no quadrants are needed for 96-wells
	val slice96to96map = Slice.values.toIterable.map((value) => value -> slice96to96(value)).toMap

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

	// Mapping to create/read Transfer objects
	val transferMapping = mapping(
		fromKey -> nonEmptyText,
		toKey -> nonEmptyText,
		fromQuadKey -> optional(enum(Quad)),
		toQuadKey -> optional(enum(Quad)),
		projectKey -> optional(text),
		sliceKey -> optional(enum(Slice))
	)(Transfer.apply)(Transfer.unapply)

	// Keys for form
	val transferKey = "transfer"
	val mandatoryKey = "dataMandatory"
	val isQuadToQuadKey = "isQuadToQuad"
	val isQuadToTubeKey = "isQuadToTube"

	// Mapping to create/read Transfer form
	val transferFormMapping = mapping(
		transferKey -> transferMapping,
		mandatoryKey -> boolean,
		isQuadToQuadKey -> boolean,
		isQuadToTubeKey -> boolean
	)(TransferForm.apply)(TransferForm.unapply)

	// Form used for transferring - it includes verification to see if proper set of quadrants are set.  If a
	// transfer is being done between two components with quadrants (i.e., 384-well component to 384-well component)
	// isQuadToQuad is true and then if the entire component is not being transferred both quadrants must be
	// specified.  If a transfer between a source with quadrants and a non-divided target then isQuadToTube is true
	// and a source quadrant must be set if slicing.
	val form = Form(transferFormMapping
		verifying ("Quadrants must be specified when slicing", f =>
		(!f.isQuadToQuad && !f.isQuadToTube) ||
			(f.transfer.slice.isEmpty && f.transfer.fromQuad.isEmpty && f.transfer.toQuad.isEmpty) ||
			(f.isQuadToQuad && f.transfer.fromQuad.isDefined && f.transfer.toQuad.isDefined) ||
			(f.isQuadToTube && f.transfer.fromQuad.isDefined)
	))

	// Same form but without verification - this is ued to get data from form in case verification failed with
	// for with verification
	val formWithoutVerify = Form(transferFormMapping)

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
					   val time: Long)
	extends Transfer(from, to, fromQuad, toQuad, project, slice)

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
			transfer.project, transfer.slice, time)
}
