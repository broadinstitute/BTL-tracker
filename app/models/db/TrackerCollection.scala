package models.db

import utils.MessageHandler
import models.Component
import play.api.{Play, Logger}
import play.api.libs.json._
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.core.commands.LastError

import scala.concurrent.Future

/**
 * MondgoDB tracker collection operations
 * Created by nnovod on 4/23/15.
 */
object TrackerCollection extends Controller with MongoController {
	/**
	 * Get tracker collection name.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection name
	 */
	private def trackerCollectionName =
		Play.current.configuration.getString("mongodb.collection.tracker").getOrElse("tracker")

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
				Logger.debug(s"Completed delete of component $id with status: $lastError")
				if (lastError.ok) None
				else throw new Exception(s"DB error: ${lastError.errMsg.getOrElse("unknown")}")
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
		doComponentDBOperation(trackerCollection.insert(_: C), "registration", data, onSuccess, onFailure)

	/**
	 * Insert components, via reactive mongo, into the tracker DB
	 * @param data components to insert
	 * @tparam C component type
	 * @return future with two lists, one of success messages, one of failure messages
	 */
	def insertComponents[C <: Component : Format](data: List[C]) = {
		val okPrefix = "OK:"
		val notOkPrefix = "NOK:"
		val inserts = for (d <- data) yield {
			doComponentDBOperation(trackerCollection.insert(_: C), "registration", d,
				(s: String) => s"$okPrefix$s", (t: Throwable) => s"$notOkPrefix${MessageHandler.exceptionMessage(t)}")
		}
		Future.fold(inserts)((List.empty[String], List.empty[String])){
			case ((ok: List[String], bad: List[String]), next: String) =>
				if (next.startsWith(okPrefix)) (ok :+ next.substring(okPrefix.length), bad)
				else if (next.startsWith(notOkPrefix)) (ok, bad :+ next.substring(notOkPrefix.length))
				else (ok, bad)
		}
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
	def updateComponent[C <: Component : Format, R](data: C, onSuccess: (String) => R, onFailure: (Throwable) => R) = {
		val selector = Json.obj(Component.idKey -> data.id, Component.typeKey -> data.component.toString)
		doComponentDBOperation(trackerCollection.update(selector, _: C), "update", data, onSuccess, onFailure)
	}

	/**
	 * Do component database operation.
	 * @param oper database function to execute
	 * @param operLabel label for operation logging message
	 * @param data component on which to do operation
	 * @param onSuccess callback if all goes well
	 * @param onFailure callback if problems
	 * @tparam C component type
	 * @tparam R return type of callbacks
	 * @return future with return type
	 */
	private def doComponentDBOperation[C <: Component, R](oper: (C) => Future[LastError], operLabel: String, data: C,
														  onSuccess: (String) => R, onFailure: (Throwable) => R) = {
		// Do db operation
		oper(data).map {
			// All went well - log that and call back with success
			lastError =>
				val success = s"Completed $operLabel of component ${data.id}"
				Logger.debug(s"$success with status: $lastError")
				if (lastError.ok)
					onSuccess(success)
				else
					throw new Exception(s"DB error: ${lastError.errMsg.getOrElse("unknown")}")
		}.recover {
			// Problems - callback to report error
			case err => onFailure(err)
		}
	}

	/**
	 * Get the set of tag values.  Unfortunately reactive mongo doesn't have a distinct command for a collection so
	 * we have to do a "raw" command to find the tag values.
	 * @return set of tags found
	 */
	def getTags = {
		import reactivemongo.core.commands.RawCommand
		val command = RawCommand(BSONDocument("distinct" -> trackerCollectionName, "key" -> "tags.tag"))
		// Execute command and map the results into a list (it's actually a set but list is more efficient and
		// uniqueness is already guaranteed by distinct command)
		db.command(command).map((doc) => {
			// "values" is the key used in the document when it is returning a set of values
			doc.getAs[List[String]]("values") match {
				case Some(nextVal) => (None, nextVal)
				case None => (None, List.empty[String])
			}
		}).recover{
			case e: Exception => (Some(s"Exception during tag retrieval: ${MessageHandler.exceptionMessage(e)}"),
				List.empty[String])
		}
	}
}
