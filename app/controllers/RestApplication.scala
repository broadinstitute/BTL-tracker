package controllers

import models.db.TrackerCollection
import models.{Tube,Component}
import play.api.libs.json.{JsValue,Format}
import play.api.mvc.{Action,Controller,Request}

import scala.concurrent.Future

/**
 * @author Nathaniel Novod
 *         Date: 11/26/14
 *         Time: 3:10 PM
 */
object RestApplication extends Controller {
	/**
	 * Create a tracker item using Json as input.
	 * @param request http request with json
	 * @tparam I type of item to be created
	 * @return method that will be executed asynchronously
	 */
	private def createItem[I <: Component : Format](request: Request[JsValue]) =
		request.body.validate[I].map { item => // Object created from validated json
			TrackerCollection.insertComponent(item,
				onSuccess = (msg) => Created,
				onFailure = (err) => InternalServerError(err.getLocalizedMessage))
		}.getOrElse(Future.successful(BadRequest("Invalid input"))) // validation failed

	/**
	 * Go create a tube from input JSON.
	 *
	 * @return request response - usually just a status and error text if there's an error
	 */
	def createTube() = Action.async(parse.json) {
		createItem[Tube]
	}
}
