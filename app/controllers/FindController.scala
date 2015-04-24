package controllers

import models.Find._
import models.db.TrackerCollection
import models.TransferHistory
import models.{ContainerDivisions, Container, Find}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Enumeratee, Iteratee}
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import reactivemongo.bson._
import scala.collection.Set

import scala.concurrent.Future

/**
 * Controller to execute Find command.
 * Created by nnovod on 4/6/15.
 */
object FindController extends Controller with MongoController {
	/**
	 * Initiate search of components - just put up form to get search criteria
	 * @return action to get id of wanted component
	 */
	def find = Action { request =>
		Ok(views.html.find(Errors.addStatusFlash(request,Find.form)))
	}

	// Get a Found object from a BSONDocument
	private def getFoundDoc(doc: BSONDocument) = BSON.readDocument[Found](doc)
	// Make two enumeratees - one if we want transfers and one if we do not
	// To get transfers make enumeratee to get transfers into/out of initial components found (note mapM takes a future)
	private val findWithTransfers = Enumeratee.mapM[BSONDocument]((doc) => {
		val found = getFoundDoc(doc)
		// Map future to change components retrieved into found objects
		TransferHistory.getAssociatedNodes(found.id).map((cIDs) =>
			cIDs.map((c) => {
				Find.Found(c.id, Some(c.component), c.description, c.project,
					c match {
						case ctr: Container => ctr.initialContent
						case _ => None
					},
					c match {
						case div: ContainerDivisions => Some(div.layout)
						case _ => None
					}
				)
			}) + found)
	})
	// This enumeratee is when transfers are not wanted - we simply change the document into a found object
	private val findWithoutTransfers = Enumeratee.map[BSONDocument]((doc) => Set(getFoundDoc(doc)))
	// Iteratee to get merge together sets of found components
	private val findResult : Iteratee[Set[Find.Found], Set[Find.Found]] =
		Iteratee.fold(Set.empty[Find.Found]) {
			case (soFar, next) => soFar ++ next
		}

	/**
	 * Get components wanted based on criteria in returned form.  If no components found we return the input form with
	 * an error, if one found then we redirect to display the component found, if more than one then we display a table
	 * that contains a summary and display link for each component found.
	 * @return action to find and display wanted component(s)
	 */
	def findFromForm = Action.async { request =>
		Find.form.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(BadRequest(views.html.find(formWithErrors.withGlobalError(Errors.validationError)))),
			data => {
				// Get find query from what was set in form
				val findQuery = BSON.writeDocument[Find](data)
				// Get enumerator to produce documents found
				val getDocs = TrackerCollection.findWithQuery(findQuery).enumerate()
				// Get enumeratee we want, depending on whether transfers are to be included
				val filter = if (data.includeTransfers) findWithTransfers else findWithoutTransfers
				// Now get results (get documents via enumerator and modify them via enumeratee and finally get
				// results via an iteratee)
				val result = getDocs &> filter |>>> findResult
				// Execute future and map results to appropriate page based on size of results
				result.map((result) => {
					result.size match {
						case 0 =>
							Ok(views.html.find(Find.form.fill(data).withGlobalError("No matching components found")))
						case 1 if result.head.component.isDefined =>
							val found = result.head
							Redirect(ComponentController.actions(found.component.get).updateRoute(found.id))
						case _ => Ok(views.html.findResults("Find Results")(result))
					}
				})
			}
		).recover {
			case err => BadRequest(
				views.html.find(Find.form.withGlobalError(Errors.exceptionMessage(err))))
		}
	}
}
