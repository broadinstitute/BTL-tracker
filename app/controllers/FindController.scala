package controllers

import models.Find._
import models.Find
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Iteratee
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._

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
	 * Get component wanted based on ID supplied in returned form.  We must first find what type of component is
	 * wanted because based on that we go to an update screen, via a redirect, specific to component type.
	 * @return action to find and display wanted component
	 */
	def findFromForm = Action.async { request =>
		Find.form.bindFromRequest()(request).fold(
			formWithErrors =>
				Future.successful(BadRequest(views.html.find(formWithErrors.withGlobalError(Errors.validationError)))),
			data => {
				val findQuery = BSON.writeDocument[Find](data)
				// Get enumerate to produce results
				val getDocs = trackerBSONCollection.find(findQuery).cursor[BSONDocument].enumerate()
				// Get Iteratee to consome results
				// For now simply folds results into a list of retrieved documents
				val lookAtDocs : Iteratee[BSONDocument, List[Found]] =
					Iteratee.fold(List.empty[Found]) {
						case (soFar, next) => BSON.readDocument[Found](next) :: soFar
					}
				// Go apply the iteratee to the enumerate
				val docs = getDocs |>>> lookAtDocs
				docs.map((result) => Ok(views.html.findResults("Find Results")(result)))
			}
		).recover {
			case err => BadRequest(
				views.html.find(Find.form.withGlobalError(Errors.exceptionMessage(err))))
		}
	}
}
