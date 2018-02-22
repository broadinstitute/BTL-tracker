package models

import initialContents.InitialContents.ContentTypeT

/**
 * A Container is something that content can be put into.  There can be initial contents, for example barcodes can be
 * set as the initial content for a plate.  Ultimately the contents of a container consists of it's initial contents
 * as well as any additional contents transferred from other components.  Those additional contents are never
 * explicitly set anywhere - they are implicit in the graphs created that show the transfers that take place into
 * a container.
 * Created by nnovod on 11/28/14.
 */
trait Container {
	/**
	 * Initial contents
	 */
	val initialContent: Option[ContentTypeT]

	/**
	 * List of valid content types for initial contents.
	 */
	val validContents: List[ContentTypeT]
}

object Container {
	// Label to use for content
	val contentKey = "initialContent"
}
