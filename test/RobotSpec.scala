/**
  * Created by nnovod on 1/13/16.
  */

import models.Robot.RobotType
import models.db.{TransferCollection, TrackerCollection}
import models.initialContents.InitialContents
import models.project.JiraProject
import models._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ScanFileOpers._
import play.api.libs.json.Format
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import RobotSpec._

import scala.concurrent.Await

@RunWith(classOf[JUnitRunner])
class RobotSpec extends TestSpec with TestConfig {
	"The robot" must {
		"make instructions" in {
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
				None, Some(InitialContents.ContentType.BSPtubes), ContainerDivisions.Division.DIM8x12))
			val abRack = insertComponent(Rack(fakeABrack, None, None, List.empty,
				None, Some(InitialContents.ContentType.ABtubes), ContainerDivisions.Division.DIM8x12))
			val a1Tube = insertComponent(Tube(fakeABtube1, None, None, List.empty, None,
				Some(InitialContents.ContentType.ABH3K4me1)))
			val a2Tube = insertComponent(Tube(fakeABtube2, None, None, List.empty, None,
				Some(InitialContents.ContentType.ABH3K4me3)))
			val a3Tube = insertComponent(Tube(fakeABtube3, None, None, List.empty, None,
				Some(InitialContents.ContentType.ABH3K27ac)))
			val abPlate = insertComponent(Plate("AB-PLATE", None, None, List.empty,
				None, None, ContainerDivisions.Division.DIM8x12))
			val inserts = for {
				r <- bspRack
				abR <- abRack
				a1T <- a1Tube
				a2T <- a2Tube
				a3T <- a3Tube
				abP <- abPlate
			} yield {(r, abR, a1T, a2T, a3T, abP)}
			import scala.concurrent.Await
			Await.result(inserts, d3secs) mustBe (None, None, None, None, None, None)

			val robot = Robot(RobotType.HAMILTON)
			val res = Await.result(robot.makeABPlate(fakeABrack, "AB-PLATE", fakeRack), d3secs)
			val resMap = res.map{
				case (None, Some(err)) => err.hashCode() -> (None, Some(err))
				case (Some(tran), None) => tran.hashCode() -> (Some(tran), None)
				case _ => "Noway".hashCode -> (None, None)
			}.toMap
			val wantedMap = TestData.abRobotInstructions.map{
				case (None, Some(err)) => err.hashCode() -> (None, Some(err))
				case (Some(tran), None) => tran.hashCode() -> (Some(tran), None)
				case _ => "Noway".hashCode -> (None, None)
			}.toMap
			res.size mustBe TestData.abRobotInstructions.size
			res.size mustBe resMap.size
			resMap.size mustBe wantedMap.size
			wantedMap.foreach{
				case (hash, found) =>
					val resFound = resMap(hash)
					resFound === found
			}
			val trans = res.flatMap{
				case (Some(tran), None) => List(tran)
				case _ => List.empty
			}
			val madeTrans =
				Robot.makeABTransfers("AB-PLATE", trans, None, ContainerDivisions.Division.DIM8x12).sortWith(_.from < _.from)
			val wantedTrans = TestData.abTransfers.sortWith(_.from < _.from)
			madeTrans.size mustBe wantedTrans.size
			wantedTrans.indices.foreach((i) => {
				val wtCherries = wantedTrans(i).cherries.get.sortWith(_ < _)
				val mCherries = madeTrans(i).cherries.get.sortWith(_ < _)
				val t1 = wantedTrans(i).copy(cherries = Some(wtCherries))
				val t2 = madeTrans(i).copy(cherries = Some(mCherries))
				t1 === t2

			})
		}
	}
}

object RobotSpec {
	/**
	  * Insert a component into the DB
	  *
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
	  *
	  * @param transfer transfer to insert
	  * @return future to complete insert
	  */
	private def insertTransfer(transfer: Transfer) = TransferCollection.insert(transfer)
}
