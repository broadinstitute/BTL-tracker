package controllers

import controllers.Errors.FlashingKeys
import models.{Transferrable,Component,Transfer}
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
	 * Take data from transfer form and check it out.
 	 * @param data data retrieved from transfer form
	 * @tparam T type to return as transfer objects if they are found
	 * @return objects found and list of missing objects - one will always be set to None
	 */
	private def getTransferInfo[T: Reads](data: Transfer) = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		for {
			to <- collection.find(Json.toJson(Json.obj(Component.idKey -> data.to))).one
			from <- collection.find(Json.toJson(Json.obj(Component.idKey -> data.from))).one
		} yield {
			def missingIDs(errs: List[String]) = {
				val notFoundErrs : Map[Option[String], String] = errs.map(Some(_) -> "ID not found").toMap
				Errors.fillAndSetFailureMsgs(notFoundErrs, Transfer.form, data)
			}
			(from,to) match {
				case (Some(f),Some(t)) => (Some(f, t), None)
				case (None,Some(_)) => (None, Some(missingIDs(List(Transfer.fromKey))))
				case (Some(_),None) => (None, Some(missingIDs(List(Transfer.toKey))))
				case _ => (None, Some(missingIDs(List(Transfer.fromKey, Transfer.toKey))))
			}
		}
	}

	/**
	 * Is the transfer valid.  Check from component can be transferred to to component.
	 * @param data transfer data
	 * @param from json from find of from component
	 * @param to json from find of to component
	 * @return if transfer valid then from and to object
	 */
	private def isTransferValid(data: Transfer, from: JsObject, to: JsObject) = {
		import models.Component.ComponentType._
		val fromC = (from \ Component.typeKey).as[ComponentType]
		val toC = (to \ Component.typeKey).as[ComponentType]
		ComponentController.actions(fromC).jsonToComponent(from) match {
			case fc: Component with Transferrable if fc.validTransfers.contains(toC) =>
				Some(fc, ComponentController.actions(toC).jsonToComponent(to))
			case invalidComponent => None
		}
	}

	/**
	 * Start of transfer - we look over IDs and see what's possible.  Depending type of components being transferred
	 * different additional information may be needed.
	 * @return action to see what step is next to complete transfer
	 */
	def transferIDs = Action.async { request =>
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		Transfer.form.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(BadRequest(
					views.html.transferStart(formWithErrors.withGlobalError(Errors.validationError)))),
			data => {
				getTransferInfo[JsObject](data).map{
					case (Some((from, to)), None) =>
						val valid = isTransferValid(data, from, to)
						if (valid.isDefined) {
							val result0 = Redirect(routes.TransferController.transferWithParams(
								data.from,data.to,Some(true),Some(true),Some(true),Some(true)))
							FlashingKeys.setFlashingValue(result0,
								FlashingKeys.Status, "Fill in additional data to complete transfer")
						} else
							BadRequest(views.html.transferStart(
								Errors.fillAndSetFailureMsgs(Map(None -> "Invalid transfer types"),
									Transfer.form, data)))
					case (None, Some(form)) => BadRequest(views.html.transferStart(form))
					// Should never have both or neither as None but...
					case _ => FlashingKeys.setFlashingValue(Redirect(routes.Application.index()),
						FlashingKeys.Status,"Internal error: Failure during transferIDs")
				}
			}.recover {
				case err => BadRequest(views.html.transferStart(
					Errors.fillAndSetFailureMsgs(Map(None -> Errors.exceptionMessage(err)), Transfer.form, data)))

			}
		).recover {
			case err => BadRequest(
				views.html.transferStart(Transfer.form.withGlobalError(Errors.exceptionMessage(err))))
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
