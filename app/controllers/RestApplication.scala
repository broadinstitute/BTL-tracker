package controllers

import models.{Tube,Component}
import play.api.Logger
import play.api.libs.json.{JsValue,Format}
import play.api.mvc.{Action,Controller,Request}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

import scala.concurrent.Future

/**
 * @author Nathaniel Novod
 *         Date: 11/26/14
 *         Time: 3:10 PM
 */
// @TODO Just an untested start
object RestApplication extends Controller with MongoController {
	/**
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 *
	 * @return collection that uses JSON for input/output
	 */
	private def collection: JSONCollection = db.collection[JSONCollection]("tracker")

	/**
	 * Create a tracker item using Json as input.
	 * @param request http request with json
	 * @tparam I type of item to be created
	 * @return method that will be executed asynchronously
	 */
	private def createItem[I <: Component : Format](request: Request[JsValue]) =
		request.body.validate[I].map { item => // Object created from validated json
			import play.api.libs.concurrent.Execution.Implicits.defaultContext
			collection.insert(item).map { lastError =>
				Logger.debug(s"Successfully inserted item ${item.id} with status: $lastError")
				Created // Return status 201
			}
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
