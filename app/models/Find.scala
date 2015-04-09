package models

import models.Component.OptionalComponentType
import play.api.libs.json.Json
import play.api.data.{ObjectMapping2, FieldMapping, Form}
import play.api.data.Forms._
import mappings.CustomMappings._

/**
 * Find
 *
 * @param id barcode
 * @param description optional description
 * @param project optional project ID
 * @param tags name/value pairs
 */
case class Find(id: Option[String], description: Option[String], project: Option[String],
				tags: List[ComponentTag], component: OptionalComponentType.Value, includeTransfers: Boolean)

/**
 * Model for doing Find command
 * Created by nnovod on 4/6/15.
 */
object Find {
	// Keys for fields in mappings
	val transfersKey = "doTransfers"
	// Little kludge - in Javascript to do tags the tag label always includes component form key first
	val tagsKey = Component.formKey + "." + Component.tagsKey

	/**
	 * Form mapping for finding a component.
	 */
	val findMap = mapping(
		Component.idKey -> optional(text),
		Component.descriptionKey -> optional(text),
		Component.projectKey -> optional(text),
		tagsKey -> list(ComponentTag.tagsForm.mapping),
		Component.typeKey -> enum(OptionalComponentType),
		transfersKey -> boolean
	)(Find.apply)(Find.unapply)

	/**
	 * Get list of find criteria
	 */
	val findCriteria = findMap.mappings.flatMap {
		case FieldMapping(key, _) => List(key)
		case ObjectMapping2(_, _, (tag,_),(value,_), _, _)
			if tag == ComponentTag.tagKey && value == ComponentTag.valueKey =>
			List(tagsKey)
		case _ => List.empty
	}

	/**
	 * Form for getting find criteria
	 */
	val form = Form(findMap)

	/**
	 * Formatter for going to/from and validating Json
	 */
	implicit val formatter = Json.format[Find]
}
