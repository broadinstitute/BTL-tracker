package controllers

import java.io.File

import models.{TransferHistory, EZPass}
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
	 * Simple action to put up the form to get the ezpass parameters.  We look up the project for the component and
	 * use that as the EZPASS output name.  If there's no project in the component sources then we use the component
	 * id as the EZPASS output filename.
 	 * @param id id of component
	 * @return form to get ezpass parameters
	 */
	def ezpass(id: String) = Action.async { request =>
		TransferHistory.makeSourceGraph(id).map((graph) => {
			val projects = TransferHistory.getGraphProjects(graph)
			val fileName = if (projects.size == 1) projects.last else id
			Ok(views.html.ezpass(MessageHandler.addStatusFlash(request, EZPass.form), id, fileName))
		}).recover {
			case err => MessageHandler.homeRedirect(MessageHandler.exceptionMessage(err))
		}
	}

	/**
	 * Action to create an EZPASS
	 * @param id id of component
	 * @param output output file name
	 * @return Future to download EZPASS
	 */
	def createEZPass(id: String, output: String) = Action.async { request =>
		EZPass.form.bindFromRequest()(request).fold(
			formWithErrors => {
				Future.successful(BadRequest(views.html.ezpass(
					MessageHandler.formGlobalError(formWithErrors, MessageHandler.validationError), id, output)))
			},
			data => {
				// Make EZPASS
				EZPass.makeEZPassWithProject(EZPass.WriteEZPassData,
					data.component, data.libSize, data.libVol, data.libConcentration).map {
					// If we got back a file with contents then use it
					case (Some(file), errs) =>
						val outFile = new File(file)
						val fileNameReturned = data.fileName.trim()
						val fileName = if (fileNameReturned.isEmpty) data.component else fileNameReturned
						Ok.sendFile(content = outFile, inline = false,
							fileName = (_) => s"$fileName.xlsx", onClose = () => outFile.delete())
					// If nothing returns then report error(s)
					case (None, errs) =>
						val filledForm = EZPass.form.fill(data)
						BadRequest(views.html.ezpass(MessageHandler.setGlobalErrors(errs, filledForm),
							data.component, data.fileName))
				}
			}.recover {
				case err => BadRequest(
					views.html.ezpass(EZPass.form.fill(data).withGlobalError(MessageHandler.exceptionMessage(err)),
						data.component, data.fileName))
			}
		)
	}
}
