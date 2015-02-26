package models

import formats.CustomFormats._
import mappings.CustomMappings._
import models.Component.ComponentType.ComponentType
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._

/**
 * Transfers are between components
 * Created by nnovod on 11/27/14.
 */

import Transfer._
case class Transfer(from: String, to: String, fromQuad: Option[Quad.Quad], toQuad: Option[Quad.Quad],
                    fromPos: Option[String], toPos: Option[String])

object Transfer {
	// Keys for form
	val fromKey = "from"
	val toKey = "to"
	val fromQuadKey = "fromQuad"
	val toQuadKey = "toQuad"
	val fromPosKey = "fromPos"
	val toPosKey = "toPos"

	object Quad extends Enumeration {
		type Quad = Value
		val Q1 = Value("1st Quadrant")
		val Q2 = Value("2nd Quadrant")
		val Q3 = Value("3rd Quadrant")
		val Q4 = Value("4th Quadrant")
	}

	// String values for dropdown lists etc.
	val quadVals = Quad.values.map(_.toString).toList

	val form = Form(
		mapping(
			fromKey -> nonEmptyText,
			toKey -> nonEmptyText,
			fromQuadKey -> optional(enum(Quad)),
			toQuadKey -> optional(enum(Quad)),
			fromPosKey -> optional(text),
			toPosKey -> optional(text)
		)(Transfer.apply)(Transfer.unapply))

	/**
	 * Formatter for going to/from and validating Json
	 * Supply our custom enum Reader and Writer for content type enum
	 * Can't use format macro because it can't handle optional enum
	 */
	import play.api.libs.functional.syntax._
	implicit val quadFormat: Format[Quad.Quad] = enumFormat(Quad)
	implicit val transferFormat: Format[Transfer] =
		((__ \ fromKey).format[String] ~
			(__ \ toKey).format[String] ~
			(__ \ fromQuadKey).format[Option[Quad.Quad]] ~
			(__ \ toQuadKey).format[Option[Quad.Quad]] ~
			(__ \ fromPosKey).format[Option[String]] ~
			(__ \ toPosKey).format[Option[String]]
	  )(Transfer.apply, unlift(Transfer.unapply))
//	implicit val formatter: Format[Transfer] = Json.format[Transfer] // Not working

	// Rules for a transfer - given two components a rule says that the transfer is (in)valid
	type TransferRules = (Component,Component) => Boolean

	/**
	 * Definition for an action
	 * @param id Identifying ID
	 * @param from component type being transferred from
	 * @param to component type being transferred to
	 * @param rules rules for this type of transfer
	 */
	case class ActionDefinition(id: Int, from: ComponentType, to: ComponentType,rules: List[TransferRules])

	/**
	 * Action that has occured
	 * @param definition id for action definition
	 */
	case class Action(definition: Int)

	/**
	 * Edge for transfer graph
	 * @param from ID of component transfer was from
	 * @param to ID of component transfer was to
	 * @param actions actions taken during transfer
	 */
	case class Edge(from: Int, to: Int, actions: List[Action])
}
