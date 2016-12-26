/**
 * Transfer file tests.
 * Created by nnovod on 12/17/16.
 */
import models.ContainerDivisions.Division
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import models.{Plate, Transfer, TransferFile}
import models.db.TrackerCollection
import org.scalatest.concurrent.ScalaFutures
import utils.{No, Yes, YesOrNo}
import DBOpers._
import models.Transfer.{Slice, Quad}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import TransferFileSpec._

@RunWith(classOf[JUnitRunner])
class TransferFileSpec extends TestSpec with TestConfig with ScalaFutures {
	"The transfer file" must {
		"Error when source not specified" in {
			val tp = await(TransferFile.insertTransfers(None, List(Map(dpH->"dp1",swH->"A01",dwH->"A01"))))
			printMsg(tp)
			tp mustBe a [No]
			// When it was an exception
			//			ScalaFutures.whenReady(tp.failed) { e =>
			//				println(e.getLocalizedMessage)
			//				e mustBe a [Exception]
			//			}
		}
		"Error when destination not specified" in {
			val tp = await(TransferFile.insertTransfers(None, List(Map(spH ->"sp1",swH->"A01",dwH->"A01"))))
			printMsg(tp)
			tp mustBe a [No]
		}
		"Error when type not specified" in {
			val tp = await(TransferFile.insertTransfers(None, List(Map(spH->"sp1",dpH->"dp1",swH->"A01",dwH->"A01"))))
			printMsg(tp)
			tp mustBe a [No]
		}
		"Error when two different types specified" in {
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",swH->"A01",dwH->"A01",stH->p96T,dtH->p96T),
				Map(spH->"sp1", dpH->"dp1",swH->"A02",dwH->"A02",stH->p384T,dtH->p384T)
			)))
			printMsg(tp)
			tp mustBe a [No]
		}
		"Error when no well specified" in {
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",dwH->"A01",stH->p96T,dtH->p96T),
				Map(spH->"sp1", dpH->"dp1",swH->"A02")
			)))
			printMsg(tp)
			tp mustBe a [No]
		}
		"Error when bad well specified" in {
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",swH->"Z01",dwH->"A01",stH->p96T,dtH->p96T),
				Map(spH->"sp1", dpH->"dp1",swH->"A02",dwH->"A99")
			)))
			printMsg(tp)
			tp mustBe a [No]
		}
		"Error if type specified different than what in DB" in {
			val i =
				TrackerCollection.insertComponent(
					Plate(id = "sp1", description = None, project = None,
						tags = List.empty, locationID = None, initialContent = None,
						layout = Division.DIM16x24),
					onSuccess = println,
					onFailure = println
				)
			await(i)
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",swH->"A01",dwH->"A01",stH->p96T,dtH->p96T),
				Map(spH->"sp1", dpH->"dp1",swH->"A02",dwH->"A02")
			)))
			printMsg(tp)
			tp mustBe a [No]
		}
		"Type specified once for cherry picking" in {
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",swH->"A01",dwH->"A01",stH->p96T,dtH->p96T),
				Map(spH->"sp1", dpH->"dp1",swH->"A02",dwH->"A02")
			)))
			printMsg(tp)
			val tWanted =
				List(Transfer("sp1","dp1",None,None,None,Some(Slice.CP),Some(List(0, 1)),None,false,false))
			tp mustBe a [Yes[_]]
			tp.getYes mustEqual (2, tWanted)
		}
		"Type specified in input for cherry picking" in {
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",swH->"A01",dwH->"A01",stH->p96T,dtH->p96T))))
			printMsg(tp)
			val tWanted =
				List(Transfer("sp1","dp1",None,None,None,Some(Slice.CP),Some(List(0)),None,false,false))
			tp mustBe a [Yes[_]]
			tp.getYes mustEqual (2, tWanted)
		}
		"Type specified only in DB for cherry picking" in {
			val i =
				TrackerCollection.insertComponent(
					Plate(id = "sp1", description = None, project = None,
						tags = List.empty, locationID = None, initialContent = None,
						layout = Division.DIM8x12),
					onSuccess = println,
					onFailure = println
				)
			await(i)
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",swH->"A01",dwH->"A01",dtH->p96T),
				Map(spH->"sp1", dpH->"dp1",swH->"A02",dwH->"A02")
			)))
			printMsg(tp)
			val tWanted =
				List(Transfer("sp1","dp1",None,None,None,Some(Slice.CP),Some(List(0, 1)),None,false,false))
			tp mustBe a [Yes[_]]
			tp.getYes mustEqual (1, tWanted)
		}
		"Cherry picking into tube" in {
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",swH->"A01",dwH->"A01",stH->p96T,dtH->tT),
				Map(spH->"sp1", dpH->"dp1",swH->"A02")
			)))
			printMsg(tp)
			val tWanted =
				List(Transfer("sp1","dp1",None,None,None,Some(Slice.CP),Some(List(0, 1)),None,false,false))
			tp mustBe a [Yes[_]]
			tp.getYes mustEqual (2, tWanted)
		}
		"Cherry picking from tube" in {
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",swH->"A01",dwH->"A01",stH->tT,dtH->p96T),
				Map(spH->"sp1", dpH->"dp1",dwH->"A02")
			)))
			printMsg(tp)
			val tWanted =
				List(Transfer("sp1","dp1",None,None,None,Some(Slice.CP),Some(List(0, 1)),None,true,false))
			tp mustBe a [Yes[_]]
			tp.getYes mustEqual (2, tWanted)
		}
		"Quad transfer" in {
			val tp = await(TransferFile.insertTransfers(None, quadList))
			printMsg(tp)
			tp mustBe a [Yes[_]]
			val tWanted = List(Transfer("sp1","dp1",None,Some(Quad.Q1),None,None,None,None,false,false))
			tp.getYes mustEqual (2, tWanted)
			// Check that retrieve of free picker works
			val t = await(findTransfer(src = "sp1", dest = "dp1"))
			t mustEqual tWanted
		}
		"Free picking from 384->96" in {
			val tp = await(TransferFile.insertTransfers(None, List(
				Map(spH->"sp1", dpH->"dp1",swH->"A01",dwH->"A01",stH->p384T,dtH->p96T),
				Map(spH->"sp1", dpH->"dp1",swH->"A02",dwH->"A02"),
				Map(spH->"sp1", dpH->"dp1",swH->"A02",dwH->"F02"),
				Map(spH->"sp1", dpH->"dp1",swH->"P02",dwH->"A02")
			)))
			printMsg(tp)
			tp mustBe a [Yes[_]]
			val tWanted = List(Transfer("sp1","dp1",None,None,None,Some(Slice.FREE),None,
				Some(List((0,List(0)), (1,List(1, 61)), (361,List(1)))),false,false))
			tp.getYes mustEqual (2, tWanted)
			// Check that retrieve of free picker works
			val t = await(findTransfer(src = "sp1", dest = "dp1"))
			t mustEqual tWanted
		}
		// Tube as input (with or without well); Tube as output
		// Transfer where all components already in DB
		// Transfers of one well to many
		// Transfers of many to one well
		// Transfers of entire component; entire quadrant
	}
}

object TransferFileSpec {
	// Headers in file
	private val spH = "Source Plate Barcode"
	private val dpH = "Destination Plate Barcode"
	private val swH = "Source Well"
	private val dwH = "Destination Well"
	private val stH = "Source Component Type"
	private val dtH = "Destination Component Type"
	private val pH = "Project"
	private val vH = "Transfer Volume"

	// Component types in file
	private val p96T = "96-well plate"
	private val p384T = "384-well plate"
	private val tT = "tube"

	/**
	 * Wait (10 seconds) for future to complete
	 * @param res future to wait for
	 * @tparam T result of future
	 * @return future result
	 */
	private def await[T](res: Future[T]) = Await.result(res, 10.seconds)
	private def awaitLong[T](res: Future[T]) = Await.result(res, 1800.seconds)

	private def printMsg(yOrN: YesOrNo[_]) =
		yOrN.getNoOption match {
			case Some(err) => println(err)
			case _ =>
		}

	private val quadList =
		List(
			Map(spH->"sp1", dpH->"dp1",swH->"A01",dwH->"A01",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A02",dwH->"A03",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A03",dwH->"A05",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A04",dwH->"A07",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A05",dwH->"A09",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A06",dwH->"A11",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A07",dwH->"A13",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A08",dwH->"A15",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A09",dwH->"A17",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A10",dwH->"A19",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A11",dwH->"A21",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"A12",dwH->"A23",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B01",dwH->"C01",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B02",dwH->"C03",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B03",dwH->"C05",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B04",dwH->"C07",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B05",dwH->"C09",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B06",dwH->"C11",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B07",dwH->"C13",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B08",dwH->"C15",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B09",dwH->"C17",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B10",dwH->"C19",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B11",dwH->"C21",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"B12",dwH->"C23",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C01",dwH->"E01",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C02",dwH->"e03"),
			Map(spH->"sp1", dpH->"dp1",swH->"C03",dwH->"e05",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C04",dwH->"e07",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C05",dwH->"e09",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C06",dwH->"e11",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C07",dwH->"e13",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C08",dwH->"e15",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C09",dwH->"e17",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C10",dwH->"e19",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C11",dwH->"e21",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"C12",dwH->"e23",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D01",dwH->"g01",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D02",dwH->"g03",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D03",dwH->"g05",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D04",dwH->"g07",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D05",dwH->"g09",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D06",dwH->"g11",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D07",dwH->"g13",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D08",dwH->"g15",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D09",dwH->"g17",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D10",dwH->"g19",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D11",dwH->"g21",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"D12",dwH->"g23",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e1",dwH->"i01",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e2",dwH->"i03",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e3",dwH->"i05",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e4",dwH->"i07",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e5",dwH->"i09",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e6",dwH->"i11",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e7",dwH->"i13",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e8",dwH->"i15",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e9",dwH->"i17",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e10",dwH->"i19",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"e11",dwH->"i21"),
			Map(spH->"sp1", dpH->"dp1",swH->"e12",dwH->"i23",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F01",dwH->"k01",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F02",dwH->"k03",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F03",dwH->"k05",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F04",dwH->"k07",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F05",dwH->"k09",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F06",dwH->"k11",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F07",dwH->"k13",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F08",dwH->"k15",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F09",dwH->"k17",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F10",dwH->"k19",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F11",dwH->"k21",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"F12",dwH->"k23",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G01",dwH->"m01",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G02",dwH->"m03",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G03",dwH->"m05",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G04",dwH->"m07",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G05",dwH->"m09",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G06",dwH->"m11",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G07",dwH->"m13",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G08",dwH->"m15",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G09",dwH->"m17",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G10",dwH->"m19",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G11",dwH->"m21",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"G12",dwH->"m23",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H01",dwH->"o01",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H02",dwH->"o03",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H03",dwH->"o05",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H04",dwH->"o07",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H05",dwH->"o09",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H06",dwH->"o11",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H07",dwH->"o13",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H08",dwH->"o15",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H09",dwH->"o17",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H10",dwH->"o19",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H11",dwH->"o21",stH->p96T,dtH->p384T),
			Map(spH->"sp1", dpH->"dp1",swH->"H12",dwH->"o23",stH->p96T,dtH->p384T)
		)
}
