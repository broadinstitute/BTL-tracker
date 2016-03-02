package controllers

import java.io.File

import models.Robot.RobotType
import models.db.TransferCollection
import models.{ Robot, RobotForm }
import play.api.mvc.{ Action, Controller }
import utils.MessageHandler

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Controller to handle robot requests.
 * Created by nnovod on 1/27/16.
 */
object RobotController extends Controller {
	/**
	 * Simple action to put up the form to get the antibody plate creation parameters.
	 *
	 * @param id id of antibody rack
	 * @return form to get ezpass parameters
	 */
	def antibodyPlate(id: String) = Action { request =>
		Ok(content = views.html.abRobot(
			robotForm = MessageHandler.addStatusFlash(request = request, data = RobotForm.form), abRack = id))
	}

	/**
	 * Return bad request for attempt to make robot instructions
	 *
	 * @param data data from input form
	 * @param err error reporting
	 * @param id id of antibody rack
	 * @return bad request with form filled in from data and error message set
	 */
	private def badRequest(data: RobotForm, err: String, id: String) = {
		BadRequest(content = views.html.abRobot(
			robotForm = RobotForm.form.fill(data).withGlobalError(err), abRack = id))
	}

	/**
	 * Create robot instructions, to be uploaded, for the antibody plate and insert the transfers that will be done
	 * between the antibody tubes and plate.
	 *
	 * @param id id of antibody rack
	 * @return either upload results or redisplay of form with error
	 */
	def createABPlate(id: String) = Action.async { request =>
		RobotForm.form.bindFromRequest()(request).fold(
			hasErrors =
				(formWithErrors) => {
				Future.successful(BadRequest(content = views.html.abRobot(
					robotForm = MessageHandler.formGlobalError(
						form = formWithErrors, err = MessageHandler.validationError), abRack = id)))
				},
			success =
				(data) => {
					val robot = Robot(RobotType.HAMILTON)
					robot.makeABPlate(abRack = data.abRack, abPlate = data.abPlate,
						sampleContainer = data.sampleContainer)
						.flatMap {
							case (_, Some(err)) =>
								Future.successful(badRequest(data = data, err = err, id = id))
							case (Some(res), _) =>
								// Get all errors together
								val errs = res.trans.flatMap((r) => r._2.toList)
								// If any errors then report them and leave
								if (errs.nonEmpty && !data.continueOnError)
									Future.successful(badRequest(data = data, err = errs.mkString("; "), id = id))
								else {
									// Get all the tube to plate transfers
									val tubeToPlateTrans = res.trans.flatMap((r) => r._1.toList)
									// Make transfer entries and put them in the DB
									val trans =
										Robot.makeABTransfers(plate = res.abPlate.id, tubeToPlateList = tubeToPlateTrans,
											project = res.sampleContainer.project, div = res.abPlate.layout)
									val transDBOpers = Future.sequence(trans.map(TransferCollection.insert))
									// Next make spreadsheet to be uploaded (use Future to do it at same time as inserts)
									val spreadSheet = Future { Robot.makeABSpreadSheet(tubeToPlateTrans) }
									// Wait for all the futures to complete (DB inserts and spreadsheet creation)
									(for {
										transDone <- transDBOpers
										sheetDone <- spreadSheet
									} yield (transDone, sheetDone)).map {
										case (transStat, (Some(file), _)) =>
											// Upload the file using wanted filename
											val tranErrs = transStat.filterNot(_.ok)
												.map(_.errMsg.getOrElse("Unknown DB Error"))
											if (tranErrs.nonEmpty) {
												badRequest(data = data,
													err = s"Errors inserting transfers for $id: ${tranErrs.mkString("; ")}",
													id = id)
											} else {
												val outFile = new File(file)
												val fileNameReturned = data.fileName.trim()
												val fileName =
													if (fileNameReturned.isEmpty) data.abPlate else fileNameReturned
												Ok.sendFile(content = outFile, inline = false,
													fileName = (_) => s"$fileName.csv", onClose = () => outFile.delete())
											}
										case (_, (_, sheetErrs)) =>
											// Errors making sheet
											badRequest(data = data, err = sheetErrs.mkString("; "), id = id)
									}
								}

							case _ =>
								// Should never get here - would mean neither errors nor results
								Future.successful(badRequest(data = data, err = "makeABPlate Internal Error", id = id))
						}
				}.recover {
					case err => badRequest(data = data, err = MessageHandler.exceptionMessage(err), id = id)
				})
	}
}
