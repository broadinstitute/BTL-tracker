package controllers

import models.{Component,Transfer}
import play.api.libs.json.JsObject
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.bson.BSONDocument
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.BSONFormats._

/**
 * @author Nathaniel Novod
 *         Date: 3/2/15
 *         Time: 12:02 PM
 */
object TransferHistory extends Controller with MongoController {

	/**
	 * Future to get list of components that were directly transferred into a component id
	 * @param id component ID
	 * @return list of components that were directly transferred to the input component id
	 */
	private def getPreviousIDs(id: String) = {
		val cursor = TransferController.transferCollection.find(BSONDocument(Transfer.toKey -> id)).cursor[BSONDocument]
		cursor.collect[List]()
	}

	/**
	 * Future to retrieve list of components, given list of component IDs
	 * @param ids list of component IDs
	 * @return list of components corresponding to IDs
	 */
	private def getComponents(ids: List[BSONDocument]) = {
		// flatMap to get rid of any froms that come back as None (shouldn't be any)
		val idList = ids.flatMap((x) => x.getAs[String](Transfer.fromKey))
		// Go find all the components that were in the from list
		val cursor = ComponentController.trackerCollection.find(BSONDocument(
			Component.idKey -> BSONDocument("$in" -> idList))).cursor[BSONDocument]
		cursor.collect[List]()
	}

	/**
	 * Get component object from bson.
	 * @param bson input bson
	 * @return component object
	 */
	private def getComponentObject(bson: BSONDocument) = {
		// Get json since model conversions are setup to do json reads/writes
		val json = BSONFormats.BSONDocumentFormat.writes(bson).as[JsObject]
		TransferController.getComponent(json)
	}

	/**
	 * Future to get component objects for the components transferred into a single component
	 * @param componentID we want to know what was transferred into this id
	 * @return list of component objects that are immediate sources for the specified component
	 */
	def getHistory(componentID: String) = {
		// First get list of components as BSON documents (note flatmap to avoid future of future)
		val previousComponents =
			getPreviousIDs(componentID).flatMap(getComponents)
		// Now convert BSON returned to component objects (first map for future, next to convert bson list)
		previousComponents.map(_.map(getComponentObject))
	}
}
