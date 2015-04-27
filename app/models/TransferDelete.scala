package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._

/**
 * Model to delete transfers.  User must supply a from and to for the transfer(s) to be deleted.
 * Created by nnovod on 4/27/15.
 */
case class TransferDelete(from: String, to: String)

/**
 * Companion object with form and
 */
object TransferDelete {
	// Keys
	val fromKey = "from"
	val toKey = "to"
	// Mapping to create/read TransferDelete objects
	val transferDeleteForm = Form(mapping(
		fromKey -> nonEmptyText,
		toKey -> nonEmptyText
	)(TransferDelete.apply)(TransferDelete.unapply))

	// Formatter for going to/from and validating Json
	implicit val transferDeleteFormat = Json.format[TransferDelete]
}
