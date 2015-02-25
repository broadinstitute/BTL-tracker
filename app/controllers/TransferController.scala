package controllers

import controllers.Errors.FlashingKeys
import models.Transfer
import play.api.mvc.{Action,Controller}

import scala.concurrent.Future

/**
 * @author Nathaniel Novod
 *         Date: 2/24/15
 *         Time: 6:27 PM
 */
object TransferController extends Controller {

	/**
	 * Initiate transfer - go bring up form to do transfer
 	 * @return action to go get transfer information
	 */
	def transfer() = Action { request =>
		Ok(views.html.transferStart(Errors.addStatusFlash(request,Transfer.form)))
	}

	/**
	 * Do transfer based on parameters from url - this is used when the transfer components are already set but
	 * there's additional information needed to complete the transfer.
	 * @param fromID component ID being transferred from
	 * @param toID component ID being transferred to
	 * @param fromQuad true if we need to get the quadrant we're transferring from
	 * @param toQuad true if we need to get the quadrant we're transferring to
	 * @param fromPos true if we need to get the position (e.g., well position) we're transferring from
	 * @param toPos true if we need to get the position we're transferring to
	 * @return action to get additional transfer information wanted
	 */
	def transferWithParams(fromID: String, toID: String,
	                       fromQuad : Option[Boolean], toQuad: Option[Boolean],
	                       fromPos: Option[Boolean], toPos: Option[Boolean]) = {
		Action { request =>
			Ok(views.html.transfer(Errors.addStatusFlash(request,Transfer.form), fromID, toID,
				fromQuad.getOrElse(false), toQuad.getOrElse(false), fromPos.getOrElse(false), toPos.getOrElse(false)))
		}
	}

	/**
	 * Do transfer based on form.
 	 * @return action to do transfer
	 */
	def transferFromForm = Action.async { request =>
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		Transfer.form.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(BadRequest(
					views.html.transferStart(formWithErrors.withGlobalError(Application.validationError)))),
			data => {
				val result = Redirect(routes.TransferController.transferWithParams(
					data.from,data.to,Some(true),Some(true),Some(true),Some(true)))
				Future.successful(FlashingKeys.setFlashingValue(
					result,FlashingKeys.Status,"Fill in additional data to complete transfer"))
			}
		).recover {
			case err => BadRequest(
				views.html.transferStart(Transfer.form.withGlobalError(Errors.exceptionMessage(err))))
		}
	}

}
