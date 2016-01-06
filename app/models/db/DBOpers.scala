package models.db

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import play.api.mvc.Controller
import play.api.Play
import play.modules.reactivemongo.MongoController
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * MongoDB Database operations for a specified type that has a BSON reader/writer.
  * Created by nnovod on 8/28/15.
  */
trait DBOpers[T <: AnyRef] extends Controller with MongoController {
	/**
	  * Key to lookup in configuration for collection name
	  */
	protected val collectionNameKey: String

	/**
	  * Default name for collection if none found in configuration
	  */
	protected val collectionNameDefault: String

	/**
	  * Get collection name.  We use a def instead of a val to avoid hot-reloading problems.
	  * @return collection name
	  */
	private def collectionName =
		Play.current.configuration.getString(collectionNameKey).getOrElse(collectionNameDefault)

	/**
	  * Get collection to do BSON mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	  * @return collection that uses BSON for input/output of data
	  */
	private def collection: BSONCollection = db.collection[BSONCollection](collectionName)

	/**
	  * Reader to convert from BSON documents to object type (read and delete).
	  */
	implicit val reader: BSONDocumentReader[T]

	/**
	  * Writer to go from objects to BSON documents (create and update)
	  */
	implicit val writer: BSONDocumentWriter[T]

	/**
	  * Create a new entry in the DB collection.
	  * @param entry object instance to set in DB
	  * @return future for write result
	  */
	def create(entry: T) =
		collection.insert(entry)

	/**
	  * Read entires based on a query.
	  * @param query BSON DB query
	  * @return collection of wanted object type
	  */
	def read(query: BSONDocument) =
		collection.find(query).
			cursor[T].
			collect[List]()

	/**
	  * Update entries based on selector.
	  * @param selector BSON DB query to select entries to update
	  * @param entry new entry settings
	  * @return future for write result
	  */
	def update(selector: BSONDocument, entry: T) =
		collection.update(selector, entry)

	/**
	  * Delete entries equal to entry.
	  * @param entry entry to be deleted
	  * @return future for write result
	  */
	def delete(entry: T) =
		collection.remove(entry)

	/**
	  * Update if document exists, otherwise insert
	  * @param selector BSON DB query to select entries to update
	  * @param entry new entry settings
	  * @return future for write result
	  */
	def upsert(selector: BSONDocument, entry: T) =
		collection.update(selector, entry, upsert = true)
}
