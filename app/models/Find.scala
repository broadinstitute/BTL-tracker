package models

import models.Component.OptionalComponentType
import play.api.libs.json.Json
import play.api.data.{ObjectMapping2, FieldMapping, Form}
import play.api.data.Forms._
import mappings.CustomMappings._
import reactivemongo.bson._

/**
 * Find
 *
 * @param id barcode
 * @param description optional description
 * @param project optional project ID
 * @param tags name/value pairs
 */
case class Find(id: Option[String], description: Option[String], project: Option[String],
				tags: List[ComponentTag], component: OptionalComponentType.Value, includeTransfers: Boolean)

/**
 * Model for doing Find command
 * Created by nnovod on 4/6/15.
 */
object Find {
	// Keys for fields in mappings
	val transfersKey = "doTransfers"
	// Little kludge - in Javascript to do tags the tag label always includes component form key first
	val tagsKey = Component.formKey + "." + Component.tagsKey

	/**
	 * Form mapping for finding a component.
	 */
	val findMap = mapping(
		Component.idKey -> optional(text),
		Component.descriptionKey -> optional(text),
		Component.projectKey -> optional(text),
		tagsKey -> list(ComponentTag.tagsForm.mapping),
		Component.typeKey -> enum(OptionalComponentType),
		transfersKey -> boolean
	)(Find.apply)(Find.unapply)

	/**
	 * Get list of find criteria
	 */
	val findCriteria = findMap.mappings.flatMap {
		case FieldMapping(key, _) => List(key)
		case ObjectMapping2(_, _, (tag,_),(value,_), _, _)
			if tag == ComponentTag.tagKey && value == ComponentTag.valueKey =>
			List(tagsKey)
		case _ => List.empty
	}

	/**
	 * Form for getting find criteria
	 */
	val form = Form(findMap)

	/**
	 * Formatter for going to/from and validating Json
	 */
	implicit val formatter = Json.format[Find]

	/**
	 * Writer to convert a Find object to a BSONDocument - used for BSON.writeDocument
	 */
	implicit object FindWriter extends BSONDocumentWriter[Find] {
		/**
		 * Regular expression to use to split around white space or commas
		 */
		private val splitRegExp = """[\s,\n]""".r

		/**
		 * Get list of IDs.  IDs can be separated by white space (space, tab, carriage return) or commas.
		 * @param inp one or more IDs
		 * @return array of IDs found
		 */
		private def getIDs(inp: String) = splitRegExp.split(inp).filter(_.length != 0)

		/**
		 * Create a BSONDocument from a find object.
		 * @param find find object to be converted to BSONDocument
		 * @return document that can be used to query for Find object request
		 */
		def write(find: Find) =
			BSONDocument(
				findCriteria.foldLeft(List.empty[(String, BSONValue)])(
					(soFar, next) => {
						next match {
							// Get all ids specified into an array of possible IDs
							case Component.idKey => find.id match {
								case Some(ids) =>
									val idsFound = getIDs(ids)
									if (idsFound.isEmpty) soFar
									else (Component.idKey -> BSONDocument("$in" -> idsFound)) :: soFar
								case _ => soFar
							}
							// Use regular expression to see if description "contains" what's specified
							case Component.descriptionKey => find.description match {
								case Some(description) if description.length != 0 =>
									(Component.descriptionKey -> BSONRegex(description, "i")) :: soFar
								case _ => soFar
							}
							// Project must match exactly
							case Component.projectKey => find.project match {
								case Some(project) if project.length != 0 =>
									(Component.projectKey -> BSONString(project)) :: soFar
								case _ => soFar
							}
							// Tags can match on tag key (exact match) or value ("contains" match) - multiple tag
							// matches have to be put together in MongoDB  as
							// $or: [ {tags: {$elemMatch : {tag: tagWanted, value: /valueWanted/i } } }, ... ]
							// $or is needed since $in can not have within it the $elemMatch.  $elemMatch is need to
							// say that if any element in the array matches our criteria we want it.
							case Find.tagsKey if find.tags.size != 0 =>
								def getTagTuple(tag: String) = ComponentTag.tagKey -> BSONString(tag)
								def getValueTuple(value: String) = ComponentTag.valueKey -> BSONRegex(value, "i")
								def getBsonElemDoc(query: BSONDocument) =
									BSONDocument(Component.tagsKey -> BSONDocument("$elemMatch" -> query))
								val tagCriteria = find.tags.flatMap((tagEntry) => {
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
							case Component.typeKey if find.component != OptionalComponentType.None =>
								(Component.typeKey -> BSONString(find.component.toString)) :: soFar
							case _ => soFar
						}
					}
				)
			)
	}
		
	/**
	 * Class to get results from the DB.
	 * @param id Component id
	 * @param component component type
	 * @param description optional description
	 * @param project optional project
	 */
	case class Found(id: String, component: Component.OptionalComponentType.ComponentType,
					 description: Option[String], project: Option[String]) {
		/**
		 * Set own equals - ids should be unique so this is more efficient which is important since Found
		 * is often used in Sets.
		 * @param o other object we're comparing against
		 * @return true if objects are equal
		 */
		override def equals(o: Any) = o match {
			case that: Found => that.id == this.id
			case _ => false
		}

		/**
		 * Set own hash code to simply use id - that's more efficient which is important since Found is often
		 * used in Sets.
		 * @return hash for class based on id hash
		 */
		override def hashCode = id.hashCode
	}

	/**
	 * Reader to convert a BSON document to a Found object - used for BSON.readDocument
	 */
	implicit object FoundReader extends BSONDocumentReader[Found] {
		// Unknow value
		private val unknown = "Unknown"

		/**
		 * Create a Found object from a BSONDocument
		 * @param doc BSON retrieved from DB
		 * @return Found object created from BSON
		 */
		def read(doc: BSONDocument) : Found = {
			Found (
				id = doc.getAs[String](Component.idKey) match {
					case Some(id) => id
					case _ => unknown
				},
				component = doc.getAs[String](Component.typeKey) match {
					case Some(key) => try {
						OptionalComponentType.withName(key)
					} catch {
						case e : Throwable => OptionalComponentType.None
					}
					case None => OptionalComponentType.None
				},
				description = doc.getAs[String](Component.descriptionKey),
				project = doc.getAs[String](Component.projectKey)
			)
		}
	}
}
