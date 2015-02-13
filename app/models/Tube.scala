package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json,Format}
import Component._

/**
 * @author Nathaniel Novod
 *         on 11/21/14
 */
/**
 * Tube - it's a container (can have initial contents) that can have a location (e.g., we can put it in a freezer) and
 * can have stuff transferred out of.
 *
 * @param id tube barcode
 * @param description optional description of tube
 * @param project optional project ID
 * @param tags name/value pairs to associate with the tube
 * @param locationID Optional ID of Component where this tube is located
 * @param contentID Optional ID of Component describing the initial contents of this tube
 */
case class Tube(override val id: String,override val description: Option[String],override val project: Option[String],
                override val tags: List[ComponentTag],
                override val locationID: Option[String],override val contentID: Option[String])
	extends Component with Location with Container with Transferrable {
	override val component = Tube.componentType
	override val validLocations = List(ComponentType.Freezer,ComponentType.Plate)
	override val validTransfers = List(ComponentType.Plate,ComponentType.Tube,ComponentType.Well)
	override val validContents = List(ComponentType.Material)
}

/**
 * Tube companion object
 */
object Tube extends ComponentObject[Tube](ComponentType.Tube) {
	/**
	 * Form mapping for a tube.  Note that component contents must be referred to as componentData.fieldName in forms
	 * since clean inheritance doesn't appear to be possible.  That's why the (un)applyWithComponent is needed
	 * (see componentMap for more details).
	 */
	private def applyWithComponent(c: Component,l: Option[String],con: Option[String]): Tube =
		Tube(c.id,c.description,c.project,c.tags,l,con)

	private def unapplyWithComponent(t: Tube) = Some(t.getComponent,t.locationID,t.contentID)

	override val form = Form(
		mapping(
			Component.formKey -> Component.componentMap,
			Location.locationKey -> optional(text),
			Container.contentKey -> optional(text)
		)(applyWithComponent)(unapplyWithComponent))

	/**
	 * Formatter for going to/from and validating Json
	 */
	override implicit val formatter: Format[Tube] = formatWithComponent(Json.format[Tube])
}
