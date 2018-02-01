package models.db

import akka.actor.Status.{Failure, Success}
import models.DBBarcodeSet.DBWell
import models.DBBarcodeWell
import models.initialContents.MolecularBarcodes.MolBarcode
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import play.api.mvc.Controller
import play.api.Play
import play.modules.reactivemongo.MongoController

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

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
		* Custom read/write for maps.
		*/
//	implicit object BSONMap extends BSONDocumentWriter[Map[String, DBBarcodeWell]] with BSONDocumentReader[Map[String, DBBarcodeWell]] {
//
//		def read(bson: BSONDocument): Map[String, DBBarcodeWell] = {
//			val elements = bson.elements.map {
//				// assume that all values in the document are BSONDocuments
//				case (key, value) => key -> DBBarcodeWell.reader.read(value.seeAsTry[BSONDocument].get)
//			}
//			elements.toMap
//		}
//
//
//		def write(map: Map[String, DBBarcodeWell]): BSONDocument = {
//			val elements = map.toStream.map {
//				case (key, value) => key -> DBBarcodeWell.writer.write(value)
//			}
//			BSONDocument(elements)
//		}
//	}
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

	def deleteByID(id: BSONObjectID) = {
		collection.remove(BSONDocument("_id" -> id))
	}


	/**
	  * Update if document exists, otherwise insert
	  * @param selector BSON DB query to select entries to update
	  * @param entry new entry settings
	  * @return future for write result
	  */
	def upsert(selector: BSONDocument, entry: T) =
		collection.update(selector, entry, upsert = true)


}
