/**
 * DB cleanup interactions
 * Created by nnovod on 4/30/15.
 */

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import scala.concurrent.{Await, duration}
import duration._

object TestDB extends Controller with MongoController {
	// Test Jira DB
	val testJira = "jiraTest"
	// Test host
	val testHost = "localhost"
	// Test tracker collection
	val testTrackerCollection = "trackerTest"
	// Test transfer collection
	val testTransferCollection = "transferTest"
	// Timeout for wait for async operations
	val timeout = Duration(2000, MILLISECONDS)

	/**
	 * Delete the test Jira database
	 * @return true if deletion went ok
	 */
	def cleanupTestJiraDB = Await.result(connection.db(testJira).drop().recover{case _ => true}, timeout)

	/**
	 * Delete a collection in the default database
	 * @param collection collection name
	 * @return true if deletion went ok
	 */
	private def cleanupCollection(collection: String) =
		Await.result(db.collection[BSONCollection](collection).drop().recover{case _ => true}, timeout)

	/**
	 * Delete test tracker collection.
	 * @return true if deletion went ok
	 */
	def cleanupTestTrackerCollection = cleanupCollection(testTrackerCollection)

	/**
	 * Delete test transfer collection.
	 * @return true if deletion went ok
	 */
	def cleanupTestTransferCollection = cleanupCollection(testTransferCollection)
}
