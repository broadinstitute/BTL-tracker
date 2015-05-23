package controllers

import models.Component.{HiddenFields,ComponentType}
import models.{ContainerDivisions, Plate}
import play.api.data.Form
import play.api.libs.json.JsObject
import play.api.mvc.Action

/**
 * @author Nathaniel Novod
 *         Date: 11/25/14
 *         Time: 10:21 AM
 */
object PlateController extends ComponentController[Plate] {
	// Play form for plate
	val form = Plate.form
	// Play formatter for Json verification/conversion
	implicit val formatter = Plate.formatter

	// Play html view to create plate
	def htmlForCreate(id: String) = views.html.plateCreateForm(_: Form[Plate], id)

	// Play html view to create stack of plates
	def htmlForCreateStack = views.html.plateCreateStackForm(_: Form[Plate])

	// Play html view to update plate
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]) =
		views.html.plateUpdateForm(_: Form[Plate], id, hiddenFields)

	// Component type
	val componentType = ComponentType.Plate

	// Way to make component from Json
	def componentFromJson(json: JsObject) = json.as[Plate]

	/**
	 * Request to add a plate - we simply put up the form to get the parameters to create the plate.
	 * @return responds to request with html form to get plate input
	 */
	def addPlate(id: String) = Action { add(id) }

	/**
	 * Get a form filled in with specified ID
	 * @param id id to set in form
	 * @return plate form filled in with id
	 */
	private def makeIdForm(id: String) = form.fill(Plate(id,
		None, None, List.empty, None, None, ContainerDivisions.Division.DIM8x12))

	/**
	 * Create html to be sent back to get parameters to make a stack.  First create the form filled in with IDs and
	 * then output the html based on the created form.
	 * @param id ids for stack
	 * @param completionStr completion status to set as global message in form
	 * @return html to create stack of plates
	 */
	def makeStackHtml(id: String, completionStr: String) = {
		val idForm = makeIdForm(id)
		val formWithStatus = Errors.formGlobalError(idForm, completionStr)
		htmlForCreateStack(formWithStatus)
	}

	/**
	 * Request to add a stack of plates
	 * @return response with completion status of plate insertions
	 */
	def addPlateStack = Action.async { request =>
		addStack(request)
	}

	/**
	 * Request to find a plate, identified by an ID.  We find the plate (or return an error if it can’t be found) and
	 * then go to the page to view/update the plate.
	 * @param id plate ID
	 * @return responds to request with html form to view/update plate
	 */
	def findPlateByID(id: String) = Action.async { request => find(id, request)}

	/**
	 * Go create a plate.  If all goes well the plate is created in the DB and the user is redirected home.
	 * @param id plate ID
	 * @return responds to request with message and form with errors or home page
	 */
	def createPlateFromForm(id: String) = Action.async { request => create(id, request)}

	/**
	 * Go update a plate.  If all goes well the plate is updated in the DB and the user is redirected home.
	 * @return responds to request with message and form with errors or home page
	 */
	def updatePlateFromForm(id: String) = Action.async { request => update(id, request)}
}
