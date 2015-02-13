package models

/**
 * @author Nathaniel Novod
 *         on 11/23/14
 */

import Component.ComponentType
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Format,Json}
import play.api.data.format.Formats._

/**
 * Freezer - it's a component where other components can be stored (i.e., it can be a location for other components).
 * Note that a freezer has an address, not a location.  It can't be stored inside another component).
 *
 * @param id freezer barcode
 * @param description optional description of freezer
 * @param project optional project ID
 * @param tags name/value pairs to associate with the freezer
 * @param address where the freezer is
 * @param temperature degrees celsius inside freezer
 */
case class Freezer(override val id: String,override val description: Option[String],override val project: Option[String],
                   override val tags: List[ComponentTag],address: String,temperature: Float) extends Component {
	override val component = Freezer.componentType
}

object Freezer extends ComponentObject[Freezer](ComponentType.Freezer) {
	/**
	 * Form mapping for a freezer.  Note that component contents must be referred to as componentData.fieldName in forms
	 * since clean inheritance doesn't appear to be possible.  That's why the (un)applyWithComponent is needed
	 * (see componentMap for more details).
	 */
	private def applyWithComponent(c: Component,l: String,t: Float): Freezer =
		Freezer(c.id,c.description,c.project,c.tags,l,t)

	private def unapplyWithComponent(f: Freezer) = Some(f.getComponent,f.address,f.temperature)

	val addressKey = "address"
	val temperatureKey = "temperature"
	override val form = Form(
		mapping(
			Component.formKey -> Component.componentMap,
			addressKey -> nonEmptyText,
			temperatureKey -> of[Float]
		)(applyWithComponent)(unapplyWithComponent))

	/**
	 * Formatter for going to/from and validating Json
	 */
	override implicit val formatter: Format[Freezer] = formatWithComponent(Json.format[Freezer])
}
