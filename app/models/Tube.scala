package models

import formats.CustomFormats._
import mappings.CustomMappings._
import models.initialContents.InitialContents
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json,Format}
import Component._
import initialContents.InitialContents.ContentTypeT

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
 * @param initialContent Optional initial contents
 */
case class Tube(override val id: String,override val description: Option[String],override val project: Option[String],
                override val tags: List[ComponentTag],
                override val locationID: Option[String],override val initialContent: Option[ContentTypeT])
	extends Component with Location with Container with Transferrable with ComponentCanBeList[Tube] {
	override val component = Tube.componentType
	override val validLocations = Tube.validLocations
	override val validTransfers = Tube.validTransfers
	override val validContents = Tube.validContents

	/**
	 * Make a new component that includes the ability to make a list of individual components from the ID list.
	 */
	def toComponentList =
		new Tube(id, description, project, tags, locationID, initialContent) with ComponentList[Tube] {
			/**
			 * Make a copy of this component with a new ID
			 * @param newId new ID to set in component copy
			 * @return component that's a copy of this one except with a new ID
			 */
			def idCopy(newId: String) = this.copy(id = newId)
		}
}

/**
 * Tube companion object
 */
object Tube extends ComponentObject[Tube](ComponentType.Tube) {
	val validLocations = List(ComponentType.Freezer,ComponentType.Rack)
	val validTransfers = List(ComponentType.Tube, ComponentType.Plate, ComponentType.Rack)
	val validContents = InitialContents.ContentType.antiBodies.map(_.toString)

	/**
	 * Form mapping for a tube.  Note that component contents must be referred to as componentData.fieldName in forms
	 * since clean inheritance doesn't appear to be possible.  That's why the (un)applyWithComponent is needed
	 * (see componentMap for more details).
	 */
	private def applyWithComponent(c: Component,l: Option[String],con: Option[ContentTypeT]): Tube =
		Tube(c.id,c.description,c.project,c.tags,l,con)

	private def unapplyWithComponent(t: Tube) = Some(t.getComponent,t.locationID,t.initialContent)

//	implicit val contentTypeFormat: Format[ContentTypeT] = enumFormat(ContentTypeT)

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
