package controllers

import models._
import models.db.{ TransferCollection, TrackerCollection }
import models.Component.{ HiddenFields, ComponentType }
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import play.twirl.api.{ HtmlFormat, Html }
import utils.{ Yes, No, YesOrNo, MessageHandler }

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * @author Nathaniel Novod
 *         Date: 11/25/14
 *         Time: 9:52 AM
 */
/**
 * Controller to do CRUD on a component to/from a Mongo database.
 * @tparam C type of component
 */
trait ComponentController[C <: Component] extends Controller {

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
	 * @param id component ID
	 * @param hiddenFields hidden fields set to what's currently in component
	 * @return method that converts a form to html to display (typically via a Play template view)
	 */
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]): (Form[C]) => Html

	/**
	 * Method to use to take a form filled with data and return the html to be displayed after a create - supplied
	 * by component type inheriting trait.
	 * @param id component ID
	 * @return method that converts a form to html to display (typically a Play template view)
	 */
	def htmlForCreate(id: String): (Form[C]) => Html

	/**
	 * Method to use to take a form filled with data and return the html to be displayed after a create of a stack
	 * of components - supplied by component type inheriting trait.
	 * @return method that converts a form to html to display (typically a Play template view)
	 */
	def htmlForCreateStack: (Form[C]) => Html

	/**
	 * Create html to be sent back to get parameters to make a stack.  First create the form filled in with IDs and
	 * then output the html based on the created form.
	 * @param id ids for stack
	 * @param completionStr completion status to set as global message in form
	 * @return html to create stack of plates
	 */
	def makeStackHtml(id: String, completionStr: String): Html

	/**
	 * Component type, supplied by component inheriting trait (maybe someday make a macro to get this based on C type?)
	 */
	val componentType: ComponentType.ComponentType

	/**
	 * Make an object from json
	 * @param json input json
	 * @return object returned via reading of Json
	 */
	def componentFromJson(json: JsObject): C

	/**
	 * Add - just say ok and display form for creating component
	 * @return Ok status with create html
	 */
	def add(id: String) = {
		Ok(htmlForCreate(id)(form))
	}

	/**
	 * Add a stack of components
	 * @param request request containing component ids and data
	 * @return result from inserts
	 */
	def addStack(request: Request[AnyContent]) = create("", request, isStack = true)

	/**
	 * Return result with message(s):
	 * a)Get form filled with data and flash status (set by previous request) as a global message on the form
	 * b)Add additional messages to be displayed in the form
	 * c)Create html from form
	 * d)Get result - status with html as associated returned data
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
		val filledForm = MessageHandler.addStatusFlash(request, form.fill(c))
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
		findByID[C, Result](id, List(componentType),
			// Found wanted component - go setup to update data
			found = (c) => viewData(status = Ok, c = c, request = request,
				html = htmlForUpdate(id, Some(c.hiddenFields)),
				msgs = Map.empty, messageSetter = MessageHandler.setMessages),
			// NotFound - just redirect to not found view
			notFound = MessageHandler.notFoundRedirect
		).recover {
				case err => BadRequest(
					views.html.index(
						Component.blankForm.withGlobalError(MessageHandler.exceptionMessage(err))
					)
				)
			}
	}

	/**
	 * Create using data returned in the request form - if all goes well insert component(s) into db and go home.
	 * @param id Component ID (ignored if a stack)
	 * @param request http request
	 * @param isStack creating a stack of components
	 * @return future to create component(s) (on error displays form with errors)
	 */
	def create(id: String, request: Request[AnyContent], isStack: Boolean = false) = {
		doRequestFromForm(form,
			// If a stack then convert to component that can make list
			afterBind = (c: C) => c match {
				case cl: ComponentCanBeList[C] if isStack => cl.toComponentList
				case _ => c
			},
			// Note that onSuccess returns a future via insertComponent(s)
			onSuccess = (c: C) => c match {
				case cl: ComponentList[C] =>
					TrackerCollection.insertComponents(cl.makeList).map {
						case (ok: List[String], nok: List[String]) =>
							def plural(x: Int) = if (x == 1) "" else "s"
							val summary = s"${ok.size} component${plural(ok.size)} succcessfully inserted, " +
								s"${nok.size} insertion${plural(nok.size)} failed" +
								(if (nok.isEmpty) "" else ":\n" + nok.mkString(";\n"))
							MessageHandler.homeRedirect(summary)
					}
				case data =>
					TrackerCollection.insertComponent(data,
						// Go home and tell everyone what we've done
						onSuccess = (status) => MessageHandler.homeRedirect(status),
						// Recover from exception - return form with errors
						onFailure =
							(err) => BadRequest(
								htmlForCreate(id)
								(form.withGlobalError(MessageHandler.exceptionMessage(err)))
							)
					)
			},
			// if trouble then show form that should be updated with error messages
			onFailure = (f: Form[C]) => if (isStack) htmlForCreateStack(f) else htmlForCreate(id)(f)
		)(request)
	}

	/**
	 * Update - if all goes well update component in db and and gh home.
	 * @param id component id
	 * @param request http request
	 * @param preUpdate callback to allow pre-update checking - returns non-empty map of error(s) to abort update
	 * @return future update component (on error displays form with errors)
	 */
	def update(id: String, request: Request[AnyContent],
			   preUpdate: (C) => Future[Map[Option[String], String]] = (_) => Future.successful(Map.empty)) = {
		doRequestFromForm(form,
			afterBind = (c: C) => c,
			// Note that onSuccess returns a future
			onSuccess = (data: C) => {
				// Allow caller to do some pre update checking
				preUpdate(data).flatMap((errs) =>
					if (errs.isEmpty) {
						// Binding was successful - now go update the wanted item with data from the form
						TrackerCollection.updateComponent(data,
							// Go home and tell everyone what we've done
							onSuccess = (msg) => MessageHandler.homeRedirect(msg),
							// Recover from exception - return form with errors
							onFailure = (err) =>
								BadRequest(
									htmlForUpdate(id = id, hiddenFields = Component.getHiddenFields(request))
									(form.withGlobalError(MessageHandler.exceptionMessage(err)))
								)
						)
					} else {
						// If pre-update returned errors then return form with those errors - remember original
						// hidden fields so any changes made to them are ignored for now
						Future.successful(
							viewData(status = BadRequest, c = data, request = request,
								html = htmlForUpdate(id = id, hiddenFields = Component.getHiddenFields(request)),
								msgs = errs, messageSetter = MessageHandler.setFailureMsgs)
						)
					}
				)
			},
			// if trouble then show form that should be updated with error messages
			onFailure = (f: Form[C]) => htmlForUpdate(id = id, hiddenFields = Component.getHiddenFields(request))(f)
		)(request)
	}
}

object ComponentController extends Controller {
	/**
	 * Go delete item and associated transfers from DB.
	 * @param id ID for item to be deleted
	 * @param deleted callback if delete went well
	 * @param error callback if error during delete
	 * @tparam R result type
	 * @return returns result of deleted or error callback
	 */
	def delete[R](id: String, deleted: (Int) => R, error: (Throwable) => R) = {
		// Start up deletes of component as well as associated transfers
		val componentRemove = TrackerCollection.remove(id)
		val transferRemove = TransferCollection.removeTransfers(id)
		// Line futures up for completion and callback with results
		(for {
			cr <- componentRemove
			tr <- transferRemove
		} yield {
			(cr, tr) match {
				case (Some(err), _) => error(err)
				case (_, Some(err)) => error(err)
				case _ => deleted(1)
			}
		}).recover {
			case err => error(err)
		}
	}

	/**
	 * Do delete an item and it subcomponents.  A "delete" means deleting the component itself as well as all transfers
	 * leading in and out of it.
	 * @param id ID for item to be deleted
	 * @param success callback if operation(s) go well (input is # of deletes done)
	 * @param error callback if error during operation
	 * @tparam R result type
	 * @return returns result of success or error callback
	 */
	def doSubsDelete[R](id: String, success: (Int) => R, error: (Throwable) => R) = {
		// Get partially completed function to do deletion - now just need to supply ID
		val doDelete =
			delete[YesOrNo[Int]](_: String,
				deleted = (_) => Yes(1), error = (err) => No(err.getLocalizedMessage))
		// Go do deletes
		doSubsOper[R, Int](id = id, oper = doDelete, success = (l) => success(l.sum), error = error)
	}

	/**
	 * Go get count of # of transfers for an item.
	 * @param id ID for item to get counts for
	 * @param counted callback if count retrieved
	 * @param error callback if error during count
	 * @tparam R result type for callbacks
	 * @return returns result of counted or error callback
	 */
	def transferCount[R](id: String, counted: (Int) => R, error: (Throwable) => R) =
		TransferCollection.countTransfers(id).map(counted)
			.recover {
				case err => error(err)
			}

	/**
	 * Partially applied function to get transfer counts - just need to supply ID to make actual call
	 */
	private val doCount = transferCount[YesOrNo[Int]](_: String,
		counted = Yes(_), error = (err) => No(err.getLocalizedMessage))

	/**
	 * Get count of # of transfers from/to and item and it's subcomponents.
	 * @param id ID for item for which to get transfer count
	 * @param success callback if operation(s) go well (called with # of transfers)
	 * @param error callback if error during operation
	 * @tparam R result type
	 * @return returns result of success or error callback
	 */
	def doSubsTransferCount[R](id: String, success: (Int) => R, error: (Throwable) => R) =
		// Go get count of transfers
		doSubsOper[R, Int](id = id, oper = doCount, success = (l) => success(l.sum), error = error)

	/**
	 * Get count of # of components and associated # of transfers.
	 * @param id ID for item for which to get counts
	 * @param success callback if operation(s) go well (called with # of components found, # of transfers found)
	 * @param error callback if error during operation
	 * @tparam R result type
	 * @return returns results of success or error callback
	 */
	def doComponentCounts[R](id: String, success: (Int, Int) => R, error: (Throwable) => R) =
		// Go get count of components & associated transfers
		doSubsOper[R, Int](id = id, oper = doCount, success = (l) => success(l.size, l.sum), error = error)

	/**
	 * Get subcomponent well->ID map.  If there are any errors we simply return an empty map.
	 * @param id id of component
	 * @return well->subcomponentID map
	 */
	def getSubsIDs(id: String) =
		TrackerCollection.findComponent(id).flatMap {
			// Should just be one found - if there are subcomponents to fetch then go get map of them
			case Some(found) if getSubFetcher(found).isDefined =>
				// Get fetcher - wouldn't get here if one wasn't there
				val subF = getSubFetcher(found).get
				// Go get map of wells to subcomponent IDs
				subF()
					.map {
						case Yes(wellsMap) => wellsMap
						case _ => Map.empty[String, String]
					}
			case _ => Future.successful(Map.empty[String, String])
		}

	/**
	 * Get method to get fetch subcomponents
	 * @param c component to get subcomponents for
	 * @return method to fetch subcomponents
	 */
	private def getSubFetcher(c: Component) =
		c match {
			case sc: SubComponents => sc.getSubFetcher
			case _ => None
		}

	/**
	 * Do operation on item and its subcomponents.
	 * @param id ID for item to be worked on
	 * @param oper operation to do for each subcomponent and the main component, ID is input to oper
	 * @param success callback if operation(s) go well (input is list of results)
	 * @param error callback if error during operation
	 * @tparam R result type for success and error callbacks
	 * @tparam O result type for operations done on items
	 * @return returns result of success or error callback
	 */
	private def doSubsOper[R, O](id: String, oper: (String) => Future[YesOrNo[O]],
		success: (List[O]) => R, error: (Throwable) => R) = {
		// Get component
		TrackerCollection.findComponent(id)
			.flatMap {
				// Should just be one found - if there are subcomponents to fetch then go operate on each of them
				case Some(found) if getSubFetcher(found).isDefined =>
					// Get subcomponent fetcher (wouldn't be here if one didn't exist)
					val subF = getSubFetcher(found).get
					// Go get subcomponents
					subF()
						.flatMap {
							case Yes(toWells) =>
								// Start up one operation for the main component and each subcomponent
								val opers = oper(id) :: toWells.values.map(oper).toList
								// Gather all the futures into one to contain list of results
								Future.sequence(opers)
									.map((yesOrNos) => {
										// Get all errors there (flatMap eliminates Nones)
										val errs = yesOrNos.flatMap(_.getNoOption)
										// If any errors throw exception to be caught
										if (errs.nonEmpty)
											throw new Exception(s"Error accessing subcomponents: ${errs.mkString("; ")}")
										// Otherwise callback to success function with list of subcomponents found
										else
											success(yesOrNos.flatMap(_.getYesOption))
									})
							// Throw exception to be caught
							case No(err) =>
								throw new Exception(s"Error finding subcomponents of $id: $err")
						}
				// No subcomponent found - do single operation and callback to success function with single entry list
				case Some(found) =>
					oper(id)
						.map {
							case Yes(ret) => success(List(ret))
							case No(err) => throw new Exception(s"Error accessing $id: $err")
						}
				// Didn't find component - throw exception to be caught
				case _ => throw new Exception(s"Failed to find ID $id")
			}
			.recover {
				// If an error (thrown by our own code or elsewhere) then callback to error function
				case err => error(err)
			}
	}

	/**
	 * Find item by querying the mongo collection using an id.  We return a future that will call back with the
	 * query results.
	 * @param id item id to search for
	 * @param componentType list of component types to search for (if empty all types are allowed)
	 * @param found callback to set result if item found
	 * @param notFound callback to set result if item not found
	 * @tparam T type of data to be found (must have associated Reads for validation and conversion of JsValue)
	 * @tparam R type of result
	 * @return future result, as determined by callbacks, of query
	 */
	def findByID[T: Reads, R](id: String, componentType: List[ComponentType.ComponentType], found: (T) => R,
		notFound: (String, List[ComponentType.ComponentType]) => R) = {
		// let’s do our query to a single item with the wanted id (and component type(s) if supplied)
		val findMap =
			if (componentType.isEmpty)
				Json.obj(Component.idKey -> id)
			else
				Json.obj(Component.idKey -> id, Component.typeKey -> Json.obj("$in" -> componentType.map(_.toString)))
		// Using implicit reader and execution context
		val item = TrackerCollection.findOneWithJsonQuery(Json.toJson(findMap))
		// First map for future (maps initial future results into new results set by found callback)
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
			case Some(id) =>
				findByID[JsObject, Option[String]](id = id, componentType = validComponents,
					found = (_) => None,
					notFound = (_, _) =>
						Some(
							MessageHandler.notFoundComponentMessage(id = id, componentType = validComponents) +
								s" - change component type set or register $idType $id"
						)
				)
			case None => Future.successful(None)
		}

	/**
	 * Complete request (e.g., create or update) for a tracker item in our mongo db collection using a form.
	 * @param form form used to retrieve input
	 * @param afterBind callback after successful bind from form
	 * @param onSuccess action to take after a successful verification of object
	 * @param onFailure callback to get html to display if verification failure (input contains form with invalid data)
	 * @param request request that contains form data
	 * @tparam I type of component being requested
	 * @return future to return request results
	 */
	private def doRequestFromForm[I <: Component: Writes](form: Form[I],
		afterBind: (I) => I,
		onSuccess: (I) => Future[Result],
		onFailure: Form[I] => HtmlFormat.Appendable)(request: Request[AnyContent]): Future[Result] = {
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		// Bind data to new form
		val bForm = form.bindFromRequest()(request)
		bForm.fold(
			formWithErrors =>
				Future.successful(
					BadRequest(onFailure(MessageHandler.formGlobalError(formWithErrors, MessageHandler.validationError)))
				),
			data => {
				// Allow data to be modified (used for stack requests)
				val newData = afterBind(data)
				// Check if data is valid (it's done via a future) and then map result
				// Validity checker returns a map with fieldName->errorMessages with fieldName left out for global
				// (non field specific) errors - if data is valid the map is empty
				newData.isRequestValid(request).flatMap {
					case e: Map[Option[String], String] if e.nonEmpty =>
						// Return complete future with form set with errors
						val formWithErrors = MessageHandler.setFailureMsgs(e, form.fill(newData))
						Future.successful(BadRequest(onFailure(formWithErrors)))
					// If all went well then callback to process data
					case _ => onSuccess(newData)
				}
			}
		).recover {
				// On exception return bad request with html filled by call back
				case err => BadRequest(onFailure(bForm.withGlobalError(MessageHandler.exceptionMessage(err))))
		}
	}

	/**
	 * Class to hold redirect calls and json conversions
	 * @param updateRoute Method to get call to redirect updates to
	 * @param addRoute Method to get call to redirect adds to
	 * @param createStackView Method to create html to be used to add stack of components
	 */
	case class ControllerActions(updateRoute: (String) => Call, addRoute: (String) => Call,
		createStackView: (String, String) => Html)

	/**
	 * Map of calls to use for redirects to find (by id) or add a component and json to component conversions
	 */
	val actions =
		Map(
			ComponentType.Freezer ->
				ControllerActions(routes.FreezerController.findFreezerByID(_: String),
					routes.FreezerController.addFreezer(_: String),
					FreezerController.makeStackHtml(_: String, _: String)),
			ComponentType.Plate ->
				ControllerActions(routes.PlateController.findPlateByID(_: String),
					routes.PlateController.addPlate(_: String),
					PlateController.makeStackHtml(_: String, _: String)),
			ComponentType.Rack ->
				ControllerActions(routes.RackController.findRackByID(_: String),
					routes.RackController.addRack(_: String),
					RackController.makeStackHtml(_: String, _: String)),
			ComponentType.Tube ->
				ControllerActions(routes.TubeController.findTubeByID(_: String),
					routes.TubeController.addTube(_: String),
					TubeController.makeStackHtml(_: String, _: String))
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
