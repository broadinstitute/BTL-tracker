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
		BSONDocument(Transfer.fromKey -> id),
		BSONDocument(Transfer.toKey -> id)))

	/**
	 * Get BSON document query for transfer between two specified components
	 * @param from transfer source component
	 * @param to transfer target component
	 * @return query to find transfers directly between specified components
	 */
	private def transferBetweenBson(from: String, to: String) =
		BSONDocument(Transfer.fromKey -> from, Transfer.toKey -> to)

	/**
	 * Remove transfers.
	 * @param bson query to find transfers to be removed
	 * @return future with optional string containing error
	 */
	private def doRemove(bson: BSONDocument, whatDeleted: () => String) = {
		transferCollectionBSON.remove(bson).map {
			(lastError) => {
				Logger.debug(s"Completed deleting transfers ${whatDeleted()} with status: $lastError")
				if (lastError.ok) None
				else throw new Exception(s"DB error deleting transfers ${whatDeleted()}: ${lastError.errMsg.getOrElse("unknown")}")
			}
		}.recover {
			case err => Some(err)
		}
	}

	/**
	 * Remove transfers involving a component.
	 * @param id component id
	 * @return future with optional string containing error
	 */
	def removeTransfers(id: String) = doRemove(transferBson(id), () => s"containing $id")

	/**
	 * Remove transfers involving a component.
	 * @param from transfer source component id
	 * @param to transfer target component id
	 * @return future with optional string containing error
	 */
	def removeBetweenTransfers(from: String, to: String) =
		doRemove(transferBetweenBson(from, to), () => s"from $from to $to")

	/**
	 * Get count of transfers involving a component.
	 * @param bson callback to get BSON document to get count for
	 * @return future with optional string containing error
	 */
	private def getCount(bson: BSONDocument) = {
		val command = Count(transferCollectionName, Some(bson))
		db.command(command)
	}

	/**
	 * Get count of transfers involving a component.
	 * @param id component id
	 * @return future with optional string containing error
	 */
	def countTransfers(id: String) = getCount(transferBson(id))

	/**
	 * Get count of transfers between two components.
	 * @param from source component id
	 * @param to target component id
	 */
	def countBetweenTransfers(from: String, to: String) = getCount(transferBetweenBson(from, to))

	/**
	 * Future to get list of components that are source/destination of transfers to/from a specified component
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
		transferCollection.insert(data).map(
			(lastError) => {
				Logger.debug(s"Completed insert of ${data.quadDesc} with status: $lastError")
				if (lastError.ok) None
				else {
					val err = lastError.errMsg.getOrElse("Unknown database error")
					Some(s"Error during insert of ${data.quadDesc}: $err")
				}
			}
		).recover {
			case err => Some(s"Exception during insert of ${data.quadDesc}: ${err.getLocalizedMessage}")
		}
	}

	/**
	 * Future to get list of transfers between two specified component IDs
	 * @param from component transfer is coming from
	 * @param to component transfer is going to
	 * @return list of transfers between specified components
	 */
	def find(from: String, to: String) = {
		val cursor = transferCollectionBSON.find(transferBetweenBson(from, to)).cursor
		cursor.collect[List]()
	}
}
