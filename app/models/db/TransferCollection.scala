package models.db

import models.Transfer
import play.api.{Play, Logger}
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONArray, BSONDocument}
import reactivemongo.core.commands.Count
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Module to interact with the transfer collection.
 * Created by nnovod on 4/23/15.
 */
object TransferCollection extends Controller with MongoController {
	/**
	 * Get transfer collection name.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection name
	 */
	private def transferCollectionName =
		Play.current.configuration.getString("mongodb.collection.transfer").getOrElse("transfer")

	/**
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses JSON for input/output of transfer data
	 */
	private def transferCollection: JSONCollection = db.collection[JSONCollection](transferCollectionName)

	/**
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses BSON for input/output of transfer data
	 */
	private def transferCollectionBSON: BSONCollection = db.collection[BSONCollection](transferCollectionName)

	/**
	 * Get BSON document query for transfers to and from a component
	 * @param id component id
	 * @return query to find transfers directly to/from a component
	 */
	private def transferBson(id: String) = BSONDocument("$or" -> BSONArray(
		BSONDocument("from" -> id),
		BSONDocument("to" -> id)))

	/**
	 * Remove transfers involving a component.
	 * @param id component id
	 * @return future with optional string containing error
	 */
	def removeTransfers(id: String) = {
		transferCollectionBSON.remove(transferBson(id)).map {
			(lastError) => {
				Logger.debug(s"Successfully deleted transfers for $id with status: $lastError")
				None
			}
		}.recover {
			case err => Some(err)
		}
	}

	/**
	 * Get count of transfers involving a component.
	 * @param id component id
	 * @return future with optional string containing error
	 */
	def countTransfers(id: String) = {
		val command = Count(transferCollectionName, Some(transferBson(id)))
		db.command(command)
	}

	/**
	 * Future to get list of component that are source/destination of transfers to/from a specified component
	 * @param id component id
	 * @param directionKey transfer key to indicate to or from
	 * @return list of components that were target or source of transfers to specified component
	 */
	private def getTransferIDs(id: String, directionKey: String) = {
		val cursor = transferCollectionBSON.find(BSONDocument(directionKey -> id)).cursor
		cursor.collect[List]()
	}

	/**
	 * Future to get list of components that were directly transferred into a component id
	 * @param id component ID
	 * @return list of components that were directly transferred to the input component id
	 */
	def getSourceIDs(id: String) = getTransferIDs(id, Transfer.toKey)

	/**
	 * Future to get list of components that were directly transferred from a component id
	 * @param id component ID
	 * @return list of components that were directly transferred to from the input component id
	 */
	def getTargetIDs(id: String) = getTransferIDs(id, Transfer.fromKey)

	/**
	 * Insert transfer into DB.
	 * @param data transfer to record in DB
	 * @return result to return with completion status
	 */
	def insert(data: Transfer) = {
		transferCollection.insert(data).map {
			(lastError) => {
				Logger.debug(s"Successfully inserted ${data.quadDesc} with status: $lastError")
				lastError
			}
		}
	}
}
