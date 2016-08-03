package models

/**
 * @author Nathaniel Novod
 *         on 11/21/14
 */
import mappings.CustomMappings._
import models.ContainerDivisions.Division
import models.initialContents.InitialContents
import models.project.JiraProject
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json,Format}
import Component._
import play.api.mvc.{AnyContent, Request}
import initialContents.InitialContents.ContentType

/**
 * Plate - it's a container (can have initial contents) and can have a location (e.g., a freezer) and
 * can have stuff transferred out of it.  Normally a plate has wells that are subcomponents but for
 * efficiency reasons, if all the wells of a plate have the same contents and are always used together
 * then the plate itself becomes the container where everything that happens to it happens to all the wells
 * on the plate.
 *
 * @param id plate barcode
 * @param description optional description of plate
 * @param project optional project ID
 * @param tags name/value pairs to associate with the plate
 * @param locationID Optional ID of Component where this plate is located
 * @param initialContent Optional initial contents of this plate
 * @param layout well layout of plate
 */
case class Plate(override val id: String,override val description: Option[String],override val project: Option[String],
                 override val tags: List[ComponentTag],
                 override val locationID: Option[String],override val initialContent: Option[ContentType.ContentType],
                 override val layout: Division.Division)
	extends Component with Location with Container with Transferrable with JiraProject with ContainerDivisions
	with ComponentCanBeList[Plate] {
	override val component = Plate.componentType
	override val validLocations = Plate.validLocations
	override val validTransfers = Plate.validTransfers
	override val validContents = Plate.validContents

	import play.api.libs.concurrent.Execution.Implicits.defaultContext
	/**
	 * Check if what's set in request is valid - specifically we check if the project specified contains the plate
	 * and if the contents are valid for the size of the plate.
	 * @param request HTTP request (has hidden field with project set before update)
	 * @return Future of map of fields to errors - empty if no errors found
	 */
	override protected def isValid(request: Request[AnyContent]) =
		isProjectValid(getHiddenFields(request)).map((errMap) => {
			initialContent match {
				case Some(content) if !InitialContents.isContentValidForDivision(content, layout) =>
					errMap + (Some(Container.contentKey) -> s"$content is invalid for plate with $layout")
				case _ => errMap
			}
		})

	/**
	 * Make a new component that includes the ability to make a list of individual components from the ID list.
	 */
	def toComponentList =
		new Plate(id, description, project, tags, locationID, initialContent, layout) with ComponentList[Plate] {
			/**
			 * Make a copy of this component with a new ID
			 * @param newId new ID to set in component copy
			 * @return component that's a copy of this one except with a new ID
			 */
			def idCopy(newId: String) = this.copy(id = newId)
		}
}

object Plate extends ComponentObject[Plate](ComponentType.Plate) {
	/**
	 * Form mapping for a plate.  Note that component contents must be referred to as componentData.fieldName in forms
	 * since clean inheritance doesn't appear to be possible.  That's why the (un)applyWithComponent is needed
	 * (see componentMap for more details).
	 */
	private def applyWithComponent(c: Component,l: Option[String],
	                               con: Option[ContentType.ContentType],layout: Division.Division) =
		Plate(c.id,c.description,c.project,c.tags,l,con,layout)

	private def unapplyWithComponent(p: Plate) = Some(p.getComponent,p.locationID,p.initialContent,p.layout)

	val validLocations = List(ComponentType.Freezer)
	val validTransfers = List(ComponentType.Plate,ComponentType.Tube,ComponentType.Rack)
	val validContents = InitialContents.ContentType.plateContents

	/**
	 * Form to use in view of plate
	 */
	override val form = Form(
		mapping(
			Component.formKey -> Component.componentMap,
			Location.locationKey -> optional(text),
			Container.contentKey -> optional(enum(ContentType)),
			ContainerDivisions.divisionKey -> enum(Division)
		)(applyWithComponent)(unapplyWithComponent))

	/**
	 * Formatter for going to/from and validating Json
	 */
	override implicit val formatter: Format[Plate] = formatWithComponent(Json.format[Plate])
}
