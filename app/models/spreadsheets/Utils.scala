package models.spreadsheets

import java.io.{ FileWriter, BufferedWriter, FileOutputStream, File }

import org.apache.poi.ss.usermodel.Cell
import org.broadinstitute.spreadsheets.HeadersToValues
import play.Play
import play.api.{ Play => PlayAPI }

import scala.annotation.tailrec

/**
 * Common methods used for creating new spreadsheets from spreadsheet templates.  Rows of data are set based
 * on the locations of headers in the template.
 * Created by nnovod on 1/28/16.
 */
object Utils {
	/**
	 * Directory to use for temporary files
	 */
	private val tempDir = PlayAPI.current.configuration.getString("temp.dir").getOrElse("/local/tmp")

	/**
	 * Initialize what's needed to write output to a new spreadsheet.
	 *
	 * @param fileName name of template spreadsheet, in play config path, with headers set
	 * @param fileHeaders keys for data to set in EZPass
	 * @return context containing spreadsheet information for setting entries in spreadsheet
	 */
	def initSheet(fileName: String, fileHeaders: List[String]) = {
		val inFile = Play.application().path().getCanonicalPath + fileName
		HeadersToValues(inpFile = inFile, sheetIndex = 0, headers = fileHeaders,
			outsideValues = Map.empty[String, (List[String], List[String])])
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
			// Get poi sheet (we must be dealing with a real spreadsheet)
			val sheet = headerParameters.getSheet.get
			// Set each field value in the right location (row we're up to and column under header)
			fields.foreach {
				case (header, value) =>
					headerParameters.getHeaderLocation(header) match {
						case Some((r, c)) =>
							val row = sheet.getRow(r + index) match {
								case null => sheet.createRow(r + index)
								case rowFound => rowFound
							}
							val cell = row.getCell(c) match {
								case null => row.createCell(c)
								case cellFound => cellFound
							}
							setCell(v1 = cell, v2 = value)
						case _ =>
					}
			}
		}
		// Set the values into the spreadsheet
		setValues[String](context = sheet, fields = strData, index = index,
			setCell = (cell, value) => cell.setCellValue(value))
		setValues[Int](context = sheet, fields = intData, index = index,
			setCell = (cell, value) => cell.setCellValue(value))
		setValues[Float](context = sheet, fields = floatData, index = index,
			setCell = (cell, value) => cell.setCellValue(value))
		sheet
	}

	/**
	 * All done setting data into the sheet.  If any entries present then write out a new file and shut things
	 * down.  The output file is a temp file that is ready to be uploaded to the user.
	 *
	 * @param headerValues context kept for handling of data
	 * @param entriesFound # of entries found
	 * @param errs list of errors found
	 * @param noneFound error to be set if no entries found to be considered an error
	 * @return (path of output file, list of errors)
	 */
	def makeFile(headerValues: HeadersToValues, entriesFound: Int, errs: List[String], noneFound: Option[String]) = {
		headerValues.getSheet match {
			case Some(sheet) =>
				if (entriesFound == 0 && noneFound.isDefined)
					(None, List(noneFound.get) ++ errs)
				else {
					// Create temporary file and write data there
					val tFile = makeTempFile(".xlsx")
					val outFile = new FileOutputStream(tFile)
					sheet.getWorkbook.write(outFile)
					outFile.close()
					(Some(tFile.getCanonicalPath), errs)
				}
			case None =>
				(None, errs)
		}
	}

	/**
	 * Get directory for temporary files.
	 * @return directory found or None if temporary directory could not be found
	 */
	private def getTempDir = {
		def tDir(d: String) = {
			val f = new File(d)
			if (f.isDirectory && f.canWrite)
				Some(f)
			else None
		}

		val out = tDir(tempDir)
		if (out.isDefined) out else tDir("/tmp")
	}

	/**
	 * Make a temporary file - if the specified temporary directory exists then use it, otherwises rely on defaults.
	 *
	 * @param ext file extension
	 * @return temporary file
	 */
	def makeTempFile(ext: String) = {
		val pre = "TRACKER_"
		getTempDir match {
			case Some(f) =>
				File.createTempFile(pre, ext, f)
			case None =>
				File.createTempFile(pre, ext)
		}
	}

	/**
	 * Set files in a csv file.
	 *
	 * @param headers headers to put in file
	 * @param input input data to use for putting data into file
	 * @param getValues callback to set values for a single input entry.
	 *                  Note input array is header labels in same order output must be set
	 * @param noneMsg error message if no output created
	 * @tparam T type of input
	 * @return (file name (if created), error messages)
	 */
	def setCSVValues[T](headers: Array[String], input: Iterable[T],
		getValues: (T, Array[String]) => Option[Array[String]], noneMsg: String) = {
		/*
		 * Set values in file for input
		 * @param files values are being put into
		 * @param input we are basing file on
		 * @param done # of entries done so far
		 * @return
		 */
		@tailrec
		def setValue(file: BufferedWriter, inputLeft: Iterable[T], done: Int): Int = {
			if (inputLeft.isEmpty)
				done
			else {
				val next = inputLeft.head
				val nextDone = getValues(v1 = next, v2 = headers) match {
					case Some(vals) =>
						file.write(vals.mkString(","))
						file.newLine()
						done + 1
					case None =>
						done
				}
				setValue(file = file, inputLeft = inputLeft.tail, done = nextDone)
			}
		}

		// Setup to write to temp file
		val file = makeTempFile(".csv")
		val fileWriter = new BufferedWriter(new FileWriter(file))
		// Write out headers
		fileWriter.write(headers.mkString(","))
		fileWriter.newLine()
		// Set values in file
		val entries = setValue(file = fileWriter, inputLeft = input, done = 0)
		// Close file and return status
		fileWriter.close()
		if (entries == 0) {
			file.delete()
			(None, List(noneMsg))
		} else
			(Some(file.getCanonicalPath), List.empty[String])
	}

}
