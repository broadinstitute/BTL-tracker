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

// Note: Must have complete path for Quad types below to avoid bug from macro Json.format which says it
// can't find matching apply and unapply methods if type simply specified as Quad.Quad
// See https://github.com/playframework/playframework/issues/1469
/**
 * Transfer data.
 * @param from ID we're transferring from
 * @param to ID we're transferring to
 * @param fromQuad optional quadrant transfer is coming from
 * @param toQuad optional quadrant transfer is going to
 * @param fromPos optional position transfer is coming from
 * @param toPos optional position transfer is going to
 */
case class Transfer(from: String, to: String, fromQuad: Option[Transfer.Quad.Quad], toQuad: Option[Transfer.Quad.Quad],
                    fromPos: Option[String], toPos: Option[String])

object Transfer {
	// Keys for form
	val fromKey = "from"
	val toKey = "to"
	val fromQuadKey = "fromQuad"
	val toQuadKey = "toQuad"
	val fromPosKey = "fromPos"
	val toPosKey = "toPos"

	object Quad extends Enumeration {
		type Quad = Value
		val Q1 = Value("1st Quadrant")
		val Q2 = Value("2nd Quadrant")
		val Q3 = Value("3rd Quadrant")
		val Q4 = Value("4th Quadrant")
	}

	// String values for dropdown lists etc.
	val quadVals = Quad.values.map(_.toString).toList

	// Form to create/read Transfer objects
	val form = Form(
		mapping(
			fromKey -> nonEmptyText,
			toKey -> nonEmptyText,
			fromQuadKey -> optional(enum(Quad)),
			toQuadKey -> optional(enum(Quad)),
			fromPosKey -> optional(text),
			toPosKey -> optional(text)
		)(Transfer.apply)(Transfer.unapply))

	// Formatter for going to/from and validating Json
	// Supply our custom enum Reader and Writer for content type enum
	implicit val quadFormat: Format[Quad.Quad] = enumFormat(Quad)
	implicit val transferFormat = Json.format[Transfer]
}
