package models.initialContents
import formats.CustomFormats._
import models.{DBBarcodeSet, ContainerDivisions}
import ContainerDivisions.Division._
import models.DBBarcodeSet
import models.initialContents.MolecularBarcodes.MolBarcodeWell
import play.api.libs.json.Format
import reactivemongo.bson.BSONDocument
import scala.concurrent.Future

/**
 * InitiaContents - Created by nnovod on 2/18/15.
 *
 * Initial contents for a container.
 */

// TODO: I am starting to wonder if this entire initial contents concept will have to go with barcodes and sets being in DB.
// Initial contents implies you have contents that don't change while the server is running, which is not actually true
// anymore once you allow users to add barcodes.

object InitialContents {
	object ContentType extends Enumeration {
		type ContentType = Value
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
		//TODO: I suspect if we can populate molBarcodes from DB somehow it would go a long way towards getting away from
		// harcoded barcodes.
		val molBarcodes = List(
			NexteraSetA,NexteraSetB,NexteraSetC,NexteraSetD,NexteraSetE,Nextera384SetA,TruGrade384Set1,
			TruGrade96Set1,TruGrade96Set2,TruGrade96Set3,TruGrade96Set4,SQM96SetA, SQM96SetAFlipped,
			TCRSetA, TCRSetB, HKSetA, HKSetB, MiRNA, LIMG
		)

		/**
		 * List of valid plate contents
		 */
		val plateContents: List[ContentType] = {
			SamplePlate :: AnonymousSamplePlate :: molBarcodes

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
		def isMolBarcode(ct: ContentType): Boolean = molBarcodes.contains(ct)

		/**
		 * Is content type a antibody?
		 * @param ct content type to check
		 * @return true if content type is a antibody
		 */
		def isAntibody(ct: ContentType): Boolean = antiBodies.contains(ct)

		// Create Format for ComponentType enum using our custom enum Reader and Writer
		implicit val contentTypeFormat: Format[ContentType] =
			enumFormat(this)

		/**
		 * Get optional content type from string.  Set as None if missing or invalid.
		 * @param content content type as string
		 * @return optional content type found
		 */
		def getContentFromStr(content: Option[String]): Option[ContentType] =
			content match {
				case Some(key) => try {
					Some(ContentType.withName(key))
				} catch {
					case _: Throwable => None
				}
				case _ => None
			}

	}
	import ContentType._

	// Map of valid container sizes for different contents
	private val fixedValidDivisions: Map[ContentType, List[Division]] =
		Map[ContentType, List[Division]] (
			SamplePlate -> List(DIM8x12, DIM16x24),
			AnonymousSamplePlate -> List(DIM8x12, DIM16x24)
		)

	/**
		* TODO: fill me out
		*
		* @return
		*/
	def validDivisions: Future[Map[String, List[Division]]] ={
		val result = DBBarcodeSet.read(BSONDocument())
		result.map(barcodeSet =>
			barcodeSet.map(bs =>
				bs.name ->
					(
						if (bs.contents.size == 96) List(DIM8x12)
						else List(DIM16x24)
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
	def contents: Future[Map[String, ContentsMap[MolBarcodeWell]]] = {
		val result = DBBarcodeSet.read(BSONDocument())
		result.map(barcodeSet =>
			barcodeSet.map(bs =>
				bs.name ->
					(
						???
						)
				// Here we finish making the first map sourced from DB and add to it the fixedValidDivisions map. To make the
				// contents consistent with validDivisions output, we convert the content of fixedValidDivisions to String.
			).toMap
		)
//		Map[ContentType, ContentsMap[MolBarcodeWell]] (
//			NexteraSetA -> MolecularBarcodes.mbSetA,
//			NexteraSetB -> MolecularBarcodes.mbSetB,
//			NexteraSetC -> MolecularBarcodes.mbSetC,
//			NexteraSetD -> MolecularBarcodes.mbSetD,
//			NexteraSetE -> MolecularBarcodes.mbSetE,
//			Nextera384SetA -> MolecularBarcodes.mbSet384A,
//			TruGrade384Set1 -> MolecularBarcodes.mbTG384S1,
//			TruGrade96Set1 -> MolecularBarcodes.mbTG96S1,
//			TruGrade96Set2 -> MolecularBarcodes.mbTG96S2,
//			TruGrade96Set3 -> MolecularBarcodes.mbTG96S3,
//			TruGrade96Set4 -> MolecularBarcodes.mbTG96S4,
//			SQM96SetA -> MolecularBarcodes.mbSQM96S1,
//			SQM96SetAFlipped -> MolecularBarcodes.mbSQM96S1flipped,
//			TCRSetA -> MolecularBarcodes.mbSet384TCellA,
//			TCRSetB -> MolecularBarcodes.mbSet384TCellB,
//			HKSetA -> MolecularBarcodes.mbSet384HKA,
//			HKSetB -> MolecularBarcodes.mbSet384HKB,
//			MiRNA -> MolecularBarcodes.mbMiRNA,
//			LIMG -> MolecularBarcodes.mbSet96LIMG
//		)
	}



	// Sorted list of display values for putting in drop down lists, etc
	def getContentDisplayValues(validContents: List[ContentType.ContentType]): List[String] = {
		// Contents to always display first
		//TODO: 1. This populates Initial Content dropdown menu on the 'add' -> plate page. This will need to change so that we pull the values from the database.
		val displayFirst = List(SamplePlate, BSPtubes, AnonymousSamplePlate, ABtubes)
		// Get group of contents in displayFirst list vs. rest of list
		val contentsByDisplayFirst = validContents.groupBy((ct) => displayFirst.contains(ct))
		// Sort contents we want sorted
		val emptyContent = List.empty[InitialContents.ContentType.ContentType]
		val contentsToSort = contentsByDisplayFirst.getOrElse(false, emptyContent)
		val sortedContents = contentsToSort.map(_.toString).sorted
		// Return display first contents followed by sorted contents
		contentsByDisplayFirst.getOrElse(true, emptyContent).map(_.toString) ++ sortedContents
	}

	// Get list of display value for all types
	def getAllContentDisplayValues: List[String] = {
		//TODO: Thought this would populate dropdown from the DB but it doesn't seem to actually do so.
//		val setsFromDB = Await.result(BarcodeSet.BarcodeSet.read(BSONDocument()), 5.seconds).map(b => b.name.asInstanceOf[ContentType.Value])
//		val c = ContentType.values.toList
		getContentDisplayValues(ContentType.values.toList)
//		getContentDisplayValues(setsFromDB)
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
