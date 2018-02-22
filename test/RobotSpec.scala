/**
  * Created by nnovod on 1/13/16.
  */

import models.Robot.RobotType
import models.initialContents.InitialContents
import models.project.JiraProject
import models._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ScanFileOpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import DBOpers._
import scala.concurrent.Future

import scala.concurrent.Await

@RunWith(classOf[JUnitRunner])
class RobotSpec extends TestSpec with TestConfig {
	"The robot" must {
		"make instructions" in {
			val fakeABPlate = "AB-PLATE"
			// Make BSP rack scan file from set data
			val scanFileName = makeRackScanFile(TestData.rackScan)
			val rackSize = TestData.rackScanSize

			// Make BSP rack scan entry from file and check it out
			val racks = JiraProject.makeRackScanList(scanFileName)
			val tubeList = racks.list.head.contents
			tubeList.size mustBe rackSize

			// Insert rack scan into DB and make sure we can retrieve it and it looks ok
			Await.result(RackScan.insertOrReplace(racks.list.head), d3secs)
			val (rackBack, rackErr) = RackScan.findRackSync(fakeRack)
			rackErr mustBe None
			rackBack.head.contents.size mustBe tubeList.size

			// Make bsp spreadsheet
			val bspFileName = makeSpreadSheet(TestData.bspData)
			// Create bsp scan entry and check it out
			makeBspScan(bspFileName, rackSize)

			// Make ab rack scan file from set data
			val abScanFileName = makeRackScanFile(TestData.rackABscan)
			val abRackSize = TestData.rackABscanSize

			// Make ab rack scan entry from file and check it out
			val abRacks = JiraProject.makeRackScanList(abScanFileName)
			val abRtubeList = abRacks.list.head.contents
			abRtubeList.size mustBe abRackSize

			// Insert rack scan into DB and make sure we can retrieve it and it looks ok
			Await.result(RackScan.insertOrReplace(abRacks.list.head), d3secs)
			val (abRackBack, abRackErr) = RackScan.findRackSync(fakeABrack)
			abRackErr mustBe None
			abRackBack.head.contents.size mustBe abRtubeList.size

			// Startup insert of components we'll be using for all tests and wait for them to complete
			val bspRack = insertComponent(Rack(fakeRack, None, None, List.empty,
				None, Some(InitialContents.ContentType.BSPtubes.toString), ContainerDivisions.Division.DIM8x12))
			val abRack = insertComponent(Rack(fakeABrack, None, None, List.empty,
				None, Some(InitialContents.ContentType.ABtubes.toString), ContainerDivisions.Division.DIM8x12))
			val a1Tube = insertComponent(Tube(fakeABtube1, None, None, List.empty, None,
				Some(InitialContents.ContentType.ABH3K4me1.toString)))
			val a2Tube = insertComponent(Tube(fakeABtube2, None, None, List.empty, None,
				Some(InitialContents.ContentType.ABH3K4me3.toString)))
			val a3Tube = insertComponent(Tube(fakeABtube3, None, None, List.empty, None,
				Some(InitialContents.ContentType.ABH3K27ac.toString)))
			// Note abPlate created automatically by makeABPlate if it's not already registered
			// val abPlate = insertComponent(Plate(fakeABPlate, None, None, List.empty,
			//  None, None, ContainerDivisions.Division.DIM8x12))
			val inserts = for {
				r <- bspRack
				abR <- abRack
				a1T <- a1Tube
				a2T <- a2Tube
				a3T <- a3Tube
			} yield {(r, abR, a1T, a2T, a3T)}
			import scala.concurrent.Await
			Await.result(inserts, d3secs) mustBe (None, None, None, None, None)

			// Make instructions for creating ab plate
			val robot = Robot(RobotType.HAMILTON)
			val res = Await.result(robot.makeABPlate(fakeABrack, fakeABPlate, fakeRack), d3secs)
			// Make sure there wasn't one big error
			res._2 mustBe None
			// Make maps of results and what we expected
			val transEntries = res._1.get.trans
			transEntries.size mustBe TestData.abRobotInstructions.size
			val resMap = transEntries.map {
				case (None, Some(err)) => err.hashCode() -> (None, Some(err))
				case (Some(tran), None) => tran.hashCode() -> (Some(tran), None)
				case _ => "Noway".hashCode -> (None, None)
			}.toMap
			val wantedMap = TestData.abRobotInstructions.map{
				case (None, Some(err)) => err.hashCode() -> (None, Some(err))
				case (Some(tran), None) => tran.hashCode() -> (Some(tran), None)
				case _ => "Noway".hashCode -> (None, None)
			}.toMap
			// Check that what we got back is what we expect
			resMap.size mustBe wantedMap.size
			wantedMap.foreach {
				case (hash, found) =>
					val resFound = resMap(hash)
					resFound === found
			}
			// Now on to make tracker transfers - first get list of tube->well instructions
			val trans = transEntries.flatMap {
				case (Some(tran), None) => List(tran)
				case _ => List.empty
			}
			// Make transfers and check out what we get vs. what expected (sorting so order of lists doesn't matter)
			val madeTrans =
				Robot.makeABTransfers(fakeABPlate, trans, None, ContainerDivisions.Division.DIM8x12)
					.sortWith(_.from < _.from)
			val wantedTrans = TestData.abTransfers.sortWith(_.from < _.from)
			madeTrans.size mustBe wantedTrans.size
			wantedTrans.indices.foreach((i) => {
				// Sort cherry lists so order doesn't matter there either
				val wtCherries = wantedTrans(i).cherries.get.sortWith(_ < _)
				val mCherries = madeTrans(i).cherries.get.sortWith(_ < _)
				val t1 = wantedTrans(i).copy(cherries = Some(wtCherries))
				val t2 = madeTrans(i).copy(cherries = Some(mCherries))
				t1 === t2

			})
			// Make sure inserts of transfers go ok
			val transDone = Await.result(Future.sequence(madeTrans.map(insertTransfer)), d3secs)
			transDone.size mustBe madeTrans.size
			transDone.foreach(_ mustBe None)
		}
	}
}
