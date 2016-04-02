/**
 * Some high level EZPass tests.
 * Created by nnovod on 4/26/15.
 */
import models.EZPass
import models.EZPass.SetEZPassData
import models.initialContents.{MolecularBarcodes, InitialContents}
import models._
import models.Transfer._
import models.project.JiraProject
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import EZPassSpec._
import ScanFileOpers._
import reactivemongo.core.commands.LastError
import models.{Transfer, Component}
import models.db.{TransferCollection, TrackerCollection}
import play.api.libs.json.Format
import org.scalatest.MustMatchers._

import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class EZPassSpec extends TestSpec with TestConfig {
	"The rackscan" must {
		"make EZPass" in {
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
			//JiraProject.insertRackIssueCollection(racks, fakeProject)
			Await.result(RackScan.insertOrReplace(racks.list.head), d3secs)
			val (rackBack, rackErr) = RackScan.findRackSync(fakeRack)
			//JiraProject.getRackIssueCollection(fakeRack)
			rackErr mustBe None
			//rackBack.head.issue mustBe fakeProject
			rackBack.head.contents.size mustBe tubeList.size

			// Make bsp spreadsheet
			val bspFileName = makeSpreadSheet(TestData.bspData)
			// Save map of barcode to associated values in bsp file
			val bspByTubeBarcode = getByValueData(bspFileName, "Manufacturer Tube Barcode", rackSize)

			// Create bsp scan entry and check it out
			makeBspScan(bspFileName, rackSize)

			// Get MID contents for set we'll be using by well and by sequence
			val midsByWell = MolecularBarcodes.mbSetA.contents
			val midsBySequence = midsByWell.map((mid) => mid._2.getSeq -> mid._1)

			// Startup insert of components we'll be using for all tests and wait for them to complete
			val rack = insertComponent(Rack(fakeRack, None, None, List.empty,
				None, None, ContainerDivisions.Division.DIM8x12))
			val atmPlate = insertComponent(Plate("ATM", None, None, List.empty,
				None, None, ContainerDivisions.Division.DIM8x12))
			val midPlate = insertComponent(Plate("MID", None, None, List.empty,
				None, Some(InitialContents.ContentType.NexteraSetA), ContainerDivisions.Division.DIM8x12))
			val tube = insertComponent(Tube("T", None, None, List.empty, None, None))
			val plate384 = insertComponent(Plate("P384", None, None, List.empty, None, None,
				ContainerDivisions.Division.DIM16x24))
			val inserts = for {
				r <- rack
				ap <- atmPlate
				mp <- midPlate
				t <- tube
				p384 <- plate384
			} yield {(r, ap, mp, t, p384)}
			import scala.concurrent.Await
			Await.result(inserts, d3secs) mustBe (None, None, None, None, None)

			/*
			 * Do test of transfers and resultant EZPass
			 * @param transfers transfers to be inserted into DB for test
			 * @param numWells number of wells that should be in resultant EZPass
			 * @param wellSet wells that should be in resultant EZPass
			 * @return
			 */
			def doTest(transfers: List[Transfer],
					   numWells: Int, wellSet: Option[Set[String]]) = {
				// Start up all the transfer inserts
				val tFutures = transfers.map(insertTransfer)
				// Wait for the result of a fold of the inserts
				Await.result(Future.fold(tFutures)(List.empty[Option[String]])((soFar, next) => soFar :+ next), d3secs)
				// Setup to make EZPass and check it out
				val trackEZPass = TrackEZPass(midsBySequence, rackScanByWell, bspByTubeBarcode, numWells, wellSet)
				// Go make the EZPass and check out the results
				Await.result(EZPass.makeEZPass(trackEZPass, "T", 10, 20, 15.0f), d3secs)
				// Cleanup transfers
				TestDB.cleanupTestTransferCollection
			}

			// Test of combining rack of samples and plate of MIDs into a plate
			doTest(List(
				Transfer(fakeRack, "ATM", None, None, None, None, None, isTubeToMany = false),
				Transfer("ATM", "T", None, None, None, None, None, isTubeToMany = false),
				Transfer("MID", "ATM", None, None, None, None, None, isTubeToMany = false)),
				rackSize, None
			)


			// Test of combining slice of rack of samples and plate of MIDs into a plate
			val sliceWells = TransferWells.slice96to96wells(Slice.S1, None).keySet
			doTest(List(
				Transfer(fakeRack, "ATM", None, None, None, Some(Slice.S1), None, isTubeToMany = false),
				Transfer("ATM", "T", None, None, None, Some(Slice.S1), None, isTubeToMany = false),
				Transfer("MID", "ATM", None, None, None, Some(Slice.S1), None, isTubeToMany = false)),
				sliceWells.size, Some(sliceWells)
			)

			// Test of transfer of rack and MIDs to and from 384 well plate with a slice of final plate sent to the tube
			val sliceWellsQS = TransferWells.slice96to96wells(Slice.S2, None).keySet
			doTest(List(Transfer(fakeRack, "P384", None, Some(Transfer.Quad.Q1), None, None, None, isTubeToMany = false),
				Transfer("MID", "P384", None, Some(Transfer.Quad.Q1), None, None, None, isTubeToMany = false),
				Transfer("P384", "ATM", Some(Transfer.Quad.Q1), None, None, None, None, isTubeToMany = false),
				Transfer("ATM", "T", None, None, None, Some(Slice.S2), None, isTubeToMany = false)),
				sliceWellsQS.size, Some(sliceWellsQS))
		}
	}
}

object EZPassSpec extends TestSpec {
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
