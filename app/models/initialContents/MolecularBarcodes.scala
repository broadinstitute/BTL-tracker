package models.initialContents

import models.initialContents.InitialContents.Contents

/**
 * @author Nathaniel Novod
 *         Date: 2/19/15
 *         Time: 4:43 PM
 */
object MolecularBarcodes {

	/**
	 * Molecular barcode
	 * @param seq sequence
	 * @param name name
	 */
	case class MolBarcode(seq: String, name: String) {
		private val comp = Map('C' -> 'G','G' -> 'C','A' -> 'T','T' -> 'A')

		/**
		 * Create reverse compliment of original sequence
		 * @return original sequence complimented and reversed
		 */
		def getRevCompliment = {
			val complement = seq.map(comp)
			complement.reverse
		}
	}

	/**
	 * Molecular barcodes in a well
	 * @param i5 i5 barcode
	 * @param i7 i7 barcode
	 */
	case class MolBarcodePair(i5: MolBarcode, i7: MolBarcode) {
		def getName = i5.name + "+" + i7.name
	}

	/**
	 * Contents for a plate of molecular barcodes.
	 * @param contents list of well contents
	 */
	case class MolBarcodeContents(contents: Map[String, MolBarcodePair]) extends Contents[MolBarcodePair]

	// Create i5 Molecular barcode
	private val mbS502 = MolBarcode("CTCTCTAT","S502")
	private val mbS503 = MolBarcode("TATCCTCT","S503")
	private val mbS505 = MolBarcode("GTAAGGAG","S505")
	private val mbS506 = MolBarcode("ACTGCATA","S506")
	private val mbS507 = MolBarcode("AAGGAGTA","S507")
	private val mbS508 = MolBarcode("CTAAGCCT","S508")
	private val mbS510 = MolBarcode("CGTCTAAT","S510")
	private val mbS511 = MolBarcode("TCTCTCCG","S511")
	private val mbS513 = MolBarcode("TCGACTAG","S513")
	private val mbS515 = MolBarcode("TTCTAGCT","S515")
	private val mbS516 = MolBarcode("CCTAGAGT","S516")
	private val mbS517 = MolBarcode("GCGTAAGA","S517")
	private val mbS518 = MolBarcode("CTATTAAG","S518")
	private val mbS520 = MolBarcode("AAGGCTAT","S520")
	private val mbS521 = MolBarcode("GAGCCTTA","S521")
	private val mbS522 = MolBarcode("TTATGCGA","S522")

	// Create i7 Molecular barcodes
	private val mbN701 = MolBarcode("TCGCCTTA","N701")
	private val mbN702 = MolBarcode("CTAGTACG","N702")
	private val mbN703 = MolBarcode("TTCTGCCT","N703")
	private val mbN704 = MolBarcode("GCTCAGGA","N704")
	private val mbN705 = MolBarcode("AGGAGTCC","N705")
	private val mbN706 = MolBarcode("CATGCCTA","N706")
	private val mbN707 = MolBarcode("GTAGAGAG","N707")
	private val mbN710 = MolBarcode("CAGCCTCG","N710")
	private val mbN711 = MolBarcode("TGCCTCTT","N711")
	private val mbN712 = MolBarcode("TCCTCTAC","N712")
	private val mbN714 = MolBarcode("TCATGAGC","N714")
	private val mbN715 = MolBarcode("CCTGAGAT","N715")
	private val mbN716 = MolBarcode("TAGCGAGT","N716")
	private val mbN718 = MolBarcode("GTAGCTCC","N718")
	private val mbN719 = MolBarcode("TACTACGC","N719")
	private val mbN720 = MolBarcode("AGGCTCCG","N720")
	private val mbN721 = MolBarcode("GCAGCGTA","N721")
	private val mbN722 = MolBarcode("CTGCGCAT","N722")
	private val mbN723 = MolBarcode("GAGCGCTA","N723")
	private val mbN724 = MolBarcode("CGCTCAGT","N724")
	private val mbN726 = MolBarcode("GTCTTAGG","N726")
	private val mbN727 = MolBarcode("ACTGATCG","N727")
	private val mbN728 = MolBarcode("TAGCTGCA","N728")
	private val mbN729 = MolBarcode("GACGTCGA","N729")

	// Create 96-well names using list initialization and patterns
	private val wPr = 12 // Wells per row
	private val rPp = 8 // Rows per plate
	private val wellList = List.tabulate(wPr*rPp)((x) => f"${(x/wPr)+'A'}%c${(x%wPr)+1}%02d")

	/**
	 * Make a MID set
 	 * @param rows list of MIDs to go across rows
	 * @param cols list of MIDs to go across cols
	 * @return map of wells to MID pairs
	 */
	def makeSet(rows: List[MolBarcode], cols: List[MolBarcode]) = {
		(for {row <- 0 until rPp
			  col <- 0 until wPr
		} yield {
			wellList((row*wPr)+col) -> MolBarcodePair(rows(row), cols(col))
		}).toMap
	}

	private val mbSetARows = List(mbS502,mbS503,mbS505,mbS506,mbS507,mbS508,mbS510,mbS511)
	private val mbSetACols = List(mbN701,mbN702,mbN703,mbN704,mbN705,mbN706,mbN707,mbN710,mbN711,mbN712,mbN714,mbN715)
	val mbSetA = MolBarcodeContents(makeSet(mbSetARows,mbSetACols))

	private val mbSetBRows = List(mbS502,mbS503,mbS505,mbS506,mbS507,mbS508,mbS510,mbS511)
	private val mbSetBCols = List(mbN716,mbN718,mbN719,mbN720,mbN721,mbN722,mbN723,mbN724,mbN726,mbN727,mbN728,mbN729)
	val mbSetB = MolBarcodeContents(makeSet(mbSetBRows,mbSetBCols))

	private val mbSetCRows = List(mbS513,mbS515,mbS516,mbS517,mbS518,mbS520,mbS521,mbS522)
	private val mbSetCCols = List(mbN701,mbN702,mbN703,mbN704,mbN705,mbN706,mbN707,mbN710,mbN711,mbN712,mbN714,mbN715)
	val mbSetC = MolBarcodeContents(makeSet(mbSetCRows,mbSetCCols))

	private val mbSetDRows = List(mbS513,mbS515,mbS516,mbS517,mbS518,mbS520,mbS521,mbS522)
	private val mbSetDCols = List(mbN716,mbN718,mbN719,mbN720,mbN721,mbN722,mbN723,mbN724,mbN726,mbN727,mbN728,mbN729)
	val mbSetD = MolBarcodeContents(makeSet(mbSetDRows,mbSetDCols))
}
