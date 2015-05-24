package controllers

import models.{Component, TransferDelete}
import models.db.TransferCollection
import play.api.mvc.{Action, Controller}
import utils.MessageHandler

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Controller to delete transfers.
 * Created by nnovod on 4/27/15.
 */
object TransferDeleteController extends Controller {

	/**
	 * Initiate delete of transfers - just put up form to get other end of transfer
	 * @return action to get id of wanted component
	 */
	def deleteTransfer(id: String) = Action { request =>
		Ok(views.html.transferDelete(MessageHandler.addStatusFlash(request,TransferDelete.transferDeleteForm), id))
	}

	/**
	 * Get settings from form and go check if transfer requested is to be done.
	 * @return action to get count of transfers that will be deleted and ask for confirmation
	 */
	def deleteTransferFromForm(id: String) = Action.async {request =>
		TransferDelete.transferDeleteForm.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(BadRequest(views.html.transferDelete(
					MessageHandler.formGlobalError(formWithErrors, MessageHandler.validationError), id))),
			data => {
				TransferCollection.countBetweenTransfers(data.from, data.to).map ((count) =>
					Ok(views.html.transferDeleteConfirm(data.from, data.to, count))
				).recover {
					case err => BadRequest(
						views.html.index(Component.blankForm.withGlobalError(MessageHandler.exceptionMessage(err))))
				}
			}
		)
	}

	/**
	 * Go do delete of transfer
	 * @param fromID component transfer was from
	 * @param toID component transfer was to
	 * @return action to delete specified transfer
	 */
	def deleteTransferByIDs(fromID: String, toID: String) = Action.async { request =>
		TransferCollection.removeBetweenTransfers(fromID, toID).map {
			case Some(err) => MessageHandler.homeRedirect(s"Delete failed: ${err.getLocalizedMessage}")
			case None => MessageHandler.homeRedirect(s"Transfer from $fromID to $toID successfully deleted")
		}
	}
}
