/**
 * Test spec trait that includes before and after operations
 * Created by nnovod on 4/26/15.
 */
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.PlaySpec

trait TestSpec extends PlaySpec with BeforeAndAfter {
	before {
		TestDB.cleanupTestJiraDB
		TestDB.cleanupTestTrackerCollection
		TestDB.cleanupTestTransferCollection
	}

	after {
		// Run some stuff here too
	}
}

