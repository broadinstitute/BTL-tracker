package controllers

import java.io.File

import models.Robot.RobotType
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
	  *
	  * @param id id of component
	  * @return form to get ezpass parameters
	  */
	def antibodyPlate(id: String) = Action { request =>
		Ok(views.html.abRobot(MessageHandler.addStatusFlash(request, RobotForm.form), id))
	}

	//@TODO Finish this
	def createABPlate(id: String) = Action.async { request =>
		RobotForm.form.bindFromRequest()(request).fold(
			formWithErrors => {
				Future.successful(BadRequest(views.html.abRobot(
					MessageHandler.formGlobalError(formWithErrors, MessageHandler.validationError), id)))
			},
			data => {
				val robot = Robot(RobotType.HAMILTON)
				robot.makeABPlate(data.abRack, data.abPlate, data.bspRack).map(
					(res) => {
						val errs = res.flatMap((r) => r._2.toList)
						if (errs.nonEmpty)
							BadRequest(
								views.html.abRobot(RobotForm.form.fill(data).withGlobalError(errs.mkString(";")), id)
							)
						else {
							"Volume in ul,Source ab type,Source ab location,Destination location"
							Ok(res.toString)
						}
					}
				)
			}.recover {
				case err =>
					BadRequest(views.html.abRobot(
						RobotForm.form.fill(data).withGlobalError(MessageHandler.exceptionMessage(err)), id
					))
			}
		)
	}
}
