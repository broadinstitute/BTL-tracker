package controllers

import models.Component.{HiddenFields,ComponentType}
import models.Freezer
import play.api.data.Form
import play.api.libs.json.JsObject
import play.api.mvc.Action

/**
 * @author Nathaniel Novod
 *         Date: 11/25/14
 *         Time: 10:21 AM
 */
object FreezerController extends ComponentController[Freezer] {
	// Play form for freezer
	val form = Freezer.form
	// Play formatter for Json verification/conversion
	implicit val formatter = Freezer.formatter

	// Play html view to create freezer
	def htmlForCreate = views.html.freezerCreateForm(_: Form[Freezer])

	// Play html view to update freezer
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]) =
		views.html.freezerUpdateForm(_: Form[Freezer], id, hiddenFields)

	// Component type
	val componentType = ComponentType.Freezer

	// Way to make component from Json
	def componentFromJson(json: JsObject) = json.as[Freezer]

	/**
	 * Request to add a freezer - we simply put up the form to get the parameters to create the freezer.
	 * @return responds to request with html form to get freezer input
	 */
	def addFreezer() = Action { add }

	/**
	 * Request to find a freezer, identified by an ID.  We find the freezer (or return an error if it can’t be found) and
	 * then go to the page to view/update the freezer.
	 * @param id freezer ID
	 * @return responds to request with html form to view/update freezer
	 */
	def findFreezerByID(id: String) = Action.async { request => find(id,request)}

	/**
	 * Go create a freezer.  If all goes well the freezer is created in the DB and the user is redirected to
	 * to view/update the newly created freezer.
	 * @return responds to request with html form to view/update added freezer
	 */
	def createFreezerFromForm() = Action.async { request => create(request)}

	/**
	 * Go update a freezer.  If all goes well the freezer is updated in the DB and the user is redirected to
	 * to view/update the newly updated freezer.
	 * @return responds to request with updated form to view/update freezer
	 */
	def updateFreezerFromForm(id: String) = Action.async { request => update(request, id)}
}
