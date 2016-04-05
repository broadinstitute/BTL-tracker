package controllers

import models.TransferContents.MergeResult
import models._
import models.db.{TrackerCollection, TransferCollection}
import models.initialContents.InitialContents
import play.api.data.Form
import play.api.mvc.{Result, Action, Controller}
import play.api.libs.json._
import utils.{Yes, No, MessageHandler}
import MessageHandler.FlashingKeys
import Transfer.Slice.CP

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * The flow of a transfer is a bit convoluted in order to make the UI as simple as possible.  First there is the option
 * on the home screen to set a to and from ID along with a project.  If the to and from IDs point to components that
 * do not have quadrants and can not be sliced then we go immediately to do the transfer.  However if more information
 * is needed we then go to the transfer form to get the quadrant/slice information.  To preserve what has been learned
 * about the transfer a bunch of parameters are sent with the request to dictate exactly what fields (e.g.,
 * quadrants) are to be queried for.
 *
 * An alternative flow is when a transfer is requested from an individual component already found.  This goes to the
 * transfer start screen which, similar to the home screen, prompts for the to component and optional project.  In
 * this case the from component is already set on the transfer start page.
 *
 * @author Nathaniel Novod
 *         Date: 2/24/15
 *         Time: 6:27 PM
 */
object TransferController extends Controller {
	/**
	 * Initiate transfer - if additional info needed bring up form otherwise try to do transfer now
	 *
	 * @return action to go get transfer information
	 */
	def transfer(fromID: Option[String], toID: Option[String], project: Option[String]) = Action.async { request =>
		if (fromID.isDefined && toID.isDefined)
			doTransfer(TransferStart(from = fromID.get, to = toID.get, project = project))
		else
			Future.successful(Ok(views.html.transferStart(
				transferForm = MessageHandler.addStatusFlash(request = request, data = Transfer.startForm),
				fromID = fromID, toID = toID, project = project, readOnly = true)))
	}

	/**
	 * Do transfer based on parameters from url - this is used when the transfer components are already set but
	 * there's additional information needed to complete the transfer.
	 *
	 * @param fromID component ID being transferred from
	 * @param toID component ID being transferred to
	 * @param project optional project associated with transfer
	 * @param fromQuad true if we need to get the quadrant we're transferring from
	 * @param toQuad true if we need to get the quadrant we're transferring to
	 * @param dataMandatory true if requested quadrant information must be specified
	 * @param isQuadToQuad true if slice transfers must have from and to quadrant
	 * @param isQuadToTube true if slice transfer must have from quadrant
	 * @param isTubeToQuad true if slice transfer must have to quadrant
	 * @param isTubeToMany true if transferring tube to a multi-welled container
	 * @return action to get additional transfer information wanted
	 */
	def transferWithParams(fromID: String, toID: String, project: Option[String],
	                       fromQuad: Boolean, toQuad: Boolean,
						   dataMandatory: Boolean, isQuadToQuad: Boolean, isQuadToTube: Boolean,
						   isTubeToQuad: Boolean, isTubeToMany: Boolean) = {
		Action { request =>
			Ok(views.html.transfer(
				transferForm = MessageHandler.addStatusFlash(request = request, data = Transfer.form),
				fromID = fromID, toID = toID, project = project,
				fromQuad = fromQuad, toQuad = toQuad, dataMandatory = dataMandatory, isQuadToQuad = isQuadToQuad,
				isQuadToTube = isQuadToTube, isTubeToQuad = isTubeToQuad, isTubeToMany = isTubeToMany))
		}
	}

	/**
	 * Take data from transfer start form and check it out.  If one or both IDs are not found we return a form with the
	 * errors set, otherwise we return the objects found.
	 *
	 * @param data data retrieved from transfer form
	 * @return objects found and form with errors - one and only one of these will be set to None
	 */
	private def getTransferInfo(data: TransferStart) = {
		// Method to return a filled in form with errors set
		def formErr(errs: Map[Option[String], String]) =
			MessageHandler.fillAndSetFailureMsgs(msgs = errs, form = Transfer.startForm, data = data)
		// Method to set form with errors - errs is a list of form keys for ID(s) not found
		def formFieldErrs(errs: List[String], err: String) = {
			val fieldErrs: Map[Option[String], String] = errs.map(Some(_) -> err).toMap
			formErr(fieldErrs)
		}
		// Method to set form with missing ID(s) - errs is a list of form keys for ID(s) not found
		def missingIDs(errs: List[String]) = formFieldErrs(errs, "ID not found")

		// Get ids to form keys
		val idsToForm = Map(data.to -> Transfer.toKey, data.from -> Transfer.fromKey)
		if (idsToForm.size != 2)
			Future.successful(None, Some(formErr(Map(None -> "Can not transfer component to itself"))))
		else {
			// Check out rack contents - returns with one of ((fromComponent, toComponent), errorForm) set
			def rackContents(from: Component, to: Component, racks: List[String]) = {
				// Get contents of racks
				RackScan.getRackContents(racks).map {
					case scans =>
						// Get errors for form
						val errs: Map[Option[String], String] = scans.flatMap {
							case (rackID, No(err)) =>
								Some(Some(idsToForm(rackID)) -> err)
							case (rackID, Yes(scan)) => None
						}
						if (errs.isEmpty)
							(Some(from, to), None)
						else
							(None, Some(formErr(errs)))
				}
			}

			val ids = idsToForm.keys
			// Go retrieve both objects via futures
			val queries = ids.map(TrackerCollection.findID[JsObject]).toList
			Future.sequence(queries).flatMap((found) => {
				// Get all components found
				val components = found.flatMap {
					case Some(c) => Some(ComponentFromJson.getComponent(c))
					case None => None
				}
				// If none found report both missing
				if (components.isEmpty)
					Future.successful(None, Some(missingIDs(idsToForm.values.toList)))
				else if (components.size == 1) {
					// Only one found - get id
					val idFound = components.head.id
					// Find id not found
					val keyNotFound = idsToForm.filterNot {
						case (id, formKey) => id == idFound
					}
					// Report one missing
					Future.successful(None, Some(missingIDs(keyNotFound.values.toList)))
				} else {
					// Both found - figure out which is which and then proceed to figure out if all is ok
					val cI = components.toIterator
					val (cHead, cOne) = (cI.next(), cI.next)
					val (fromC, toC) =
						if (idsToForm(cHead.id) == Transfer.fromKey)
							(cHead, cOne)
						else
							(cOne, cHead)
					// Check if transfer between types is ok
					isTransferValid(fromC, toC) match {
						case (_, _, Some(err)) => Future.successful(None, Some(formErr(Map(None -> err))))
						case (from: Rack, to: Rack, _) =>
							rackContents(from, to, List(from.id, to.id))
						case (from: Rack, to, _) =>
							rackContents(from, to, List(from.id))
						case (from, to: Rack, _) =>
							rackContents(from, to, List(to.id))
						case (from, to, _) =>
							Future.successful(Some(from, to), None)
					}
				}
			})
		}
	}

	/**
	 * Is the transfer valid?  Check from component can be transferred to to component.
	 *
	 * @param from from component
	 * @param to to component
	 * @return if transfer valid then from and to object
	 */
	private def isTransferValid(from: Component, to: Component) = {
		/*
		 * Check that rack can be transferred into
		 */
		def checkToTransfer(c: Component) = {
			c match {
				case r: Rack if r.initialContent.contains(InitialContents.ContentType.BSPtubes) =>
					Some(f"Transfer not allowed to BSP rack")
				case _ => None
			}
		}

		// Get from component and see if transfer from it is valid
		from match {
			case fromC: Component with Transferrable if fromC.validTransfers.contains(to.component) =>
				(fromC, to, checkToTransfer(to))
			case fromC => (fromC, to, Some("Transfer not allowed from a " + fromC.component.toString +
				" to a " + to.component.toString))
		}
	}

	/**
	 * Result when transfer completes
	 * @param getMsg callback to get messages to set in response form
	 * @return redirect to home page with wanted message
	 */
	private def transferComplete(getMsg: () => String) =
		FlashingKeys.setFlashingValue(r = Redirect(routes.Application.index()), k = FlashingKeys.Status, s = getMsg())

	/**
	 * Result when transfer needs more information.  Based on the quadrant information wanted we set parameters for
	 * the transfer screen
	 * @param data transfer info known so far
	 * @param fromQuad true if need query for quadrant to transfer from
	 * @param toQuad true if need query for quadrant to transfer to
	 * @param dataMandatory true if quadrant information requested must be filled in
	 * @param isQuadToQuad true if sliced transfer must be between quadrants
	 * @param isQuadToTube true if sliced transfer is from quadrant to non-divided component
	 * @param isTubeToQuad true if slice transfer must have to quadrant
	 * @param isTubeToMany true if transferring tube to a multi-welled container
	 * @return redirect to transferWithParams to query for additional information
	 */
	private def transferIncomplete(data: TransferStart, fromQuad: Boolean, toQuad: Boolean,
								   dataMandatory: Boolean, isQuadToQuad : Boolean, isQuadToTube: Boolean,
								   isTubeToQuad: Boolean, isTubeToMany: Boolean) = {
		val result = Redirect(routes.TransferController.transferWithParams(fromID = data.from, toID = data.to,
			project = data.project, fromQuad = fromQuad, toQuad = toQuad, dataMandatory = dataMandatory,
			isQuadToQuad = isQuadToQuad, isQuadToTube = isQuadToTube,
			isTubeToQuad = isTubeToQuad, isTubeToMany = isTubeToMany))
		val quadPlural = if (fromQuad && toQuad) "s" else ""
		val quadsThere = if (fromQuad || toQuad) " and quadrant" else ""
		val infoType = if (dataMandatory) s"Specify quadrant$quadPlural and optionally slice"
		else s"If transferring a slice specify slice$quadsThere$quadPlural"
		FlashingKeys.setFlashingValue(r = result, k = FlashingKeys.Status,
			s = s"$infoType before completing transfer")
	}

	/**
	 * Result when transfer needs more information for transfer between divided containers.  Based on the quadrant
	 * information wanted we set parameters for the transfer screen
	 * @param data transfer info known so far
	 * @param fromQuad true if need query for quadrant to transfer from
	 * @param toQuad true if need query for quadrant to transfer to
	 * @return redirect to transferWithParams to query for additional information
	 */
	private def containerTransferIncomplete(data: TransferStart,
											fromQuad: Boolean, toQuad: Boolean) = {
		// dataMandatory set via xor of quad settings (if only one quad wanted then it's transfer between different
		// size containers)
		transferIncomplete(data = data, fromQuad = fromQuad, toQuad = toQuad,
			dataMandatory = (fromQuad && !toQuad) || (toQuad && !fromQuad),
			isQuadToQuad = toQuad && fromQuad, isQuadToTube = false, isTubeToQuad = false, isTubeToMany = false)
	}

	/**
	 * Result when transfer needs more information for transfer from undivided container to divided containers.
	 * Based on the quadrant information wanted we set parameters for the transfer screen.
	 * @param data transfer info known so far
	 * @param toQuad true if need query for quadrant to transfer to
	 * @return redirect to transferWithParams to query for additional information
	 */
	private def fromTubeTransferIncomplete(data: TransferStart, toQuad: Boolean) = {
		transferIncomplete(data = data, fromQuad = false, toQuad = toQuad, dataMandatory = false,
			isQuadToQuad = false, isQuadToTube = false, isTubeToQuad = toQuad, isTubeToMany = true)
	}

	/**
	 * Result when transfer needs more information for transfer from divided container into non-divided container.
	 * Based on the quadrant information wanted we set parameters for the transfer screen
	 * @param data transfer info known so far
	 * @param fromQuad true if need query for quadrant to transfer from
	 * @return redirect to transferWithParams to query for additional information
	 */
	private def toTubeTransferIncomplete(data: TransferStart, fromQuad: Boolean) =
		transferIncomplete(data = data, fromQuad  = fromQuad, toQuad = false, dataMandatory = false,
			isQuadToQuad = false, isQuadToTube = fromQuad, isTubeToQuad = false, isTubeToMany = false)

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
		BadRequest(views.html.transferStart(transferForm = form, fromID = fromID, toID = toID,
			project = project, readOnly = false))

	/**
	 * Result when transfer start had errors
	 * @param data data found in transfer form
	 * @param errs errors to set in form
	 * @return BadRequest to transferStart with form set with errors
	 */
	private def transferStartErrorResult(data: TransferStart, errs: Map[Option[String], String]) =
		transferStartFormErrorResult(
			form = MessageHandler.fillAndSetFailureMsgs(msgs = errs, form = Transfer.startForm, data = data),
			fromID = Some(data.from), toID = Some(data.to), project = data.project)

	/**
	 * Complete a future immediately with a result
	 * @param res result to set for future completion
	 * @return future completed successfully with result
	 */
	private def now(res: Result) = Future.successful(res)

	/**
	 * Start of transfer - we look over IDs and see what's possible.  Depending on type of components being transferred
	 * the transfer is done now (e.g., if between tubes) or different additional information is requested (e.g., if
	 * between components with quadrants and/or slices).
	 * @return action to see what step is next to complete transfer
	 */
	def transferIDs = Action.async { request =>
		Transfer.startForm.bindFromRequest()(request).fold(
			formWithErrors => Future.successful(transferStartFormErrorResult(
				form = MessageHandler.formGlobalError(form = formWithErrors, err = MessageHandler.validationError),
				fromID = None, toID = None, project = None)),
			data => doTransfer(data)
		).recover {
			case err => transferStartFormErrorResult(
				form = Transfer.startForm.withGlobalError(MessageHandler.exceptionMessage(err)),
				fromID = None, toID = None, project = None)
		}
	}

	/**
	 * Complete the transfer or request additional information needed.
	 * @param data transfer information gathered so far
	 * @return future completed if more information needed, otherwise future waits for transfer insertion completion
	 */
	def doTransfer(data: TransferStart) = {
		// Got data from form - get from and to data (as json) - flatMap is mapping future from retrieving DB
		// data - flatMap continuation is either a future that completes immediately because transfer can not
		// be done now or a future that will complete when the DB insertion is done
		getTransferInfo(data).flatMap {
			// Report attempt to transfer to itself
			case (Some((fromData, toData)), None) if fromData.id == toData.id =>
				now(transferStartErrorResult(data = data,
					errs = Map(None -> "Can not transfer component to itself")))
			// OK so far - now we need to check if it's ok to add this transfer to the graph leading in
			case (Some((fromData, toData)), None) => checkGraph(data, fromData, toData).flatMap {
				// Problem found
				case Some(err) => now(transferStartErrorResult(data = data, errs = Map(None -> err)))
				// All is well - now just check if transfer should be done now or we need additional
				// information about what quadrants/slices to transfer from/to
				case None =>
					import ContainerDivisions.Division._
					(fromData, toData) match {
						// Transferring between divided containers
						case (f: ContainerDivisions, t: ContainerDivisions) =>
							(f.layout,t.layout) match {
								// Check if slicing wanted
								case (DIM8x12, DIM8x12) =>
									now(containerTransferIncomplete(data = data, fromQuad = false, toQuad = false))
								// if doing slices between 384-well plates then need to pick quadrant
								case (DIM16x24, DIM16x24) =>
									now(containerTransferIncomplete(data = data, fromQuad = true, toQuad = true))
								// Go ask for quadrant/slice to set in larger destination plate
								case (DIM8x12, DIM16x24) =>
									now(containerTransferIncomplete(data = data, fromQuad = false, toQuad = true))
								// Go ask for quadrant/slice to get in larger source plate
								case (DIM16x24, DIM8x12) =>
									now(containerTransferIncomplete(data = data, fromQuad = true, toQuad = false))
							}
						// Transferring from a divided container to an undivided component
						case (f: ContainerDivisions, _) =>
							f.layout match {
								// See if slicing wanted
								case DIM8x12 =>
									now(toTubeTransferIncomplete(data = data, fromQuad = false))
								// See if slicing or quadrant wanted
								case DIM16x24 =>
									now(toTubeTransferIncomplete(data = data, fromQuad = true))
							}
						// Transferring from an undivided container to a divided container
						case (_, t: ContainerDivisions) =>
							t.layout match {
								case DIM8x12 =>
									now(fromTubeTransferIncomplete(data = data, toQuad = false))
								case DIM16x24 =>
									now(fromTubeTransferIncomplete(data = data, toQuad = true))
							}

						// Source and destination aren't divided - go complete transfer of its contents
						case _ =>
							val tForm = data.toTransferForm
							insertTransferWithoutCherries(tForm)
					}
			}
			// Couldn't find one or both data - form returned contains errors - return it now
			case (None, Some(form)) => now(transferStartFormErrorResult(form = form,
				fromID = Some(data.from), toID = Some(data.to), project = data.project))
			// Should never have both or neither as None but...
			case _ => now(FlashingKeys.setFlashingValue(r = Redirect(routes.Application.index()),
				k = FlashingKeys.Status, s = "Internal error: Failure during transferIDs"))
		}
	}.recover {
		case err => transferStartErrorResult(data = data, errs = Map(None -> MessageHandler.exceptionMessage(err)))
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
		// @TODO if from or to is a rack then need to break into tube transfers
		TransferHistory.makeSourceGraph(data.from).map((graph) => {
			// Check if addition of target of transfer will make graph cyclic - if yes then complete with error
			val isCyclic = TransferHistory.isGraphAdditionCyclic(addition = data.to, graph = graph) match {
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
								Some(s"Project $projectWanted not in ${from.id} or its derivatives. $projectsFoundStr")
							}
						// No project on transfer so nothing to check there
						case _ => None
					}
			}
		}).recover {
			case err => Some(MessageHandler.exceptionMessage(err))
		}
	}

	/**
	 * Result when transfer form had errors.
	 * @param form form filled with error data
	 * @param data data found in transfer form
	 * @return BadRequest to transfer with input form
	 */
	private def transferFormErrorResult(form: Form[TransferForm], data: TransferForm) = {
		BadRequest(views.html.transfer(transferForm = form,
			fromID = data.transfer.from, toID = data.transfer.to, project = data.transfer.project,
			fromQuad = data.isQuadToQuad || data.isQuadToTube || data.transfer.fromQuad.isDefined,
			toQuad = data.isQuadToQuad || data.isTubeToQuad || data.transfer.toQuad.isDefined,
			dataMandatory = data.dataMandatory, isQuadToQuad = data.isQuadToQuad, isQuadToTube = data.isQuadToTube,
			isTubeToQuad = data.isTubeToQuad, isTubeToMany = data.transfer.isTubeToMany))
	}

	/**
	 * Result when transfer had errors
	 * @param data data found in transfer form
	 * @param errs errors to set in form
	 * @return BadRequest to transfer with form set with errors
	 */
	private def transferErrorResult(data: TransferForm, errs: Map[Option[String], String]) =
		transferFormErrorResult(form = MessageHandler.fillAndSetFailureMsgs(
			msgs = errs, form = Transfer.form, data = data
		), data = data)

	/**
	 * Insert transfer without cherry picking into DB and return Result.
	 * @param data transfer to record in DB
	 * @return result to return with completion status
	 */
	private def insertTransferWithoutCherries(data: TransferForm) = {
		insertTransfer(data = data.transfer, onError = (_, err) =>
			transferErrorResult(data = data, errs = Map(None -> err)))
	}

	/**
	 * Insert transfer into DB and return Result.
	 * @param data transfer to record in DB
	 * @return result to return with completion status
	 */
	private def insertTransfer(data: Transfer, onError: (Transfer, String) => Result) = {
		data.insert().map {
			case (trans, errs) =>
				val errFound = errs match {
					case Some(err) => s" with error: $err"
					case None => ""
				}
				transferComplete(() =>
					s"Completed $trans transfer${if (trans == 1) "" else "s"} for ${data.quadDesc}$errFound")
		}.recover {
			case err => onError(v1 = data, v2 = MessageHandler.exceptionMessage(err))
		}
	}

	/**
	 * Do transfer based on form with quadrant inputs.  If form fails with errors then it is most likely because of
	 * extra verification done in form so we try bind again, with a form without extra verification, to successfully
	 * bind to pick up data in form to do better error reporting.  If user wants to do cherry picking we need to go
	 * to one more form to allow cherry picking.
	 * @return action to do transfer
	 */
	def transferFromForm = Action.async { request =>
		Transfer.form.bindFromRequest()(request).fold(
			formWithErrors => Transfer.formWithoutVerify.bindFromRequest()(request).fold(
				// If still error then must go back to start
				errors => Future.successful(transferStartFormErrorResult(form = MessageHandler.formGlobalError(
					form = Transfer.startForm, err = MessageHandler.validationError
				), fromID = None, toID = None, project = None)),
				formData => Future.successful(transferFormErrorResult(form = MessageHandler.formGlobalError(
					formWithErrors, MessageHandler.validationError
				), data = formData))),
			data => {
				val transfer = data.transfer
				transfer.slice match {
					// If doing cherry picking we need to find out more about the source place: what samples are in
					// wells and source dimensions
					case Some(slice) if slice == CP =>
						getCherryPickingView(transfer = data.transfer, form = Transfer.formForCherryPicking)
					// Go do insert into DB of transfer
					case _ => insertTransferWithoutCherries(data)
				}
			}
		).recover {
			// If an exception go back to transfer start and report on exception
			case err =>
				transferStartFormErrorResult(
					form = Transfer.startForm.withGlobalError(MessageHandler.exceptionMessage(err)),
					fromID = None, toID = None, project = None)
		}
	}

	/**
	 * Get information to be used for cherry picking wells.  For each well we get the sample contained in the well.
	 * The dimensions of the plate are also retrieved
	 * @param fromID ID of component being transferred from
	 * @param fromQuad optional quadrant being transferred from
	 * @return future containing map of wells to samples; # of rows; # of columns; list of errors
	 */
	private def getWells(fromID: String, fromQuad: Option[Transfer.Quad.Quad]) :
	Future[(Option[Map[String, Option[String]]], Int, Int, List[String])] = {
		TransferContents.getContents(fromID).map((contents) => {
			// Get any errors setup to be displayed
			val msgs = contents.map((content) => content.errs)
			val displayErrs = msgs.getOrElse(List.empty[String])
			// Go through optional contents and get well by well results
			contents match {
				case Some(content) =>
					// Get layout of component
					content.component match {
						case c: ContainerDivisions =>
							val div = ContainerDivisions.divisionDimensions(c.layout)
							// Get # of rows, columns and optional mapping of original wells to quadrant wells
							val (rows, columns, quadWells) =
								fromQuad match {
									case Some(q) =>
										// Taking quad of 384-well component
										(div.rows/2, div.columns/2, Some(TransferWells.qFrom384(q)))
									case _ => (div.rows, div.columns, None)
								}

							// Method to get the library name - a merge of the contents' library names
							def getLibraryName (results: Set[MergeResult]) =
								results.foldLeft(None: Option[String])((sofar, next) => {
									// Get optional library from this result
									val lib = next.bsp.flatMap(_.library)
									// Add it to what found so far
									sofar match {
										case Some(res) => if (lib.isDefined) Some(s"$res $lib") else Some(res)
										case None => lib
									}
								})

							// Make map of well -> optionalLibraryContent
							val wells = content.wells.flatMap {
								case (well, results) =>
									quadWells match {
										// Only looking at a quadrant
										case Some(qw) => qw.get(well) match {
											// If well found then set it to quadrant well
											case Some(quadWell) => Map(quadWell -> getLibraryName(results))
											// If well not in quadrant then filter it out
											case _ => Map.empty[String, Option[String]]
										}
										// Not limited to quadrant
										case _ => Map(well -> getLibraryName(results))
									}
							}
							(Some(wells), rows, columns, displayErrs)
						// Error - we're trying to cherry pick an undivided component
						case _ => (None, 0, 0, displayErrs :+ "Not welled component")
					}
				// Error - no contents found in component
				case None => (None, 0, 0, displayErrs :+ "No contents Found")
			}
		}).recoverWith {
			case e => Future.successful((None, 0, 0, List(MessageHandler.exceptionMessage(e))))
		}
	}

	/**
	 * Get result for doing cherry picking.  Get well information and return view with input form and well information
	 * found.
	 * @param transfer transfer information for cherry picking
	 * @param form form to be used for cherry picking
	 * @return response with page to display to do cherry picking
	 */
	private def getCherryPickingView(transfer: Transfer, form: Form[Transfer]) = {
		val (cherryContainer, cherryQuad) =
			if (transfer.isTubeToMany) (transfer.to, transfer.toQuad) else (transfer.from, transfer.fromQuad)
		getWells(cherryContainer, cherryQuad).map {
			// Report errors found and then bring up page with cherry picking
			case (Some(wells), rows, columns, errs) =>
				val errorForm = errs.foldLeft(form)(
					(soFar, next) => soFar.withGlobalError(next)).
					withGlobalError("Pick wells to be transferred")
				Ok(views.html.transferCherries(transferForm = errorForm, fromID = transfer.from,
					toID = transfer.to, wells = wells, rows = rows, columns = columns, project = transfer.project,
					fromQuad = transfer.fromQuad, toQuad = transfer.toQuad, isTubeToMany = transfer.isTubeToMany))
			// If we couldn't get well information go back to transfer start with errors
			case (_, _, _, errs) =>
				val allErrs = form.globalErrors.map((err) => err.message).toList ++ errs
				transferStartFormErrorResult(form = MessageHandler.setGlobalErrors(
					msgs = allErrs, form = Transfer.startForm
				), fromID = None, toID = None, project = None)
		}
	}

	/**
	 * Called upon submission of transfer form that includes cherry picking.  If all is well we now go add the transfer
	 * information to the database.
	 * @return Response
	 */
	def transferCherriesFromForm = Action.async {request =>
		Transfer.formForCherryPicking.bindFromRequest()(request).fold(
			// If an error in form go retry, reporting errors
			formWithErrors => {
				getCherryPickingView(transfer = formWithErrors.get, form = formWithErrors)
			},
			data => {
				// If no wells selected then go back to get some; otherwise go do the transfer
				if (data.cherries.isEmpty || data.cherries.get.isEmpty) {
					val form = MessageHandler.setGlobalErrors(msgs = List("No wells selected"),
						form = Transfer.formForCherryPicking)
					getCherryPickingView(data, form)
				} else {
					// Go insert transfer
					insertTransfer(data = data, onError =
						(tran, err) => transferComplete(() => "Error completing " + data.quadDesc + ": " + err))
				}
			}
		)
	}

	/**
	 * Display the well transfers between two components.
	 *
	 * @param from source component
	 * @param to target component
	 * @return puts up nice display of source plate with contents of wells set to destination wells
	 */
	def transferDisplay(from: String, to: String) = Action.async {
		// Get transfers between components (flatmap to avoid future of future)
		TransferCollection.find(from = from, to = to).flatMap((trans) => {
			// Get components
			TrackerCollection.findIds(List(from, to)).map((ids) => {
				// Map bson to components (someday direct mapping should be possible but too painful for now)
				val components = ComponentFromJson.bsonToComponents(ids)
				// Find from and to components
				val fromComponent = components.find(_.id == from)
				val toComponent = components.find(_.id == to)
				// Make bson into objects and sort results by time (later transfers should override previous ones)
				val transSorted = trans.map(TransferHistory.getTransferObject).sortWith(_.time < _.time)
				// Get which component has wells being transferred from/to
				val isFromTube = transSorted.forall(_.isTubeToMany)
				// Get map of wells (map of input to output wells)
				val wells = transSorted.foldLeft(Map.empty[String, List[String]]) {
					case (outSoFar, next) if fromComponent.isDefined && toComponent.isDefined =>
						TransferContents.getNextWellMapping(soFar = outSoFar, fromComponent = fromComponent.get,
							toComponent = toComponent.get, fromQuad = next.fromQuad, toQuad = next.toQuad,
							quadSlice = next.slice, cherries = next.cherries,
							isTubeToMany = isFromTube, getSameMapping = true) {
							case (wellsSoFar, div, newWells) =>
								val keys = wellsSoFar.keySet ++ newWells.keySet
								keys.map((k) => {
									k -> ((wellsSoFar.get(k), newWells.get(k)) match {
										case (Some(v), None) => v
										case (None, Some(v)) => v
										case (Some(v1), Some(v2)) => v1 ++ v2
										case (None, None) => List.empty[String]
									})
								}).toMap
						}
					case (out, _) => out
				}
				// Make destination wells into options (view takes the map that way)
				// If from tube then display destination multi-well container with marks where tube contents is
				// transferred into.  Otherwise source multi-well container is displayed with destination wells set in
				// input well locations.
				val optWells =
					wells.flatMap {
						case (key, value) =>
							// If from tube should always be from single well so just mark where tube was transferred
							if (isFromTube) {
								// Get # of times tube transferred to each well
								val vals = value.groupBy((s) => s)
								// Make that into appropriate number of marks
								vals.map {
									case (destWell, count) => destWell -> Some(List.fill(count.size)("XX").mkString(","))
								}
							} // If between divided components then show well transfers
							else if (toComponent.get.isInstanceOf[ContainerDivisions])
								Map(key -> Some(value.mkString(",")))
							// If to non-divided component from a divided one then show which wells were transferred
							else
								Map(key -> Some(value.map(_ => "XX").mkString(",")))
					}
				// Get number of rows and columns
				val wellComponent = if (isFromTube) toComponent else fromComponent
				val (rows, cols) =
					wellComponent.map {
						case c: ContainerDivisions =>
							val div = ContainerDivisions.divisionDimensions(c.layout)
							(div.rows, div.columns)
						case _ => (1, 1) // If component isn't divided then a single "well"
					}.getOrElse((0, 0)) // If component doesn't exist then nothing there
				// Go display the results
				Ok(views.html.transferDisplay(from = fromComponent, to = toComponent, tableID = "TransferDisplay",
					grid = optWells, rows = rows, cols = cols, sourceDisplay = !isFromTube))
			})
		})
	}

}
