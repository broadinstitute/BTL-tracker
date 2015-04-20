package controllers

import controllers.Errors.FlashingKeys
import models.Transfer.Quad.Quad
import models.Transfer.Slice.Slice
import models._
import play.api.Logger
import play.api.data.Form
import play.api.mvc.{Result, Action, Controller}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONArray, BSONDocument}
import reactivemongo.core.commands.Count

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * @author Nathaniel Novod
 *         Date: 2/24/15
 *         Time: 6:27 PM
 */
// @TODO Need ability to delete individual transfers
object TransferController extends Controller with MongoController {
	/**
	 * Get collection to do mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
	 * @return collection that uses JSON for input/output of transfer data
	 */
	val transferCollectionName = "transfer"
	def transferCollection: JSONCollection = db.collection[JSONCollection](transferCollectionName)
	def transferCollectionBSON: BSONCollection = db.collection[BSONCollection](transferCollectionName)

	/**
	 * Initiate transfer - if additional info needed bring up form otherwise try to do transfer now
	 * @return action to go get transfer information
	 */
	def transfer(fromID: Option[String], toID: Option[String], project: Option[String]) = Action.async { request =>
		if (fromID.isDefined && toID.isDefined)
			doTransfer(TransferStart(fromID.get, toID.get, project))
		else
			Future.successful(Ok(views.html.transferStart(Errors.addStatusFlash(request, Transfer.startForm),
				fromID, toID, project, true)))
	}

	/**
	 * Do transfer based on parameters from url - this is used when the transfer components are already set but
	 * there's additional information needed to complete the transfer.
	 * @param fromID component ID being transferred from
	 * @param toID component ID being transferred to
	 * @param project optional project associated with transfer
	 * @param fromQuad true if we need to get the quadrant we're transferring from
	 * @param toQuad true if we need to get the quadrant we're transferring to
	 * @param dataMandatory true if requested quadrant information must be specified
	 * @param isQuadToQuad true if quadrant transfers must have from and to quadrant
	 * @return action to get additional transfer information wanted
	 */
	def transferWithParams(fromID: String, toID: String, project: Option[String],
	                       fromQuad: Boolean, toQuad: Boolean, dataMandatory: Boolean, isQuadToQuad: Boolean) = {
		Action { request =>
			Ok(views.html.transfer(Errors.addStatusFlash(request, Transfer.form), fromID, toID, project,
				fromQuad, toQuad, dataMandatory, isQuadToQuad))
		}
	}

	/**
	 * Take data from transfer form and check it out.  If one or both IDs are not found we return a form with the
	 * errors set, otherwise we return the objects found.
	 * @param data data retrieved from transfer form
	 * @tparam T type to return as transfer objects if they are found
	 * @return objects found and form with errors - one and only one will be set to None
	 */
	private def getTransferInfo[T: Reads](data: TransferStart) = {
		// Go retrieve both objects via futures
		for {
			to <- ComponentController.trackerCollection.find(Json.toJson(Json.obj(Component.idKey -> data.to))).one
			from <- ComponentController.trackerCollection.find(Json.toJson(Json.obj(Component.idKey -> data.from))).one
		} yield {
			// Method to set form with missing ID(s) - errs is a list of form keys for ID(s) not found
			def missingIDs(errs: List[String]) = {
				val notFoundErrs: Map[Option[String], String] = errs.map(Some(_) -> "ID not found").toMap
				Errors.fillAndSetFailureMsgs(notFoundErrs, Transfer.startForm, data)
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
	private def isTransferValid(data: TransferStart, from: JsObject, to: JsObject) = {
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
	private def transferIncompleteResult(data: TransferStart,
										 fromQuad: Boolean, toQuad: Boolean) = {
		// dataMandatory set via xor of quad settings (if only one quad wanted then it's transfer between different
		// size components)
		val dataMandatory = (fromQuad && !toQuad) || (toQuad && !fromQuad)
		val result = Redirect(routes.TransferController.
			transferWithParams(data.from, data.to, data.project, fromQuad, toQuad, dataMandatory, fromQuad && toQuad))
		val quadPlural = if (fromQuad && toQuad) "s" else ""
		val quadsThere = if (fromQuad || toQuad) " and quadrant" else ""
		val infoType = if (dataMandatory) s"Specify quadrant$quadPlural and optionally slice"
		else s"If transferring a slice specify slice$quadsThere$quadPlural"
		FlashingKeys.setFlashingValue(result, FlashingKeys.Status,
			s"$infoType before completing transfer")
	}

	/**
	 * Result when transfer start form had errors
	 * @param form form filled with error data
	 * @param fromID ID of component we're transferrring from
	 * @param toID ID of component we're transferring to
	 * @param project project associated with transfer
	 * @return BadRequest to transferStart with input form
	 */
	private def transferStartFormErrorResult(form: Form[TransferStart],
											 fromID: Option[String], toID: Option[String], project: Option[String]) =
		BadRequest(views.html.transferStart(form, fromID, toID, project, false))

	/**
	 * Result when transfer start had errors
	 * @param data data found in transfer form
	 * @param errs errors to set in form
	 * @return BadRequest to transferStart with form set with errors
	 */
	private def transferStartErrorResult(data: TransferStart, errs: Map[Option[String], String]) =
		transferStartFormErrorResult(Errors.fillAndSetFailureMsgs(errs, Transfer.startForm, data),
			Some(data.from), Some(data.to), data.project)

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
		Transfer.startForm.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(transferStartFormErrorResult(formWithErrors.withGlobalError(Errors.validationError),
					None, None, None)),
			data => doTransfer(data)
		).recover {
			case err => transferStartFormErrorResult(Transfer.startForm.withGlobalError(Errors.exceptionMessage(err)),
				None, None, None)
		}
	}

	def doTransfer(data: TransferStart) = {
		// Got data from form - get from and to data (as json) - flatMap is mapping future from retrieving DB
		// data - flatMap continuation is either a future that completes immediately because transfer can not
		// be done now or a future that will complete when the DB insertion is done
		getTransferInfo[JsObject](data).flatMap {
			// Found both objects - now check if we can transfer between them
			case (Some((from, to)), None) =>
				isTransferValid(data, from, to) match {
					// Report attempt to transfer to itself
					case (fromData, toData, None) if fromData.id == toData.id =>
						now(transferStartErrorResult(data, Map(None -> "Can not transfer component to itself")))
					// Report problem transferring between requested components
					case (fromData, toData, Some(err)) => now(transferStartErrorResult(data, Map(None -> err)))
					// OK so far - now we need to check if it's ok to add this transfer to the graph leading in
					case (fromData, toData, None) => checkGraph(data, fromData, toData).flatMap {
						// Problem found
						case Some(err) => now(transferStartErrorResult(data, Map(None -> err)))
						// All is well - now just check if transfer should be done now or we need additional
						// information about what quadrants/slices to transfer from/to
						case None =>
							import ContainerDivisions.Division._
							(fromData, toData) match {
								case (f: ContainerDivisions, t: ContainerDivisions) =>
									(f.layout,t.layout) match {
										// Check if slicing wanted
										case (DIM8x12, DIM8x12) =>
											now(transferIncompleteResult(data, fromQuad = false, toQuad = false))
										// if doing slices between 384-well plates then need to pick quadrant
										case (DIM16x24, DIM16x24) =>
											now(transferIncompleteResult(data, fromQuad = true, toQuad = true))
										// Go ask for quadrant/slice to set in larger destination plate
										case (DIM8x12, DIM16x24) =>
											now(transferIncompleteResult(data, fromQuad = false, toQuad = true))
										// Go ask for quadrant/slice to get in larger source plate
										case (DIM16x24, DIM8x12) =>
											now(transferIncompleteResult(data, fromQuad = true, toQuad = false))
									}
								case (_: ContainerDivisions, _) =>
									now(transferIncompleteResult(data, fromQuad = false, toQuad = false))
								case _ =>
									val tForm = data.toTransferForm
									insertTransfer(tForm, () => quadDesc(tForm.transfer))
							}
					}
				}
			// Couldn't find one or both data - form returned contains errors - return it now
			case (None, Some(form)) => now(transferStartFormErrorResult(form,
				Some(data.from), Some(data.to), data.project))
			// Should never have both or neither as None but...
			case _ => now(FlashingKeys.setFlashingValue(Redirect(routes.Application.index()),
				FlashingKeys.Status, "Internal error: Failure during transferIDs"))
		}
	}.recover {
		case err => transferStartErrorResult(data, Map(None -> Errors.exceptionMessage(err)))
	}

	/**
	 * Check if what will lead into this transfer is legit.  First we create a graph of what leads into the from
	 * component of this transfer.  Then we check that adding the new transfer will not make the graph cyclic and
	 * insure that any project specified with the transfer exists in the graph.
	 * @param data transfer data
	 * @param from where transfer is taking place from
	 * @param to where transfer is taking place to
	 * @return Future with error message if check fails, otherwise None
	 */
	private def checkGraph(data: TransferStart, from: Component, to: Component) = {
		// Make graph of what leads into source of this transfer
		TransferHistory.makeSourceGraph(data.from).map((graph) => {
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
							// If specified project there then return with no error, otherwise complete with error
							if (projects.contains(projectWanted)) None else {
								// Project not in graph's project list
								val projectsFound = from.project match {
									case Some(project) => projects + project
									case None => projects
								}
								val plural = if (projectsFound.size != 1) "s" else ""
								val projectsFoundStr = if (projectsFound.isEmpty) "" else
									s"Project$plural found: ${projectsFound.mkString(",")}"
								Some(s"$projectWanted not in ${from.id} or its derivatives. $projectsFoundStr")
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
	 * Result when transfer form had errors.
	 * @param form form filled with error data
	 * @param data data found in transfer form
	 * @return BadRequest to transfer with input form
	 */
	private def transferFormErrorResult(form: Form[TransferForm], data: TransferForm) = {
		BadRequest(views.html.transfer(form, data.transfer.from, data.transfer.to, data.transfer.project,
			data.isQuadToQuad || data.transfer.fromQuad.isDefined,
			data.isQuadToQuad || data.transfer.toQuad.isDefined, data.dataMandatory, data.isQuadToQuad))
	}

	/**
	 * Result when transfer had errors
	 * @param data data found in transfer form
	 * @param errs errors to set in form
	 * @return BadRequest to transfer with form set with errors
	 */
	private def transferErrorResult(data: TransferForm, errs: Map[Option[String], String]) =
		transferFormErrorResult(Errors.fillAndSetFailureMsgs(errs, Transfer.form, data), data)

	/**
	 * Insert transfer into DB and return Result.
	 * @param data transfer to record in DB
	 * @return result to return with completion status
	 */
	private def insertTransfer(data: TransferForm, whatDone: () => String) = {
		transferCollection.insert(data.transfer).map {
			(lastError) => {
				Logger.debug(s"Successfully inserted ${whatDone()} with status: $lastError")
				transferCompleteResult(() => "Completed " + whatDone())
			}
		}.recover {
			case err => transferErrorResult(data, Map(None -> Errors.exceptionMessage(err)))
		}
	}

	/**
	 * Get BSON document query for transfers to and from a component
	 * @param id component id
	 * @return query to find transfers directly to/from a component
	 */
	private def transferBson(id: String) = BSONDocument("$or" -> BSONArray(
		BSONDocument("from" -> id),
		BSONDocument("to" -> id)))

	/**
	 * Remove transfers involving a component.
	 * @param id component id
	 * @return future with optional string containing error
	 */
	def removeTransfers(id: String) = {
		transferCollectionBSON.remove(transferBson(id)).map {
			(lastError) => {
				Logger.debug(s"Successfully deleted transfers for $id with status: $lastError")
				None
			}
		}.recover {
			case err => Some(err)
		}
	}

	/**
	 * Get count of transfers involving a component.
	 * @param id component id
	 * @return future with optional string containing error
	 */
	def countTransfers(id: String) = {
		val command = Count(transferCollectionName, Some(transferBson(id)))
		db.command(command)
	}

	/**
	 * Make a description of the transfer, including quadrant descriptions.
	 * @param data data transferred
	 * @return description of transfer, including quadrant information
	 */
	private def quadDesc(data: Transfer) = {
		def qDesc(id: String, quad: Option[Quad], slice: Option[Slice]) =
			slice.map((s) => s"slice $s of ").getOrElse("") + quad.map((q) => s"$q of $id").getOrElse(id)
		"transfer from " + qDesc(data.from, data.fromQuad, data.slice) + " to " + qDesc(data.to, data.toQuad, data.slice)
	}

	/**
	 * Do transfer based on form with quadrant inputs.  If form fails with errors then it is most likely because of
	 * extra verification so we try bind again, without verification, to pick up data in form to report error.
 	 * @return action to do transfer
	 */
	def transferFromForm = Action.async { request =>
		Transfer.form.bindFromRequest()(request).fold(
			formWithErrors => Transfer.formWithoutVerify.bindFromRequest()(request).fold(
				errors => Future.successful(// If still error then must go back to start
					transferStartFormErrorResult(Transfer.startForm.withGlobalError(Errors.validationError),
						None, None, None)),
				formData => Future.successful(
					transferFormErrorResult(formWithErrors.withGlobalError(Errors.validationError), formData))),
			data =>	insertTransfer(data, () => quadDesc(data.transfer))
		).recover {
			case err =>
				transferStartFormErrorResult(Transfer.startForm.withGlobalError(Errors.exceptionMessage(err)),
					None, None, None)
		}
	}
}
