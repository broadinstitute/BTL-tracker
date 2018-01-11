import models.{Component, Transfer}
import models.db.{TrackerCollection, TransferCollection}
import play.api.libs.json.Format

import scala.concurrent.Future

/**
 * DB operations needed for tests
 * Created by nnovod on 12/1/16.
 */
object  DBOpers {
	/**
	 * Insert a component into the DB
	 *
	 * @param data component to insert
	 * @tparam C type of component
	 * @return future with error message returned on failure
	 */
	def insertComponent[C <: Component : Format](data: C): Future[Option[String]] = {
		TrackerCollection.insertComponent(data, onSuccess = (s) => None,
			onFailure = (t) => Some(t.getLocalizedMessage))
	}


	/**
	 * Go insert a transfer
	 *
	 * @param transfer transfer to insert
	 * @return future to complete insert
	 */
	def insertTransfer(transfer: Transfer): Future[Option[String]] = TransferCollection.insert(transfer)

	/**
	 * Go find a transfer
	 *
	 * @param src source of transfer
	 * @param dest destination of transfer
	 * @return future to complete find
	 */
	def findTransfer(src: String, dest: String): Future[List[Transfer]] =
		TransferCollection.findTransfer(from = src, to = dest)
}
