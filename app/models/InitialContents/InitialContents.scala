package models.InitialContents

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
		val Nothing = Value(noContents)
	}
	import ContentType._


	// Sorted list of display values for putting in drop down lists, etc.
	val contentDisplayValues = List(noContents) ++
		ContentType.values.map(_.toString).toList.filterNot(_ == noContents).sorted

	// Map of types to fake IDs we'll be using
	val	contentIDs = ContentType.values.map((v) => v -> ("@#" + v.toString + "#@")).toMap

	// Get contents for each type
	val contents = Map[ContentType, Contents] (
		NexteraSetA -> MolecularBarcodes.mbSetA,
		NexteraSetB -> MolecularBarcodes.mbSetB,
		NexteraSetC -> MolecularBarcodes.mbSetC,
		NexteraSetD -> MolecularBarcodes.mbSetD,
		Nothing -> NoContents
	)

	/**
	 * Initial contents
	 * @tparam C content class type
	 */
	trait Contents[C] {
		// Identifier for content
		val id: String
		// type of contents
		val contentType: ContentType.ContentType
		// list of contents
		val contents: List[C]
	}

	case object NoContents extends Contents[Any] {
		val id = contentIDs(Nothing)
		val contentType: ContentType = Nothing
		val contents: List[Any] = List.empty
	}
}