package models

import scala.concurrent.Future

/**
 * Created by nnovod on 3/31/16.
 */
trait SubComponents {
	/**
	 * Subcomponent fetcher - given a component get a map of it's wells to subcomponent ids
	 */
	type SubFetcher = () => Future[(Map[String, String], Option[String])]

	/**
	 * Get sub component fetcher, if one exists
	 * @return method to retrieve subcomponents
	 */
	def getSubFetcher : Option[SubFetcher]
}
