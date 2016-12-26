package controllers

import controllers.RackController._
import models.Component.ComponentType
import play.api.Routes
import play.api.libs.json._
import play.api.mvc._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.MessageHandler
import utils.MessageHandler.FlashingKeys
import java.io.File

import models.TransferContents.MergeResult

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
		Ok(views.html.index(MessageHandler.addStatusFlash(request, Component.blankForm)))
	}

	/**
	 * Test action
	 * @param id id to hand to test
	 * @return string to say we got here
	 */
	def test(id: String) = Action { request =>
		Ok(s"Test $id")
	}

	/**
	 * Create the walkup sequencing file for a component.  The file includes sample name and molecular barcodes.
	 * @param id Component ID
	 * @return if all goes well upload the created walkup sequencing file
	 */
	def walkup(id: String) = Action.async { request =>
		handleContents(id = id,
			redirectOnErr = (res) => ComponentController.actions(res.component.component).updateRoute(id),
			setWellContents = (_, c) => Walkup.makeWUS(c),
			getResult = (res, wellMap: Map[String, (Option[String], List[String])]) => {
				// Should only be one well (should be a tube) - get results if all looks well, otherwise set error
				val wellResult =
					if (wellMap.isEmpty)
						(None, List(s"No samples found for $id"))
					else if (wellMap.size > 1)
						(None, List(s"Invalid multi-well component - $id tube must be used"))
					else
						wellMap.head._2
				wellResult match {
					// If we got back a file with contents then use it
					case (Some(file), errs) =>
						// Get project (if it's there) and component id as upload file name
						val fileName = res.component.project match {
							case Some(proj) => s"${proj}_$id"
							case None => id
						}
						// Go upload the file
						val outFile = new File(file)
						Ok.sendFile(content = outFile, inline = false,
							fileName = (_) => s"Walkup_$fileName.csv", onClose = () => outFile.delete())
					// If nothing returns then report error(s)
					case (None, errs) =>
						val result = Redirect(ComponentController.actions(res.component.component).updateRoute(id))
						// Set error to be picked up
						FlashingKeys.setFlashingValue(r = result, k = FlashingKeys.Status, s = errs.mkString(";"))
				}
			})
	}

	/**
	 * Single content found in a well
	 * @param molID molecular barcode
	 * @param sampleID sample ID
	 * @param ab antibody
	 */
	private case class Content(molID: Option[String], sampleID: Option[String], ab: Option[String])

	/**
	 * Contents found in a well
	 * @param subTube sub-container (tube in a rack) ID
	 * @param contents contents found in well
	 */
	private case class Contents(subTube: Option[String], contents: Set[Content])

	/**
	 * Output contents of a component.
	 * @param id component id
	 * @param getOutput callback to set output
	 * @return response with error or what to display
	 */
	private def outputContents(id: String, getOutput: (Component, List[(String, Contents)]) => Result): Future[Result] = {

		/*
		 * Get a contents for one sample in a well.
		 * @param well well with contents
		 * @param contents results of scan of graph to find well's contents
		 * @return contents of well (without subtube set)
		 */
		def getContents(well: String, contents: Set[MergeResult]) = {
			// Map well-by-well results to html to display subcomponent/library name/mid/antibody
			val allContents = contents.map((content) => {
				// Get sample, molecular barcodes and antibodies
				val sample = content.sample match {
					case Some(sample) => Some(sample.sampleID.getOrElse("unknown"))
					case None => None
				}
				val mids =
					if (content.mid.isEmpty)
						None
					else
						Some(content.mid.map((m) => m.name).mkString(";"))
				val abs =
					if (content.antibody.isEmpty)
						None
					else
						Some(content.antibody.mkString(";"))
				Content(molID = mids, sampleID = sample, ab = abs)
			})
			// Tube is set later
			Contents(subTube = None, contents = allContents)
		}

		/*
		 * Add subtubes into contents.
		 * @param subs well to subtube map
		 * @param wells well to contents map
		 * @return new map of contents that includes subtubes
		 */
		def putInSubs(subs: Map[String, String], wells: Map[String, Contents]) = {
			// Get subcomponent wells not set yet (what's in subs but not wells)
			val subWellsNotSet =
				subs.keys.toSet
					.diff(wells.keys.toSet)
			// Make new contents for wells that don't have any other contents
			val subWellComponents = subWellsNotSet.map((well) =>
				well -> Contents(subTube = subs.get(well), contents = Set.empty))
			// Add subtube to contents that already existed
			val wellsWithSubs =
				wells.map {
					case (well, contents) =>
						well ->
							(subs.get(well) match {
								case Some(subTube) =>
									contents.copy(subTube = Some(subTube))
								case None => contents
							})
				}
			// Put two sets together
			wellsWithSubs ++ subWellComponents
		}

		// Start off by getting map of subcomponents (i.e., tubes in a rack) - if any there we'll setup links later
		ComponentController.getSubsIDs(id)
			.flatMap((subWells) => {
				// Now do the real work - get contents
				handleContents[Contents](id = id,
					// If an error go back to the update page
					redirectOnErr = (res) => ComponentController.actions(res.component.component).updateRoute(id),
					// Take a well's results and map it into an HTML string showing contents for well
					setWellContents = (well, contents) => getContents(well, contents),
					// Put together final results
					getResult = (res, wellMap) => {
						// Put together final results and sort it by well location for nice output
						val allContents =
							putInSubs(subWells, wellMap)
								.toList.sortBy {
								case (well, _) => well
							}
						getOutput(res.component, allContents)
					})
			})
			.recover {
				case err => BadRequest(
					views.html.index(Component.blankForm.withGlobalError(MessageHandler.exceptionMessage(err))))
			}
	}

	/**
	 * Display the contents for a component.  In a grid, with each entry representing a well, the contents of the
	 * component are displayed.
	 * @param id component id
	 */
	def contents(id: String) = Action.async { request =>

		/*
		 * Create html output to display
		 * @param component component with contents
		 * @param allContents all the contents, by well, found for component
		 */
		def getOutput(component: Component, allContents: List[(String, Contents)]) = {
			// Make href for subcomponent (tube)
			def getSubTubeURL(id: String) =
				"""<a href="""+routes.Application.findByID(id).url+""">"""+id+"""</a>"""
			// Little function to help putting together strings
			def addBrk(s1: String, s2: String) = {
				if (s1.nonEmpty && s2.nonEmpty)
					s1 + "<br>" + s2
				else
					s1 + s2
			}


			// Setup by well display
			val contentsToDisplay = allContents.map {
				case (well, contents) =>
					// If a subtube start with that
					val subLink =
						contents.subTube match {
							case Some(subID) => getSubTubeURL(subID)
							case None => ""
						}

					val oneContent = contents.contents.map((content) => {
						// Get sample, molecular barcodes and antibodies to display
						val sample = content.sampleID.getOrElse("")
						val withMids = addBrk(sample, content.molID.getOrElse(""))
						addBrk(withMids, content.ab.getOrElse(""))
					}).mkString("<br><br>")
					well -> Some(addBrk(subLink, oneContent))
			}.toMap

			// Get component division (for display)
			val (rows, cols) =
				component match {
					case c: ContainerDivisions =>
						val div = ContainerDivisions.divisionDimensions(c.layout)
						(div.rows, div.columns)
					case _ => (1, 1) // If component isn't divided then a single "well"
				}

			// Go display contents
			Ok(views.html.contentDisplay(component = component.id, tableID = "Contents",
				grid = contentsToDisplay, none = "", rows = rows, cols = cols))
		}

		// Setup output
		outputContents(id, getOutput)
	}

	/**
	 * Dump the contents of a component into a csv file - one line per well per sample.
	 * @param id component id
	 */
	def contentsDump(id: String) = Action.async { request =>

		/*
		 * Headers for csv file to dump contents into
		 */
		val wellHdr = "Well"
		val sampleIDHdr = "Sample ID"
		val midHdr = "Sequencing Barcode"
		val subHdr = "SubComponent"
		val abHdr = "Antibodies"
		val headers = Array(wellHdr, sampleIDHdr, midHdr, subHdr, abHdr)

		/*
		 * Create csv file from found contents.
		 * @param component component with contents
		 * @param allContents all the contents, by well, found for component
		 */
		def getOutput(component: Component, allContents: List[(String, Contents)]) = {
			/*
			 * Set output line for contents
			 * @param well content's well
			 * @param contents summary of well's contents
			 * @param content individual sample's contents
			 * @return
			 */
			def contentToLine(well: String, contents: Contents, content: Content) =
				headers.map((header) =>
					if (header == wellHdr)
						well
					else if (header == sampleIDHdr)
						content.sampleID.getOrElse("")
					else if (header == subHdr)
						contents.subTube.getOrElse("")
					else if (header == midHdr)
						content.molID.getOrElse("")
					else if (header == abHdr)
						content.ab.getOrElse("")
					else ""
				)

			// Create csv file
			val file =
				spreadsheets.Utils.setCSVValues[(String, Contents)](
					headers = headers,
					input = allContents,
					getValues = (next, headers) => {
						val (well, contents) = next
						// Get list of different entries for well eliminating any without content
						val finalContents = contents.contents
							.map((content) => {
								if (content.ab.isEmpty && content.molID.isEmpty && content.sampleID.isEmpty)
									None
								else
									Some(contentToLine(well, contents, content))
							})
							.toList.flatten
						if (finalContents.isEmpty && contents.subTube.isDefined)
							List(contentToLine(well, contents,
								Content(molID = None, sampleID = None, ab = None)))
						else
							finalContents
					},
					noneMsg = s"No sample data found for $id"
				)
			// Finish
			file match {
				case (Some(fileName), _) =>
					val outName = s"Contents_$id"
					// Go upload the file
					val outFile = new File(fileName)
					Ok.sendFile(content = outFile, inline = false,
						fileName = (_) => s"$outName.csv", onClose = () => outFile.delete())
				case (None, errs) =>
					val result =
						Redirect(ComponentController.actions(component.component).updateRoute(id))
					// Set error to be picked up
					FlashingKeys.setFlashingValue(r = result, k = FlashingKeys.Status, s = errs.mkString(";"))
			}
		}

		// Go output the file
		outputContents(id, getOutput)
	}

	/**
	 * Do action based on a components contents
	 * @param id component id
	 * @param redirectOnErr call to redirect to if an error
	 * @param setWellContents set contents of a well
	 * @param getResult get the result if all went well
	 * @tparam T type of a well contents
	 * @return UI result
	 */
	private def handleContents[T](id: String,
		redirectOnErr: (TransferContents.MergeTotalContents) => Call,
		setWellContents: (String, Set[TransferContents.MergeResult]) => T,
		getResult: (TransferContents.MergeTotalContents, Map[String, T]) => Result) =
		TransferContents.getContents(id).map {
			// If nothing found then redirect to wanted page and display error
			case Some(res) if res.wells.isEmpty && res.errs.nonEmpty =>
				// Redirect to component update (note not simple find by id to avoid multiple redirects)
				val result = Redirect(redirectOnErr(res))
				// Set error to be picked up
				FlashingKeys.setFlashingValue(r = result, k = FlashingKeys.Status, s = res.errs.mkString(";"))

			case Some(res) =>
				// Get well by well contents wanted
				val wellMap =
					res.wells.map {
						case (well, contents) =>
							val wellKey = if (well == TransferContents.oneWell) "A01" else well
							wellKey -> setWellContents(wellKey, contents)
					}
				// Get final results
				getResult(res, wellMap)

			case None =>
				// No results - assume component not found - should never happen
				BadRequest(views.html.index(Component.blankForm.withGlobalError(s"ID $id not found")))
		}

	/**
	 * Display the graph of transfers, direct and indirect, from and to specified component.  First the graph is made
	 * into "dot" format and then the view is called to convert it to pretty html.
	 * @param id component id
	 * @return puts up pretty picture of graph of transfers to and from a component
	 */
	def graphDisplay(id: String) = Action.async {
		TransferHistory.makeBidirectionalDot(componentID = id, accessPath = routes.Application.findByID(_: String),
			transferDisplay = routes.TransferController.transferDisplay(_: String, _: String))
			.map((dot) => {
				val htmlStr = "\'" + dot.replaceAll("""((\r\n)|\n|\r])""", """\\$1""") + "\'"
				Ok(views.html.graphDisplay(htmlStr, id))
			})
	}

	/**
	 * Initial request to add.  We put up the form to get initial data for the add.
	 * @param id component id
	 * @return action to execute to initiate add of component
	 */
	def add(id: String) = Action { request =>
		Ok(views.html.add(MessageHandler.addStatusFlash(request, Component.idAndTypeForm), id))
	}

	/**
	 * Initial request to get ids for a stack of components.  We put up the form to get the ids and type.
	 * @return handler to bring up view to get stack IDs
	 */
	def addStack() = Action { request =>
		Ok(views.html.addStackStart(MessageHandler.addStatusFlash(request, Component.idAndTypeForm)))
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
			})
	}

	/**
	 * Using type supplied from form go to next step to fill in type specific data for components.
	 * @param id component ID
	 * @return action to execute to put up form to get component data
	 */
	def addFromForm(id: String) = Action { request =>
		Component.idAndTypeForm.bindFromRequest()(request).fold(
			formWithErrors =>
				BadRequest(views.html.add(addForm = MessageHandler.formGlobalError(form = formWithErrors,
					err = MessageHandler.validationError), id = id)),
			data =>
				Redirect(ComponentController.actions(data.t).addRoute(data.id)))
	}

	/**
	 * Go bring up view to confirm if delete is to proceed.  If subcomponents then ask if subcomponents should be
	 * deleted with main component, otherwise simply ask for confirmation.
	 * @param id component id to delete
	 * @return action to take to check what's next
	 */
	def deleteCheck(id: String) = Action.async { request =>
		ComponentController.doComponentCounts(id,
			success = (components, transfers) => {
				// If only 1 component then no subcomponents
				if (components <= 1)
					Ok(views.html.deleteConfirm(id = id, count = transfers))
				else
					Ok(views.html.deleteWithSubConfirm(id = id, subComps = components - 1, trans = transfers))
			},
			error = (err) =>
				BadRequest(views.html.index(Component.blankForm.withGlobalError(MessageHandler.exceptionMessage(err))))
		)
	}

	/**
	 * Confirm to go ahead with single component (no subcomponents).  We get the # of transfers to confirm that they
	 * will be deleted as well.
	 * @param id id of component
	 * @return action to take to confirm deletion
	 */
	def deleteSingleConfirm(id: String) = Action.async { request =>
		ComponentController.transferCount(id,
			counted = (count) => Ok(views.html.deleteConfirm(id = id, count = count)),
			error = (err) =>
				BadRequest(views.html.index(Component.blankForm.withGlobalError(MessageHandler.exceptionMessage(err))))
		)
	}

	/**
	 * Go bring up view to confirm if delete of component and subcomponents is to proceed
	 * @param id component id to delete
	 * @return action to take to delete component
	 */
	def deleteSubConfirm(id: String) = Action.async { request =>
		ComponentController.doSubsTransferCount(id,
			success = (count) => Ok(views.html.deleteSubConfirm(id = id, count = count)),
			error = (err) =>
				BadRequest(views.html.index(Component.blankForm.withGlobalError(MessageHandler.exceptionMessage(err))))
		)
	}

	/**
	 * Go do delete.
	 * @param id ID of component being deleted
	 * @param doDel callback to do delete
	 * @return redirect to home page with completion status
	 */
	private def doDelete(id: String, doDel: (String, (Int) => Result, (Throwable) => Result) => Future[Result]) = {
		// Make result - redirect to home page and set flashing value (to be set as global error) to be picked up
		// on home page
		def getResult(msg: String) = MessageHandler.homeRedirect(msg)
		// Do delete
		doDel(id,
			// On success
			(_) => getResult(id + " successfully deleted"),
			// On failure
			(t: Throwable) => getResult("Error deleting " + id + ": " + MessageHandler.exceptionMessage(t))
		)
	}

	/**
	 * Delete component based on ID supplied in get request
	 * @param id id of component to be deleted
	 * @return action to deleted specified component
	 */
	def deleteByID(id: String) = Action.async { request =>
		doDelete(id, ComponentController.delete[Result])
	}

	/**
	 * Delete component and subcomponents based on ID supplied in get request
	 * @param id id of parent component to be deleted
	 * @return action to take to delete specified component
	 */
	def deleteSubByID(id: String) = Action.async { request =>
		doDelete(id, ComponentController.doSubsDelete[Result])
	}

	/**
	 * Get component for specified id (specified as parameter for GET).  First we get the component type and then
	 * we redirect to the update screen for that component type.
	 * @param id component ID
	 * @return action to find and display wanted component
	 */
	def findByID(id: String) = Action.async { request =>
		findRequestUsingID(id, request)(doUpdateRedirect(id, _, _, _)).recover {
			case err => BadRequest(
				views.html.index(Component.blankForm.withGlobalError(MessageHandler.exceptionMessage(err))))
		}
	}

	/**
	 * Get html to display component for specified id.
	 * @param id component ID
	 * @return form filled in with component data requested
	 */
	def findRequestUsingID(id: String, request: Request[_], okComponents: List[ComponentType.ComponentType] = List.empty)
						  (getResult: (ComponentType.ComponentType, JsObject, Request[_]) => Result): Future[Result] = {
		ComponentController.findByID[JsObject, Result](id = id, componentType = okComponents,
			found = (json) => {
				val cType = (json \ Component.typeKey).as[String]
				getResult(v1 = ComponentType.withName(cType), v2 = json, v3 = request)
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
				routes.javascript.Application.tags)).as("text/javascript")
	}
}
