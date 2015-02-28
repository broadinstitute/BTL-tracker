package controllers

import controllers.Errors.FlashingKeys
import models.Transfer.Quad.Quad
import models.{ContainerDivisions, Transferrable, Component, Transfer}
import ContainerDivisions.Division._
import play.api.Logger
import play.api.data.Form
import play.api.mvc.{Action,Controller}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * @author Nathaniel Novod
 *         Date: 2/24/15
 *         Time: 6:27 PM
 */
object TransferController extends Controller with MongoController {
	/**
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses JSON for input/output of tracker data
	 */
	private def trackerCollection: JSONCollection = db.collection[JSONCollection]("tracker")

	/**
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses JSON for input/output of transfer data
	 */
	private def transferCollection: JSONCollection = db.collection[JSONCollection]("transfer")

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
	 * Take data from transfer form and check it out.  If one or both IDs are not found we return a form with the
	 * errors set, otherwise we return the objects found.
 	 * @param data data retrieved from transfer form
	 * @tparam T type to return as transfer objects if they are found
	 * @return objects found and form with errors - one and only one will be set to None
	 */
	private def getTransferInfo[T: Reads](data: Transfer) = {
		// Go retrieve both objects via futures
		for {
			to <- trackerCollection.find(Json.toJson(Json.obj(Component.idKey -> data.to))).one
			from <- trackerCollection.find(Json.toJson(Json.obj(Component.idKey -> data.from))).one
		} yield {
			// Method to set form with missing ID(s) - errs is a list of form keys for ID(s) not found
			def missingIDs(errs: List[String]) = {
				val notFoundErrs : Map[Option[String], String] = errs.map(Some(_) -> "ID not found").toMap
				Errors.fillAndSetFailureMsgs(notFoundErrs, Transfer.form, data)
			}
			// Check out results from DB queries
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
		// Convert json to component object
		def getComponent(json: JsObject) = {
			val componentType = (json \ Component.typeKey).as[ComponentType]
			ComponentController.actions(componentType).jsonToComponent(json)
		}
		// Get to component
		val toC = getComponent(to)
		// Get from component and see if transfer from it is valid
		getComponent(from) match {
			case fromC: Component with Transferrable if fromC.validTransfers.contains(toC.component) =>
				(fromC, toC, None)
			case fromC => (fromC, toC, Some("Can't do transfer from a " + fromC.component.toString +
				" to a " + toC.component.toString))
		}
	}

	/**
	 * Make message describing component transfer
 	 * @param from component we're transferring from
	 * @param to component we're transferring to
	 * @return componentType ID to ComponentType ID
	 */
	private def fromToMsg(from: Component, to: Component) = {
		def componentStr(c: Component) = c.component.toString + " " + c.id
		componentStr(from) + " to " + componentStr(to)
	}

	/**
	 * Result when transfer completes
	 * @param getMsg callback to get messages to set in response form
	 * @return redirect to index with wanted message
	 */
	private def transferCompleteResult(getMsg: () => String) =
		FlashingKeys.setFlashingValue(Redirect(routes.Application.index()), FlashingKeys.Status, getMsg())

	/**
	 * Result when transfer needs more information
	 * @param data transfer info known so far
	 * @param fromQuad true if need query for quadrant to transfer from
	 * @param toQuad true if need query for quadrant to transfer to
	 * @return redirect to transferWithParams to query for additional information
	 */
	private def transferIncompleteResult(data: Transfer, fromQuad: Boolean, toQuad: Boolean) = {
		val result = Redirect(routes.TransferController.transferWithParams(
			data.from, data.to, Some(fromQuad), Some(toQuad), None, None))
		FlashingKeys.setFlashingValue(result, FlashingKeys.Status, "Fill in additional data to complete transfer")
	}

	/**
	 * Result when transfer form had errors
	 * @param form form filled with error data
	 * @return BadRequest to transferStart with input form
	 */
	private def transferErrorResult(form: Form[Transfer]) = BadRequest(views.html.transferStart(form))

	/**
	 * Result when transfer had errors
	 * @param data data found in transfer form
	 * @param errs errors to set in form
	 * @return BadRequest to transferStart with form set with errors
	 */
	private def transferErrorResult(data: Transfer, errs: Map[Option[String], String]) =
		transferErrorResult(Errors.fillAndSetFailureMsgs(errs, Transfer.form, data))


	/**
	 * Start of transfer - we look over IDs and see what's possible.  Depending type of components being transferred
	 * different additional information may be needed.
	 * @return action to see what step is next to complete transfer
	 */
	def transferIDs = Action.async { request =>
		Transfer.form.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(transferErrorResult(formWithErrors.withGlobalError(Errors.validationError))),
			data => {
				// Got data from form - get from and to data (as json) - map is mapping future from retrieving DB data
				getTransferInfo[JsObject](data).map {
					// Found both objects - now check if we can transfer between them
					case (Some((from, to)), None) =>
						isTransferValid(data, from, to) match {
							case (fromData, toData, None) if fromData.id == toData.id =>
								transferErrorResult(data, Map(None -> "Can not transfer component to itself"))
							case (fromData, toData, None) if fromData.isInstanceOf[ContainerDivisions] &&
									toData.isInstanceOf[ContainerDivisions] =>
								(fromData.asInstanceOf[ContainerDivisions].layout,
									toData.asInstanceOf[ContainerDivisions].layout) match {
									case (DIM8x12, DIM8x12) | (DIM16x24, DIM16x24) => transferCompleteResult(() =>
										"Transfer completed from " + fromToMsg(fromData, toData))
									case (DIM8x12, DIM16x24) =>
										transferIncompleteResult(data, fromQuad = false, toQuad = true)
									case (DIM16x24, DIM8x12) =>
										transferIncompleteResult(data, fromQuad = true, toQuad = false)
								}
							case (fromData, toData, None) => transferCompleteResult(() =>
								"Transfer completed from " + fromToMsg(fromData, toData))
							case (fromData, toData, Some(err)) => transferErrorResult(data, Map(None -> err))
						}
					// Couldn't find one or both data - form returned contains errors - return it now
					case (None, Some(form)) => transferErrorResult(form)
					// Should never have both or neither as None but...
					case _ => FlashingKeys.setFlashingValue(Redirect(routes.Application.index()),
						FlashingKeys.Status,"Internal error: Failure during transferIDs")
				}
			}.recover {
				case err => transferErrorResult(data, Map(None -> Errors.exceptionMessage(err)))
			}
		).recover {
			case err => transferErrorResult(Transfer.form.withGlobalError(Errors.exceptionMessage(err)))
		}
	}

	/**
	 * Do transfer based on form.
 	 * @return action to do transfer
	 */
	def transferFromForm = Action.async { request =>
		Transfer.form.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(transferErrorResult(formWithErrors.withGlobalError(Errors.validationError))),
			data => {
				transferCollection.insert(data).map { lastError =>
					val success = s"Successfully inserted transfer from ${data.from} to ${data.to}"
					Logger.debug(s"$success with status: $lastError")
					transferCompleteResult(() => {
						def quadDesc(id: String, quad: Option[Quad]) =
							(if (quad.isDefined) quad.get.toString + " of " else "") + id
						"Transfer completed from " + quadDesc(data.from, data.fromQuad) +
							" to " + quadDesc(data.to, data.toQuad)	 // Return view of data just created
					})}
			}.recover {
				case err => transferErrorResult(data, Map(None -> Errors.exceptionMessage(err)))
			}
		).recover {
			case err => transferErrorResult(Transfer.form.withGlobalError(Errors.exceptionMessage(err)))
		}
	}

}
