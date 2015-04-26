package controllers

import models.Component.{HiddenFields,ComponentType}
import models.Tube
import play.api.data.Form
import play.api.libs.json.JsObject
import play.api.mvc.Action

/**
 * @author Nathaniel Novod
 *         Date: 11/25/14
 *         Time: 10:21 AM
 */
object TubeController extends ComponentController[Tube] {
	// Play form for tube
	val form = Tube.form
	// Play formatter for Json verification/conversion
	implicit val formatter = Tube.formatter

	// Play html view to create tube
	def htmlForCreate(id: String) = views.html.tubeCreateForm(_: Form[Tube], id)

	// Play html view to update tube
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]) =
		views.html.tubeUpdateForm(_: Form[Tube], id, hiddenFields)

	// Component type
	val componentType = ComponentType.Tube

	// Way to make component from Json
	def componentFromJson(json: JsObject) = json.as[Tube]

	/**
	 * Request to add a tube - we simply put up the form to get the parameters to create the tube.
	 * @return responds to request with html form to get tube input
	 */
	def addTube(id: String) = Action { add(id) }

	/**
	 * Request to find a tube, identified by an ID.  We find the tube (or return an error if it can’t be found) and
	 * then go to the page to view/update the tube.
	 * @param id tube ID
	 * @return responds to request with html form to view/update tube
	 */
	def findTubeByID(id: String) = Action.async { request => find(id,request)}

	/**
	 * Go create a tube.  If all goes well the tube is created in the DB and the user is redirected home.
	 * @param id tube ID
	 * @return responds to request with message and form with errors or home page
	 */
	def createTubeFromForm(id: String) = Action.async { request => create(id, request)}

	/**
	 * Go update a tube.  If all goes well the tube is updated in the DB and the user is redirected home.
	 * @return responds to request with message and form with errors or home page
	 */
	def updateTubeFromForm(id: String) = Action.async { request => update(id, request)}
}
