import java.io.{PrintWriter, FileOutputStream, File}

import models.project.JiraProject
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.broadinstitute.spreadsheets.Utils._
import org.broadinstitute.spreadsheets.{HeaderSheet, CellSheet}
import org.scalatest.MustMatchers._

/**
  * Created by nnovod on 1/5/16.
  */
object ScanFileOpers {
	// Fake antibody rack
	val fakeABrack = "AB-RACK"
	// Fake antibody tubes
	val fakeABtube1 = "AB-T1"
	val fakeABtube2 = "AB-T2"
	val fakeABtube3 = "AB-T3"
	// Fake project
	val fakeProject = "FakeProject"
	// Fake rack barcode
	val fakeRack = "XX-11227502"
	// Regular expression to split across lines
	private val lineSplitter = """(?m)$""".r
	// Regular expression to split across tabs
	private val colSplitter = """\t""".r

	// Duration to wait for async operations to complete
	import scala.concurrent.duration._
	val d3secs = Duration(3000, MILLISECONDS)

	/**
	  * Get headers and associated values from spreadsheet/csv file
	  * @param file file specification
	  * @return maps values in rows to keys specified in file headers
	  */
	private def getFileData(file: String) = {
		val sheetToVals = (sh: CellSheet) => new HeaderSheet(sh)
		// Get data from file into header maps
		if (isSpreadSheet(file)) getSheetData(file, 0, sheetToVals)
		else getCSVFileData(file, sheetToVals)
	}

	/**
	  * Method to make a map of chosen header key row values to other values in row
	  * @param data input data values
	  * @param header header key
	  * @return map of values found in rows for wanted header key to other values in row
	  */
	private def getByValue(data: HeaderSheet, header: String) = {
		// Get values for wanted header
		val values = data.getHeaderValues(header)
		// Make map of header value to other values in row
		values.indices.map((i) => values(i) -> data.getRowValues(i + 1)).toMap
	}

	/**
	  * Make a spreadsheet of a string with a starting line of header keys and following rows as tab separated values
	  * matching up to header keys.
	  * @param data input data
	  * @return name of spreadsheet file created
	  */
	def makeSpreadSheet(data: String) = {
		// Initialize spreadsheet
		val wb = new XSSFWorkbook()
		val sheet = wb.createSheet("sheet 0")
		// Split input data into array of individual lines
		val lines = lineSplitter.split(data)
		// For each line split across tabs and create cells of values
		lines.indices.foreach((r) => {
			val line = lines(r).trim
			val cols = colSplitter.split(line)
			// Make new row in spreadsheet containing fields of line
			val row = sheet.createRow(r)
			cols.indices.foreach((c) => {
				val cell = row.createCell(c)
				cell.setCellValue(cols(c))
			})
		})
		// Write out spreadsheet to temp file
		val tBspFile = File.createTempFile("TRACKER_", ".xlsx")
		tBspFile.deleteOnExit()
		val out = new FileOutputStream(tBspFile)
		wb.write(out)
		out.close()
		// Return file name
		tBspFile.getCanonicalPath
	}

	/**
	  * Make a rack scan file.
	  * @param data data to set in file
	  * @return full path for file created
	  */
	def makeRackScanFile(data: String) = {
		// Make rack scan file
		val tFile = File.createTempFile("TRACKER_", ".csv")
		tFile.deleteOnExit()
		val outFile = new PrintWriter(tFile, "UTF-8")
		outFile.write(data.toArray[Char])
		outFile.close()
		tFile.getCanonicalPath
	}

	/**
	  * Make map data from file with header lines.  We also check that the map size is correct.
	  * @param file file with data
	  * @param key key to base map on
	  * @param size size map must be
	  * @return map of keyValue->dataInRowWithKey
	  */
	def getByValueData(file: String, key: String, size: Int) = {
		// Get back data from file
		val data = getFileData(file)
		val dataByKey = getByValue(data, key)
		dataByKey.size mustBe size
		dataByKey
	}

	/**
	  * Put the bsp data into the Jira collection.  We also check that the total # of tubes inserted is correct.
	  * @param bspFileName name of file with bsp data
	  * @param size total number of entries in bsp file
	  */
	def makeBspScan(bspFileName: String, size: Int) = {
		// Make bsp entry and check it out
		val bspscan = JiraProject.makeBspScanList(bspFileName)
		JiraProject.insertBspIssueCollection(bspscan, fakeProject)
		// Get back what was entered into DB and check that it's looking good
		val (bspBack, bspErr) = JiraProject.getBspIssueCollection(fakeRack)
		bspErr mustBe None
		bspBack.head.issue mustBe fakeProject
		bspBack.size mustBe 1
		bspBack.head.list.foldLeft(0)((soFar, next) => soFar + next.contents.length) mustBe size
		val ourRack = bspBack.head.list.find((bp) => bp.barcode == fakeRack)
		ourRack.isDefined mustBe true
	}


}
