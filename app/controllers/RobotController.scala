package controllers

import java.io.File

import models.Robot.RobotType
import models.db.TransferCollection
import models.{Robot, RobotForm}
import play.api.mvc.{Action, Controller}
import utils.MessageHandler

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by nnovod on 1/27/16.
  */
object RobotController extends Controller {
	/**
	  * Simple action to put up the form to get the antibody plate creation parameters.
	  * @param id id of antibody rack
	  * @return form to get ezpass parameters
	  */
	def antibodyPlate(id: String) = Action { request =>
		Ok(views.html.abRobot(MessageHandler.addStatusFlash(request, RobotForm.form), id))
	}

	/**
	  * Return bad request for attempt to make robot instructions
	  * @param data data from input form
	  * @param err error reporting
	  * @param id id of antibody rack
	  * @return bad request with form filled in from data and error message set
	  */
	private def badRequest(data: RobotForm, err: String, id: String) = {
		BadRequest(views.html.abRobot(RobotForm.form.fill(data).withGlobalError(err), id))
	}

	/**
	  * Create robot instructions, to be uploaded, for the antibody plate and insert the transfers that will be done
	  * between the antibody tubes and plate.
	  * @param id id of antibody rack
	  * @return either upload results or redisplay of form with error
	  */
	def createABPlate(id: String) = Action.async { request =>
		RobotForm.form.bindFromRequest()(request).fold(
			formWithErrors => {
				Future.successful(BadRequest(views.html.abRobot(
					MessageHandler.formGlobalError(formWithErrors, MessageHandler.validationError), id)))
			},
			data => {
				val robot = Robot(RobotType.HAMILTON)
				robot.makeABPlate(data.abRack, data.abPlate, data.bspRack).flatMap {
					case (_, Some(err)) =>
						Future.successful(badRequest(data, err, id))
					case (Some(res), _) =>
						// Get all errors together
						val errs = res.trans.flatMap((r) => r._2.toList)
						// If any errors then report them and leave
						if (errs.nonEmpty)
							Future.successful(badRequest(data, errs.mkString("; "), id))
						else {
							// Get all the tube to plate transfers
							val tubeToPlateTrans = res.trans.flatMap((r) => r._1.toList)
							// Make transfer entries and put them in the DB
							val trans = Robot.makeABTransfers(res.abPlate.id, tubeToPlateTrans,
								res.bspRack.project, res.abPlate.layout)
							val transDBOpers = Future.sequence(trans.map(TransferCollection.insert))
							// Next make spreadsheet to be uploaded
							val spreadSheet = Robot.makeABSpreadSheet(tubeToPlateTrans, data.fileName)
							// Wait for all the futures to complete (DB inserts and spreadsheet creation)
							(for {
								transDone <- transDBOpers
								sheetDone <- spreadSheet
							} yield sheetDone).map {
								case (Some(file), _) =>
									// Upload the file using wanted filename
									val outFile = new File(file)
									val fileNameReturned = data.fileName.trim()
									val fileName = if (fileNameReturned.isEmpty) data.abPlate else fileNameReturned
									Ok.sendFile(content = outFile, inline = false,
										fileName = (_) => s"$fileName.xlsx", onClose = () => outFile.delete())
								case (_, sheetErrs) =>
									// Errors making sheet
									badRequest(data, sheetErrs.mkString("; "), id)
							}
						}
					case _ =>
						// Should never get here - would mean neither errors nor results
						Future.successful(badRequest(data, "makeABPlate Internal Error", id))
				}
			}.recover {
				case err => badRequest(data, MessageHandler.exceptionMessage(err), id)
			}
		)
	}
}
