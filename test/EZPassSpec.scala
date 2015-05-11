/**
 * Some high level EZPass tests.
 * Created by nnovod on 4/26/15.
 */
import java.io.{FileOutputStream, PrintWriter, File}

import models.EZPass
import models.EZPass.SetEZPassData
import models.db.{TransferCollection, TrackerCollection}
import models.initialContents.{MolecularBarcodes, InitialContents}
import models._
import models.Transfer._
import models.project.JiraProject
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.broadinstitute.spreadsheets.Utils._
import org.broadinstitute.spreadsheets.{HeaderSheet, CellSheet}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Format
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import EZPassSpec._

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class EZPassSpec extends TestSpec with TestConfig {
	"The rackscan" must {
		"be input" in {
			// Make rack scan file from set data
			val scanFileName = makeRackScanFile(TestData.rackScan)
			val rackSize = TestData.rackScanSize

			// Get map of well->rackScanData
			val rackScanByWell = getByValueData(scanFileName, "TUBE", rackSize)

			// Make rack scan entry and check it out
			val racks = JiraProject.makeRackScanList(scanFileName)
			val tubeList = racks.list.head.contents
			tubeList.size mustBe rackSize

			// Insert rack scan into DB and make sure we can retrieve it and it looks ok
			JiraProject.insertRackIssueCollection(racks, fakeProject)
			val (rackBack, rackErr) = JiraProject.getRackIssueCollection(fakeRack)
			rackErr mustBe None
			rackBack.head.issue mustBe fakeProject
			rackBack.head.list.head.contents.size mustBe tubeList.size

			// Make bsp spreadsheet
			val bspFileName = makeSpreadSheet(TestData.bspData)
			// Save map of barcode to associated values in bsp file
			val bspByTubeBarcode = getByValueData(bspFileName, "Manufacturer Tube Barcode", rackSize)

			// Create bsp scan entry and check it out
			makeBspScan(bspFileName, rackSize)

			// Startup insert of components we'll be using and wait for them to complete
			val rack = insertComponent(Rack(fakeRack, None, None, List.empty,
				None, None, ContainerDivisions.Division.DIM8x12))
			val atmPlate = insertComponent(Plate("ATM", None, None, List.empty,
				None, None, ContainerDivisions.Division.DIM8x12))
			val midPlate = insertComponent(Plate("MID", None, None, List.empty,
				None, Some(InitialContents.ContentType.NexteraSetA), ContainerDivisions.Division.DIM8x12))
			val midsByWell = MolecularBarcodes.mbSetA.contents
			val midsBySequence = midsByWell.map((mid) => mid._2.getSeq -> mid._1)
			val tube = insertComponent(Tube("T", None, None, List.empty, None, None))
			val inserts = for {
				r <- rack
				ap <- atmPlate
				mp <- midPlate
				t <- tube
			} yield {(r, ap, mp, t)}
			import scala.concurrent.Await
			Await.result(inserts, d3secs) mustBe (None, None, None, None)

			// Startup insert of needed transfers and wait for them to complete
			val rackToAtm = insertTransfer(Transfer(fakeRack, "ATM", None, None, None, None))
			val atmToT = insertTransfer(Transfer("ATM", "T", None, None, None, None))
			val midToAtm = insertTransfer(Transfer("MID", "ATM", None, None, None, None))
			val transfers = for {
				ra <- rackToAtm
				at <- atmToT
				ma <- midToAtm
			} yield {(ra, at, ma)}
			Await.result(transfers, d3secs)

			// Setup to make EZPass and check it out
			val trackEZPass = TrackEZPass(midsBySequence, rackScanByWell, bspByTubeBarcode, rackSize, None)

			// Go make the EZPass and check out the results
			Await.result(EZPass.makeEZPass(trackEZPass, "T", 10, 20, 15.0f), d3secs)

			TestDB.cleanupTestTransferCollection

			// Startup insert of needed transfers and wait for them to complete
			val rackToAtmS = insertTransfer(Transfer(fakeRack, "ATM", None, None, None, Some(Slice.S1)))
			val atmToTS = insertTransfer(Transfer("ATM", "T", None, None, None, Some(Slice.S1)))
			val midToAtmS = insertTransfer(Transfer("MID", "ATM", None, None, None, Some(Slice.S1)))
			val transfersS = for {
				ra <- rackToAtmS
				at <- atmToTS
				ma <- midToAtmS
			} yield {(ra, at, ma)}
			Await.result(transfersS, d3secs)

			// Setup to make EZPass and check it out
			val sliceWells = slice96to96map(Slice.S1).keySet
			val trackEZPassS = TrackEZPass(midsBySequence, rackScanByWell, bspByTubeBarcode,
				sliceWells.size, Some(sliceWells))

			// Go make the EZPass and check out the results
			Await.result(EZPass.makeEZPass(trackEZPassS, "T", 10, 20, 15.0f), d3secs)
		}
	}
}

object EZPassSpec extends TestSpec {
	// Fake project
	private val fakeProject = "FakeProject"
	// Fake rack barcode
	private val fakeRack = "XX-11227502"
	// Regular expression to split across lines
	private val lineSplitter = """(?m)$""".r
	// Regular expression to split across tabs
	private val colSplitter = """\t""".r

	// Duration to wait for async operations to complete
	import scala.concurrent.duration._
	private val d3secs = Duration(3000, MILLISECONDS)

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
	 * Insert a component into the DB
	 * @param data component to insert
	 * @tparam C type of component
	 * @return future with error message returned on failure
	 */
	private def insertComponent[C <: Component : Format](data: C) = {
		TrackerCollection.insertComponent(data, onSuccess = (s) => None,
			onFailure = (t) => Some(t.getLocalizedMessage))
	}


	/**
	 * Go insert a transfer
	 * @param transfer transfer to insert
	 * @return future to complete insert
	 */
	private def insertTransfer(transfer: Transfer) = TransferCollection.insert(transfer)

	/**
	 * Make a spreadsheet of a string with a starting line of header keys and following rows as tab separated values
	 * matching up to header keys.
	 * @param data input data
	 * @return name of spreadsheet file created
	 */
	private def makeSpreadSheet(data: String) = {
		// Initialize spreadsheet
		val wb = new XSSFWorkbook()
		val sheet = wb.createSheet("sheet 0")
		// Split input data into array of individual lines
		val lines = lineSplitter.split(data)
		// For each line split across tabs and create cells of values
		(0 until lines.length).foreach((r) => {
			val line = lines(r).trim
			val cols = colSplitter.split(line)
			// Make new row in spreadsheet containing fields of line
			val row = sheet.createRow(r)
			(0 until cols.length).foreach((c) => {
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
	private def makeRackScanFile(data: String) = {
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
	private def getByValueData(file: String, key: String, size: Int) = {
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
	private def makeBspScan(bspFileName: String, size: Int) = {
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

	// Type of Data returned for EZPass creation
	private type EZPassData = (Map[String, String], Map[String, Int], Map[String, Float])
	private case class EZPassSaved(data: List[EZPassData], midSet: Set[String])

	/**
	 * Class used to track mock creation of an ezpass.  We look at data passed back by EZPass creation and check that
	 * it's what we expected.
	 * @param midsBySequence MID map of barcode sequences to MID wells
	 * @param rackScanByWell rack scan map of well to map of all values from scan for well
	 * @param bspByTubeBarcode bsp data map of tube barcode to all values set in bsp data for that tube
	 * @param numSamples # of samples EZPass is expected to create
	 * @param wells legitimate wells to be in EZPass
	 */
	private case class TrackEZPass(midsBySequence: Map[String, String],
					  rackScanByWell: Map[String, Map[String, String]],
					  bspByTubeBarcode: Map[String, Map[String, String]], numSamples: Int, wells: Option[Set[String]])
		extends SetEZPassData[EZPassSaved, Unit] {
		/**
		 * Initialize EZPass context
		 * @param c component EZPass is being created for
		 * @param h headers to set in EZPass
		 * @return context to be used in other interface calls
		 */
		def initData(c: String, h: List[String]) =
			EZPassSaved(List.empty[EZPassData], Set.empty[String])

		/**
		 * Check entry out and save EZPass data
		 * @param context context being kept for setting EZPass data
		 * @param strData strings to be set for next spreadsheet entry (fieldName -> data)
		 * @param intData integers to be set for next spreadsheet entry (fieldName -> data)
		 * @param floatData floating points to be set for next spreadsheet entry (fieldName -> data)
		 * @param index index of entry (1-based)
		 * @return context to continue operations
		 */
		def setFields(context: EZPassSaved, strData: Map[String, String], intData: Map[String, Int],
					  floatData: Map[String, Float], index: Int) = {
			// Get barcode sequence from EZPass data
			val mid = strData("Molecular Barcode Sequence")
			// Get well for that barcode sequence from MIDs
			val midWell = midsBySequence(mid)
			// If well set specified make sure well in set
			wells match {
				case Some(wellSet) => wellSet.contains(midWell) mustBe true
				case _ =>
			}
			// Get tube barcode for well from rack scan
			val scanBarcode = rackScanByWell(midWell)
			// Get bsp entry for tube
			val bspEntry = bspByTubeBarcode(scanBarcode("BARCODE"))
			// Get sample ID from bsp
			val bspSample = bspEntry("Sample ID")
			// Check if sample ID from BSP is one that wound up in EZPass
			// If this test passes then barcode sequence assigned to sample is correct and lots of stuff
			// worked correctly - more of a system test than a unit one
			bspSample mustBe strData("Library Name (External Collaborator Library ID)")
			EZPassSaved(context.data :+ (strData, intData, floatData), context.midSet + mid)
		}
		/**
		 * EZPass create is all done.  Make sure amount of data is right (note mid collection is a set to make sure
		 * there are no duplicates)
		 * @param context context kept for handling EZPass data
		 * @param samples # of sample
		 * @param errs list of errors found
		 * @return (Unit, list of errors)
		 */
		def allDone(context: EZPassSaved, samples: Int, errs: List[String]) = {
			samples mustBe numSamples
			context.midSet.size mustBe numSamples
			context.data.size mustBe numSamples
			Future.successful((Unit, errs))
		}
	}

}
