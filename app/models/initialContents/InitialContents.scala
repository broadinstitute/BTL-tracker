package models.initialContents

import formats.CustomFormats._
import models.{initialContents, ContainerDivisions}
import ContainerDivisions.Division._
import models.initialContents.MolecularBarcodes.MolBarcodeWell
import play.api.libs.json.Format

/**
 * InitiaContents - Created by nnovod on 2/18/15.
 *
 * Initial contents for a container.
 */
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
		// Plate of samples
		val SamplePlate = Value("Sample Plate")
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
		val molBarcodes = List(
			NexteraSetA,NexteraSetB,NexteraSetC,NexteraSetD,NexteraSetE,Nextera384SetA,TruGrade384Set1,
			TruGrade96Set1,TruGrade96Set2,TruGrade96Set3,TruGrade96Set4,SQM96SetA, SQM96SetAFlipped,
			TCRSetA, TCRSetB, HKSetA, HKSetB
		)

		/**
		 * List of valid plate contents
		 */
		val plateContents = SamplePlate :: molBarcodes

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
		def isMolBarcode(ct: ContentType.ContentType) = molBarcodes.contains(ct)

		/**
		 * Is content type a antibody?
		 * @param ct content type to check
		 * @return true if content type is a antibody
		 */
		def isAntibody(ct: ContentType.ContentType) = antiBodies.contains(ct)

		// Create Format for ComponentType enum using our custom enum Reader and Writer
		implicit val contentTypeFormat: Format[ContentType] =
			enumFormat(this)

		/**
		 * Get optional content type from string.  Set as None if missing or invalid.
		 * @param content content type as string
		 * @return optional content type found
		 */
		def getContentFromStr(content: Option[String]) =
			content match {
				case Some(key) => try {
					Some(ContentType.withName(key))
				} catch {
					case e: Throwable => None
				}
				case _ => None
			}

	}
	import ContentType._

	// Map of valid container sizes for different contents
	val validDivisions = Map[ContentType, List[Division]] (
		NexteraSetA -> List(DIM8x12),
		NexteraSetB -> List(DIM8x12),
		NexteraSetC -> List(DIM8x12),
		NexteraSetD -> List(DIM8x12),
		NexteraSetE -> List(DIM8x12),
		Nextera384SetA -> List(DIM16x24),
		TruGrade384Set1 -> List(DIM16x24),
		TruGrade96Set1 -> List(DIM8x12),
		TruGrade96Set2 -> List(DIM8x12),
		TruGrade96Set3 -> List(DIM8x12),
		TruGrade96Set4 -> List(DIM8x12),
		SQM96SetA -> List(DIM8x12),
		SQM96SetAFlipped -> List(DIM8x12),
		TCRSetA -> List(DIM16x24),
		TCRSetB -> List(DIM16x24),
		HKSetA -> List(DIM16x24),
		HKSetB -> List(DIM16x24),
		SamplePlate -> List(DIM8x12, DIM16x24)
	)

	/**
	 * Is the content valid for the division?
	 * @param content content of container
	 * @param div division type of container
	 * @return true if container with division can hold content
	 */
	def isContentValidForDivision(content: ContentType, div: Division) = validDivisions(content).contains(div)

	// Get contents for each type
	val contents = Map[ContentType, ContentsMap[MolBarcodeWell]] (
		NexteraSetA -> MolecularBarcodes.mbSetA,
		NexteraSetB -> MolecularBarcodes.mbSetB,
		NexteraSetC -> MolecularBarcodes.mbSetC,
		NexteraSetD -> MolecularBarcodes.mbSetD,
		NexteraSetE -> MolecularBarcodes.mbSetE,
		Nextera384SetA -> MolecularBarcodes.mbSet384A,
		TruGrade384Set1 -> MolecularBarcodes.mbTG384S1,
		TruGrade96Set1 -> MolecularBarcodes.mbTG96S1,
		TruGrade96Set2 -> MolecularBarcodes.mbTG96S2,
		TruGrade96Set3 -> MolecularBarcodes.mbTG96S3,
		TruGrade96Set4 -> MolecularBarcodes.mbTG96S4,
		SQM96SetA -> MolecularBarcodes.mbSQM96S1,
		SQM96SetAFlipped -> MolecularBarcodes.mbSQM96S1flipped,
		TCRSetA -> MolecularBarcodes.mbSet384TCellA,
		TCRSetB -> MolecularBarcodes.mbSet384TCellB,
		HKSetA -> MolecularBarcodes.mbSet384HKA,
		HKSetB -> MolecularBarcodes.mbSet384HKB
	)

	// Sorted list of display values for putting in drop down lists, etc
	def getContentDisplayValues(validContents: List[ContentType.ContentType]) = {
		// Contents to always display first
		val displayFirst = List(SamplePlate, BSPtubes, ABtubes)
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
	def getAllContentDisplayValues = getContentDisplayValues(ContentType.values.toList)

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
		ABRnaPolII -> AntiBodyData(1, "Mouse", true, false),
		ABH3K4me1 -> AntiBodyData(1, "Rabbit", true, false),
		ABH3K4me3 -> AntiBodyData(1, "Rabbit", true, false),
		ABH3K9me3 -> AntiBodyData(1, "Rabbit", true, false),
		ABH3K27ac -> AntiBodyData(1, "Rabbit", true, false),
		ABH3K27me3 -> AntiBodyData(1, "Rabbit", true, false),
		ABH3K36me3 -> AntiBodyData(1, "Rabbit", true, false),
		ABV5 -> AntiBodyData(5, "Mouse", true, false),
		ABBrd4 -> AntiBodyData(5, "Rabbit", false, false),
		ABMyb -> AntiBodyData(1, "Rabbit", true, false),
		ABH3Cntrl -> AntiBodyData(1, "Rabbit", true, true),
		ABSirt6 -> AntiBodyData(1, "Rabbit", true, false)
	)
}
