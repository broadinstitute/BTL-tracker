package controllers

import java.io.File

import models.EZPass
import play.api.mvc.{Action, Controller}

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
	 * @return form to get ezpass parameters
	 */
	def ezpass(id: String) = Action { request =>
		Ok(views.html.ezpass(Errors.addStatusFlash(request, EZPass.form), id))
	}

	/**
	 * Action to create an EZPASS
	 * @param id id of component
	 * @return Future to download EZPASS
	 */
	def createEZPass(id: String) = Action.async { request =>
		EZPass.form.bindFromRequest()(request).fold(
			formWithErrors => {
				Future.successful(BadRequest(
					views.html.ezpass(formWithErrors.withGlobalError(Errors.validationError), id)))
			},
			data => {
				//@TODO update processing to get Squid project.
				EZPass.makeEZPass(EZPass.writeEZPassData, id, data.libSize, data.libVol, data.libConcentration).map {
					case (Some(file), errs) =>
						val outFile = new File(file)
						Ok.sendFile(content = outFile, inline = false,
							fileName = (_) => s"${id}_EZPASS.xlsx", onClose = () => outFile.delete())
					case (None, errs) =>
						val filledForm = EZPass.form.fill(data)
						BadRequest(views.html.ezpass(Errors.setGlobalErrors(errs, filledForm), id))
				}
			}.recover {
				case err => BadRequest(
					views.html.ezpass(EZPass.form.fill(data).withGlobalError(Errors.exceptionMessage(err)), id))
			}
		)
	}
}
