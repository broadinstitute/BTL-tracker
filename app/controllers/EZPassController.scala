package controllers

import java.io.File

import models.EZPass
import play.api.mvc.{Action, Controller}
import utils.MessageHandler

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created by nnovod on 3/10/15.
 * Methods to make an EZPass
 */
object EZPassController extends Controller {
	/**
	 * Simple action to put up the form to get the ezpass parameters
 	 * @param id id of component
	 * @param output default filename for EZPass
	 * @return form to get ezpass parameters
	 */
	def ezpass(id: String, output: String) = Action { request =>
		Ok(views.html.ezpass(MessageHandler.addStatusFlash(request, EZPass.form), id, output))
	}

	/**
	 * Action to create an EZPASS
	 * @param id id of component
	 * @return Future to download EZPASS
	 */
	def createEZPass(id: String, output: String) = Action.async { request =>
		EZPass.form.bindFromRequest()(request).fold(
			formWithErrors => {
				Future.successful(BadRequest(views.html.ezpass(
					MessageHandler.formGlobalError(formWithErrors, MessageHandler.validationError), id, output)))
			},
			data => {
				EZPass.makeEZPassWithProject(EZPass.WriteEZPassData,
					id, data.libSize, data.libVol, data.libConcentration).map {
					case (Some(file), errs) =>
						val outFile = new File(file)
						val fileNameReturned = data.fileName.trim()
						val fileName = if (fileNameReturned.isEmpty) id else fileNameReturned
						Ok.sendFile(content = outFile, inline = false,
							fileName = (_) => s"${fileName}.xlsx", onClose = () => outFile.delete())
					case (None, errs) =>
						val filledForm = EZPass.form.fill(data)
						BadRequest(views.html.ezpass(MessageHandler.setGlobalErrors(errs, filledForm), id, data.fileName))
				}
			}.recover {
				case err => BadRequest(
					views.html.ezpass(
						EZPass.form.fill(data).withGlobalError(MessageHandler.exceptionMessage(err)), id, data.fileName))
			}
		)
	}
}
