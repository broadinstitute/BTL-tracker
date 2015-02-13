package models

import Component.ComponentType
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Format,Json}

/**
 * @author Nathaniel Novod
 *         on 11/23/14
 */
/**
 * Sample - It can have a location (e.g., we can put it in a freezer) and can have stuff transferred out of.  Note
 * that it's not a container because you can not set initial contents for it.  It is what it is - you can't put
 * something into it.
 *
 * @param id sample barcode
 * @param description optional description of sample
 * @param project optional project ID
 * @param tags name/value pairs to associate with the sample
 * @param locationID Optional ID of Component where this sample is located
 * @param externalID external ID for sample
 */
case class Sample(override val id: String,override val description: Option[String],override val project: Option[String],
                  override val tags: List[ComponentTag],
                  override val locationID: Option[String],externalID: String)
	extends Component with Location with Transferrable {
	override val component = Sample.componentType
	override val validLocations = List(ComponentType.Freezer)
	override val validTransfers = List(ComponentType.Plate,ComponentType.Tube,ComponentType.Well)
}

object Sample extends ComponentObject[Sample](ComponentType.Sample) {
	/**
	 * Form mapping for a sample.  Note that component contents must be referred to as componentData.fieldName in forms
	 * since clean inheritance doesn't appear to be possible.  That's why the (un)applyWithComponent is needed
	 * (see componentMap for more details).
	 */
	private def applyWithComponent(c: Component,l: Option[String],eID: String) =
		Sample(c.id,c.description,c.project,c.tags,l,eID)

	private def unapplyWithComponent(s: Sample) = Some(s.getComponent,s.locationID,s.externalID)

	val externalIDKey = "externalID"
	override val form = Form(
		mapping(
			Component.formKey -> Component.componentMap,
			Location.locationKey -> optional(text),
			externalIDKey -> nonEmptyText
		)(applyWithComponent)(unapplyWithComponent))

	/**
	 * Formatter for going to/from and validating Json
	 */
	override implicit val formatter: Format[Sample] = formatWithComponent(Json.format[Sample])
}
