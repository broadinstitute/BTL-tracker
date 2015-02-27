package models

import mappings.CustomMappings._
import models.ContainerDivisions._
import play.api.data.Form
import play.api.data.Forms._

/**
 * Created by nnovod on 1/28/15.
 * Trait to contain layout of a container that has divisions (e.g., 96 well plate)
 */
trait ContainerDivisions {
	val layout: Division.Division
}

/**
 * Companion object with division types and associated layout information
 */
object ContainerDivisions {

	/**
	 * Enumeration for all division types
	 */
	object Division extends Enumeration {
		type Division = Value
		val DIM8x12 = Value("96 wells (A01-H12)")
		val DIM16x24 = Value("384 wells (A01-P24)")
	}

	/**
	 * Division data
	 * @param rows # of rows
	 * @param columns # of columns
	 */
	case class DivisionData(rows: Int,columns: Int)

	/**
	 * Map of Divisions to Division descriptions
	 */
	val divisionDimensions = Map(Division.DIM8x12 -> DivisionData(8,12),
		Division.DIM16x24 -> DivisionData(16,24))

	/**
	 * Keyword to use for division type
	 */
	val divisionKey = "division"

	/**
	 * List of division types as strings - make sure 8X12 is first so it can show up as default in lists
	 */
	val divisionTypes = List(Division.DIM8x12.toString) ++
		Division.values.map(_.toString).toList.filterNot(_ == Division.DIM8x12.toString)

	/**
	 * Little form for just getting a division type
	 * @param t division type
	 */
	case class DivisionTypeClass(t: Division.Division)

	val typeForm =
		Form(
			mapping(
				divisionKey -> enum(Division)
			)(DivisionTypeClass.apply)(DivisionTypeClass.unapply))

}
