package controllers

import controllers.Errors.FlashingKeys
import models.Transfer.Quad.Quad
import models.{ContainerDivisions, Transferrable, Component, Transfer}
import ContainerDivisions.Division._
import play.api.Logger
import play.api.data.Form
import play.api.mvc.{Result, Action, Controller}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * @author Nathaniel Novod
 *         Date: 2/24/15
 *         Time: 6:27 PM
 */
// @TODO Need to check for transfers when deleting component and need ability to delete individual transfers
object TransferController extends Controller with MongoController {
	/**
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses JSON for input/output of transfer data
	 */
	def transferCollection: JSONCollection = db.collection[JSONCollection]("transfer")

	/**
	 * Initiate transfer - go bring up form to do transfer
	 * @return action to go get transfer information
	 */
	def transfer() = Action { request =>
		Ok(views.html.transferStart(Errors.addStatusFlash(request, Transfer.form)))
	}

	/**
	 * Do transfer based on parameters from url - this is used when the transfer components are already set but
	 * there's additional information needed to complete the transfer.
	 * @param fromID component ID being transferred from
	 * @param toID component ID being transferred to
	 * @param project optional project associated with transfer
	 * @param fromQuad true if we need to get the quadrant we're transferring from
	 * @param toQuad true if we need to get the quadrant we're transferring to
	 * @return action to get additional transfer information wanted
	 */
	def transferWithParams(fromID: String, toID: String, project: Option[String],
	                       fromQuad: Option[Boolean], toQuad: Option[Boolean]) = {
		Action { request =>
			Ok(views.html.transfer(Errors.addStatusFlash(request, Transfer.form), fromID, toID, project,
				fromQuad.getOrElse(false), toQuad.getOrElse(false)))
		}
	}

	/**
	 * Take data from transfer form and check it out.  If one or both IDs are not found we return a form with the
	 * errors set, otherwise we return the objects found.
	 * @param data data retrieved from transfer form
	 * @tparam T type to return as transfer objects if they are found
	 * @return objects found and form with errors - one and only one will be set to None
	 */
	private def getTransferInfo[T: Reads](data: Transfer) = {
		// Go retrieve both objects via futures
		for {
			to <- ComponentController.trackerCollection.find(Json.toJson(Json.obj(Component.idKey -> data.to))).one
			from <- ComponentController.trackerCollection.find(Json.toJson(Json.obj(Component.idKey -> data.from))).one
		} yield {
			// Method to set form with missing ID(s) - errs is a list of form keys for ID(s) not found
			def missingIDs(errs: List[String]) = {
				val notFoundErrs: Map[Option[String], String] = errs.map(Some(_) -> "ID not found").toMap
				Errors.fillAndSetFailureMsgs(notFoundErrs, Transfer.form, data)
			}
			// Check out results from DB queries
			(from, to) match {
				case (Some(f), Some(t)) => (Some(f, t), None)
				case (None, Some(_)) => (None, Some(missingIDs(List(Transfer.fromKey))))
				case (Some(_), None) => (None, Some(missingIDs(List(Transfer.toKey))))
				case _ => (None, Some(missingIDs(List(Transfer.fromKey, Transfer.toKey))))
			}
		}
	}

	/**
	 * Get component object from json.
	 * @param json input json
	 * @return component object
	 */
	def getComponent(json: JsObject) = {
		import models.Component.ComponentType._
		val componentType = (json \ Component.typeKey).as[ComponentType]
		ComponentController.actions(componentType).jsonToComponent(json)
	}

	/**
	 * Is the transfer valid.  Check from component can be transferred to to component.
	 * @param data transfer data
	 * @param from json from find of from component
	 * @param to json from find of to component
	 * @return if transfer valid then from and to object
	 */
	private def isTransferValid(data: Transfer, from: JsObject, to: JsObject) = {
		// Get to component
		val toC = getComponent(to)
		// Get from component and see if transfer from it is valid
		getComponent(from) match {
			case fromC: Component with Transferrable if fromC.validTransfers.contains(toC.component) =>
				(fromC, toC, None)
			case fromC => (fromC, toC, Some("Can't do transfer from a " + fromC.component.toString +
				" to a " + toC.component.toString))
		}
	}

	/**
	 * Make message describing component transfer
	 * @param from component we're transferring from
	 * @param to component we're transferring to
	 * @return componentType ID to ComponentType ID
	 */
	private def fromToMsg(from: Component, to: Component) = {
		def componentStr(c: Component) = c.component.toString + " " + c.id
		"transfer of " + componentStr(from) + " to " + componentStr(to)
	}

	/**
	 * Result when transfer completes
	 * @param getMsg callback to get messages to set in response form
	 * @return redirect to index with wanted message
	 */
	private def transferCompleteResult(getMsg: () => String) =
		FlashingKeys.setFlashingValue(Redirect(routes.Application.index()), FlashingKeys.Status, getMsg())

	/**
	 * Result when transfer needs more information
	 * @param data transfer info known so far
	 * @param fromQuad true if need query for quadrant to transfer from
	 * @param toQuad true if need query for quadrant to transfer to
	 * @return redirect to transferWithParams to query for additional information
	 */
	private def transferIncompleteResult(data: Transfer, fromQuad: Boolean, toQuad: Boolean) = {
		val result = Redirect(routes.TransferController.transferWithParams(
			data.from, data.to, data.project, Some(fromQuad), Some(toQuad)))
		FlashingKeys.setFlashingValue(result, FlashingKeys.Status, "Fill in additional data to complete transfer")
	}

	/**
	 * Result when transfer form had errors
	 * @param form form filled with error data
	 * @return BadRequest to transferStart with input form
	 */
	private def transferErrorResult(form: Form[Transfer]) : Result = BadRequest(views.html.transferStart(form))

	/**
	 * Result when transfer had errors
	 * @param data data found in transfer form
	 * @param errs errors to set in form
	 * @return BadRequest to transferStart with form set with errors
	 */
	private def transferErrorResult(data: Transfer, errs: Map[Option[String], String]) : Result =
		transferErrorResult(Errors.fillAndSetFailureMsgs(errs, Transfer.form, data))

	/**
	 * Complete a future with a result
	 * @param res result to set as future
	 * @return future completed successfully with result
	 */
	private def now(res: Result) = Future.successful(res)

	/**
	 * Start of transfer - we look over IDs and see what's possible.  Depending on type of components being transferred
	 * different additional information may be needed.
	 * @return action to see what step is next to complete transfer
	 */
	def transferIDs = Action.async { request =>
		Transfer.form.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(transferErrorResult(formWithErrors.withGlobalError(Errors.validationError))),
			data => {
				// Got data from form - get from and to data (as json) - flatMap is mapping future from retrieving DB
				// data - flatMap continuation is either a future that completes immediately because transfer can not
				// be done now or a future that will complete when the DB insertion is done
				getTransferInfo[JsObject](data).flatMap {
					// Found both objects - now check if we can transfer between them
					case (Some((from, to)), None) =>
						isTransferValid(data, from, to) match {
							// Report attempt to transfer to itself
							case (fromData, toData, None) if fromData.id == toData.id =>
								now(transferErrorResult(data, Map(None -> "Can not transfer component to itself")))
							// Report problem transferring between requested components
							case (fromData, toData, Some(err)) => now(transferErrorResult(data, Map(None -> err)))
							// OK so far - now we need to check if it's ok to add this transfer to the graph leading in
							case (fromData, toData, None) => checkGraph(data, fromData, toData).flatMap {
								// Problem found
								case Some(err) => now(transferErrorResult(data, Map(None -> err)))
								// All is well - now just check if transfer should be done now or we need additional
								// information about what quadrants to transfer from/to
								case None =>
									if (fromData.isInstanceOf[ContainerDivisions] &&
										toData.isInstanceOf[ContainerDivisions])
										// See how quadrants line up
										(fromData.asInstanceOf[ContainerDivisions].layout,
											toData.asInstanceOf[ContainerDivisions].layout) match {
											// Go insert transfer between containers that are the same shape
											case (DIM8x12, DIM8x12) | (DIM16x24, DIM16x24) =>
												insertTransfer(data, () => fromToMsg(fromData, toData))
											// Go ask for quadrant to set in larger destination plate
											case (DIM8x12, DIM16x24) =>
												now(transferIncompleteResult(data, fromQuad = false, toQuad = true))
											// Go ask for quadrant to get in larger source plate
											case (DIM16x24, DIM8x12) =>
												now(transferIncompleteResult(data, fromQuad = true, toQuad = false))
										}
									else insertTransfer(data, () => fromToMsg(fromData, toData))
							}
						}
					// Couldn't find one or both data - form returned contains errors - return it now
					case (None, Some(form)) => now(transferErrorResult(form))
					// Should never have both or neither as None but...
					case _ => now(FlashingKeys.setFlashingValue(Redirect(routes.Application.index()),
						FlashingKeys.Status, "Internal error: Failure during transferIDs"))
				}
			}.recover {
				case err => transferErrorResult(data, Map(None -> Errors.exceptionMessage(err)))
			}
		).recover {
			case err => transferErrorResult(Transfer.form.withGlobalError(Errors.exceptionMessage(err)))
		}
	}

	/**
	 * Check if what will lead into this transfer is legit.  First we create a graph of what leads into the from
	 * component of this transfer.  Then we check that adding the new transfer will not make the graph cyclic and
	 * then we insure that any project specified with the transfer exists in the graph.
	 * @param data transfer data
	 * @param from where transfer is taking place from
	 * @param to where transfer is taking place to
	 * @return Future with error message if check fails, otherwise None
	 */
	private def checkGraph(data: Transfer, from: Component, to: Component) = {
		// Make graph of what leads into source of this transfer
		TransferHistory.makeGraph(data.from).map((graph) => {
			// Check if addition of target of transfer will make graph cyclic - if yes then complete with error
			val isCyclic = TransferHistory.isGraphAdditionCyclic(data.to,graph) match {
				case true =>
					Some(s"Error: Adding transfer will create a cyclic graph (${data.to} is already a source for ${data.from})")
				case _ => None
			}
			isCyclic match {
				// Graph would become cylic - report error
				case Some(err) => Some(err)
				// Graph will not be cylic - go on to check more
				case None =>
					// If project specified make sure it is part of graph
					data.project match {
						case Some(projectWanted) if from.project.isDefined && from.project.get == projectWanted => None
						case Some(projectWanted) =>
							// Get projects in graph
							val projects = TransferHistory.getGraphProjects(graph)
							// If specified project there then proceed with insert, otherwise complete with error
							if (projects.exists(_ == projectWanted)) None else {
								val projectsFound = from.project match {
									case Some(project) => projects + project
									case None => projects
								}
								val plural = if (projectsFound.size != 1) "s" else ""
								val projectsFoundStr = if (projectsFound.isEmpty) "" else
									s"Project${plural} found: ${projectsFound.mkString(",")}"
								Some(s"${projectWanted} not in ${from.id} or its derivatives. ${projectsFoundStr}")
							}
						// No project on transfer so nothing to check there
						case _ => None
					}
			}
		}).recover {
			case err => Some(Errors.exceptionMessage(err))
		}
	}

	/**
	 * Insert transfer into DB and return Result.
	 * @param data transfer to record in DB
	 * @return result to return with completion status
	 */
	private def insertTransfer(data: Transfer, whatDone: () => String) = {
		transferCollection.insert(data).map {
			(lastError) => {
				val success = "Inserted " + whatDone()
				Logger.debug(s"$success with status: $lastError")
				transferCompleteResult(() => "Completed " + whatDone())
			}
		}.recover {
			case err => transferErrorResult(data, Map(None -> Errors.exceptionMessage(err)))
		}
	}

	/**
	 * Make a description of the transfer, including quadrant descriptions.
	 * @param data data transferred
	 * @return description of transfer, including quadrant information
	 */
	private def quadDesc(data: Transfer) = {
		def qDesc(id: String, quad: Option[Quad]) = (if (quad.isDefined) quad.get.toString + " of " else "") + id
		"transfer from " + qDesc(data.from, data.fromQuad) + " to " + qDesc(data.to, data.toQuad)
	}

	/**
	 * Do transfer based on form with quadrant inputs
 	 * @return action to do transfer
	 */
	def transferFromForm = Action.async { request =>
		Transfer.form.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(transferErrorResult(formWithErrors.withGlobalError(Errors.validationError))),
			data => insertTransfer(data, () => quadDesc(data))
		).recover {
			case err => transferErrorResult(Transfer.form.withGlobalError(Errors.exceptionMessage(err)))
		}
	}

}
