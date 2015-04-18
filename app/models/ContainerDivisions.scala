package models

import formats.CustomFormats._
import models.ContainerDivisions._
import play.api.libs.json.Format

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

		// Create Format for ComponentType enum using our custom enum Reader and Writer
		implicit val divisionFormat: Format[Division] =
			enumFormat(this)

		/**
		 * Get optional division from optional string
		 * @param division optional division as string
		 * @return optional enumerated value, none if input none or invalid input string
		 */
		def getDivisionFromStr(division: Option[String]) =
			division match {
				case Some(key) => try {
					Some(Division.withName(key))
				} catch {
					case e: Throwable => None
				}
				case _ => None
			}
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
	val divisionKey = "layout"

	/**
	 * List of division types as strings - make sure 8X12 is first so it can show up as default in lists
	 */
	val divisionTypes = List(Division.DIM8x12.toString) ++
		Division.values.map(_.toString).toList.filterNot(_ == Division.DIM8x12.toString)
}
