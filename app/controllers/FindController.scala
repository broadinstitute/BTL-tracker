package controllers

import models.Component.OptionalComponentType
import models.{ComponentTag, Component, Find}
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
				// Build up find query looking at each possible criteria and folding all the criteria into a single request
				val findCriteria =
					Find.findCriteria.foldLeft(List.empty[(String, BSONValue)])((soFar, next) => {
						next match {
							// Get all ids specified into an array of possible IDs
							case Component.idKey => data.id match {
								case Some(ids) =>
									val idsFound = getIDs(ids)
									if (idsFound.isEmpty) soFar
									else (Component.idKey -> BSONDocument("$in" -> idsFound)) :: soFar
								case _ => soFar
							}
							// Use regular expression to see if description "contains" what's specified
							case Component.descriptionKey => data.description match {
								case Some(description) if description.length != 0 =>
									(Component.descriptionKey -> BSONRegex(description, "i")) :: soFar
								case _ => soFar
							}
							// Project must match exactly
							case Component.projectKey => data.project match {
								case Some(project) if project.length != 0 =>
									(Component.projectKey -> BSONString(project)) :: soFar
								case _ => soFar
							}
							// Tags can match on tag key (exact match) or value ("contains" match) - multiple tag
							// matches have to be put together in MongoDB  as
							// $or: [ {tags: {$elemMatch : {tag: tagWanted, value: /valueWanted/i } } }, ... ]
							// $or is needed since $in can not have within it the $elemMatch.  $elemMatch is need to
							// say that if any element in the array matches our criteria we want it.
							case Find.tagsKey if data.tags.size != 0 =>
								def getTagTuple(tag: String) = ComponentTag.tagKey -> BSONString(tag)
								def getValueTuple(value: String) = ComponentTag.valueKey -> BSONRegex(value, "i")
								def getBsonElemDoc(query: BSONDocument) =
									BSONDocument(Component.tagsKey -> BSONDocument("$elemMatch" -> query))
								val tagCriteria = data.tags.flatMap((tagEntry) => {
									(tagEntry.tag, tagEntry.value) match {
										// Tag and value there
										case (tag, Some(value)) if tag.length != 0 && value.length != 0 =>
											List(getBsonElemDoc(BSONDocument(getTagTuple(tag), getValueTuple(value))))
										// Just tag there
										case (tag, _) if tag.length != 0 =>
											List(getBsonElemDoc(BSONDocument(getTagTuple(tag))))
										// Just value there (actually UI doesn't allow this, but we will here)
										case (_, Some(value)) if value.length != 0 =>
											List(getBsonElemDoc(BSONDocument(getValueTuple(value))))
										case _ => List.empty
									}
								})
								"$or" -> BSONArray(tagCriteria) :: soFar
							// Type must match exactly
							case Component.typeKey if data.component != OptionalComponentType.None =>
								(Component.typeKey -> BSONString(data.component.toString)) :: soFar
							//@TODO include transfers
							case Find.transfersKey if data.includeTransfers => soFar
							case _ => soFar
						}
					})
				// Combine all criteria into a single query
				val findQuery = BSONDocument(findCriteria)
				// Get enumerate to produce results
				val getDocs = trackerBSONCollection.find(findQuery).cursor[BSONDocument].enumerate()
				// Get Iteratee to consome results
				// For now simply folds results into a list of retrieved documents
				val lookAtDocs : Iteratee[BSONDocument, List[BSONDocument]] =
					Iteratee.fold(List.empty[BSONDocument]) {
						case (soFar, next) => next :: soFar
					}
				// Go apply the iteratee to the enumerate
				val docs = getDocs |>>> lookAtDocs
				//@TODO For now just display the results
				docs.map((result) => Ok(if (result.isEmpty) "No results" else result.foldLeft(""){
					case (soFar, next) => soFar + BSONDocument.pretty(next)
				}))

			}
		).recover {
			case err => BadRequest(
				views.html.find(Find.form.withGlobalError(Errors.exceptionMessage(err))))
		}
	}

	/**
	 * Regular expression to use to split around white space or commas
	 */
	val splitRegExp = """[\s,]""".r

	/**
	 * Get list of IDs.  IDs can be separated by white space (space, tab, carriage return) or commas.
	 * @param inp one or more IDs
	 * @return array of IDs found
	 */
	def getIDs(inp: String) = splitRegExp.split(inp).filter(_.length != 0)
}
