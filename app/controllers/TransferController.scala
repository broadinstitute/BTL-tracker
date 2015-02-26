package controllers

import controllers.Errors.FlashingKeys
import models.{Component,Transfer}
import play.api.mvc.{Action,Controller}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json._

import scala.concurrent.Future

/**
 * @author Nathaniel Novod
 *         Date: 2/24/15
 *         Time: 6:27 PM
 */
object TransferController extends Controller with MongoController {
	/**
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses JSON for input/output
	 */
	private def collection: JSONCollection = db.collection[JSONCollection]("tracker")

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
	 * Start of transfer - we look over IDs and see what's possible.  Depending type of components being transferred
	 * different additional information may be needed.
	 * @return action to see what step is next to complete transfer
	 */
	def transferIDs = Action.async { request =>
		// This finds implicit reader - couldn't find it on my own
		def doTransfer[T: Reads] = {
			import play.api.libs.concurrent.Execution.Implicits.defaultContext
			Transfer.form.bindFromRequest()(request).fold(
				formWithErrors =>
					Future.successful(BadRequest(
						views.html.transferStart(formWithErrors.withGlobalError(Errors.validationError)))),
				data => {
					for {
						to <- collection.find(Json.toJson(Json.obj(Component.idKey -> data.to))).one
						from <- collection.find(Json.toJson(Json.obj(Component.idKey -> data.from))).one
					} yield {
						def missingResult(errs: List[String]) = {
							val f = errs.foldLeft(Transfer.form.fill(data))((f, k) => f.withError(k, "ID not found"))
							val form = f.withGlobalError("Fix errors below")
							BadRequest(views.html.transferStart(form))
						}
						(to,from) match {
							case (Some(t),Some(f)) =>
								val result0 = Redirect(routes.TransferController.transferWithParams(
									data.from,data.to,Some(true),Some(true),Some(true),Some(true)))
								FlashingKeys.setFlashingValue(
									result0,FlashingKeys.Status,"Fill in additional data to complete transfer")
							case (None,Some(fid)) => missingResult(List(Transfer.fromKey))
							case (Some(tid),None) => missingResult(List(Transfer.toKey))
							case _ => missingResult(List(Transfer.fromKey, Transfer.toKey))
						}
					}
				}
			).recover {
				case err => BadRequest(
					views.html.transferStart(Transfer.form.withGlobalError(Errors.exceptionMessage(err))))
			}
		}
		doTransfer[JsObject]
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
					views.html.transferStart(formWithErrors.withGlobalError(Errors.validationError)))),
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
