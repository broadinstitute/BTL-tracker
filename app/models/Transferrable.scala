package models

import Component.ComponentType.ComponentType

/**
 * Created by nnovod on 11/28/14.
 */
/**
 * Transferrable trait indicates that contents of a component can be transferred to other components of the type
 * listed below as valid.  A component's contents are both explicit initial contents as well as contents transferred
 * into the component.
 */
trait Transferrable {
	/**
	 * List of components that this component's contents can be transferred to.
	 */
	val validTransfers: List[ComponentType]
}
