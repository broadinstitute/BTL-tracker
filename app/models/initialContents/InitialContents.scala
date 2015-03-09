package models.initialContents

import models.ContainerDivisions
import ContainerDivisions.Division._
import models.initialContents.MolecularBarcodes.MolBarcodeWell

/**
 * InitiaContents - Created by nnovod on 2/18/15.
 *
 * Initial contents for a container.  For now these are just Molecular Barcode sets but someday there can be more.
 */
object InitialContents {
	private val noContents = "none"

	object ContentType extends Enumeration {
		type ContentType = Value
		val NexteraSetA = Value("NexteraXP v2 Index Set A")
		val NexteraSetB = Value("NexteraXP v2 Index Set B")
		val NexteraSetC = Value("NexteraXP v2 Index Set C")
		val NexteraSetD = Value("NexteraXP v2 Index Set D")
		val TruGrade384Set1 = Value("Trugrade 384-well Set 1")
		val TruGrade96Set1 = Value("Trugrade 96-well Set 1")
		val TruGrade96Set2 = Value("Trugrade 96-well Set 2")
		val TruGrade96Set3 = Value("Trugrade 96-well Set 3")
		val TruGrade96Set4 = Value("Trugrade 96-well Set 4")
		val NoContents = Value(noContents)
	}
	import ContentType._

	// Map of valid container sizes for different contents
	// Note this must be lazy to avoid it being created before all ContentTypes are initialized - in particular
	// NoContents is not be initialized before this is set because val noContents is not initialized before
	// the object ContentType is setup
	lazy val validDivisions = Map[ContentType, List[Division]] (
		NexteraSetA -> List(DIM8x12),
		NexteraSetB -> List(DIM8x12),
		NexteraSetC -> List(DIM8x12),
		NexteraSetD -> List(DIM8x12),
		TruGrade384Set1 -> List(DIM16x24),
		TruGrade96Set1 -> List(DIM8x12),
		TruGrade96Set2 -> List(DIM8x12),
		TruGrade96Set3 -> List(DIM8x12),
		TruGrade96Set4 -> List(DIM8x12),
		NoContents -> List(DIM8x12, DIM16x24)
	)

	/**
	 * Is the content valid for the division?
	 * @param content content of container
	 * @param div division type of container
	 * @return true if container with division can hold content
	 */
	def isContentValidForDivision(content: ContentType, div: Division) = validDivisions(content).exists(_ == div)

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

	// Sorted list of display values for putting in drop down lists, etc. with no contents first
	def getContentDisplayValues(validContents: List[ContentType.ContentType]) =
		List(noContents) ++ validContents.map(_.toString).filterNot(_ == noContents).sorted

	/**
	 * Initial contents
	 * @tparam C content class type
	 */
	trait ContentsMap[C] {
		// map of wells to contents
		val contents: Map[String, C]
	}
}
