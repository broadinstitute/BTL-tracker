/**
  * Created by nnovod on 1/5/16.
  */
/**
  * Some high level EZPass tests.
  * Created by nnovod on 4/26/15.
  */
import models._
import models.project.JiraProject
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ScanFileOpers._
//import org.scalatest.MustMatchers

import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class RackScanSpec extends TestSpec with TestConfig {
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
			Await.result(RackScan.insertOrReplace(racks.list.head), d3secs)
			val res = Await.result(RackScan.findRack(racks.list.head.barcode), d3secs)
			res.head.barcode mustBe racks.list.head.barcode
			res.head.contents.size mustBe racks.list.head.contents.size
			val resSorted = res.head.contents.sortWith(_.pos < _.pos)
			val racksSorted = racks.list.head.contents.sortWith(_.pos < _.pos)
			resSorted.indices.foreach((i) => resSorted(i) === racksSorted(i))
			TestDB.cleanupTestRackCollection
		}
	}
}
