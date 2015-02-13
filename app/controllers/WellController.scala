package controllers


import models.Component.{HiddenFields,ComponentType}
import models.Well
import play.api.data.Form
import play.api.mvc.Action

/**
 * @author Nathaniel Novod
 *         Date: 11/25/14
 *         Time: 10:21 AM
 */
object WellController extends ComponentController[Well] {
	// Play form for well
	val form = Well.form
	// Play formatter for Json verification/conversion
	implicit val formatter = Well.formatter

	// Play html view to create well
	def htmlForCreate = views.html.wellCreateForm(_: Form[Well])

	// Play html view to update well
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]) =
		views.html.wellUpdateForm(_: Form[Well], id, hiddenFields)

	// Play redirect route to find a well
	def routeFind = routes.WellController.findWellByID(_: String)

	// Component type
	val componentType = ComponentType.Well

	/**
	 * Request to add a well - we simply put up the form to get the parameters to create the well.
	 * @return responds to request with html form to get well input
	 */
	def addWell() = Action { add }

	/**
	 * Request to find a well, identified by an ID.  We find the well (or return an error if it can’t be found) and
	 * then go to the page to view/update the well.
	 * @param id well ID
	 * @return responds to request with html form to view/update well
	 */
	def findWellByID(id: String) = Action.async { request => find(id,request)}

	/**
	 * Go create a well.  If all goes well the well is created in the DB and the user is redirected to
	 * to view/update the newly created well.
	 * @return responds to request with html form to view/update added well
	 */
	def createWellFromForm() = Action.async { request => create(request)}

	/**
	 * Go update a well.  If all goes well the well is updated in the DB and the user is redirected to
	 * to view/update the newly updated well.
	 * @return responds to request with updated form to view/update well
	 */
	def updateWellFromForm(id: String) = Action.async { request => update(request, id)}
}
