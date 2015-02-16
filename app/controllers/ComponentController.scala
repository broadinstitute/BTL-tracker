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
	 * @return method that creates html (typically via a Play template view) to display
	 */
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]): (Form[C]) => Html

	/**
	 * Method to use to take a form filled with data and return the html to be displayed after a create - supplied
	 * by component type inheriting trait.
	 * @return html (typically a Play template view) to display
	 */
	def htmlForCreate: (Form[C]) => Html

	/**
	 * Component type, supplied by component inheriting trait (maybe someday make a macro to get this based on C type?)
	 */
	val componentType: ComponentType.ComponentType

	/**
	 * Add - just say ok and display form for creating component
	 * @return Ok status with create html
	 */
	def add = Ok(htmlForCreate(form))

	/**
	 * Fill a form with data and set any flash status (set by previous request) as global error(s) on the form.  Flash
	 * is used to pick up any errors set by a previous request redirected to the current request containing the
	 * input form.
 	 * @param c component to display
	 * @param request html request
	 * @return form filled in with data and status message from flash
	 */
	private def fillFormWithStatus(c: C, request: Request[AnyContent]) = Errors.addStatusFlash(request, form.fill(c))

	/**
	 * Return result with message.
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
	                     messageSetter: (Map[Option[String], String], Form[C]) => Form[C]) =
		status(html(messageSetter(msgs, fillFormWithStatus(c, request))))


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
			found = (c) => viewData(Ok, c, request,	htmlForUpdate(id, Some(c.hiddenFields)), Map.empty,setMessages),
			// NotFound - just redirect to not found view
			notFound = Errors.notFoundRedirect
		).recover {
			case err => BadRequest(views.html.find(Component.idForm.withGlobalError(Errors.exceptionMessage(err))))
		}
	}

	/**
	 * Create using data returned in the request form - if all goes well insert component into db and display newly
	 * entered data.
	 * @param request http request
	 * @return future to get resulting html to display created component (on error displays form with errors)
	 */
	def create(request: Request[AnyContent]) = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		doRequestFromForm(form,
			// Note that afterbind returns a future
			afterBind = (data: C) => {
				collection.insert(data).map { lastError =>
					val success = s"Successfully inserted item ${data.id}"
					Logger.debug(s"$success with status: $lastError")
					// Return view of data just created
					viewData(Ok, data, request,
						htmlForUpdate(data.id, Some(data.hiddenFields)), Map(None -> success), setMessages)
				}
			},
			// if trouble then show form that should be updated with error messages
			onFailure = (f: Form[C]) => htmlForCreate(f)
		)(request).recover {
			case err => BadRequest(htmlForCreate(form.withGlobalError(Errors.exceptionMessage(err))))
		}
	}

	/**
	 * Update - if all goes well update component in db and redirect to display newly entered data.
	 * @param request http request
	 * @param id component id
	 * @param preUpdate callback to allow pre-update checking - returns non-empty map of error(s) to abort update
	 * @return future to get resulting html to display updated component (on error displays form with errors)
	 */
	def update(request: Request[AnyContent], id: String,
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
					collection.update(selector,data).map { lastError =>
						val success = s"Successfully updated ${data.id}"
						Logger.debug(s"$success with status: $lastError")
						// Return view of data just updated
						viewData(Ok, data, request,
							htmlForUpdate(id, Some(data.hiddenFields)), Map(None -> success), setMessages)
					}
				} else {
					// If pre-update returned errors then return form with those errors - remember original
					// hidden fields so any changes made to them are ignored for now
					Future.successful(
						viewData(BadRequest, data, request,
							htmlForUpdate(id, Component.getHiddenFields(request)), errs, setFailureMsgs))
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
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses JSON for input/output
	 */
	private def collection: JSONCollection = db.collection[JSONCollection]("tracker")

	/**
	 * Go delete item from DB
	 * @param id ID for item to be deleted
	 * @param deleted callback if delete went well
	 * @param error callback if error during delete
	 */
	def delete[R](id: String, deleted: () => R, error: (Throwable) => R) = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		val key = Json.obj(Component.idKey -> id)
		collection.remove(Json.toJson(key)).map { lastError =>
			val success = s"Successfully deleted item ${id}"
			Logger.debug(s"$success with status: $lastError")
			deleted()
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
		val item = collection.find(Json.toJson(findMap)).one
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
						" - change " + idType + " set or add " + idType + " " + id))
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
					case e: Map[Option[String],String] if !e.isEmpty =>
						// Return complete future with form set with errors
						val formWithErrors = setFailureMsgs(e, form.fill(data))
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
	 * Method to take messages and put them on the form
	 *
	 * @param msgs map of error messages in form of fieldName->errorMessage (no fieldname for global error)
	 * @return form to display with messages
	 */
	private def setMessages[I](msgs: Map[Option[String],String], form: Form[I]) = {
		// Add messages to form - getting a new form each time a message is added
		msgs.foldLeft(form) {
			case (f, (Some(k),m)) => f.withError(k,m)
			case (f, (None,m)) => f.withGlobalError(m)
		}
	}

	/**
	 * Method to take errors and put them on the form
	 *
	 * @param msgs map of error messages in form of fieldName->errorMessage
	 * @return Complete future with result of "BadRequest" and form to display and set with errors
	 */
	private def setFailureMsgs[I](msgs: Map[Option[String],String], form: Form[I]) = {
		// Make form with data filled in and errors set with fields
		val formWithFieldErrors = setMessages(msgs, form)
		// See if global error already exists
		val isGlobalErrors = msgs.exists(_._1 == None)
		// Add global error in form - if other errors set are not global they will be field specific later in form
		formWithFieldErrors.withGlobalError("Operation not completed - fix errors" +
			(if (!isGlobalErrors) " below" else ""))
	}


	/**
	 * Class to hold redirect calls
	 * @param update Method to get call to redirect updates to
	 * @param add Method to get call to redirect adds to
	 */
	case class Redirects(update: (String) => Call, add: () => Call)

	/**
	 * Map of calls to use for redirects to find (by id) or add a component
	 */
	val redirects =
		Map(
			ComponentType.Freezer ->
				Redirects(routes.FreezerController.findFreezerByID(_: String), routes.FreezerController.addFreezer),
			ComponentType.Plate ->
				Redirects(routes.PlateController.findPlateByID(_: String), routes.PlateController.addPlate),
			ComponentType.Rack ->
				Redirects(routes.RackController.findRackByID(_:String), routes.RackController.addRack),
			ComponentType.Sample ->
				Redirects(routes.SampleController.findSampleByID(_: String), routes.SampleController.addSample),
			ComponentType.Tube ->
				Redirects(routes.TubeController.findTubeByID(_: String), routes.TubeController.addTube),
			ComponentType.Well ->
				Redirects(routes.WellController.findWellByID(_: String), routes.WellController.addWell),
			ComponentType.Material ->
				Redirects(routes.MaterialController.findMaterialByID(_: String), routes.MaterialController.addMaterial)
		)

	/**
	 * Little check to make sure we didn't miss any components in the redirects
	 */
	private val redirectKeys = redirects.keySet
	assert(ComponentType.values.forall(redirectKeys.contains),	"Incomplete redirect map")

	/**
	 * Some implicits needed to go to/from Json
	 */
	object Implicits {
		implicit val freezerFormatter = FreezerController.formatter
		implicit val sampleFormatter = SampleController.formatter
		implicit val wellFormatter = WellController.formatter
		implicit val plateFormatter = PlateController.formatter
		implicit val tubeFormatter = TubeController.formatter
		implicit val rackFormatter = RackController.formatter
		implicit val materialFormatter = MaterialController.formatter
	}

}
