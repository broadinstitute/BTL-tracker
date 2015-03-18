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
 */
case class Transfer(from: String, to: String, fromQuad: Option[Transfer.Quad.Quad], toQuad: Option[Transfer.Quad.Quad])

// Companion object
object Transfer {
	// Keys for form
	val fromKey = "from"
	val toKey = "to"
	val fromQuadKey = "fromQuad"
	val toQuadKey = "toQuad"

	// Quadrant enumeration
	object Quad extends Enumeration {
		type Quad = Value
		val Q1 = Value("1st quadrant")
		val Q2 = Value("2nd quadrant")
		val Q3 = Value("3rd quadrant")
		val Q4 = Value("4th quadrant")
	}

	// String values for dropdown lists etc.
	val quadVals = Quad.values.map(_.toString).toList

	import Quad._
	/**
	 * Make a well mapping going between a quadrant of a 384 well plate and a 96 well plate.
	 * @param qtr which quarter to move to/from
	 * @param toTuple callback to make a tuple of destination well and original well
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
	private def well96(x: Char, y: Int) = f"${x}${y}%02d"

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
	val q1to384 = q96to384(Q1)
	val q2to384 = q96to384(Q2)
	val q3to384 = q96to384(Q3)
	val q4to384 = q96to384(Q4)
	val q1from384 = q384to96(Q1)
	val q2from384 = q384to96(Q2)
	val q3from384 = q384to96(Q3)
	val q4from384 = q384to96(Q4)
	val qTo384 = Map(Q1 -> q1to384, Q2 -> q2to384, Q3 -> q3to384, Q4 -> q4to384)
	val qFrom384 = Map(Q1 -> q1from384, Q2 -> q2from384, Q3 -> q3from384, Q4 -> q4from384)

	// Form to create/read Transfer objects
	val form = Form(
		mapping(
			fromKey -> nonEmptyText,
			toKey -> nonEmptyText,
			fromQuadKey -> optional(enum(Quad)),
			toQuadKey -> optional(enum(Quad))
		)(Transfer.apply)(Transfer.unapply))

	// Formatter for going to/from and validating Json
	// Supply our custom enum Reader and Writer for content type enum
	implicit val quadFormat: Format[Quad.Quad] = enumFormat(Quad)
	implicit val transferFormat = Json.format[Transfer]
}

/**
 * Transfer with time added
 * @param from ID we're transferring from
 * @param to ID we're transferring to
 * @param fromQuad optional quadrant transfer is coming from
 * @param toQuad optional quadrant transfer is going to
 * @param time time transfer was created
 */
class TransferWithTime(override val from: String, override val to: String,
					   override val fromQuad: Option[Transfer.Quad.Quad],
					   override val toQuad: Option[Transfer.Quad.Quad], val time: Long)
	extends Transfer(from, to, fromQuad, toQuad)

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
		new TransferWithTime(transfer.from, transfer.to, transfer.fromQuad, transfer.toQuad, time)
}
