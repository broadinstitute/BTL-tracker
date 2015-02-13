package models

import Component.ComponentType
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Format,Json}

/**
 * @author Nathaniel Novod
 *         On 11/23/14.
 */
/**
 * Well - it's a container (can have initial contents) that can have a location (it's always on a plate) and
 * can have stuff transferred out of.
 *
 * @param id well barcode
 * @param description optional description of well
 * @param project optional project ID
 * @param tags name/value pairs to associate with the well
 * @param locationID Optional ID of Component where this well is located
 * @param contentID Optional ID of Component describing the initial contents of this well
 * @param column well column on plate
 * @param row well row on place
 */
case class Well(override val id: String,override val description: Option[String],override val project: Option[String],
                override val tags: List[ComponentTag],
                override val locationID: Option[String],contentID: Option[String],column: String,row: Int)
	extends Component with Location with Container with Transferrable {
	override val component = Well.componentType
	override val validLocations = List(ComponentType.Plate)
	override val validTransfers = List(ComponentType.Plate,ComponentType.Tube,ComponentType.Well)
	override val validContents = List(ComponentType.Material)
}

object Well extends ComponentObject[Well](ComponentType.Well) {
	/**
	 * Form mapping for a well.  Note that component contents must be referred to as componentData.fieldName in forms
	 * since clean inheritance doesn't appear to be possible.  That's why the (un)applyWithComponent is needed
	 * (see componentMap for more details).
	 */
	private def applyWithComponent(c: Component,l: Option[String],con: Option[String],col: String,r: Int) =
		Well(c.id,c.description,c.project,c.tags,l,con,col,r)

	private def unapplyWithComponent(w: Well) = Some(w.getComponent,w.locationID,w.contentID,w.column,w.row)

	val columnKey = "column"
	val rowKey = "row"
	override val form = Form(
		mapping(
			Component.formKey -> Component.componentMap,
			Location.locationKey -> optional(text),
			Container.contentKey -> optional(text),
			columnKey -> nonEmptyText,
			rowKey -> number(min = 0)
		)(applyWithComponent)(unapplyWithComponent))

	/**
	 * Formatter for going to/from and validating Json
	 */
	override implicit val formatter: Format[Well] = formatWithComponent(Json.format[Well])
}
