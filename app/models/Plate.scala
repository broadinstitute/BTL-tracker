package models

/**
 * @author Nathaniel Novod
 *         on 11/21/14
 */

import formats.CustomFormats._
import mappings.CustomMappings._
import models.ContainerDivisions.Division
import models.project.JiraProject
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json,Format}
import Component._
import play.api.mvc.{AnyContent, Request}

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
 * @param contentID Optional ID of Component describing the initial contents of this plate
 * @param layout well layout of plate
 */
case class Plate(override val id: String,override val description: Option[String],override val project: Option[String],
                 override val tags: List[ComponentTag],
                 override val locationID: Option[String],override val contentID: Option[String],
                 layout: Division.Division)
	extends Component with Location with Container with Transferrable with JiraProject {
	override val component = Plate.componentType
	override val validLocations = List(ComponentType.Freezer)
	override val validTransfers = List(ComponentType.Plate,ComponentType.Rack)
	override val validContents = List(ComponentType.Material)

	/**
	 * Check if what's set in request is valid - specifically we check if the project set contains a DGE plate.
	 * @param request HTTP request (has hidden field with project set before update)
	 * @return Future of map of fields to errors - empty if no errors found
	 */
	override protected def isValid(request: Request[AnyContent]) = {
		isProjectValid(request,
			finalCheck = (issues) => {
				if (!JiraProject.isDGE(issues))
					Map(Some(formKey + "." + projectKey) -> "Project is not for a DGE plate")
				else
					Map.empty
			})
	}
}

object Plate extends ComponentObject[Plate](ComponentType.Plate) {
	/**
	 * Form mapping for a plate.  Note that component contents must be referred to as componentData.fieldName in forms
	 * since clean inheritance doesn't appear to be possible.  That's why the (un)applyWithComponent is needed
	 * (see componentMap for more details).
	 */
	private def applyWithComponent(c: Component,l: Option[String],con: Option[String],layout: Division.Division) =
		Plate(c.id,c.description,c.project,c.tags,l,con,layout)

	private def unapplyWithComponent(p: Plate) = Some(p.getComponent,p.locationID,p.contentID,p.layout)

	val layoutKey = "layout"
	// Supply our custom enum Reader and Writer for layout enum
	implicit val layoutTypeFormat: Format[ContainerDivisions.Division.Division] = enumFormat(ContainerDivisions.Division)

	override val form = Form(
		mapping(
			Component.formKey -> Component.componentMap,
			Location.locationKey -> optional(text),
			Container.contentKey -> optional(text),
			layoutKey -> enum(Division)
		)(applyWithComponent)(unapplyWithComponent))

	/**
	 * Formatter for going to/from and validating Json
	 */
	override implicit val formatter: Format[Plate] = formatWithComponent(Json.format[Plate])
}
