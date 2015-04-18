package controllers

import models.Component
import models.Component.{HiddenFields,ComponentType}
import play.api.Logger
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.twirl.api.{HtmlFormat,Html}

import scala.concurrent.Future

/**
 * @author Nathaniel Novod
 *         Date: 11/25/14
 *         Time: 9:52 AM
 */
/**
 * Controller to do CRUD on a component to/from a Mongo database.
 * @tparam C type of component
 */
trait ComponentController[C <: Component] extends Controller with MongoController {

	import ComponentController._

	/**
	 * Play Form to use in UI for displaying/gathering component data - supplied by user of trait
	 */
	val form: Form[C]

	/**
	 * Play Formatter to convert and/or validate JSON for component - supplied by user of trait
	 */
	implicit val formatter: Format[C]

	/**
	 * Method to use to take a form filled with data and return the html to be displayed after an update - supplied
	 * by component type inheriting trait.
	 * @return method that converts a form to html to display (typically via a Play template view)
	 */
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]): (Form[C]) => Html

	/**
	 * Method to use to take a form filled with data and return the html to be displayed after a create - supplied
	 * by component type inheriting trait.
	 * @return method that converts a form to html to display (typically a Play template view)
	 */
	def htmlForCreate(id: String): (Form[C]) => Html

	/**
	 * Component type, supplied by component inheriting trait (maybe someday make a macro to get this based on C type?)
	 */
	val componentType: ComponentType.ComponentType

	/**
	 * Make an object from json
 	 * @param json input json
	 * @return object returned via reading of Json
	 */
	def componentFromJson(json: JsObject) : C

	/**
	 * Add - just say ok and display form for creating component
	 * @return Ok status with create html
	 */
	def add(id: String) = {
		Ok(htmlForCreate(id)(form))
	}

	/**
	 * Return result with message(s):
	 * a)Get form filled with data and flash status (set by previous request) as a global message on the form
	 * b)Add additional messages to be displayed in the form
	 * c)Create html from form
	 * d)Get result - status with html as associated returned data
	 *
	 * @param status result status (e.g., Ok or BadRequest)
	 * @param c component to display
	 * @param request html request
	 * @param html callback to create html from form
	 * @param msgs messages to display in form (optional form field key -> error message)
	 * @param messageSetter callback to set messages in form
	 * @return result containing html to be displayed
	 */
	private def viewData(status: Status, c: C, request: Request[AnyContent], html: (Form[C]) => Html,
	                     msgs: Map[Option[String], String],
	                     messageSetter: (Map[Option[String], String], Form[C]) => Form[C]) = {
		val filledForm = Errors.addStatusFlash(request, form.fill(c))
		val formWithMsgs = messageSetter(msgs, filledForm)
		status(html(formWithMsgs))
	}


	/**
	 * Find - if all goes well return with display of found component.
	 * @param id component id
	 * @param request http request
	 * @return resulting html to display wanted component
	 */
	def find(id: String, request: Request[AnyContent]) = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		findByID[C,Result](id,List(componentType),
			// Found wanted component
			found = (c) => viewData(Ok, c, request,	htmlForUpdate(id, Some(c.hiddenFields)),
				Map.empty, Errors.setMessages),
			// NotFound - just redirect to not found view
			notFound = Errors.notFoundRedirect
		).recover {
			case err => BadRequest(views.html.index(Component.blankForm.withGlobalError(Errors.exceptionMessage(err))))
		}
	}

	/**
	 * Create using data returned in the request form - if all goes well insert component into db and go home.
	 * @param id Component ID
	 * @param request http request
	 * @return future to create component (on error displays form with errors)
	 */
	def create(id: String, request: Request[AnyContent]) = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		doRequestFromForm(form,
			// Note that afterbind returns a future
			afterBind = (data: C) => {
				trackerCollection.insert(data).map { lastError =>
					val success = s"Successfully inserted item ${data.id}"
					Logger.debug(s"$success with status: $lastError")
					// Go home and tell everyone what we've done
					Errors.homeRedirect(success)
				}
			},
			// if trouble then show form that should be updated with error messages
			onFailure = (f: Form[C]) => htmlForCreate(id)(f)
		)(request).recover {
			case err => BadRequest(htmlForCreate(id)(form.withGlobalError(Errors.exceptionMessage(err))))
		}
	}

	/**
	 * Update - if all goes well update component in db and and gh home.
	 * @param id component id
	 * @param request http request
	 * @param preUpdate callback to allow pre-update checking - returns non-empty map of error(s) to abort update
	 * @return future update component (on error displays form with errors)
	 */
	def update(id: String, request: Request[AnyContent],
	           preUpdate: (C) => Map[Option[String], String] = (_) => Map.empty) = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		doRequestFromForm(form,
			// Note that afterbind returns a future
			afterBind = (data: C) => {
				// Allow caller to do some pre update checking
				val errs = preUpdate(data)
				if (errs.isEmpty) {
					// Binding was successful - now go update the wanted item with data from the form
					val selector = Json.obj(Component.idKey -> data.id,Component.typeKey -> data.component.toString)
					trackerCollection.update(selector,data).map { lastError =>
						val success = s"Successfully updated ${data.id}"
						Logger.debug(s"$success with status: $lastError")
						// Go home and tell everyone what we've done
						Errors.homeRedirect(success)
					}
				} else {
					// If pre-update returned errors then return form with those errors - remember original
					// hidden fields so any changes made to them are ignored for now
					Future.successful(
						viewData(BadRequest, data, request,
							htmlForUpdate(id, Component.getHiddenFields(request)), errs, Errors.setFailureMsgs))
				}
			},
			// if trouble then show form that should be updated with error messages
			onFailure = (f: Form[C]) => htmlForUpdate(id, Component.getHiddenFields(request))(f)
		)(request).recover {
			// Recover from exception - return form with errors
			case err => BadRequest(htmlForUpdate(id, Component.getHiddenFields(request))
				(form.withGlobalError(Errors.exceptionMessage(err))))
		}
	}
}

object ComponentController extends Controller with MongoController {
	/**
	 * Tracker collection name
	 */
	val trackerCollectionName = "tracker"
	/**
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses JSON for input/output
	 */
	def trackerCollection: JSONCollection = db.collection[JSONCollection](trackerCollectionName)

	/**
	 * Go delete item and associated transfers from DB.
	 * @param id ID for item to be deleted
	 * @param deleted callback if delete went well
	 * @param error callback if error during delete
	 */
	def delete[R](id: String, deleted: () => R, error: (Throwable) => R) = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		val key = Json.obj(Component.idKey -> id)
		// Start up deletes of component as well as associated transfers
		val componentRemove = trackerCollection.remove(Json.toJson(key))
		val transferRemove = TransferController.removeTransfers(id)
		// Wait for completions of deletes
		componentRemove.flatMap { lastError =>
			Logger.debug(s"Successfully deleted item $id with status: $lastError")
			transferRemove.map {
				case Some(err) => error(err)
				case None => deleted()
			}
		}.recover{
			case err => error(err)
		}
	}

	/**
	 * Find item by querying the mongo collection using an id.  We return a future that will call back with the
	 * query results.
	 *
	 * @param id item id to search for
	 * @param componentType list of component types to search for (if empty all types are allowed)
	 * @param found callback to set result if item found
	 * @param notFound callback to set result if item not found
	 * @tparam T type of data to be found (must have associated Reads for validation and conversion of JsValue)
	 * @tparam R type of result
	 * @return future result, as determined by callbacks, of query
	 */
	def findByID[T : Reads, R](id: String,componentType: List[ComponentType.ComponentType], found: (T) => R,
	                           notFound: (String,List[ComponentType.ComponentType]) => R) = {
		// let’s do our query to a single item with the wanted id (and component type(s) if supplied)
		val findMap =
			if (componentType.isEmpty)
				Json.obj(Component.idKey -> id)
			else
				Json.obj(Component.idKey -> id,Component.typeKey -> Json.obj("$in" -> componentType.map(_.toString)))
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		// Using implicit reader and execution context
		val item = trackerCollection.find(Json.toJson(findMap)).one
		// First map for future (returns new future that will map original future results into new results)
		// Next map for option that is returned when original future completes
		// When original future completes callback is used to get results
		item.map { _.map(found).getOrElse(notFound(id, componentType)) }
	}

	/**
	 * Method to see if id for a component is valid.  Note operation is done via a future.
	 * @param componentId - ID for component
	 * @param validComponents - List of valid component types (if empty all types are valid)
	 * @param idType type of ID (used for error strings)
	 * @return None if id found, otherwise error message
	 */
	def isIdValid(componentId: Option[String], validComponents: List[ComponentType.ComponentType],
	              idType: String) =
		componentId match {
			case Some(id) => findByID[JsObject,Option[String]](id,validComponents,
				found = (_) => None,
				notFound = (_,_) =>
					Some(Errors.notFoundComponentMessage(id,validComponents) +
						" - change " + idType + " set or register " + idType + " " + id))
			case None => Future.successful(None)
		}

	/**
	 * Complete request (e.g., create or update) for a tracker item in our mongo db collection using a form.
	 *
	 * @param form form used to retrieve input
	 * @param afterBind action to take after a successful binding to the object from the form (returns a future)
	 * @param onFailure callback to get html to display upon verification failure (input contains form with invalid data)
	 * @param request request that contains form data
	 * @tparam I type of component being requested
	 * @return future to return request results
	 */
	private def doRequestFromForm[I <: Component : Writes](form: Form[I],
	                                                       afterBind: (I) => Future[Result],
	                                                       onFailure: Form[I] => HtmlFormat.Appendable)
	                                                      (request: Request[AnyContent]): Future[Result] = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		// Bind data to new form
		val bForm = form.bindFromRequest()(request)
		bForm.fold(
			formWithErrors => Future.successful(
				BadRequest(onFailure(formWithErrors.withGlobalError(Errors.validationError)))),
			data =>
				// Check if data is valid (it's done via a future) and then map result
				// Validity checker returns a map with fieldName->errorMessages with fieldName left out for global
				// (non field specific) errors - if data is valid the map is empty
				data.isRequestValid(request).flatMap {
					case e: Map[Option[String],String] if e.nonEmpty =>
						// Return complete future with form set with errors
						val formWithErrors = Errors.setFailureMsgs(e, form.fill(data))
						Future.successful(BadRequest(onFailure(formWithErrors)))
					// If all went well then callback to process data
					case _ => afterBind(data)
				}
		).recover {
			// On exception return bad request with html filled by call back
			case err => BadRequest(onFailure(bForm.withGlobalError(Errors.exceptionMessage(err))))
		}
	}

	/**
	 * Class to hold redirect calls and json conversions
	 * @param updateRoute Method to get call to redirect updates to
	 * @param addRoute Method to get call to redirect adds to
	 * @param jsonToComponent Method to create component object from json
	 */
	case class ControllerActions(updateRoute: (String) => Call, addRoute: (String) => Call,
	                             jsonToComponent: (JsObject) => Component)

	/**
	 * Map of calls to use for redirects to find (by id) or add a component and json to component conversions
	 */
	val actions =
		Map(
			ComponentType.Freezer ->
				ControllerActions(routes.FreezerController.findFreezerByID(_: String),
					routes.FreezerController.addFreezer(_: String), FreezerController.componentFromJson(_: JsObject)),
			ComponentType.Plate ->
				ControllerActions(routes.PlateController.findPlateByID(_: String),
					routes.PlateController.addPlate(_: String), PlateController.componentFromJson(_: JsObject)),
			ComponentType.Rack ->
				ControllerActions(routes.RackController.findRackByID(_: String),
					routes.RackController.addRack(_: String), RackController.componentFromJson(_: JsObject)),
			ComponentType.Tube ->
				ControllerActions(routes.TubeController.findTubeByID(_: String),
					routes.TubeController.addTube(_: String), TubeController.componentFromJson(_: JsObject))
		)

	/**
	 * Little check to make sure we didn't miss any components in the actions
	 */
	private val actionKeys = actions.keySet
	assert(ComponentType.values.forall(actionKeys.contains), "Incomplete action map")

	/**
	 * Some implicits needed to go to/from Json
	 */
	object Implicits {
		implicit val freezerFormatter = FreezerController.formatter
		implicit val plateFormatter = PlateController.formatter
		implicit val tubeFormatter = TubeController.formatter
		implicit val rackFormatter = RackController.formatter
	}

}
