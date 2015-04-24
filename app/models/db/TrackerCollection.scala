package models.db

import models.Component
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import play.api.libs.concurrent.Execution.Implicits.defaultContext


/**
 * MondgoDB tracker collection operations
 * Created by nnovod on 4/23/15.
 */
object TrackerCollection extends Controller with MongoController {
	// Collection name
	private val trackerCollectionName = "tracker"

	/**
	 * Get collection to do JSON mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses JSON for input/output of tracker data
	 */
	private def trackerCollection: JSONCollection = db.collection[JSONCollection](trackerCollectionName)

	/**
	 * Get collection to do BSON mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses BSON for input/output of tracker data
	 */
	private def trackerCollectionBSON: BSONCollection = db.collection[BSONCollection](trackerCollectionName)

	/**
	 * Get BSON document query for tracker entry for a component
	 * @param id component id
	 * @return query to find tracker component
	 */
	private def trackerBson(id: String) = BSONDocument(Component.idKey -> id)

	/**
	 * Remove entry for a component.
	 * @param id component id
	 * @return future with optional string containing error
	 */
	def remove(id: String) = {
		trackerCollectionBSON.remove(trackerBson(id)).map {
			(lastError) => {
				Logger.debug(s"Successfully deleted component $id with status: $lastError")
				None
			}
		}.recover {
			case err => Some(err)
		}
	}

	/**
	 * Do query returning a cursor that will asynchronously get results.
	 * @param findQuery tracker DB query
	 * @return cursor that will retrieve results asynchronously
	 */
	def findWithQuery(findQuery: BSONDocument) =
		trackerCollectionBSON.find(findQuery).cursor[BSONDocument]

	/**
	 * Find components from a list of IDs
	 * @param ids component IDs
	 * @return cursor where all found entries are collected
	 */
	def findIds(ids: List[String]) = {
		val query = BSONDocument(Component.idKey -> BSONDocument("$in" -> ids))
		findWithQuery(query).collect[List]()
	}

	/**
	 * Do find with Json query.
	 * @param query Json value to query for
	 * @tparam T Result type (must have implicit Reads)
	 * @return single object matching query
	 */
	def findOneWithJsonQuery[T: Reads](query: JsValue) =
		trackerCollection.find(query).one

	/**
	 * Get one component entry.  Done in Json so Json reader can be used.
	 * @param id component ID
	 * @tparam T result type (must have implicit Reads)
	 * @return single object returned by query
	 */
	def findID[T: Reads](id: String) =
		findOneWithJsonQuery(Json.toJson(Json.obj(Component.idKey -> id)))

	/**
	 * Insert a component, via reactive mongo, into the tracker DB
	 * @param data component to insert
	 * @param onSuccess callback upon success (paramater is success message)
	 * @param onFailure callback upon failure (parameter is exception)
	 * @tparam C component type
	 * @tparam R return type of callbacks
	 * @return future with return type
	 */
	def insertComponent[C <: Component : Format, R](data: C, onSuccess: (String) => R, onFailure: (Throwable) => R) =
		trackerCollection.insert(data).map { lastError =>
			val success = s"Successfully inserted item ${data.id}"
			Logger.debug(s"$success with status: $lastError")
			onSuccess(success)
		}.recover {
			case err => onFailure(err)
		}

	/**
	 * Update a component, via reactive mongo, into the tracker DB
	 * @param data component to update
	 * @param onSuccess callback upon success (paramater is success message)
	 * @param onFailure callback upon failure (parameter is exception)
	 * @tparam C component type
	 * @tparam R return type of callbacks
	 * @return future with return type
	 */
	def updateComponent[C <: Component : Format, R](selector: JsObject, data: C,
													onSuccess: (String) => R, onFailure: (Throwable) => R) =
		trackerCollection.update(selector, data).map { lastError =>
			val success = s"Successfully updated item ${data.id}"
			Logger.debug(s"$success with status: $lastError")
			onSuccess(success)
		}.recover {
			case err => onFailure(err)
		}
}
