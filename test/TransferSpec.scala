import models.initialContents.{InitialContents, MolecularBarcodes}
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
import models.initialContents.MolecularBarcodes.{MolBarcodeContents, MolBarcodeWell}

/**
 * Tests for transfers between plates and tubes
 * Created by nnovod on 12/1/16.
 */
@RunWith(classOf[JUnitRunner])
class TransferSpec extends TestSpec with TestConfig {
	"The transfer" must {
		"make tubes" in {
			checkInserts(makeMidTubes())
			val (tc0, tc1, tc2, tc3) = retrieveMidTubeContents()
			checkTubes(List(
				(tc0, midTubeID0, q0mid), (tc1, midTubeID1, q1mid),
				(tc2, midTubeID2, q2mid), (tc3, midTubeID3, q3mid)
			))
		}
		"Make quads of MIDs (single MID in all wells of quad) from tubes" in {
			checkInserts(makeMidTubes())
			checkInserts(make384MidFromTubesToQuads())
			check384SingleMIDToQuad(get384Tran())
		}
		"Make sections of MIDs (single MID in all wells of quad) from tubes" in {
			checkInserts(makeMidTubes())
			checkInserts(make384MidFromTubesToSecs())
			check384SingleMIDToQuad(get384Tran())
		}
		"Make cherries of MIDs (single MID in all wells of quad) from tubes" in {
			checkInserts(makeMidTubes())
			checkInserts(make384MidFromTubesToCherries())
			check384SingleMIDToQuad(get384Tran())
		}
		"Make 384-well plate (entire MID plate in each quad) from plate of MIDs" in {
			checkInserts(make384MidFrom96PlateToQuads())
			check384Quads(get384Tran(), MolecularBarcodes.mbSetA)
		}
		"Make 384-well plate (entire MID plate in each quad) from sections of MIDs" in {
			checkInserts(make384MidFrom96PlateToSecs())
			check384Quads(get384Tran(), MolecularBarcodes.mbSetA)
		}
		"Make 384-well plate (entire MID plate in each quad) from cherries of MIDs" in {
			checkInserts(make384MidFrom96PlateToCPs())
			check384Quads(get384Tran(), MolecularBarcodes.mbSetA)
		}
		"Make 96-well plate with MIDs from all quadrants of 384-well MID plate" in {
			checkInserts(
				do384To96Plate(
					from = mid384,
					transfers =
						List(
							(Some(Quad.Q1), None, None), (Some(Quad.Q2), None, None),
							(Some(Quad.Q3), None, None), (Some(Quad.Q4), None, None)
						)
				)
			)
			check96MultipleMids(get96Tran(), MolecularBarcodes.mbSet384A)
		}
		"Make 96-well plate with MIDs from all sections of 384-well MID plate" in {
			checkInserts(
				do384To96Plate(
					from = mid384,
					transfers =
						List(
							(Some(Quad.Q1), Some(Slice.S5), None), (Some(Quad.Q1), Some(Slice.S6), None),
							(Some(Quad.Q2), Some(Slice.S5), None), (Some(Quad.Q2), Some(Slice.S6), None),
							(Some(Quad.Q3), Some(Slice.S5), None), (Some(Quad.Q3), Some(Slice.S6), None),
							(Some(Quad.Q4), Some(Slice.S5), None), (Some(Quad.Q4), Some(Slice.S6), None)
						)
				)
			)
			check96MultipleMids(transfers = get96Tran(), mids = MolecularBarcodes.mbSet384A)
		}
		"Make 96-well plate with MIDs from all cherries of 384-well MID plate" in {
			checkInserts(
				do384To96Plate(
					from = mid384,
					transfers =
						List(
							(Some(Quad.Q1), Some(Slice.CP), cherries96),
							(Some(Quad.Q2), Some(Slice.CP), cherries96),
							(Some(Quad.Q3), Some(Slice.CP), cherries96),
							(Some(Quad.Q4), Some(Slice.CP), cherries96)
						)
				)
			)
			check96MultipleMids(transfers = get96Tran(), mids = MolecularBarcodes.mbSet384A)
		}
	}

	// Note - all methods including a must clause need to be within TestSpec class
	/**
	 * Get contents of 384-well plate.
	 * @return 384-well contents
	 */
	private def get384Tran() = getTran(p384ID)

	/**
	 * Get contents of 96-well plate.
	 * @return 96-well contents
	 */
	private def get96Tran() = getTran(p96ID)

	/**
	 * Get contents of plate.
	 * @return plate contents
	 */
	private def getTran(id: String) = {
		val pTran = getTransRes(id)
		pTran mustEqual Some(_: MergeTotalContents)
		pTran.get
	}

	/**
	 * Check that contents of 384-well plate are a single known MID per quadrant.
	 * @param mtc contents of 384-well plate
	 */
	private def check384SingleMIDToQuad(mtc: MergeTotalContents) = {
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

	/**
	 * Check that inserts all didn't have an error message
	 * @param stat optional error messages
	 */
	private def checkInserts(stat: List[Option[String]]) =
		stat.foreach(_ mustEqual None)

	/**
	 * Check that tubes have expected contents
	 * @param tubes (tubeTotalContents, tubeID, tubeContents)
	 */
	private def checkTubes(tubes: List[(Option[MergeTotalContents], String, Set[MergeResult])]) = {
		tubes.foreach {
			case (tc, tubeID, mr) =>
				tc mustEqual Some(
					MergeTotalContents(
						component = Tube(
							id = tubeID,
							description = None,
							project = None,
							tags = List(),
							locationID = None,
							initialContent = None
						),
						wells = Map(TransferContents.oneWell -> mr),
						errs = List()
					)
				)
		}
	}

	/**
	 * Assuming a 96-well MID plate was transferred complete into each quadrant of a 384-well plate this checks that
	 * the contents of the 384-well plate is as expected.
	 * @param transfers result of transfers into 384-well plate
	 * @param mids MID barcodes by well
	 */
	private def check384Quads(transfers: MergeTotalContents, mids: MolBarcodeContents) =
		check384QuadsByQuad(transfers, Map(Quad.Q1 -> mids, Quad.Q2 -> mids, Quad.Q3 -> mids, Quad.Q4 -> mids))

	/**
	 * Assuming a different 96-well MID plate was transferred complete into each quadrant of a 384-well plate this
	 * checks that the contents of the 384-well plate is as expected
	 * @param transfers result of transfers into 384-well plate
	 * @param mids MID barcodes by quadrant by well
	 */
	private def check384QuadsByQuad(transfers: MergeTotalContents, mids: Map[Quad.Quad, MolBarcodeContents]) = {
		transfers.wells.size mustEqual 384
		transfers.wells.foreach {
			case (well, mergeRes) =>
				// Get quad well is on and then equivalent well on 96-well plate
				val quad = TransferWells.wellsQuad(well)
				val wellOn96 = TransferWells.qFrom384(quad)(well)
				// Get MID that is on that well in 96-well MID plate
				val midWanted = mids(quad).contents(wellOn96)
				// Check that MID in transfer result is what's expected
				mergeRes.size mustEqual 1
				mergeRes.head.mid.size mustEqual 1
				val midFound = mergeRes.head.mid.head
				checkMid(midFound = midFound, midWanted = midWanted)
		}
	}

	/**
	 * Check that multiple quadrants of 384-well MID plate transferred into 96-well plate are valid.
	 * @param transfers contents of 96-well plate found
	 * @param mids MIDs in 384-well MID plate
	 */
	private def check96MultipleMids(transfers: MergeTotalContents, mids: MolBarcodeContents) = {
		transfers.wells.size mustEqual 96
		transfers.wells.foreach {
			case (well, mergeRes) =>
				// Get mids put in well (one from each quadrant)
				val midsInWell = Quad.values.map((q) => mids.contents(TransferWells.qTo384(q)(well)))
				// Make sure result has proper number of MIDs
				mergeRes.size mustEqual midsInWell.size
				mergeRes.foreach(_.mid.size mustEqual 1)
				// Check that each MID that is supposed to be there is actually there
				midsInWell.foreach((mid) => {
					val matchFound = mergeRes.find((midF) => midF.mid.head.sequence == mid.getSeq)
					matchFound mustEqual Some(_: MergeResult)
					val midMatch = matchFound.get.mid.head
					checkMid(midFound = midMatch, midWanted = mid)
				})
		}
	}

	/**
	 * Check that a MID found is equivalent to a MID wanted
	 * @param midFound MID found
	 * @param midWanted MID wanted
	 */
	private def checkMid(midFound: MergeMid, midWanted: MolBarcodeWell) = {
		midFound.sequence mustEqual midWanted.getSeq
		midFound.name mustEqual midWanted.getName
		midFound.isNextera mustEqual midWanted.isNextera
	}
}

object TransferSpec {
	private val midTubeID0 = "MIDT0"
	private val midTubeID1 = "MIDT1"
	private val midTubeID2 = "MIDT2"
	private val midTubeID3 = "MIDT3"

	private val mid96ID = "MID96"
	private val mid96 = Plate(id = mid96ID, description = None, project = None, tags = List.empty,
		locationID = None, initialContent = Some(InitialContents.ContentType.NexteraSetA),
		layout = ContainerDivisions.Division.DIM8x12)

	private val mid384ID = "MID384"
	private val mid384 = Plate(id = mid384ID, description = None, project = None, tags = List.empty,
		locationID = None, initialContent = Some(InitialContents.ContentType.Nextera384SetA),
		layout = ContainerDivisions.Division.DIM16x24)

	private val p384ID = "P384"
	val p384 = Plate(id = p384ID, description = None, project = None,
		tags = List.empty, locationID = None, initialContent = None, layout = ContainerDivisions.Division.DIM16x24)

	private val p96ID = "P96"
	val p96 = Plate(id = p96ID, description = None, project = None,
		tags = List.empty, locationID = None, initialContent = None, layout = ContainerDivisions.Division.DIM8x12)

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

	private val cherries96 = Some((0 to 95).toList)

	// Get MID contents for set we'll be using by well and by sequence
	//private val midsByWell = MolecularBarcodes.mbSetA.contents
	//private val midsBySequence = midsByWell.map((mid) => mid._2.getSeq -> mid._1)

	/**
	 * Make 4 tubes, each containing a single MID
	 * @return results of inserts/transfers
	 */
	private def makeMidTubes() = {
		doPlateToTubes(
			plate = mid96,
			tubes = List((midTubeID0, List(0)), (midTubeID1, List(10)), (midTubeID2, List(50)), (midTubeID3, List(95)))
		)
	}

	/**
	 * Transfer some wells from a plate to tubes.
	 * @param plate plate to transfer from
	 * @param tubes tubes to transfer into (tube, wellsToTransferFrom)
	 * @return results of inserts/transfers
	 */
	private def doPlateToTubes(plate: Plate, tubes: List[(String, List[Int])]) = {
		val pInsert = insertComponent(plate)
		val tInserts = tubes.map((t) => insertComponent(Tube(t._1, None, None, List.empty, None, None)))
		val trans = tubes.map((t) =>
			insertTransfer(Transfer(
				from = plate.id, to = t._1,
				fromQuad = None, toQuad = None,
				project = None, slice = Some(Slice.CP), cherries = Some(t._2),
				isTubeToMany = false, isSampleOnly = false
			))
		)
		val futs = Future.sequence(pInsert :: (tInserts ++ trans))
		Await.result(futs, d3secs)
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
		} yield { (t0, t1, t2, t3) }
		Await.result(trans, d3secs)
	}

	/**
	 * Make a 384 well plate to contain one mid (from one of mid tubes) in all the wells of a quadrant.  Made via
	 * transfers of 4 mid tubes into quadrants - one per quadrant.
	 * @return results of component and transfer inserts
	 */
	private def make384MidFromTubesToQuads() = {
		doTubesTo384Plate(List(
			(midTubeID0, Quad.Q1, None, None), (midTubeID1, Quad.Q2, None, None),
			(midTubeID2, Quad.Q3, None, None), (midTubeID3, Quad.Q4, None, None)
		))
	}

	/**
	 * Make a 384 well plate to contain one mid (from one of mid tubes) in all the wells of a quadrant.  Made via
	 * transfers of 4 mid tubes into slices - one MID appears per quadrant.
	 * @return results of component and transfer inserts
	 */
	private def make384MidFromTubesToSecs() = {
		doTubesTo384Plate(List(
			(midTubeID0, Quad.Q1, Some(Slice.S5), None), (midTubeID0, Quad.Q1, Some(Slice.S6), None),
			(midTubeID1, Quad.Q2, Some(Slice.S5), None), (midTubeID1, Quad.Q2, Some(Slice.S6), None),
			(midTubeID2, Quad.Q3, Some(Slice.S5), None), (midTubeID2, Quad.Q3, Some(Slice.S6), None),
			(midTubeID3, Quad.Q4, Some(Slice.S5), None), (midTubeID3, Quad.Q4, Some(Slice.S6), None)
		))
	}

	/**
	 * Make a 384 well plate to contain one mid (from one of mid tubes) in all the wells of a quadrant.  Made via
	 * transfers of 4 mid tubes into cherries - one MID appears per quadrant.
	 * @return results of component and transfer inserts
	 */
	private def make384MidFromTubesToCherries() = {
		doTubesTo384Plate(List(
			(midTubeID0, Quad.Q1, Some(Slice.CP), cherries96),
			(midTubeID1, Quad.Q2, Some(Slice.CP), cherries96),
			(midTubeID2, Quad.Q3, Some(Slice.CP), cherries96),
			(midTubeID3, Quad.Q4, Some(Slice.CP), cherries96)
		))
	}

	/**
	 * Transfer tubes into a 384-well plate
	 * @param tubes tubes to transfer (tubeID, QuadToTransferInto, SliceOfQuad)
	 * @return results of component and transfer inserts
	 */
	private def doTubesTo384Plate(tubes: List[(String, Quad.Quad, Option[Slice.Slice], Option[List[Int]])]) = {
		val pInsert = insertComponent(p384)
		val trans = tubes.map {
			case (tube, quad, slice, cherries) =>
				insertTransfer(Transfer(
					from = tube, to = p384.id,
					fromQuad = None, toQuad = Some(quad),
					project = None, slice = slice, cherries = cherries,
					isTubeToMany = true, isSampleOnly = false
				))
		}
		val futs = Future.sequence(pInsert :: trans)
		Await.result(futs, d3secs)
	}

	/**
	 * Make a 384 well plate to contain mids from plate in each quadrant via quadrant transfers.
	 * @return results of component and transfer inserts
	 */
	private def make384MidFrom96PlateToQuads() = {
		do96To384Plate(mid96, List(
			(Some(Quad.Q1), None, None), (Some(Quad.Q2), None, None),
			(Some(Quad.Q3), None, None), (Some(Quad.Q4), None, None)
		))
	}

	/**
	 * Make a 384 well plate to contain mids from plate in each quadrant via section transfers.
	 * @return results of component and transfer inserts
	 */
	private def make384MidFrom96PlateToSecs() = {
		do96To384Plate(mid96, List(
			(Some(Quad.Q1), Some(Slice.S5), None), (Some(Quad.Q1), Some(Slice.S6), None),
			(Some(Quad.Q2), Some(Slice.S5), None), (Some(Quad.Q2), Some(Slice.S6), None),
			(Some(Quad.Q3), Some(Slice.S5), None), (Some(Quad.Q3), Some(Slice.S6), None),
			(Some(Quad.Q4), Some(Slice.S5), None), (Some(Quad.Q4), Some(Slice.S6), None)
		))
	}

	/**
	 * Make a 384 well plate to contain mids from plate in each quadrant via CP transfers.
	 * @return results of component and transfer inserts
	 */
	private def make384MidFrom96PlateToCPs() = {
		do96To384Plate(mid96, List(
			(Some(Quad.Q1), Some(Slice.CP), cherries96), (Some(Quad.Q2), Some(Slice.CP), cherries96),
			(Some(Quad.Q3), Some(Slice.CP), cherries96), (Some(Quad.Q4), Some(Slice.CP), cherries96)
		))
	}

	/**
	 * Transfer from a 96 well plate to a 384 well plate
	 * @param from plate to transfer from
	 * @param transfers how to do transfers (toQuad, slice, cherries)
	 * @return results of component and transfer inserts
	 */
	private def do96To384Plate(
		from: Plate,
		transfers: List[(Option[Quad.Quad], Option[Slice.Slice], Option[List[Int]])]
	) =
		doPlateToPlate(
			from = from,
			to = p384,
			transfers = transfers.map {
				case (quad, slice, cp) => (None, quad, slice, cp)
			}
		)

	/**
	 * Transfer from a 384 well plate to a 96 well plate
	 * @param from plate to transfer from
	 * @param transfers how to do transfers (fromQuad, slice, cherries)
	 * @return results of component and transfer inserts
	 */
	private def do384To96Plate(
		from: Plate,
		transfers: List[(Option[Quad.Quad], Option[Slice.Slice], Option[List[Int]])]
	) =
		doPlateToPlate(
			from = from,
			to = p96,
			transfers = transfers.map {
				case (quad, slice, cp) => (quad, None, slice, cp)
			}
		)

	/**
	 * Transfer between plates.
	 * @param from plate to transfer from
	 * @param transfers how to do transfers (fromQuad, toQuad, slice, cherries)
	 * @return results of component and transfer inserts
	 */
	private def doPlateToPlate(from: Plate, to: Plate, transfers: List[(Option[Quad.Quad], Option[Quad.Quad], Option[Slice.Slice], Option[List[Int]])]) = {
		val fromInsert = insertComponent(from)
		val toInsert = insertComponent(to)
		val trans = transfers.map((tran) =>
			insertTransfer(Transfer(
				from = from.id, to = to.id,
				fromQuad = tran._1, toQuad = tran._2,
				project = None, slice = tran._3, cherries = tran._4,
				isTubeToMany = false, isSampleOnly = false
			)))
		val futs = Future.sequence(fromInsert :: toInsert :: trans)
		Await.result(futs, d3secs)
	}

	/**
	 * Get contents of a component, based on transfers into the component.
	 * @param id id of component
	 * @return contents of component
	 */
	private def getTransRes(id: String) =
		Await.result(TransferContents.getContents(id), d3secs)
}
