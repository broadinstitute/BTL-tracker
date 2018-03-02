import models.BarcodeSet._
import models.initialContents.MolecularBarcodes._
import org.specs2.mutable._
import models.{BarcodeSet, DBBarcodeSet}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import play.api.test._
import play.api.test.Helpers._
import reactivemongo.core.commands.LastError


class WriteHardcodedSets extends Specification {

	def writeSet(name: String, st: String, mbc: MolBarcodeContents): LastError = {
		val bs = BarcodeSet(
			name = name,
			setType = st,
			contents = mbc.contents
		)
		Await.result(DBBarcodeSet.writeSet(bs), Duration(10, SECONDS))
	}
	val NexteraSetA = "NexteraXP v2 Index Set A"
	val NexteraSetB = "NexteraXP v2 Index Set B"
	val NexteraSetC = "NexteraXP v2 Index Set C"
	val NexteraSetD = "NexteraXP v2 Index Set D"
	val NexteraSetE = "NexteraXP v2 Index Set E"
	val Nextera384SetA = "NexteraXP v2 Index 384-well Set A"
	val TruGrade384Set1 = "Trugrade 384-well Set 1"
	val TruGrade96Set1 = "Trugrade 96-well Set 1"
	val TruGrade96Set2 = "Trugrade 96-well Set 2"
	val TruGrade96Set3 = "Trugrade 96-well Set 3"
	val TruGrade96Set4 = "Trugrade 96-well Set 4"
	val SQM96SetA = "SQM Set A"
	val SQM96SetAFlipped = "SQM Set A Flipped"
	val TCRSetA = "T-cell 384-well Set 1"
	val TCRSetB = "T-cell 384-well Set 2"
	val HKSetA = "Housekeeping 384-well Set 1"
	val HKSetB = "Housekeeping 384-well Set 2"
	val MiRNA = "MiRNA P7 Set"
	val LIMG = "Low Input Metagenomic Set"

	val hcSets = List(mbSetA, mbSetB, mbSetC, mbSetD, mbSetE, mbSet384A, mbSet384TCellA, mbSet384TCellB, mbSet384HKA, mbSet384HKB, mbSet96LIMG, mbTG384S1, mbTG96S1, mbTG96S2, mbTG96S3, mbTG96S4, mbSQM96S1)
	val hcNames = List(NexteraSetA, NexteraSetB, NexteraSetC, NexteraSetD, NexteraSetE, Nextera384SetA, TCRSetA, TCRSetB, HKSetA, HKSetB, LIMG, TruGrade384Set1, TruGrade96Set1, TruGrade96Set2, TruGrade96Set3, TruGrade96Set4, SQM96SetA)
	      "all barcode sets should be written" in {
	        running(TestServer(3333)) {
						hcSets.length mustEqual hcNames.length
						val bcSets = hcNames zip hcSets
						val res = bcSets.map(s => {
							val setName = s._1
							val setContents = s._2
							writeSet(name = setName,
								mbc = setContents,
								st = {
									val well = setContents.contents.values.head
									well match {
										case npair: MolBarcodeNexteraPair => NEXTERA_PAIR
										case nsingle: MolBarcodeNexteraSingle => NEXTERA_SINGLE
										case single: MolBarcodeSingle => SINGLE
										case sqm: MolBarcodeSQMPair => SQM_PAIR
									}
								}
							)
						})
						val okays = res.map(_.ok)
						okays must not contain false
					 }
				}
}