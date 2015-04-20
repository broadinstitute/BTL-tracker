package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Format, Json}
import play.api.data.format.Formats._

/**
 * Module to use for form to create an EZPass.
 * Created by nnovod on 3/20/15.
 */

/**
 * EZPASS parameters to be taken from form
 * @param component component to make EZPASS for
 * @param libSize library insert size including adapters
 * @param libVol library volume (ul)
 * @param libConcentration library concentration (ng/ul)
 * @return list of errors
 */
case class EZPass(component: String, libSize: Int, libVol: Int, libConcentration: Float)
object EZPass {
	val idKey = "id"
	val libSizeKey = "libSize"
	val libVolKey = "libVol"
	val libConcKey = "libConc"
	val form =
		Form(mapping(
			idKey -> nonEmptyText,
			libSizeKey -> number,
			libVolKey -> number(min=20),
			libConcKey -> of[Float]
		)(EZPass.apply)(EZPass.unapply))

	/**
	 * Formatter for going to/from and validating Json
	 */
	implicit val ezpassFormat : Format[EZPass] = Json.format[EZPass]
}
