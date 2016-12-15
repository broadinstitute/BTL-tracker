package models
import models.db.TrackerCollection
import org.broadinstitute.spreadsheets.{CellSheet, HeaderSheet}
import org.broadinstitute.spreadsheets.Utils._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Do transfers via a csv file containing multiple anywhere-to-anywhere transfers.
 * Created by nnovod on 12/14/16.
 */
object TransferFile {
	//@TODO This entire file is just the start - not to be called yet!!!!
	private val sourcePlateHdr = "Source Plate Barcode"
	private val destPlateHdr = "Destination Plate Barcode"
	private val sourceWellHdr = "Source Well"
	private val destWellHdr = "Destination Well"
	private val sourceTypeHdr = "Source Component Type"
	private val destTypeHdr = "Destination Component Type"
	private val projectHdr = "Project"
	private val volHdr = "Transfer Volume"

	private object ComponentType extends Enumeration {
		val plate96 = Value("96-well plate")
		val plate384 = Value("384-well plate")
		val tube = Value("tube")
	}

	def getFile(file: String): HeaderSheet = {
		val sheetToVals = (sh: CellSheet) => new HeaderSheet(sh)
		if (isSpreadSheet(file)) getSheetData(file, 0, sheetToVals)
		else getCSVFileData(file, sheetToVals)
	}

	def parseFile(sheet: HeaderSheet) = {
		val iter = new sheet.RowValueIter
		val (plates, errs) = iter.foldLeft((Set.empty[(String, Option[String])], Set.empty[String])) {
			case ((plates, errs), row) => {
				(row.get(sourcePlateHdr), row.get(destPlateHdr), row.get(sourceTypeHdr), row.get(destTypeHdr)) match {
					case ((None, _, _, _) | (_, None, _, _)) =>
						(plates, errs + s"$sourcePlateHdr and $destPlateHdr must be specified")
					case (Some(source), Some(dest), sType, dType) =>
						(plates + ((source, sType), (dest, dType)), errs)
				}
			}
		}
		val platesMap = plates.groupBy(_._1)
		val errs1 = platesMap.foldLeft(errs) {
			case (errSet, (plate, entries)) =>
				val types = entries.groupBy(_._2).keys.toList
				if (types.size > 2 || (types.size == 2 && types.find(_ == None).isEmpty))
					errSet + s"Component ${plate} has multiple types"
				else
					errSet
		}
		TrackerCollection.findIds(platesMap.keys.toList)
	}

	/**
	 * From a list of IDs, presumably from a rack, find which are registered and which are tubes.
	 * @param ids list of ids for wanted tubes
	 * @return (list of tubes found, list of non-tube components found, list of ids not found)
	 */
	def findComponents(ids: List[String]): Future[(List[Tube], List[Component], List[String])] =
		TrackerCollection.findIds(ids).map((tubes) => {
			// Get objects from bson
			val rackContents = ComponentFromJson.bsonToComponents(tubes)
			// Get list of ids found
			val rackContentsIds = rackContents.map(_.id)
			// See if there were any ids not found
			val notFound = ids.diff(rackContentsIds)
			// Separate tubes from non-tubes
			val isOrNotATube = rackContents.groupBy {
				case _ : Tube => true
				case _ => false
			}
			(isOrNotATube.getOrElse(true, List.empty).map(_.asInstanceOf[Tube]),
				isOrNotATube.getOrElse(false, List.empty), notFound)
		})

}
