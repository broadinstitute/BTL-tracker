package controllers

import models.Component.ComponentType
import models.db.TransferCollection
import play.api.libs.json._
import play.api.mvc._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

object Application extends Controller {

	/**
	 * Original play default action - brings up play documentation
	 *
	 * @return action to show play documentation
	 */
	def playDoc = Action {
		Ok(views.html.playDoc("Your new application is ready."))
	}

	/**
	 * Home page
	 * @return action to show initial choices on home page
	 */
	def index = Action { request =>
		Ok(views.html.index(Errors.addStatusFlash(request,Component.blankForm)))
	}

	/**
	 * Just a test form
	 */
	// @TODO Add register for multiple IDs
	def test(id: String) = Action {
		Ok(s"Test of $id")
	}

	/**
	 * Display the graph of transfers, direct and indirect, from and to specified component.  First the graph is made
	 * into "dot" format and then the view is called to convert it to pretty html.
	 * @param id component id
	 * @return puts up pretty picture of graph of transfers to and from a component
	 */
	def graphDisplay(id: String) = Action.async {
		TransferHistory.makeBidirectionalDot(id).map((dot) => {
			val htmlStr = "\'" + dot.replaceAll("""((\r\n)|\n|\r])""", """\\$1""") + "\'"
			Ok(views.html.graphDisplay(htmlStr, id))
		})
	}

	/**
	 * Initial request to add - we just put up the form to get initial data for the form
	 * @return action to execute to initiate add of component
	 */
	def add(id: String) = Action { request =>
		Ok(views.html.add(Errors.addStatusFlash(request,Component.idAndTypeForm), id))
	}

	/**
	 * Using type supplied from form go to next step to fill in type specific data for components.
	 * @return action to execute to put up form to get component data
	 */
	def addFromForm() = Action { request =>
		Component.idAndTypeForm.bindFromRequest()(request).fold(
			formWithErrors =>
				BadRequest(views.html.add(formWithErrors.withGlobalError(Errors.validationError), formWithErrors.get.id)),
			data =>
				Redirect(ComponentController.actions(data.t).addRoute(data.id))
		)
	}

	/**
	 * Go bring up view to confirm if delete is to proceed
	 * @param id component id to delete
	 * @return result with
	 */
	def deleteCheck(id: String) = Action.async { request =>
		TransferCollection.countTransfers(id).map((count) => {
			Ok(views.html.deleteConfirm(id, count))
		}).recover {
			case err => BadRequest(
				views.html.index(Component.blankForm.withGlobalError(Errors.exceptionMessage(err))))
		}
	}

	/**
	 * Delete component based on ID supplied in get request
	 * @param id id of component to be deleted
	 * @return action to deleted specified component
	 */
	def deleteByID(id: String) = Action.async { request =>
		// Make result - redirect to home page and set flashing value (to be set as global error) to be picked up
		// on home page
		def getResult(msg: String) = {
			Errors.homeRedirect(msg)
		}
		// Do delete
		ComponentController.delete(id,
			deleted = () => getResult(id + " successfully deleted"),
			error = (t: Throwable) => getResult("Error deleting " + id + ": " + t.getLocalizedMessage)
		)
	}

	/**
	 * Get component for specified id (specified as parameter for GET).  First we get the component type and then
	 * we redirect to the update screen for that component type.
	 * @param id component ID
	 * @return action to find and display wanted component
	 */
	def findByID(id: String) = Action.async { request =>
		findRequestUsingID(id,request)(doUpdateRedirect(id,_,_,_)).recover {
			case err => BadRequest(
				views.html.index(Component.blankForm.withGlobalError(Errors.exceptionMessage(err))))
		}
	}

	/**
	 * Get html to display component for specified id.
	 * @param id component ID
	 * @return form filled in with component data requested
	 */
	def findRequestUsingID(id: String,request: Request[_],okComponents: List[ComponentType.ComponentType] = List.empty)
	                      (getResult: (ComponentType.ComponentType,JsObject,Request[_]) => Result): Future[Result] = {
		ComponentController.findByID[JsObject,Result](id, okComponents,
			found = (json) => {
				val cType = (json \ Component.typeKey).as[String]
				getResult(ComponentType.withName(cType),json,request)
			},
			notFound = Errors.notFoundRedirect)
	}

	/**
	 * Do a redirect to do an update for the given component.
	 * @param id component ID
	 * @param ct type of component
	 * @param json json with data associated with component (fetched from DB)
	 * @param request original request to do update
	 * @return results now ready for update
	 */
	def doUpdateRedirect(id: String, ct: ComponentType.ComponentType, json: JsObject, request: Request[_]) =
		Redirect(ComponentController.actions(ct).updateRoute(id))
}
