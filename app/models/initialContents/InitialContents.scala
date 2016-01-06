package models.initialContents

import formats.CustomFormats._
import models.ContainerDivisions
import ContainerDivisions.Division._
import models.initialContents.MolecularBarcodes.MolBarcodeWell
import play.api.libs.json.Format

/**
 * InitiaContents - Created by nnovod on 2/18/15.
 *
 * Initial contents for a container.  For now these are just Molecular Barcode sets but someday there can be more.
 */
object InitialContents {
	object ContentType extends Enumeration {
		type ContentType = Value
		// Molecular barcode sets (for placement in welled containers (e.g., plate))
		val NexteraSetA = Value("NexteraXP v2 Index Set A")
		val NexteraSetB = Value("NexteraXP v2 Index Set B")
		val NexteraSetC = Value("NexteraXP v2 Index Set C")
		val NexteraSetD = Value("NexteraXP v2 Index Set D")
		val TruGrade384Set1 = Value("Trugrade 384-well Set 1")
		val TruGrade96Set1 = Value("Trugrade 96-well Set 1")
		val TruGrade96Set2 = Value("Trugrade 96-well Set 2")
		val TruGrade96Set3 = Value("Trugrade 96-well Set 3")
		val TruGrade96Set4 = Value("Trugrade 96-well Set 4")
		// Antibodies (only for placement in single sample containers (e.g., tube))
		val ABRnaPollII = Value("RNA-Pol-II")
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
			NexteraSetA,NexteraSetB,NexteraSetC,NexteraSetD,TruGrade384Set1,TruGrade96Set1,TruGrade96Set2,
			TruGrade96Set3,TruGrade96Set4
		)

		/**
		 * List of all antibodies
		 */
		val antiBodies = List(
			ABRnaPollII,ABH3K4me1,ABH3K4me3,ABH3K9me3,ABH3K27ac,ABH3K27me3,ABH3K36me3,ABV5,ABBrd4,
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
		TruGrade384Set1 -> List(DIM16x24),
		TruGrade96Set1 -> List(DIM8x12),
		TruGrade96Set2 -> List(DIM8x12),
		TruGrade96Set3 -> List(DIM8x12),
		TruGrade96Set4 -> List(DIM8x12)
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
		TruGrade384Set1 -> MolecularBarcodes.mbTG384S1,
		TruGrade96Set1 -> MolecularBarcodes.mbTG96S1,
		TruGrade96Set2 -> MolecularBarcodes.mbTG96S2,
		TruGrade96Set3 -> MolecularBarcodes.mbTG96S3,
		TruGrade96Set4 -> MolecularBarcodes.mbTG96S4
	)

	// Sorted list of display values for putting in drop down lists, etc
	def getContentDisplayValues(validContents: List[ContentType.ContentType]) =
		validContents.map(_.toString).sorted

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
	case class AntiBodyData(volume: Float, species: String, isMono: Boolean, isControl: Boolean)

	/**
	  * Map to get fixed information for each antibody
	  */
	lazy val antibodyMap = Map(
		ABRnaPollII -> AntiBodyData(1, "Mouse", true, false),
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
