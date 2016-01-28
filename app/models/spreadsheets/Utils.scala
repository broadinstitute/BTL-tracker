package models.spreadsheets

import java.io.{FileOutputStream, File}

import org.apache.poi.ss.usermodel.Cell
import org.broadinstitute.spreadsheets.HeadersToValues
import play.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext


import scala.concurrent.Future

/**
  * Common methods used for creating new spreadsheets from spreadsheet templates.  Rows of data are set based
  * on the locations of headers in the template.
  * Created by nnovod on 1/28/16.
  */
object Utils {
	/**
	  * Initialize what's needed to write output to a new spreadsheet.
	  * @param fileName name of template spreadsheet, in play config path, with headers set
	  * @param fileHeaders keys for data to set in EZPass
	  * @return context containing spreadsheet information for setting entries in spreadsheet
	  */
	def initSheet(fileName: String, fileHeaders: List[String]) = {
		val inFile = Play.application().path().getCanonicalPath + fileName
		HeadersToValues(inFile, 0, fileHeaders, Map.empty[String, (List[String], List[String])])
	}

	/**
	  * Set the fields for a row in the spreadsheet.  Retrieve all the values and set them in the proper row
	  * (based on index) under the proper headers.
	  *
	  * @param sheet spreadsheet information
	  * @param strData strings to be set for next spreadsheet entry (fieldName -> data)
	  * @param intData integers to be set for next spreadsheet entry (fieldName -> data)
	  * @param floatData floating points to be set for next spreadsheet entry (fieldName -> data)
	  * @param index index of entry (1-based)
	  * @return context with spreadsheet information to keep using
	  */
	def setSheetValues(sheet: HeadersToValues, strData: Map[String, String], intData: Map[String, Int],
					   floatData: Map[String, Float], index: Int): HeadersToValues = {
		/*
		 * Set values in spreadsheet cells
		 * @param fields fields to set (header name -> value to set)
		 * @param index sample number
		 * @param setCell callback to set cell value
		 * @tparam T type of cell value
		 */
		def setValues[T](context: HeadersToValues, fields: Map[String, T], index: Int, setCell: (Cell, T) => Unit) = {
			val headerParameters = context
			val sheet = headerParameters.getSheet.get
			fields.foreach {
				case (header, value) => headerParameters.getHeaderLocation(header) match {
					case Some((r, c)) =>
						val row = sheet.getRow(r + index) match {
							case null => sheet.createRow(r + index)
							case rowFound => rowFound
						}
						val cell = row.getCell(c) match {
							case null => row.createCell(c)
							case cellFound => cellFound
						}
						setCell(cell, value)
					case _ =>
				}
			}
		}
		// Set the values into the spreadsheet
		setValues[String](sheet, strData, index, (cell, value) => cell.setCellValue(value))
		setValues[Int](sheet, intData, index, (cell, value) => cell.setCellValue(value))
		setValues[Float](sheet, floatData, index, (cell, value) => cell.setCellValue(value))
		sheet
	}

	/**
	  * All done setting data into the sheet.  If any entries present then write out a new file and shut things
	  * down.  The output file is a temp file that is ready to be uploaded to the user.
	  *
	  * @param sheet context kept for handling of data
	  * @param entriesFound # of entries found
	  * @param errs list of errors found
	  * @param noneFound error to be set if no entries found to be considered an error
	  * @return (path of output file, list of errors)
	  */
	def makeFile(sheet: HeadersToValues, entriesFound: Int, errs: List[String], noneFound: Option[String]) = {
		Future {
			sheet.getSheet match {
				case Some(sheet) =>
					if (entriesFound == 0 && noneFound.isDefined)
						(None, List(noneFound.get) ++ errs)
					else {
						// Create temporary file and write data there
						val tFile = File.createTempFile("TRACKER_", ".xlsx")
						val outFile = new FileOutputStream(tFile)
						sheet.getWorkbook.write(outFile)
						outFile.close()
						(Some(tFile.getCanonicalPath), errs)
					}
				case None =>
					(None, errs)
			}
		}
	}


}
