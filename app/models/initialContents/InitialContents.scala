package models.initialContents

import models.ContainerDivisions
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
		val Nothing = Value(noContents)
	}
	import ContentType._

	// Map of types to fake IDs we'll be using
	val	contentIDs = ContentType.values.map((v) => v -> ("@#" + v.toString + "#@")).toMap

	// @TODO Will need to make sure contents are for proper size plate
	import ContainerDivisions.Division._
	val validDivisions = Map[ContentType, Division] (
		NexteraSetA -> DIM8x12,
		NexteraSetB -> DIM8x12,
		NexteraSetC -> DIM8x12,
		NexteraSetD -> DIM8x12,
		TruGrade384Set1 -> DIM16x24,
		TruGrade96Set1 -> DIM8x12,
		TruGrade96Set2 -> DIM8x12,
		TruGrade96Set3 -> DIM8x12,
		TruGrade96Set4 -> DIM8x12
	)

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
