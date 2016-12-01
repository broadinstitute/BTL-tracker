import models.initialContents.InitialContents
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import DBOpers._
import ScanFileOpers.d3secs
import models.Transfer.Quad
import models.TransferContents.{MergeMid, MergeResult, MergeTotalContents}
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Await
import TransferSpec._


/**
 * Tests for transfers between plates and tubes
 * Created by nnovod on 12/1/16.
 */
@RunWith(classOf[JUnitRunner])
class TransferSpec  extends TestSpec with TestConfig {
	"The transfer" must {
		"make tubes" in {
			makeMidTubes() mustEqual (None, None, None, None, None, None, None, None, None)
			val (tc1, tc2, tc3, tc4) = retrieveMidTubeContents()
			tc1 mustEqual Some(
					MergeTotalContents(
						Tube(midTubeID0,None,None,List(),None,None),
						Map(TransferContents.oneWell -> q1mid),
						List()
					)
			)
			tc2 mustEqual Some(
					MergeTotalContents(
						Tube(midTubeID1,None,None,List(),None,None),
						Map(TransferContents.oneWell -> q2mid),
						List()
					)
			)
			tc3 mustEqual Some(
					MergeTotalContents(
						Tube(midTubeID2,None,None,List(),None,None),
						Map(TransferContents.oneWell -> q3mid),
						List()
					)
			)
			tc4 mustEqual Some(
					MergeTotalContents(
						Tube(midTubeID3,None,None,List(),None,None),
						Map(TransferContents.oneWell -> q4mid),
						List()
					)
			)
		}
		"Make quads of MIDs" in {
			makeMidTubes() mustEqual (None, None, None, None, None, None, None, None, None)
			make384Mid() mustEqual (None, None, None, None, None)
			val trans = for {
				t0 <- TransferContents.getContents(p384)
			} yield t0
			val p384Tran = Await.result(trans, d3secs)
			p384Tran mustEqual Some(_: MergeTotalContents)
			val mtc = p384Tran.get
			mtc.wells.size mustEqual 384
			mtc.wells.foreach{
				case (well, mergeRes) =>
					val q = TransferWells.wellsQuad(well)
					q match {
						case Quad.Q1 => mergeRes mustEqual q1mid
						case Quad.Q2 => mergeRes mustEqual q2mid
						case Quad.Q3 => mergeRes mustEqual q3mid
						case Quad.Q4 => mergeRes mustEqual q4mid
					}
			}
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
	private val q1mid = Set(
		MergeResult(
			sample = None,
			mid = Set(MergeMid(sequence = "CTCTCTAT-TAAGGCGA-", name = "Illumina_P5-Lexof_P7-Waren", isNextera = true)),
			antibody = Set()
		)
	)
	private val q2mid = Set(
		MergeResult(
			sample = None,
			mid = Set(MergeMid(sequence = "CTCTCTAT-GCTCATGA-", name = "Illumina_P5-Lexof_P7-Pohon", isNextera = true)),
			antibody = Set()
		)
	)
	private val q3mid = Set(
		MergeResult(
			sample = None,
			mid = Set(MergeMid(sequence = "AAGGAGTA-AGGCAGAA-", name = "Illumina_P5-Biniw_P7-Dihib", isNextera = true)),
			antibody = Set()
		)
	)
	private val q4mid = Set(
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
		// Startup insert of components we'll be using for all tests and wait for them to complete
		// Make MID plate
		val midPlate = insertComponent(Plate(midID, None, None, List.empty,
			None, Some(InitialContents.ContentType.NexteraSetA), ContainerDivisions.Division.DIM8x12))
		val midTube0 = insertComponent(Tube(midTubeID0, None, None, List.empty, None, None))
		val midTube1 = insertComponent(Tube(midTubeID1, None, None, List.empty, None, None))
		val midTube2 = insertComponent(Tube(midTubeID2, None, None, List.empty, None, None))
		val midTube3 = insertComponent(Tube(midTubeID3, None, None, List.empty, None, None))
		val pToT0 = insertTransfer(Transfer(
			from = midID, to = midTubeID0,
			fromQuad = None, toQuad = None,
			project = None, slice = Some(Transfer.Slice.CP), cherries = Some(List(0)),
			isTubeToMany = false, isSampleOnly = false
		))
		val pToT1 = insertTransfer(Transfer(
			from = midID, to = midTubeID1,
			fromQuad = None, toQuad = None,
			project = None, slice = Some(Transfer.Slice.CP), cherries = Some(List(10)),
			isTubeToMany = false, isSampleOnly = false
		))
		val pToT2 = insertTransfer(Transfer(
			from = midID, to = midTubeID2,
			fromQuad = None, toQuad = None,
			project = None, slice = Some(Transfer.Slice.CP), cherries = Some(List(50)),
			isTubeToMany = false, isSampleOnly = false
		))
		val pToT3 = insertTransfer(Transfer(
			from = midID, to = midTubeID3,
			fromQuad = None, toQuad = None,
			project = None, slice = Some(Transfer.Slice.CP), cherries = Some(List(95)),
			isTubeToMany = false, isSampleOnly = false
		))
		val inserts = for {
			mp <- midPlate
			mt0 <- midTube0
			tr0 <- pToT0
			mt1 <- midTube1
			tr1 <- pToT1
			mt2 <- midTube2
			tr2 <- pToT2
			mt3 <- midTube3
			tr3 <- pToT3
		} yield {
			(mp, mt0, tr0, mt1, tr1, mt2, tr2, mt3, tr3)
		}
		Await.result(inserts, d3secs)
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
		val p384I = insertComponent(Plate(p384, None, None, List.empty, None, None, ContainerDivisions.Division.DIM16x24))
		val pToT0 = insertTransfer(Transfer(
			from = midTubeID0, to = p384,
			fromQuad = None, toQuad = Some(Quad.Q1),
			project = None, slice = None, cherries = None,
			isTubeToMany = true, isSampleOnly = false
		))
		val pToT1 = insertTransfer(Transfer(
			from = midTubeID1, to = p384,
			fromQuad = None, toQuad = Some(Quad.Q2),
			project = None, slice = None, cherries = None,
			isTubeToMany = true, isSampleOnly = false
		))
		val pToT2 = insertTransfer(Transfer(
			from = midTubeID2, to = p384,
			fromQuad = None, toQuad = Some(Quad.Q3),
			project = None, slice = None, cherries = None,
			isTubeToMany = true, isSampleOnly = false
		))
		val pToT3 = insertTransfer(Transfer(
			from = midTubeID3, to = p384,
			fromQuad = None, toQuad = Some(Quad.Q4),
			project = None, slice = None, cherries = None,
			isTubeToMany = true, isSampleOnly = false
		))
		val pToTfuture = for {
			pI <- p384I
			pt0 <- pToT0
			pt1 <- pToT1
			pt2 <- pToT2
			pt3 <- pToT3
		} yield {(pI, pt0, pt1, pt2, pt3)}
		Await.result(pToTfuture, d3secs)
	}
}
