package models

import Component.ComponentType.ComponentType
import controllers.ComponentController

/**
 * Created by nnovod on 11/28/14.
 */
/**
 * A Container is something that content can be put into.  There can be initial content, for example a Material can be
 * set as the initial content for a tube.  Ultimately the contents of a container consists of it's initial contents
 * as well as any additional contents transferred from other components.  Those additional contents are never
 * explicitly set anywhere - they are implicit in the graphs created that show the transfers that take place into
 * a container.
 */
trait Container {
	/**
	 * Component ID for initial content
	 */
	val contentID: Option[String]

	/**
	 * List of valid content component types for object's content.
	 */
	val validContents: List[ComponentType]

	/**
	 * Method to see if id set for a content is valid.  Note operation is done via a future.
	 * @return None if id found, otherwise error message
	 */
	def isContentValid = ComponentController.isIdValid(contentID,validContents,"Content")
}

object Container {
	// Label to use for content
	val contentKey = "content"
}
