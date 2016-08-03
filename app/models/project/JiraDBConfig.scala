package models.project

import org.broadinstitute.LIMStales.mongo.config.DatabaseUsingConfigFactory
import org.broadinstitute.LIMStales.mongo.
{BtllimsPlateOpers,BtllimsBspOpers,BtllimsRackOpers,BtllimsSampleMapOpers}
import org.broadinstitute.LIMStales.mongo.
{BtllimsPlateCollection,BtllimsBspCollection, BtllimsRackCollection,BtllimsSampleMapCollection}
import play.api.Play

/**
 * Configuration put on top of regular DB configuration to allow for play configuration to override regular
 * config factory.  This matters when fakeapplication is used - in that case when configuration settings
 * are set they are not picked up by the regular config factory but are seen in play's current configuration.
 * Created by nnovod on 4/28/15.
 */
trait JiraDBConfig extends DatabaseUsingConfigFactory {
	abstract override def host = Play.current.configuration.getString("DBConfig.host").getOrElse(super.host)
	abstract override def port = Play.current.configuration.getInt("DBConfig.port").getOrElse(super.port)
	abstract override def jiraDB = Play.current.configuration.getString("DBConfig.jiraDB").getOrElse(super.jiraDB)
	abstract override def plateCollection =
		Play.current.configuration.getString("DBConfig.plateCollection").getOrElse(super.plateCollection)
	abstract override def bspCollection =
		Play.current.configuration.getString("DBConfig.bspCollection").getOrElse(super.bspCollection)
	abstract override def rackCollection =
		Play.current.configuration.getString("DBConfig.rackCollection").getOrElse(super.rackCollection)
	abstract override def sampleMapCollection =
		Play.current.configuration.getString("DBConfig.sampleMapCollection").getOrElse(super.sampleMapCollection)
}

object JiraDBConfig extends JiraDBConfig

/**
 * DB access defined using configuration factory
 */
object JiraDBs {
	object BtllimsRackOpers extends BtllimsRackCollection(JiraDBConfig) with BtllimsRackOpers
	object BtllimsBspOpers extends BtllimsBspCollection(JiraDBConfig) with BtllimsBspOpers
	object BtllimsPlateOpers extends BtllimsPlateCollection(JiraDBConfig) with BtllimsPlateOpers
	object BtllimsSampleMapOpers extends BtllimsSampleMapCollection(JiraDBConfig) with BtllimsSampleMapOpers
}


