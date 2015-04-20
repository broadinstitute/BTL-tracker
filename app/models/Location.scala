package models

import Component.ComponentType.ComponentType
import controllers.ComponentController

/**
 * A location is someplace a component can be stored.  The location ID must point to another container - for example
 * tubes or plates can be located in a freezer.  Locations can also be recursive, meaning that one component can
 * be located in another which in turn is located in another component.  For example wells are located on plates
 * and plates can be located in a freezer.
 * Created by nnovod on 11/28/14.
 */
trait Location {
	/**
	 * Component ID for location
	 */
	val locationID: Option[String]
	/**
	 * List of component types that can be a location for object.
	 */
	val validLocations: List[ComponentType]

	/**
	 * Method to see if id set for a location is valid.  Note operation is done via a future.
	 * @return None if id found, otherwise error message
	 */
	def isLocationValid = ComponentController.isIdValid(locationID,validLocations,"Location")
}

object Location {
	val locationKey = "location"
}
