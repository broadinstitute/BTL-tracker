package models

import play.api.data.Form
import play.api.data.Forms._

/**
  * Created by nnovod on 1/27/16.
  */
case class RobotForm(abRack: String, abPlate: String, bspRack: String, fileName: String)

object RobotForm {
	// Form keys
	val abRackKey = "abRack"
	val abPlateKey = "abPlate"
	val bspRackKey = "bspRack"
	val fileNameKey = "fileName"
	// Form with mapping to object and validation
	val form =
		Form(mapping(
			abRackKey -> nonEmptyText,
			abPlateKey -> nonEmptyText,
			bspRackKey -> nonEmptyText,
			fileNameKey -> nonEmptyText
		)(RobotForm.apply)(RobotForm.unapply))
}