package models

/**
 * @author Nathaniel Novod
 *         Date: 11/10/14
 *         Time: 4:46 PM
 */

import formats.CustomFormats._
import mappings.CustomMappings._
import Component.ComponentType
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.Future

/**
 * Base for all component objects.
 * @param componentType type of component (tube, plate, ...)
 * @tparam C object type of component
 */
abstract class ComponentObject[C <: Component](val componentType: ComponentType.ComponentType) {
	/**
	 * A bit of magic since the json automatically created from a Component object does not contain the component
	 * type (it's not in case class constructor).  We create a new format for components that includes the component
	 * type by including a json transformer that puts in the component type if it's not already there.
	 *
	 * @param base original format for type
	 * @return new format that includes component type
	 */
	protected def formatWithComponent(base: Format[C]) = {
		/*
		 * Make a transformer to convert the json to include a "component":"componentType".  If one's already there
		 * then pick it, otherwise create Json with the component type string.
		 *
		 * @return method to transform json
		 */
		def componentTransformer =
			__.json.update(
				(__ \ Component.typeKey).json.copyFrom(
					(__ \ Component.typeKey).json.pick orElse
						Reads.pure(Json.toJson(componentType.toString))))

		/**
		 * Return new format that includes transformer to add component type
		 */
		new Format[C] {
			def reads(json: JsValue): JsResult[C] =
				base.compose(componentTransformer).reads(json)

			def writes(c: C): JsValue = base.writes(c).transform(componentTransformer).asOpt.get
		}
	}

	// Create Format for ComponentType enum using our custom enum Reader and Writer
	implicit val componentTypeFormat: Format[Component.ComponentType.ComponentType] =
		enumFormat(Component.ComponentType)
	// Make sure a form is supplied by all inheriting classes
	val form: Form[C]
	// Make sure a formatter is supplied by all inheriting classes
	implicit val formatter: Format[C]
}

/**
 * User specified name/value pairs to associate with an instance.  Note that the value is optional to allow
 * simple tagging of an item.
 * @param tag tag name
 * @param value value to associated with tag
 */
case class ComponentTag(tag: String, value: Option[String])

object ComponentTag {
	/**
	 * Key strings to use for forms
	 */
	val tagKey = "tag"
	val valueKey = "value"
	/**
	 * Form mapping for a tag
	 */
	val tagsForm = Form(
		mapping(
			tagKey -> nonEmptyText,
			valueKey -> optional(text)
		)(ComponentTag.apply)(ComponentTag.unapply))

	// Formatting to go to/from json
	implicit val instanceTagFormat = Json.format[ComponentTag]
}

/**
 * Base for all items - mostly abstract values to be filled in by inheriting classes.
 */
trait Component {

	import Component._

	/**
	 * ID - unique ID for item.  Usually a barcode
	 */
	val id: String
	/**
	 * Optional description.
	 */
	val description: Option[String]
	/**
	 * Optional project (e.g., Jira) ID
	 */
	val project: Option[String]
	/**
	 * List of tags associated with item.  Each tag can optionally have a value associated with it.
	 */
	val tags: List[ComponentTag]

	/**
	 * Type of component (e.g., tube, plate, ...)
	 */
	val component: ComponentType.ComponentType // Type of item

	/**
	 * Method that can be overriden to check if data returned from request is valid - this is called as part of
	 * general check done in isRequestValid
	 * @return Future of map of fields to errors - empty if no errors found
	 */
	protected def isValid(request: Request[AnyContent]): Future[Map[Option[String], String]] =
		Future.successful(Map.empty[Option[String], String])

	/**
	 * Get anonymous class object for this component
	 * @return Component object
	 */
	def getComponent = Component(id, description, project, tags, component)

	/**
	 * Check if component is valid - check that location and container are valid and also any type specific check needed
	 * @return map of errors (fieldName->errorString).  Empty if no errors were found.
	 */
	def isRequestValid(request: Request[AnyContent]) = {
		val locValid =
			if (this.isInstanceOf[Location]) {
				this.asInstanceOf[Location].isLocationValid
			} else Future.successful(None)
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		for {loc <- locValid
		     valid <- isValid(request)
		} yield {
			// Futures complete - now return any errors reported
			(loc, valid) match {
				case (None, v) => v
				case (Some(loc), v) => Map(Some(Location.locationKey) -> loc) ++ v
			}
		}
	}

	/**
	 * Return hidden fields initialized to what was found in a component.  Hidden fields are used to pass original
	 * values for fields between the server and client.  The server sets them in a form and then reads them later
	 * when the form is returned after processing by the client.
	 * @return hidden fields found (for now just project)
	 */
	def hiddenFields = HiddenFields(project)
}

object Component {
	/**
	 * Apply for a component - it creates an anonymous class with the specific data.  We need this to create the form
	 * mapping below that requires an apply/unapply to go back and forth from forms to an object.
	 *
	 * @param id0 component id
	 * @param description0 component description
	 * @param project0 project id
	 * @param tags0 list of component tags
	 * @param component0 component type
	 * @return component object
	 */
	def apply(id0: String, description0: Option[String], project0: Option[String],
	          tags0: List[ComponentTag], component0: ComponentType.ComponentType) =
		new Component {
			override val id: String = id0
			override val description: Option[String] = description0
			override val project: Option[String] = project0
			override val tags: List[ComponentTag] = tags0
			override val component: ComponentType.ComponentType = component0
		}

	/**
	 * Unapply for component needed for object to form mapping.
	 *
	 * @param c component to be unapplied
	 * @return component data
	 */
	def unapply(c: Component) = Some((c.id, c.description, c.project, c.tags, c.component))

	/**
	 * Keyword to use for component type
	 */
	val typeKey = "component"

	/**
	 * Keywords used in .scala.html files to reference division with tags and functions to add/remove tags.
	 * We keep them here as constants so different files can be sure that they all use the same keywords.
	 * Our attempt to keep the javascript/html a bit safer.
	 */
	var inputDiv = "addInput"
	val addTag = "addNew"
	val remTag = "remNew"

	/**
	 * Keywords to use for component info with form
	 */
	val formKey = "componentData"
	val idKey = "id"
	val tagsKey = "tags"
	val descriptionKey = "description"
	val projectKey = "project"
	val hiddenProjectKey = "hiddenProject"

	/**
	 * Form mapping for a component.  Forms for objects that extend from component are a bit strange since forms do not
	 * easily allow for clean inheritance - you can say your form contains a mapping defined elsewhere but there doesn't
	 * appear to be an easy way to simply say that one mapping inherits from another to make the fields a single level.
	 * This all means that the objects that extend from a component, such as plate, must have forms that contain a
	 * component via the mapping below and the component fields must later be flattened into the extended object when
	 * an apply is done and unflattened for unapply.
	 * Note this also means component contents must be referred to as componentData.fieldName in the form.  To keep
	 * that consistent all mappings must point to the componentMap using the formKey value above as the key that points
	 * to the componentMap.
	 */
	val componentMap = mapping(
		idKey -> nonEmptyText,
		descriptionKey -> optional(text),
		projectKey -> optional(text),
		tagsKey -> list(ComponentTag.tagsForm.mapping),
		typeKey -> enum(ComponentType)
	)(Component.apply)(Component.unapply)

	/**
	 * Case classes to make a mapping for a form to see hidden fields.  We must make this multi-layered because
	 * the hidden fields are within the component.  Hidden fields appear only in the form - they are not
	 * transferred to the data object.  They can be used to save values across the request.  For example a project
	 * can be saved to see if it was changed during the request - if it was then additional validity checking may
	 * need to be done.
	 * @param fields contained class with hidden fields
	 */
	private case class HiddenComponent(fields: HiddenFields)

	/**
	 * The hidden fields within the component
	 * @param project project as set before update
	 */
	case class HiddenFields(project: Option[String])

	/**
	 * Form to get the hidden fields - a mapping to the component fields from which we pick out the hidden fields
	 */
	private val hiddenForm =
		Form(mapping(
			Component.formKey -> mapping(
				Component.hiddenProjectKey -> optional(text)
			)(HiddenFields.apply)(HiddenFields.unapply)
		)(HiddenComponent.apply)(HiddenComponent.unapply))

	/**
	 * Get a field that is hidden (in hidden form).
	 * @param found callback to get optional hidden field
	 * @param request request with form
	 * @return optional value found in form
	 */
	def getHiddenField[T](request: Request[AnyContent], found: (HiddenFields) => Option[T]) =
		hiddenForm.bindFromRequest()(request).fold(
			formWithErrors => None, // Shouldn't reach here unless hidden fields not in request
			userData => found(userData.fields))

	/**
	 * Get the hidden fields found.
	 * @param request request with form
	 * @return optional hidden fields found
	 */
	def getHiddenFields(request: Request[AnyContent]) = getHiddenField(request, Some(_))

	/**
	 * Enumeration for all component types
	 */
	object ComponentType extends Enumeration {
		type ComponentType = Value
		val Tube, Plate, Rack, Freezer = Value
	}

	/**
	 * List of component types as strings - make Plate and Rack first since they are most frequently used
	 */
	val componentTypes = {
		val listStart = List(ComponentType.Plate.toString, ComponentType.Rack.toString)
		listStart ++ ComponentType.values.map(_.toString).toList.filterNot(listStart.contains(_))
	}

	/**
	 * Small form that can hold error message (or nothing if to be used to just get global messages with a blank
	 * form).
	 */
	val errMsgKey = "msg"
	case class Error(msg: Option[String])
	val blankForm =
		Form(
			mapping(
				errMsgKey -> optional(text)
			)(Error.apply)(Error.unapply))

	/**
	 * Little form for just getting a component type
	 * @param t component type
	 */
	case class ComponentTypeClass(t: ComponentType.ComponentType)
	val typeForm =
		Form(
			mapping(
				typeKey -> enum(ComponentType)
			)(ComponentTypeClass.apply)(ComponentTypeClass.unapply))

	/**
	 * Little form for just getting an ID
	 * @param id id for component
	 */
	case class ComponentIDClass(id: String)
	val idForm =
		Form(
			mapping(
				idKey -> nonEmptyText
			)(ComponentIDClass.apply)(ComponentIDClass.unapply))
}
