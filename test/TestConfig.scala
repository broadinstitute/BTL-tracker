/**
 * Framework for using test configuration
 * Created by nnovod on 4/26/15.
 */
import org.scalatest._
import play.api.Play
import play.api.test.FakeApplication

trait TestConfig extends SuiteMixin { this: Suite =>

	// Make fake application that uses test jira DB and test tracker and transfer collections
	implicit lazy val app: FakeApplication = new FakeApplication(
		// Overwrite configuration settings
		additionalConfiguration = Map(
		// First two must be loaded as VM parameters (e.g., -DDBConfig.host="localhost") since outside library
		// doesn't look at play current config
		// "DBConfig.host" -> "localhost",
		// "DBConfig.jiraDB" -> "jiraTest",
			"mongodb.collection.tracker" -> "trackerTest",
			"mongodb.collection.transfer" -> "transferTest"
		)
	)

	/**
	 * Make a stacked method (abstract override) that will be called and then can call it's parent.  Here we point the
	 * app to the fake application that includes our overwritten configuration settings.
	 * @param testName name of test to run
	 * @param args arguments to test
	 * @return status of test run
	 */
	abstract override def run(testName: Option[String], args: Args): Status = {
		Play.start(app)
		try {
			val newConfigMap = args.configMap + ("org.scalatestplus.play.app" -> app)
			val newArgs = args.copy(configMap = newConfigMap)
			// Make sure we're dealing with a fake jira DB
			import com.typesafe.config.ConfigFactory
			val conf = ConfigFactory.load()
			val confMust =
				"VM parameters -DDBConfig.host=\"localhost\" -DDBConfig.jiraDB=\"jiraTest\" must be specified"
			assert(conf.getString("DBConfig.host") == "localhost", confMust)
			assert(conf.getString("DBConfig.jiraDB") == "jiraTest", confMust)
			// Go run the test
			val status = super.run(testName, newArgs)
			status.whenCompleted { _ => Play.stop() }
			status
		}
		catch {
			case ex: Throwable =>
				Play.stop()
				throw ex
		}
	}
}
