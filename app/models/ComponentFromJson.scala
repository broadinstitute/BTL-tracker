package models

import models.Component.ComponentType
import play.api.libs.json.JsObject

/**
 * Little fellow to be able to convert from json to a model object
 * Created by nnovod on 4/23/15.
 */
object ComponentFromJson {
	/**
	  * Get a tube object from Json using a formatter from Tube.
	  * @param json json to be converted
	  * @return Tube created from json
	  */
	private def tubeFromJson(json: JsObject) = {
		// Play formatter for Json verification/conversion
		implicit val formatter = Tube.formatter
		json.as[Tube]
	}

	/**
	  * Get a rack object from Json using a formatter from Rack.
	  * @param json json to be converted
	  * @return Rack created from json
	  */
	private def rackFromJson(json: JsObject) = {
		// Play formatter for Json verification/conversion
		implicit val formatter = Rack.formatter
		json.as[Rack]
	}

	/**
	  * Get a freezer object from Json using a formatter from Freezer.
	  * @param json json to be converted
	  * @return Freezer created from json
	  */
	private def freezerFromJson(json: JsObject) = {
		// Play formatter for Json verification/conversion
		implicit val formatter = Freezer.formatter
		json.as[Freezer]
	}

	/**
	  * Get a plate object from Json using a formatter from Plate.
	  * @param json json to be converted
	  * @return Plate created from json
	  */
	private def plateFromJson(json: JsObject) = {
		// Play formatter for Json verification/conversion
		implicit val formatter = Plate.formatter
		json.as[Plate]
	}

	/**
	  * Map of calls to use for redirects to find (by id) or add a component and json to component conversions
	  */
	private val fromJson =
		Map(
			ComponentType.Freezer -> (freezerFromJson(_)),
			ComponentType.Plate -> (plateFromJson(_)),
			ComponentType.Rack -> (rackFromJson(_)),
			ComponentType.Tube -> (tubeFromJson(_))
		)

	/**
	  * Little check to make sure we didn't miss any components in the actions
	  */
	private val fromJsonKeys = fromJson.keySet
	assert(ComponentType.values.forall(fromJsonKeys.contains), "Incomplete fromJson map")

	/**
	  * Get component object from json.
	  * @param json input json
	  * @return component object
	  */
	def getComponent(json: JsObject) = {
		import models.Component.ComponentType._
		val componentType = (json \ Component.typeKey).as[ComponentType]
		fromJson(componentType)(json)
	}
}
