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
import models.project.JiraProject
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.broadinstitute.spreadsheets.Utils._
import org.broadinstitute.spreadsheets.{HeaderSheet, CellSheet}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Format
import play.api.libs.concurrent.Execution.Implicits.defaultContext

@RunWith(classOf[JUnitRunner])
class EZPassSpec extends TestSpec with TestConfig {
	"The rackscan" must {
		"be input" in {
			val fakeProject = "FakeProject"
			val fakeRack = "XX-11227502"
			// Make regular expression to split across lines
			val lineSplitter = """(?m)$""".r

			// Make rack scan file
			val tFile = File.createTempFile("TRACKER_", ".csv")
			tFile.deleteOnExit()
			val outFile = new PrintWriter(tFile, "UTF-8")
			outFile.write(TestData.rackScan.toArray[Char])
			outFile.close()
			val scanFileName = tFile.getCanonicalPath

			def getFileData(file: String) = {
				// Method to use to get headers and associated values from spreadsheet/csv file
				val sheetToVals = (sh: CellSheet) => new HeaderSheet(sh)
				// Get data from file into header maps
				if (isSpreadSheet(file)) getSheetData(file, 0, sheetToVals)
				else getCSVFileData(file, sheetToVals)
			}

			// Method to make a map of header value to other values in row
			def getByValue(data: HeaderSheet, header: String) = {
				// Get values for wanted header
				val values = data.getHeaderValues(header)
				// Make map of header value to other values in row
				values.indices.map((i) => values(i) -> data.getRowValues(i + 1)).toMap
			}

			val rackScanData = getFileData(scanFileName)
			val rackScanByWell = getByValue(rackScanData, "TUBE")
			rackScanByWell.size mustBe 96

			// Make rack scan entry and check it out
			val racks = JiraProject.makeRackScanList(scanFileName)
			val tubeList = racks.list.head.contents
			tubeList.size mustBe 96

			// Insert rack scan into DB and make sure when we can retrieve it and looks ok
			JiraProject.insertRackIssueCollection(racks, fakeProject)
			val (rackBack, rackErr) = JiraProject.getRackIssueCollection(fakeRack)
			rackErr mustBe None
			rackBack.head.issue mustBe fakeProject
			rackBack.head.list.head.contents.size mustBe tubeList.size

			// Make bsp spreadsheet
			val wb = new XSSFWorkbook()
			val sheet = wb.createSheet("sheet 0")
			// Split input data into array of individual lines
			val lines = lineSplitter.split(TestData.bspData)
			// For each line split across tabs
			(0 until lines.length).foreach((r) => {
				val line = lines(r).trim
				val colSplitter = """\t""".r
				val cols = colSplitter.split(line)
				// Make new row in spreadsheet containing fields of line
				val row = sheet.createRow(r)
				(0 until cols.length).foreach((c) => {
					val cell = row.createCell(c)
					cell.setCellValue(cols(c))
				})
			})
			// Write out spreadsheet to file
			val tBspFile = File.createTempFile("TRACKER_", ".xlsx")
			tBspFile.deleteOnExit()
			val out = new FileOutputStream(tBspFile)
			wb.write(out)
			out.close()
			val bspFileName = tBspFile.getCanonicalPath
			// Save map of barcode to associated values in bsp file
			val bspFileData = getFileData(bspFileName)
			val bspByTubeBarcode = getByValue(bspFileData, "Manufacturer Tube Barcode")
			bspByTubeBarcode.size mustBe 96

			// Make bsp entry and check it out
			val bspscan = JiraProject.makeBspScanList(bspFileName)
			JiraProject.insertBspIssueCollection(bspscan, fakeProject)
			// Get back what was entered into DB and check that it's looking good
			val (bspBack, bspErr) = JiraProject.getBspIssueCollection(fakeRack)
			bspErr mustBe None
			bspBack.head.issue mustBe fakeProject
			bspBack.size mustBe 1
			bspBack.head.list.size mustBe 2
			bspBack.head.list.foldLeft(0)((soFar, next) => soFar + next.contents.length) mustBe 96
			val ourRack = bspBack.head.list.find((bp) => bp.barcode == fakeRack)
			ourRack.isDefined mustBe true
			val bspRack = ourRack.get
			bspRack.contents.size mustBe 88

			// Insert a component into the DB
			def insertComponent[C <: Component : Format](data: C) = {
				TrackerCollection.insertComponent(data, onSuccess = (s) => None,
					onFailure = (t) => Some(t.getLocalizedMessage))
			}

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
			import scala.concurrent.duration._
			val d2 = Duration(2000, MILLISECONDS)
			Await.result(inserts, d2) mustBe (None, None, None, None)

			// Go insert needed transfers
			def insertTransfer(transfer: Transfer) = TransferCollection.insert(transfer)
			val rackToAtm = insertTransfer(Transfer(fakeRack, "ATM", None, None, None, None))
			val atmToT = insertTransfer(Transfer("ATM", "T", None, None, None, None))
			val midToAtm = insertTransfer(Transfer("MID", "ATM", None, None, None, None))
			val transfers = for {
				ra <- rackToAtm
				at <- atmToT
				ma <- midToAtm
			} yield {(ra, at, ma)}
			Await.result(transfers, d2)

			// Type of Data returned for EZPass creation
			type EZPassData = (Map[String, String], Map[String, Int], Map[String, Float])
			case class EZPassSaved(data: List[EZPassData], midSet: Set[String])

			object TrackEZPass extends SetEZPassData[EZPassSaved] {
				def initData(c: String, h: List[String]) =
					EZPassSaved(List.empty[EZPassData], Set.empty[String])
				// Check entry out and save EZPass data
				def setFields(context: EZPassSaved, strData: Map[String, String], intData: Map[String, Int],
							  floatData: Map[String, Float], index: Int) = {
					// Get barcode sequence from EZPass data
					val mid = strData("Molecular Barcode Sequence")
					// Get well for that barcode sequence from MIDs
					val midWell = midsBySequence(mid)
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
				// Make sure amount of data is right (note mid collection is a set to make sure there are no duplicates)
				def allDone(context: EZPassSaved, samples: Int, errs: List[String]) = {
					samples mustBe 96
					context.midSet.size mustBe 96
					context.data.size mustBe 96
					(Some("All done"), errs)
				}
			}
			val ezpassResult = Await.result(EZPass.makeEZPass(TrackEZPass, "T", 10, 20, 15.0f), d2)
		}
	}
}
