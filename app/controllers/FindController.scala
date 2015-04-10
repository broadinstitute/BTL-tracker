package controllers

import models.Component.OptionalComponentType
import models.Find._
import models.{Component, Find}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Enumerator, Enumeratee, Iteratee}
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import Component.ComponentTypeImplicits._

import scala.concurrent.Future

/**
 * Controller to execute Find command.
 * Created by nnovod on 4/6/15.
 */
object FindController extends Controller with MongoController {
	/**
	 * Get collection to do mongo operations with BSON
	 */
	def trackerBSONCollection: BSONCollection = db.collection[BSONCollection](ComponentController.trackerCollectionName)

	/**
	 * Initiate find of component - just put up form to get ID of component
	 * @return action to get id of wanted component
	 */
	def find = Action { request =>
		Ok(views.html.find(Errors.addStatusFlash(request,Find.form)))
	}

	/**
	 * Get components wanted based on criteria in returned form.  If no components found we return the input form with
	 * an error, if one found then we redirect to display the component found, if more than one then we display a table
	 * that for contains a summary and display link for each component found.
	 * @return action to find and display wanted component(s)
	 */
	def findFromForm = Action.async { request =>
		Find.form.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(BadRequest(views.html.find(formWithErrors.withGlobalError(Errors.validationError)))),
			data => {
				val findQuery = BSON.writeDocument[Find](data)
				// Get enumerate to produce results
				val getDocs = trackerBSONCollection.find(findQuery).cursor[BSONDocument].enumerate()


				def getGraphItems(components: List[Found]) = Enumerator.enumerate(components) &>
					Enumeratee.mapM((found) => TransferHistory.getAssociatedNodes(found.id).map((cIDs) =>
						cIDs.map((c) => Find.Found(c.id, c.component, c.description, c.project)) + found))
				val lookAtGraphs : Iteratee[scala.collection.Set[Find.Found], scala.collection.Set[Find.Found]] =
					Iteratee.fold(Set.empty[Find.Found]) {
						case (soFar, next) => soFar ++ next
					}
				def getAssociatedItems(components: List[Found]) = getGraphItems(components) |>>> lookAtGraphs


				// Get Iteratee to consume results
				// For now simply folds results into a list of retrieved documents
				val lookAtDocs : Iteratee[BSONDocument, List[Found]] =
					Iteratee.fold(List.empty[Found]) {
						case (soFar, next) => BSON.readDocument[Found](next) :: soFar
					}

				// Go apply the iteratee to the enumerate
				val docs = getDocs |>>> lookAtDocs
				// Return results
				docs.flatMap((result) => {
					def giveResults(foundList: List[Found]) =
						foundList.size match {
							case 0 =>
								Ok(views.html.find(Find.form.fill(data).withGlobalError("No matching components found")))
							case 1 if foundList.head.component != OptionalComponentType.None =>
								val found = result.head
								Redirect(ComponentController.actions(found.component).updateRoute(found.id))
							case _ => Ok(views.html.findResults("Find Results")(foundList))
						}
					if (data.includeTransfers) getAssociatedItems(result).map((items) => giveResults(items.toList))
					else Future.successful(giveResults(result))
				})
			}
		).recover {
			case err => BadRequest(
				views.html.find(Find.form.withGlobalError(Errors.exceptionMessage(err))))
		}
	}
}
