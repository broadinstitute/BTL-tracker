package models

import play.api.data.Form
import play.api.data.Forms._

/**
 * Robot form to create robot instructions.
 * Created by nnovod on 1/27/16.
 */

/**
 * Class to map to form for making robot instructions
 * @param abRack Rack containing antibody tube
 * @param abPlate ID of plate to create with antibodies
 * @param sampleContainer container with samples for which antibodies are to be created
 * @param fileName filename for robot instructions
 * @param continueOnError true if non-fatal errors are to be ignored
 */
case class RobotForm(abRack: String, abPlate: String, sampleContainer: String,
	fileName: String, continueOnError: Boolean)

object RobotForm {
	// Form keys
	val abRackKey = "abRack"
	val abPlateKey = "abPlate"
	val sampleContainerKey = "sampleContainer"
	val fileNameKey = "fileName"
	val continueOnErrorKey = "continueOnError"
	// Form with mapping to object and validation
	val form =
		Form(mapping(
			abRackKey -> nonEmptyText,
			abPlateKey -> nonEmptyText,
			sampleContainerKey -> nonEmptyText,
			fileNameKey -> nonEmptyText,
			continueOnErrorKey -> boolean)(RobotForm.apply)(RobotForm.unapply))
}