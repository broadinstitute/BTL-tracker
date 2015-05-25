package controllers

import models.Component.{HiddenFields,ComponentType}
import models.Freezer
import play.api.data.Form
import play.api.libs.json.JsObject
import play.api.mvc.Action
import utils.MessageHandler

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
	def htmlForCreate(id: String) = views.html.freezerCreateForm(_: Form[Freezer], id)

	// Play html view to create stack of freezers
	def htmlForCreateStack = views.html.freezerCreateStackForm(_: Form[Freezer])

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
	def addFreezer(id: String) = Action { add(id) }

	/**
	 * Request to find a freezer, identified by an ID.  We find the freezer (or return an error if it can’t be found)
	 * and go to the page to view/update the freezer.
	 * @param id freezer ID
	 * @return responds to request with html form to view/update freezer
	 */
	def findFreezerByID(id: String) = Action.async { request => find(id,request) }

	/**
	 * Go create a freezer.  If all goes well the freezer is created in the DB and the user is redirected home.to
	 * @param id freezer ID
	 * @return responds to request with message and form with errors or home page
	 */
	def createFreezerFromForm(id: String) = Action.async { request => create(id, request) }

	/**
	 * Get a form filled in with specified ID
	 * @param id id to set in form
	 * @return tube form filled in with id
	 */
	private def makeIdForm(id: String) = form.fill(Freezer(id, None, None, List.empty, "", -20.0f))

	/**
	 * Create html to be sent back to get parameters to make a stack.  First create the form filled in with IDs and
	 * then output the html based on the created form.
	 * @param id ids for stack
	 * @param completionStr completion status to set as global message in form
	 * @return html to create stack of freezers
	 */
	def makeStackHtml(id: String, completionStr: String) = {
		val idForm = makeIdForm(id)
		val formWithStatus = MessageHandler.formGlobalError(idForm, completionStr)
		htmlForCreateStack(formWithStatus)
	}

	/**
	 * Request to add a stack of freezers
	 * @return response with completion status of freezer insertions
	 */
	def addFreezerStack = Action.async { request => addStack(request) }

	/**
	 * Go update a freezer.  If all goes well the freezer is updated in the DB and the user is redirected home.
	 * @return responds to request with message and form with errors or home page
	 */
	def updateFreezerFromForm(id: String) = Action.async { request => update(id, request) }
}
