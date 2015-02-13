package controllers

import models.Component.{HiddenFields,ComponentType}
import models.Plate
import play.api.data.Form
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
	def htmlForCreate = views.html.plateCreateForm(_: Form[Plate])

	// Play html view to update plate
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]) =
		views.html.plateUpdateForm(_: Form[Plate], id, hiddenFields)

	// Play redirect route to find a plate
	def routeFind = routes.PlateController.findPlateByID(_: String)

	// Component type
	val componentType = ComponentType.Plate

	/**
	 * Request to add a plate - we simply put up the form to get the parameters to create the plate.
	 * @return responds to request with html form to get plate input
	 */
	def addPlate() = Action { add }

	/**
	 * Request to find a plate, identified by an ID.  We find the plate (or return an error if it can’t be found) and
	 * then go to the page to view/update the plate.
	 * @param id plate ID
	 * @return responds to request with html form to view/update plate
	 */
	def findPlateByID(id: String) = Action.async { request => find(id,request)}

	/**
	 * Go create a plate.  If all goes well the plate is created in the DB and the user is redirected to
	 * to view/update the newly created plate.
	 * @return responds to request with html form to view/update added plate
	 */
	def createPlateFromForm() = Action.async { request => create(request)}

	/**
	 * Go update a plate.  If all goes well the plate is updated in the DB and the user is redirected to
	 * to view/update the newly updated plate.
	 * @return responds to request with updated form to view/update plate
	 */
	def updatePlateFromForm(id: String) = Action.async { request => update(request, id)}
}
