package controllers

import models.Component.ComponentType
import models.db.{TrackerCollection, TransferCollection}
import play.api.Routes
import play.api.libs.json._
import play.api.mvc._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.BSONFormats
import utils.MessageHandler

import scala.concurrent.Future

object Application extends Controller {

	/**
	 * Original play default action - brings up play documentation
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
		Ok(views.html.index(MessageHandler.addStatusFlash(request,Component.blankForm)))
	}

	/**
	 * Just a test form
	 */
	def test(id: String) = Action { request =>
		Ok("Got" + id)
	}

	/**
	 * Display the graph of transfers, direct and indirect, from and to specified component.  First the graph is made
	 * into "dot" format and then the view is called to convert it to pretty html.
	 * @param id component id
	 * @return puts up pretty picture of graph of transfers to and from a component
	 */
	def graphDisplay(id: String) = Action.async {
		TransferHistory.makeBidirectionalDot(id, routes.Application.findByID(_: String)).map((dot) => {
			val htmlStr = "\'" + dot.replaceAll("""((\r\n)|\n|\r])""", """\\$1""") + "\'"
			Ok(views.html.graphDisplay(htmlStr, id))
		})
	}

	/**
	 * Display the well transfers between two components.
	 * @param from source component
	 * @param to target component
	 * @return puts up nice display of source plate with contents of wells set to destination wells
	 */
	def transferDisplay(from: String, to: String) = Action.async {
		// Get transfers between components (flatmap to avoid future of future)
		TransferCollection.find(from, to).flatMap((trans) => {
			// Get components
			TrackerCollection.findIds(List(from, to)).map((ids) => {
				// Map bson to components (someday direct mapping should be possible but too painful for now)
				val components = ids.map((bson) => {
					// Get json since model conversions are setup to do json reads/writes
					val json = BSONFormats.BSONDocumentFormat.writes(bson).as[JsObject]
					// Do conversion to model component object
					ComponentFromJson.getComponent(json)
				})
				// Find from and to components
				val fromComponent = components.find(_.id == from)
				val toComponent = components.find(_.id == to)
				// Combine all the well transfers together
				val wells = trans.foldLeft(Map.empty[String, String]){
					case (out, bson) if (fromComponent.isDefined) =>
						val next = TransferHistory.getTransferObject(bson)
						TransferContents.getWellMapping(out, fromComponent.get,
							next.fromQuad, next.toQuad, next.slice, next.cherries) {
							case (wellsSoFar, div, newWells) => wellsSoFar ++ newWells
						}
					case (out, next) => out
				}
				// Make destination wells into options (template takes the map that way)
				val optWells =
					wells.map{
						case (key, value) => key -> Some(value)
					}
				// Get number of rows and columns
				val (rows, cols) =
					fromComponent.map {
						case c: ContainerDivisions =>
							val div = ContainerDivisions.divisionDimensions(c.layout)
							(div.rows, div.columns)
						case _ => (0,0)
					}.getOrElse((0, 0))
				// Go display the results
				Ok(views.html.transferDisplay(fromComponent, toComponent, "TransferDisplay", optWells, rows, cols))
			})
		})
	}

	/**
	 * Initial request to add.  We put up the form to get initial data for the add.
	 * @param id component id
	 * @return action to execute to initiate add of component
	 */
	def add(id: String) = Action { request =>
		Ok(views.html.add(MessageHandler.addStatusFlash(request,Component.idAndTypeForm), id))
	}

	/**
	 * Initial request to get ids for a stack of components.  We put up the form to get the ids and type.
	 * @return handler to bring up view to get stack IDs
	 */
	def addStack() = Action { request =>
		Ok(views.html.addStackStart(MessageHandler.addStatusFlash(request,Component.idAndTypeForm)))
	}

	/**
	 * Request to add a stack of IDs.  We get the IDs and type and request to get detailed type information.
	 * @return handler for stack of IDs
	 */
	def getStackIDs = Action { request =>
		Component.idAndTypeForm.bindFromRequest()(request).fold(
			formWithErrors =>
				BadRequest(views.html.addStackStart(MessageHandler.formGlobalError(formWithErrors,
					MessageHandler.validationError))),
			data => {
				def plural(s: String, i: Int) = s"$i $s${if (i == 1) "" else "s"}"
				val ids = Utils.getIDs(data.id)
				val idSet = ids.toSet
				val repeats = ids.length - idSet.size
				val repeatStr = if (repeats == 0) "" else s" (${plural("duplicate", repeats)} eliminated)"
				val completionView =
					ComponentController.actions(data.t).createStackView(idSet.mkString("\n"),
						s"Submit to complete registration of " +
							s"${plural(data.t.toString.toLowerCase, idSet.size)}$repeatStr")
				Ok(completionView)
			}
		)
	}

	/**
	 * Using type supplied from form go to next step to fill in type specific data for components.
	 * @param id component ID
	 * @return action to execute to put up form to get component data
	 */
	def addFromForm(id: String) = Action { request =>
		Component.idAndTypeForm.bindFromRequest()(request).fold(
			formWithErrors =>
				BadRequest(views.html.add(MessageHandler.formGlobalError(formWithErrors,
					MessageHandler.validationError), id)),
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
				views.html.index(Component.blankForm.withGlobalError(MessageHandler.exceptionMessage(err))))
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
		def getResult(msg: String) = MessageHandler.homeRedirect(msg)
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
				views.html.index(Component.blankForm.withGlobalError(MessageHandler.exceptionMessage(err))))
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
			notFound = MessageHandler.notFoundRedirect)
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

	/**
	 * Get the list of tags - this is typically invoked as an ajax function
	 */
	def tags = Action.async {
		models.db.TrackerCollection.getTags.map {
			// Return error if there is one
			case (Some(e), _) => BadRequest(Json.toJson(e))
			// Return list found
			case (_, found) => Ok(Json.toJson(found))
		}
	}

	/**
	 * Make javascript script with ajax routes map.  Within javascript this can be invoked as follows:
	 * <script type="text/javascript" src="@routes.Application.javascriptRoutes"></script>
	 * @return script containing calls to listed http functions
	 */
	def javascriptRoutes = Action { implicit request =>
		Ok(
			Routes.javascriptRouter("jsRoutes")(
				routes.javascript.Application.tags
			)
		).as("text/javascript")
	}
}
