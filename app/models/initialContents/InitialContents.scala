package models.initialContents
import formats.CustomFormats._
import models.{BarcodeSet, ContainerDivisions, DBBarcodeSet}
import ContainerDivisions.Division._
import models.initialContents.MolecularBarcodes.MolBarcodeWell
import play.api.libs.json.Format
import reactivemongo.bson.BSONDocument

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * InitiaContents - Created by nnovod on 2/18/15.
 *
 * Initial contents for a container.
 */

// Initial contents implies you have contents that don't change while the server is running, which is not actually true
// anymore once you allow users to add barcodes.

object InitialContents {
	type ContentTypeT = String

	def getSetNames: List[String] =
		Await.result(DBBarcodeSet.getSetNames, Duration(30, SECONDS))


	object ContentType extends Enumeration {
		type ContentTypeFixed = Value
		// Molecular barcode sets (for placement in welled containers (e.g., plate))
		val NexteraSetA = Value("NexteraXP v2 Index Set A")
		val NexteraSetB = Value("NexteraXP v2 Index Set B")
		val NexteraSetC = Value("NexteraXP v2 Index Set C")
		val NexteraSetD = Value("NexteraXP v2 Index Set D")
		val NexteraSetE = Value("NexteraXP v2 Index Set E")
		val Nextera384SetA = Value("NexteraXP v2 Index 384-well Set A")
		val TruGrade384Set1 = Value("Trugrade 384-well Set 1")
		val TruGrade96Set1 = Value("Trugrade 96-well Set 1")
		val TruGrade96Set2 = Value("Trugrade 96-well Set 2")
		val TruGrade96Set3 = Value("Trugrade 96-well Set 3")
		val TruGrade96Set4 = Value("Trugrade 96-well Set 4")
		val SQM96SetA = Value("SQM Set A")
		val SQM96SetAFlipped = Value("SQM Set A Flipped")
		val TCRSetA = Value("T-cell 384-well Set 1")
		val TCRSetB = Value("T-cell 384-well Set 2")
		val HKSetA = Value("Housekeeping 384-well Set 1")
		val HKSetB = Value("Housekeeping 384-well Set 2")
		val MiRNA = Value("MiRNA P7 Set")
		val LIMG = Value("Low Input Metagenomic Set")

		val replace = List(
						NexteraSetA,NexteraSetB,NexteraSetC,NexteraSetD,NexteraSetE,Nextera384SetA,TruGrade384Set1,
						TruGrade96Set1,TruGrade96Set2,TruGrade96Set3,TruGrade96Set4,SQM96SetA, SQM96SetAFlipped,
						TCRSetA, TCRSetB, HKSetA, HKSetB, MiRNA, LIMG
					)
		// Plate of samples with sample Map
		val SamplePlate = Value("Sample Plate")
		// Plate of samples with no map
		val AnonymousSamplePlate = Value("Anonymous Sample Plate")
		// Antibodies (only for placement in single sample containers (e.g., tube))
		val ABRnaPolII = Value("RNAPolII")
		val ABH3K4me1 = Value("H3K4me1")
		val ABH3K4me3 = Value("H3K4me3")
		val ABH3K9me3 = Value("H3K9me3")
		val ABH3K27ac = Value("H3K27ac")
		val ABH3K27me3 = Value("H3K27me3")
		val ABH3K36me3 = Value("H3K36me3")
		val ABV5 = Value("V5")
		val ABBrd4 = Value("Brd4")
		val ABMyb = Value("Myb")
		val ABH3Cntrl = Value("H3")
		val ABSirt6 = Value("Sirt6")
		// Rack initial contents
		val BSPtubes = Value("BSP samples")
		val ABtubes = Value("Antibodies")

		/**
		 * List of all molecular barcode sets
		 */
		def molBarcodes = {
			Await.result(DBBarcodeSet.getSetNames, Duration(30, SECONDS))

		}

		/**
		 * List of valid plate contents
		 */
		def plateContents: List[ContentTypeT] = {
			//TODO: Here SamplePlate
			SamplePlate.toString :: AnonymousSamplePlate.toString :: molBarcodes
		}

		/**
		 * List of all antibodies
		 */
		val antiBodies = List(
			ABRnaPolII,ABH3K4me1,ABH3K4me3,ABH3K9me3,ABH3K27ac,ABH3K27me3,ABH3K36me3,ABV5,ABBrd4,
			ABMyb,ABH3Cntrl,ABSirt6
		)

		/**
		  * List of all rack tube types
		  */
		val rackTubes = List(BSPtubes, ABtubes)

		/**
		 * Is content type a molecular barcode set?
		 * @param ct content type to check
		 * @return true if content type is a molecular barcode set
		 */
		def isMolBarcode(ct: ContentTypeT): Boolean = molBarcodes.contains(ct)

		/**
		 * Is content type a antibody?
		 * @param ct content type to check
		 * @return true if content type is a antibody
		 */
		def isAntibody(ct: ContentTypeT): Boolean = antiBodies.contains(ct)

		// Create Format for ComponentType enum using our custom enum Reader and Writer
//		implicit val contentTypeFormat: Format[ContentTypeT] =
//			enumFormat(this)

		/**
		 * Get optional content type from string.  Set as None if missing or invalid.
		 * @param content content type as string
		 * @return optional content type found
		 */
		def getContentFromStr(content: Option[String]): Option[ContentTypeT] =
			content match {
				case Some(key) => try {
					Some(key)
				} catch {
					case _: Throwable => None
				}
				case _ => None
			}

	}
	import ContentType._

	// Map of valid container sizes for different contents
	private val fixedValidDivisions: Map[ContentTypeFixed, List[Division]] =
		Map[ContentTypeFixed, List[Division]] (
			SamplePlate -> List(DIM8x12, DIM16x24),
			AnonymousSamplePlate -> List(DIM8x12, DIM16x24)
		)

	/**
		* TODO: fill me out
		*
		* @return
		*/
	def validDivisions: Future[Map[String, List[Division]]] ={
		def findDivision(bs: BarcodeSet): Option[Division] = {
			val c = bs.contents.keys
			try {
				val not96 = c.filterNot(w => {
					validations.BarcodesValidation.BarcodeWellValidations.getWellParts(w) match {
					case Some((r, c)) =>
						val ruc = r.toUpperCase
						if (ruc.length !=1) throw new Exception("Well row designation not a single character.")
						val rucC = ruc.head
						if (rucC < 'A' || rucC > 'Z') throw new Exception("Well row not ASCII.")
						if (c <= 0) throw new Exception("Column is not valid.")
						rucC <= 'H' || c <= 12
					case None => throw new Exception("invalid barcode well.")
					}
				})
				if (not96.isEmpty) Some(DIM8x12)
				else {
					val not384 = not96.filterNot(w => {
						validations.BarcodesValidation.BarcodeWellValidations.getWellParts(w) match {
							case Some((r, c)) =>
								val ruc = r.toUpperCase
								val rucC = ruc.head
								rucC <= 'P' || c <= 24
							case None => throw new Exception("invalid barcode well.")
						}
					})
					if (not384.isEmpty) Some(DIM16x24)
					else None
				}
			} catch {
				case e: Exception => None
			}

		}
		val result = DBBarcodeSet.readAllSets()
		result.map(barcodeSet =>
			barcodeSet.map(bs =>
				bs.name ->
					(
						findDivision(bs) match {
							case Some(d) => List(d)
							case None => throw new Exception("Invalid well location.")
						}
					)
				// Here we finish making the first map sourced from DB and add to it the fixedValidDivisions map. To make the
				// contents consistent with validDivisions output, we convert the content of fixedValidDivisions to String.
			).toMap ++ fixedValidDivisions.map {
				case (ct, div) => ct.toString -> div
			}
		)
	}

	/**
	 * Is the content valid for the division?
	 * @param content content of container
	 * @param div division type of container
	 * @return true if container with division can hold content
	 */
	// Here, since the first argument is used only once, we use underscore instead of an explicit label. We are then
	// checking to see if the content(in this case a set name) is a valid division.
	def isContentValidForDivision(content: String, div: Division): Future[Boolean] = validDivisions.map(_(content).contains(div))

	// Get contents for each type
	def barcodeContents(setName: String): Future[ContentsMap[MolBarcodeWell]] = {
		DBBarcodeSet.readSet(setName)
	}

	// Sorted list of display values for putting in drop down lists, etc
	def getContentDisplayValues(validContents: List[ContentTypeT]): List[String] = {
		// Contents to always display first
		val displayFirst = List(SamplePlate, BSPtubes, AnonymousSamplePlate, ABtubes).map(_.toString)
		// Get group of contents in displayFirst list vs. rest of list
		val contentsByDisplayFirst = validContents.groupBy((ct) => displayFirst.contains(ct))
		// Sort contents we want sorted
		val emptyContent = List.empty[ContentTypeT]
		val contentsToSort = contentsByDisplayFirst.getOrElse(false, emptyContent)
		val sortedContents = contentsToSort.map(_.toString).sorted
		// Return display first contents followed by sorted contents
		contentsByDisplayFirst.getOrElse(true, emptyContent).map(_.toString) ++ sortedContents
	}

	// Get list of display value for all types
	def getAllContentDisplayValues: List[String] = {
		getContentDisplayValues(ContentType.values.map(_.toString).toList ++ getSetNames)
	}


	/**
	 * Initial contents
	 * @tparam C content class type
	 */
	trait ContentsMap[C] {
		// map of wells to contents
		val contents: Map[String, C]
	}

	/**
	 * Antibody data.
	 * @param volume Volume (uL) to use
	 * @param species species
	 * @param isMono mono or poly clonal
	 * @param isControl control antibody
	 */
	case class AntiBodyData(volume: Int, species: String, isMono: Boolean, isControl: Boolean)

	/**
	 * Map to get fixed information for each antibody
	 */
	lazy val antibodyMap = Map(
		ABRnaPolII -> AntiBodyData(volume = 1, species = "Mouse", isMono = true, isControl = false),
		ABH3K4me1 -> AntiBodyData(volume = 1, species = "Rabbit", isMono = true, isControl = false),
		ABH3K4me3 -> AntiBodyData(volume = 1, species = "Rabbit", isMono = true, isControl = false),
		ABH3K9me3 -> AntiBodyData(volume = 1, species = "Rabbit", isMono = true, isControl = false),
		ABH3K27ac -> AntiBodyData(volume = 1, species = "Rabbit", isMono = true, isControl = false),
		ABH3K27me3 -> AntiBodyData(volume = 1, species = "Rabbit", isMono = true, isControl = false),
		ABH3K36me3 -> AntiBodyData(volume = 1, species = "Rabbit", isMono = true, isControl = false),
		ABV5 -> AntiBodyData(volume = 5, species = "Mouse", isMono = true, isControl = false),
		ABBrd4 -> AntiBodyData(volume = 5, species = "Rabbit", isMono = false, isControl = false),
		ABMyb -> AntiBodyData(volume = 1, species = "Rabbit", isMono = true, isControl = false),
		ABH3Cntrl -> AntiBodyData(volume = 1, species = "Rabbit", isMono = true, isControl = true),
		ABSirt6 -> AntiBodyData(volume = 1, species = "Rabbit", isMono = true, isControl = false)
	)
}
