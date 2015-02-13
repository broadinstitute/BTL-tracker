package controllers

import models.Component.ComponentType
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import models._

import scala.concurrent.Future

object Application extends Controller with MongoController {

	/**
	 * Validation error report
	 */
	private val validationError = "Data entry error - see below"

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
	def index = Action {
		Ok(views.html.index())
	}

	/**
	 * Just a test form
	 */
	def test = Action {
		Ok("You went to the test page")
	}

	/**
	 * Initial request to add - we just put up the form to get initial data for the form
	 * @return action to execute to initiate add of component
	 */
	def add = Action { request =>
		Ok(views.html.add(ComponentController.addStatusFlash(request,Component.typeForm)))
	}

	/**
	 * Using type supplied from form go to next step to fill in type specific data for components.
	 * @return action to execute to put up form to get component data
	 */
	def addFromForm() = Action { request =>
		Component.typeForm.bindFromRequest()(request).fold(
			formWithErrors => {
				BadRequest(views.html.add(formWithErrors.withGlobalError(validationError)))
			},
			data => {
				// Redirect to appropriate add form
				data.t match {
					case ComponentType.Freezer => Redirect(routes.FreezerController.addFreezer())
					case ComponentType.Plate => Redirect(routes.PlateController.addPlate())
					case ComponentType.Rack => Redirect(routes.RackController.addRack())
					case ComponentType.Sample => Redirect(routes.SampleController.addSample())
					case ComponentType.Tube => Redirect(routes.TubeController.addTube())
					case ComponentType.Well => Redirect(routes.WellController.addWell())
					case ComponentType.Material => Redirect(routes.MaterialController.addMaterial())
				}
			}
		)
	}

	/**
	 * Initiate find of component - just put up form to get ID of component
	 * @return action to get id of wanted component
	 */
	def find = Action { request =>
		Ok(views.html.find(ComponentController.addStatusFlash(request,Component.idForm)))
	}

	/**
	 * Get component wanted based on ID supplied in returned form
	 * @return action to find and display wanted component
	 */
	def findFromForm = Action.async { request =>
		Component.idForm.bindFromRequest()(request).fold(
			formWithErrors => {
				Future.successful(BadRequest(views.html.find(formWithErrors.withGlobalError(validationError))))
			},
			data => {
				findByID(data.id,request)(doUpdateRedirect(data.id,_,_,_))
			}
		)
	}

	/**
	 * Get component for specified id (specified as parameter for GET)
	 * @param id component ID
	 * @return action to find and display wanted component
	 */
	def findWithID(id: String) = Action.async { request =>
		findByID(id,request)(doUpdateRedirect(id,_,_,_))
	}

	/**
	 * Get html to display component for specified id.
	 * @param id component ID
	 * @return form filled in with component data requested
	 */
	def findByID(id: String,request: Request[_],okComponents: List[ComponentType.ComponentType] = List.empty)
	            (getResult: (ComponentType.ComponentType,JsObject,Request[_]) => Result): Future[Result] = {
		ComponentController.findByID[JsObject,Result](id,okComponents,
			found = (json) => {
				val cType = (json \ Component.typeKey).as[String]
				getResult(ComponentType.withName(cType),json,request)
			},
			notFound = ComponentController.notFoundRedirect)
	}

	/**
	 * Do an update for the given component
	 * @param ct type of component
	 * @param json json with data associated with component (fetched from DB)
	 * @param request original request to do update
	 * @return results now ready for update
	 */
	private def doUpdateRedirect(id: String,ct: ComponentType.ComponentType,json: JsObject,request: Request[_]) = {
		ct match {
			case ComponentType.Freezer => Redirect(routes.FreezerController.findFreezerByID(id))
			case ComponentType.Plate => Redirect(routes.PlateController.findPlateByID(id))
			case ComponentType.Rack => Redirect(routes.RackController.findRackByID(id))
			case ComponentType.Sample => Redirect(routes.SampleController.findSampleByID(id))
			case ComponentType.Tube => Redirect(routes.TubeController.findTubeByID(id))
			case ComponentType.Well => Redirect(routes.WellController.findWellByID(id))
			case ComponentType.Material => Redirect(routes.MaterialController.findMaterialByID(id))
		}
	}
}
