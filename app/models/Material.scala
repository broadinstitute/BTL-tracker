package models

import formats.CustomFormats._
import Component._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json,Format}
import mappings.CustomMappings._

/**
 * Created by nnovod on 11/27/14.
 */
/**
 * A material can be set as the initial contents for Components with the Container trait.
 * @param id material component ID
 * @param description optional description of material
 * @param project optional project ID
 * @param tags optional tags for material
 * @param materialType type of material
 *
 *                     Note that complete type (Material...) must be specified for materialType in case class due to bug:
 *                     https://github.com/playframework/playframework/issues/1469
 */
case class Material(override val id: String,override val description: Option[String],override val project: Option[String],
                    override val tags: List[ComponentTag],materialType: Material.MaterialType.MaterialType)
	extends Component {
	override val component = Material.componentType
}

object Material extends ComponentObject[Material](ComponentType.Material) {
	/**
	 * Form mapping for a material.  Note that component contents must be referred to as componentData.fieldName in forms
	 * since clean inheritance doesn't appear to be possible.  That's why the (un)applyWithComponent is needed
	 * (see componentMap for more details).
	 */
	private def applyWithComponent(c: Component,materialType: MaterialType.MaterialType): Material =
		Material(c.id,c.description,c.project,c.tags,materialType)

	private def unapplyWithComponent(m: Material) = Some(m.getComponent,m.materialType)

	val materialTypeKey = "materialType"
	// Supply our custom enum Reader and Writer for MaterialType enum
	implicit val materialTypeFormat: Format[Material.MaterialType.MaterialType] =
		enumFormat(Material.MaterialType)
	/**
	 * Material form
	 */
	override val form = Form(
		mapping(
			Component.formKey -> Component.componentMap,
			materialTypeKey -> enum(MaterialType)
		)(applyWithComponent)(unapplyWithComponent))

	/**
	 * Formatter for going to/from and validating Json
	 */
	override implicit val formatter: Format[Material] = formatWithComponent(Json.format[Material])

	/**
	 * Material types
	 */
	object MaterialType extends Enumeration {
		type MaterialType = Value
		val Unknown,Reagent,Antibody = Value
		val materialValues = MaterialType.values.map(_.toString).toList
	}

}
