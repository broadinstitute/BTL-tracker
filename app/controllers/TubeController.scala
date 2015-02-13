package controllers

import models.Component.{HiddenFields,ComponentType}
import models.Tube
import play.api.data.Form
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
	def htmlForCreate = views.html.tubeCreateForm(_: Form[Tube])

	// Play html view to update tube
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]) =
		views.html.tubeUpdateForm(_: Form[Tube], id, hiddenFields)

	// Component type
	val componentType = ComponentType.Tube

	/**
	 * Request to add a tube - we simply put up the form to get the parameters to create the tube.
	 * @return responds to request with html form to get tube input
	 */
	def addTube() = Action { add }

	/**
	 * Request to find a tube, identified by an ID.  We find the tube (or return an error if it can’t be found) and
	 * then go to the page to view/update the tube.
	 * @param id tube ID
	 * @return responds to request with html form to view/update tube
	 */
	def findTubeByID(id: String) = Action.async { request => find(id,request)}

	/**
	 * Go create a tube.  If all goes well the tube is created in the DB and the user is redirected to
	 * to view/update the newly created tube.
	 * @return responds to request with html form to view/update added tube
	 */
	def createTubeFromForm() = Action.async { request => create(request)}

	/**
	 * Go update a tube.  If all goes well the tube is updated in the DB and the user is redirected to
	 * to view/update the newly updated tube.
	 * @return responds to request with updated form to view/update tube
	 */
	def updateTubeFromForm(id: String) = Action.async { request => update(request, id)}
}
