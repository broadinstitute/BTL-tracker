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
