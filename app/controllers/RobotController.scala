package controllers

import java.io.File

import models.RobotForm
import play.api.mvc.{Action, Controller}
import utils.MessageHandler

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by nnovod on 1/27/16.
  */
class RobotController extends Controller {
	/**
	  * Simple action to put up the form to get the antibody plate creation parameters.
	  *
	  * @param id id of component
	  * @return form to get ezpass parameters
	  */
	def antibodyPlate(id: String) = Action { request =>
		Ok(views.html.abRobot(MessageHandler.addStatusFlash(request, RobotForm.form), id, "ABPlate_"))
	}

	def createABPlate(id: String) = Action { request =>
		Ok("Got here")
	}
}
