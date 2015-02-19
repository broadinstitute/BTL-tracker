package models

import formats.CustomFormats._
import play.api.libs.json.{Json, Format}

/**
 * Created by nnovod on 2/18/15.
 * Initial contents for a container.  For now these are just Molecular Barcode sets but someday there can be more.
 */
trait Contents {
	object ContentType extends Enumeration {
		type ContentType = Value
		val NexteraSetA,NexteraSetB,NexteraSetC,NexteraSetD = Value

		// String list for displaying in dropdowns etc.
		val contentValues = ContentType.values.map {
			case NexteraSetA => "NexteraXP v2 Index Set A"
			case NexteraSetB => "NexteraXP v2 Index Set B"
			case NexteraSetC => "NexteraXP v2 Index Set C"
			case NexteraSetD => "NexteraXP v2 Index Set D"
			case t => t.toString
		}.toList
		// Map of types to fake IDs we'll be using
		val	contentIDs = ContentType.values.map((v) => (v -> ("@*" + v.toString + "*@")))
	}

	/**
	 * Initial contents
	 * @tparam C content class type
	 */
	trait Content[C] {
		// Identifier for content
		val id: String
		// type of contents
		val contentType: ContentType.ContentType
		// list of contents
		val contents: List[C]
	}

	/**
	 * Molecular barcode
 	 * @param seq sequence
	 * @param name name
	 */
	case class MolBarcode(seq: String, name: String) {
		private val comp = Map('C' -> 'G', 'G' -> 'C', 'A' -> 'T', 'T' -> 'A')
		def getRevCompliment = {
			val complement = seq.map(comp)
			complement.reverse
		}
	}

	/**
	 * Molecular barcodes in a well
	 * @param pos well position
	 * @param i5 i5 barcode
	 * @param i7 i7 barcode
	 */
	case class MolBarcodeWell(pos: String, i5: MolBarcode, i7: MolBarcode) {
		def getName = i5.name + "+" + i7.name
	}

	/**
	 * Contents for a plate of molecular barcodes.
	 * @param id id for plate
	 * @param contentType content type
	 * @param contents list of well contents
	 */
	case class MolBarcodeContents(id: String, contentType: ContentType.ContentType, contents: List[MolBarcodeWell])
		extends Content[MolBarcodeWell]

	/**
	 * Formatter for going to/from and validating Json
	 */
	// Supply our custom enum Reader and Writer for content type enum
	implicit val contentTypeFormat: Format[ContentType.ContentType] = enumFormat(ContentType)
	implicit val molBarcodeFormatter = Json.format[MolBarcode]
	implicit val molBarcodeWellFormatter = Json.format[MolBarcodeWell]
	implicit val molBarcodeContentsFormatter = Json.format[MolBarcodeContents]
}
