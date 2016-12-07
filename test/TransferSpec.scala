import models.initialContents.InitialContents
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import DBOpers._
import ScanFileOpers.d3secs
import models.Transfer.{Quad, Slice}
import models.TransferContents.{MergeMid, MergeResult, MergeTotalContents}
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.{Await, Future}
import TransferSpec._

/**
 * Tests for transfers between plates and tubes
 * Created by nnovod on 12/1/16.
 */
@RunWith(classOf[JUnitRunner])
class TransferSpec  extends TestSpec with TestConfig {
	"The transfer" must {
		"make tubes" in {
			checkInserts(makeMidTubes())
			val (tc0, tc1, tc2, tc3) = retrieveMidTubeContents()
			checkTubes(List(
				(tc0, midTubeID0, q0mid), (tc1, midTubeID1, q1mid), (tc2, midTubeID2, q2mid), (tc3, midTubeID3, q3mid)
			))
		}
		"Make quads of MIDs" in {
			checkInserts(makeMidTubes())
			checkInserts(make384Mid())
			val trans = for {
				t0 <- TransferContents.getContents(p384)
			} yield t0
			val p384Tran = Await.result(trans, d3secs)
			p384Tran mustEqual Some(_: MergeTotalContents)
			checkQuadTotalContents(p384Tran.get)
		}
		"Make sections of MIDs" in {
			checkInserts(makeMidTubes())
			checkInserts(make384MidBySec())
			val trans = for {
				t0 <- TransferContents.getContents(p384)
			} yield t0
			val p384Tran = Await.result(trans, d3secs)
			p384Tran mustEqual Some(_: MergeTotalContents)
			checkQuadTotalContents(p384Tran.get)
		}
	}

	private def checkQuadTotalContents(mtc: MergeTotalContents) = {
		mtc.wells.size mustEqual 384
		mtc.wells.foreach {
			case (well, mergeRes) =>
				val q = TransferWells.wellsQuad(well)
				q match {
					case Quad.Q1 => mergeRes mustEqual q0mid
					case Quad.Q2 => mergeRes mustEqual q1mid
					case Quad.Q3 => mergeRes mustEqual q2mid
					case Quad.Q4 => mergeRes mustEqual q3mid
				}
		}
	}

	private def checkInserts(stat: List[Option[String]]) =
		stat.foreach(_ mustEqual None)

	private def checkTubes(tubes: List[(Option[MergeTotalContents], String, Set[MergeResult])]) = {
		tubes.foreach {
			case (tc, tubeID, mr) =>
				tc mustEqual Some(
					MergeTotalContents(
						Tube(tubeID,None,None,List(),None,None),
						Map(TransferContents.oneWell -> mr),
						List()
					)
				)
		}
	}
}

object TransferSpec {
	private val midID = "MID"
	private val midTubeID0 = "MIDT0"
	private val midTubeID1 = "MIDT1"
	private val midTubeID2 = "MIDT2"
	private val midTubeID3 = "MIDT3"
	private val p384 = "P384"
	private val q0mid = Set(
		MergeResult(
			sample = None,
			mid = Set(MergeMid(sequence = "CTCTCTAT-TAAGGCGA-", name = "Illumina_P5-Lexof_P7-Waren", isNextera = true)),
			antibody = Set()
		)
	)
	private val q1mid = Set(
		MergeResult(
			sample = None,
			mid = Set(MergeMid(sequence = "CTCTCTAT-GCTCATGA-", name = "Illumina_P5-Lexof_P7-Pohon", isNextera = true)),
			antibody = Set()
		)
	)
	private val q2mid = Set(
		MergeResult(
			sample = None,
			mid = Set(MergeMid(sequence = "AAGGAGTA-AGGCAGAA-", name = "Illumina_P5-Biniw_P7-Dihib", isNextera = true)),
			antibody = Set()
		)
	)
	private val q3mid = Set(
		MergeResult(
			sample = None,
			mid = Set(MergeMid(sequence = "TCTCTCCG-ATCTCAGG-", name = "Illumina_P5-Xolek_P7-Fexar", isNextera = true)),
			antibody = Set()
		)
	)
	// Get MID contents for set we'll be using by well and by sequence
	//private val midsByWell = MolecularBarcodes.mbSetA.contents
	//private val midsBySequence = midsByWell.map((mid) => mid._2.getSeq -> mid._1)

	/**
	 * Make 4 tubes, each containing a single MID
	 * @return results of inserts
	 */
	private def makeMidTubes() = {
		doPlateToTubes(
			plate =
				Plate(id = midID, description = None, project = None, tags = List.empty, locationID = None,
					initialContent = Some(InitialContents.ContentType.NexteraSetA),
					layout = ContainerDivisions.Division.DIM8x12),
			tubes = List((midTubeID0, List(0)), (midTubeID1, List(10)), (midTubeID2, List(50)), (midTubeID3, List(95)))
		)
	}

	/**
	 * Get contents of four mid tubes
	 * @return contents of each tube
	 */
	private def retrieveMidTubeContents() = {
		val trans = for {
			t0 <- TransferContents.getContents(midTubeID0)
			t1 <- TransferContents.getContents(midTubeID1)
			t2 <- TransferContents.getContents(midTubeID2)
			t3 <- TransferContents.getContents(midTubeID3)
		} yield {(t0, t1, t2, t3)}
		Await.result(trans, d3secs)
	}

	/**
	 * Make a 384 well plate to contain one mid (from one of mid tubes) in all the wells of a quadrant.  Made via
	 * transfers of 4 mid tubes into quadrants - one per quadrant.
	 * @return results of inserts
	 */
	private def make384Mid() = {
		doTubesTo384Plate(List(
			(midTubeID0, Quad.Q1, None), (midTubeID1, Quad.Q2, None),
			(midTubeID2, Quad.Q3, None), (midTubeID3, Quad.Q4, None)
		))
	}

	/**
	 * Make a 384 well plate to contain one mid (from one of mid tubes) in all the wells of a quadrant.  Made via
	 * transfers of 4 mid tubes into quadrants - one per quadrant.
	 * @return results of inserts
	 */
	private def make384MidBySec() = {
		doTubesTo384Plate(List(
			(midTubeID0, Quad.Q1, Some(Slice.S5)), (midTubeID0, Quad.Q1, Some(Slice.S6)),
			(midTubeID1, Quad.Q2, Some(Slice.S5)), (midTubeID1, Quad.Q2, Some(Slice.S6)),
			(midTubeID2, Quad.Q3, Some(Slice.S5)), (midTubeID2, Quad.Q3, Some(Slice.S6)),
			(midTubeID3, Quad.Q4, Some(Slice.S5)), (midTubeID3, Quad.Q4, Some(Slice.S6))
		))
	}

	private def doTubesTo384Plate(tubes: List[(String, Quad.Quad, Option[Slice.Slice])]) =
		doTubeToManyInserts(
			plate = Plate(p384, None, None, List.empty, None, None, ContainerDivisions.Division.DIM16x24),
			inputs = tubes
		)

	private def doTubeToManyInserts(plate: Plate,
						  inputs: List[(String, Quad.Quad, Option[Slice.Slice])]) = {
		val pInsert = insertComponent(plate)
		val tInserts = inputs.map {
			case (tube, quad, slice) =>
				insertTransfer(Transfer(
					from = tube, to = plate.id,
					fromQuad = None, toQuad = Some(quad),
					project = None, slice = slice, cherries = None,
					isTubeToMany = true, isSampleOnly = false
				))
		}
		val futs = Future.sequence(pInsert :: tInserts)
		Await.result(futs, d3secs)
	}

	private def doPlateToTubes(plate: Plate, tubes: List[(String, List[Int])]) = {
		val pInsert = insertComponent(plate)
		val tInserts = tubes.map((t) => insertComponent(Tube(t._1, None, None, List.empty, None, None)))
		val trans = tubes.map((t) =>
			insertTransfer(Transfer(
				from = plate.id, to = t._1,
				fromQuad = None, toQuad = None,
				project = None, slice = Some(Transfer.Slice.CP), cherries = Some(t._2),
				isTubeToMany = false, isSampleOnly = false
			))
		)
		val futs = Future.sequence(pInsert :: (tInserts ++ trans))
		Await.result(futs, d3secs)
	}
}
