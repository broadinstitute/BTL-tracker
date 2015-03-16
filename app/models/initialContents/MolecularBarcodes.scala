package models.initialContents

import models.Transfer
import models.initialContents.InitialContents.ContentsMap

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
	 * Common interface for molecular barcodes placed in wells
	 */
	trait MolBarcodeWell {
		def getName: String
		def getSeq: String
	}

	/**
	 * Molecular barcode pairs in a well
	 * @param i5 i5 barcode
	 * @param i7 i7 barcode
	 */
	case class MolBarcodePair(i5: MolBarcode, i7: MolBarcode) extends MolBarcodeWell {
		def getName = i5.name + "+" + i7.name
		def getSeq = i5.seq + "-" + i7.getRevCompliment + "-"
	}

	/**
	 * Molecular barcode in a well
 	 * @param m molecular barcode
	 */
	case class MolBarcodeSingle(m: MolBarcode) extends MolBarcodeWell {
		def getName = m.name
		def getSeq = m.seq
	}

	/**
	 * Contents for a plate of molecular barcodes.
	 * @param contents list of well contents
	 */
	case class MolBarcodeContents(contents: Map[String, MolBarcodeWell]) extends ContentsMap[MolBarcodeWell]

	// Create i5 Nextera Molecular barcodes
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

	// Create i7 Nextera Molecular barcodes
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

	/**
	 * Trait for MID plates made up pairs
 	 */
	private trait MIDPairPlate {
		// Wells per row
		val wPr: Int
		// Rows per plate
		val rPp: Int
		// List of well names (important to be lazy to get initialized at right time - in particular if makeSet
		// is called before it is initialized that's trouble if it's not lazy)
		lazy val wellList = List.tabulate(wPr * rPp)((x) => f"${(x / wPr) + 'A'}%c${(x % wPr) + 1}%02d")

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
				wellList((row * wPr) + col) -> MolBarcodePair(rows(row), cols(col))
			}).toMap
		}
	}

	/**
	 * 96-well MID plate with pairs
	 */
	private object MIDPair96 extends MIDPairPlate {
		val wPr = 12 // 12 Wells per row
		val rPp = 8  // 8 rows per plate
	}

	/**
	 * 384-well MID plate with pairs
	 */
	private object MIDPair384 extends MIDPairPlate {
		val wPr = 24 // 24 Wells per row
		val rPp = 16 // 8 rows per plate
	}

	// Set up row and column contents for Nextera paired barcodes - then making the sets are easy
	private val mbSetABRows = List(mbS502,mbS503,mbS505,mbS506,mbS507,mbS508,mbS510,mbS511)
	private val mbSetCDRows = List(mbS513,mbS515,mbS516,mbS517,mbS518,mbS520,mbS521,mbS522)
	private val mbSetACCols = List(mbN701,mbN702,mbN703,mbN704,mbN705,mbN706,mbN707,mbN710,mbN711,mbN712,mbN714,mbN715)
	private val mbSetBDCols = List(mbN716,mbN718,mbN719,mbN720,mbN721,mbN722,mbN723,mbN724,mbN726,mbN727,mbN728,mbN729)
	// NexteraXP v2 Index Set A
	val mbSetA = MolBarcodeContents(MIDPair96.makeSet(mbSetABRows,mbSetACCols))
	// NexteraXP v2 Index Set B
	val mbSetB = MolBarcodeContents(MIDPair96.makeSet(mbSetABRows,mbSetBDCols))
	// NexteraXP v2 Index Set C
	val mbSetC = MolBarcodeContents(MIDPair96.makeSet(mbSetCDRows,mbSetACCols))
	// NexteraXP v2 Index Set D
	val mbSetD = MolBarcodeContents(MIDPair96.makeSet(mbSetCDRows,mbSetBDCols))

	// Trugrade Set1 (384 well plate)
	val mbTG384S1 = MolBarcodeContents(Map(
		"A01" -> MolBarcodeSingle(MolBarcode("AAAACT", "A01")),
		"A02" -> MolBarcodeSingle(MolBarcode("AAAATC", "A02")),
		"A03" -> MolBarcodeSingle(MolBarcode("AAAGTT", "A03")),
		"A04" -> MolBarcodeSingle(MolBarcode("AAATAC", "A04")),
		"A05" -> MolBarcodeSingle(MolBarcode("AAATTG", "A05")),
		"A06" -> MolBarcodeSingle(MolBarcode("AACAAT", "A06")),
		"A07" -> MolBarcodeSingle(MolBarcode("AAGATT", "A07")),
		"A08" -> MolBarcodeSingle(MolBarcode("AAGTAT", "A08")),
		"A09" -> MolBarcodeSingle(MolBarcode("AATACA", "A09")),
		"A10" -> MolBarcodeSingle(MolBarcode("AATAGT", "A10")),
		"A11" -> MolBarcodeSingle(MolBarcode("AATCTT", "A11")),
		"A12" -> MolBarcodeSingle(MolBarcode("AATGAT", "A12")),
		"A13" -> MolBarcodeSingle(MolBarcode("AATTCT", "A13")),
		"A14" -> MolBarcodeSingle(MolBarcode("AATTGA", "A14")),
		"A15" -> MolBarcodeSingle(MolBarcode("ACAATA", "A15")),
		"A16" -> MolBarcodeSingle(MolBarcode("ACATAA", "A16")),
		"A17" -> MolBarcodeSingle(MolBarcode("ACTTAT", "A17")),
		"A18" -> MolBarcodeSingle(MolBarcode("ACTTTA", "A18")),
		"A19" -> MolBarcodeSingle(MolBarcode("AGATTA", "A19")),
		"A20" -> MolBarcodeSingle(MolBarcode("AGTAAT", "A20")),
		"A21" -> MolBarcodeSingle(MolBarcode("ATAAAC", "A21")),
		"A22" -> MolBarcodeSingle(MolBarcode("ATAACA", "A22")),
		"A23" -> MolBarcodeSingle(MolBarcode("ATACAA", "A23")),
		"A24" -> MolBarcodeSingle(MolBarcode("ATACTT", "A24")),
		"B01" -> MolBarcodeSingle(MolBarcode("AAACAT", "B01")),
		"B02" -> MolBarcodeSingle(MolBarcode("AAACTA", "B02")),
		"B03" -> MolBarcodeSingle(MolBarcode("AAATCA", "B03")),
		"B04" -> MolBarcodeSingle(MolBarcode("AAATGT", "B04")),
		"B05" -> MolBarcodeSingle(MolBarcode("AACATA", "B05")),
		"B06" -> MolBarcodeSingle(MolBarcode("AACTAA", "B06")),
		"B07" -> MolBarcodeSingle(MolBarcode("AAGTTA", "B07")),
		"B08" -> MolBarcodeSingle(MolBarcode("AATAAC", "B08")),
		"B09" -> MolBarcodeSingle(MolBarcode("AATATG", "B09")),
		"B10" -> MolBarcodeSingle(MolBarcode("AATCAA", "B10")),
		"B11" -> MolBarcodeSingle(MolBarcode("AATGTA", "B11")),
		"B12" -> MolBarcodeSingle(MolBarcode("AATTAG", "B12")),
		"B13" -> MolBarcodeSingle(MolBarcode("AATTTC", "B13")),
		"B14" -> MolBarcodeSingle(MolBarcode("ACAAAT", "B14")),
		"B15" -> MolBarcodeSingle(MolBarcode("ACTAAA", "B15")),
		"B16" -> MolBarcodeSingle(MolBarcode("ACTATT", "B16")),
		"B17" -> MolBarcodeSingle(MolBarcode("AGAATT", "B17")),
		"B18" -> MolBarcodeSingle(MolBarcode("AGATAT", "B18")),
		"B19" -> MolBarcodeSingle(MolBarcode("AGTATA", "B19")),
		"B20" -> MolBarcodeSingle(MolBarcode("AGTTAA", "B20")),
		"B21" -> MolBarcodeSingle(MolBarcode("ATAAGT", "B21")),
		"B22" -> MolBarcodeSingle(MolBarcode("ATAATG", "B22")),
		"B23" -> MolBarcodeSingle(MolBarcode("ATAGAT", "B23")),
		"B24" -> MolBarcodeSingle(MolBarcode("ATAGTA", "B24")),
		"C01" -> MolBarcodeSingle(MolBarcode("ATATAG", "C01")),
		"C02" -> MolBarcodeSingle(MolBarcode("ATATCT", "C02")),
		"C03" -> MolBarcodeSingle(MolBarcode("ATCAAA", "C03")),
		"C04" -> MolBarcodeSingle(MolBarcode("ATCATT", "C04")),
		"C05" -> MolBarcodeSingle(MolBarcode("ATGAAT", "C05")),
		"C06" -> MolBarcodeSingle(MolBarcode("ATGATA", "C06")),
		"C07" -> MolBarcodeSingle(MolBarcode("ATTACT", "C07")),
		"C08" -> MolBarcodeSingle(MolBarcode("ATTAGA", "C08")),
		"C09" -> MolBarcodeSingle(MolBarcode("ATTCTA", "C09")),
		"C10" -> MolBarcodeSingle(MolBarcode("ATTGAA", "C10")),
		"C11" -> MolBarcodeSingle(MolBarcode("ATTTCA", "C11")),
		"C12" -> MolBarcodeSingle(MolBarcode("ATTTGT", "C12")),
		"C13" -> MolBarcodeSingle(MolBarcode("CAAATA", "C13")),
		"C14" -> MolBarcodeSingle(MolBarcode("CAATAA", "C14")),
		"C15" -> MolBarcodeSingle(MolBarcode("CATTAT", "C15")),
		"C16" -> MolBarcodeSingle(MolBarcode("CATTTA", "C16")),
		"C17" -> MolBarcodeSingle(MolBarcode("CTATAT", "C17")),
		"C18" -> MolBarcodeSingle(MolBarcode("CTATTA", "C18")),
		"C19" -> MolBarcodeSingle(MolBarcode("CTTTAA", "C19")),
		"C20" -> MolBarcodeSingle(MolBarcode("GAAATT", "C20")),
		"C21" -> MolBarcodeSingle(MolBarcode("GATAAT", "C21")),
		"C22" -> MolBarcodeSingle(MolBarcode("GATATA", "C22")),
		"C23" -> MolBarcodeSingle(MolBarcode("GTAATA", "C23")),
		"C24" -> MolBarcodeSingle(MolBarcode("GTATAA", "C24")),
		"D01" -> MolBarcodeSingle(MolBarcode("ATATGA", "D01")),
		"D02" -> MolBarcodeSingle(MolBarcode("ATATTC", "D02")),
		"D03" -> MolBarcodeSingle(MolBarcode("ATCTAT", "D03")),
		"D04" -> MolBarcodeSingle(MolBarcode("ATCTTA", "D04")),
		"D05" -> MolBarcodeSingle(MolBarcode("ATGTAA", "D05")),
		"D06" -> MolBarcodeSingle(MolBarcode("ATTAAG", "D06")),
		"D07" -> MolBarcodeSingle(MolBarcode("ATTATC", "D07")),
		"D08" -> MolBarcodeSingle(MolBarcode("ATTCAT", "D08")),
		"D09" -> MolBarcodeSingle(MolBarcode("ATTGTT", "D09")),
		"D10" -> MolBarcodeSingle(MolBarcode("ATTTAC", "D10")),
		"D11" -> MolBarcodeSingle(MolBarcode("ATTTTG", "D11")),
		"D12" -> MolBarcodeSingle(MolBarcode("CAAAAT", "D12")),
		"D13" -> MolBarcodeSingle(MolBarcode("CATAAA", "D13")),
		"D14" -> MolBarcodeSingle(MolBarcode("CATATT", "D14")),
		"D15" -> MolBarcodeSingle(MolBarcode("CTAAAA", "D15")),
		"D16" -> MolBarcodeSingle(MolBarcode("CTAATT", "D16")),
		"D17" -> MolBarcodeSingle(MolBarcode("CTTAAT", "D17")),
		"D18" -> MolBarcodeSingle(MolBarcode("CTTATA", "D18")),
		"D19" -> MolBarcodeSingle(MolBarcode("GAATAT", "D19")),
		"D20" -> MolBarcodeSingle(MolBarcode("GAATTA", "D20")),
		"D21" -> MolBarcodeSingle(MolBarcode("GATTAA", "D21")),
		"D22" -> MolBarcodeSingle(MolBarcode("GTAAAT", "D22")),
		"D23" -> MolBarcodeSingle(MolBarcode("GTTAAA", "D23")),
		"D24" -> MolBarcodeSingle(MolBarcode("GTTATT", "D24")),
		"E01" -> MolBarcodeSingle(MolBarcode("GTTTAT", "E01")),
		"E02" -> MolBarcodeSingle(MolBarcode("GTTTTA", "E02")),
		"E03" -> MolBarcodeSingle(MolBarcode("TAAAGT", "E03")),
		"E04" -> MolBarcodeSingle(MolBarcode("TAAATG", "E04")),
		"E05" -> MolBarcodeSingle(MolBarcode("TAAGAT", "E05")),
		"E06" -> MolBarcodeSingle(MolBarcode("TAAGTA", "E06")),
		"E07" -> MolBarcodeSingle(MolBarcode("TAATGA", "E07")),
		"E08" -> MolBarcodeSingle(MolBarcode("TAATTC", "E08")),
		"E09" -> MolBarcodeSingle(MolBarcode("TACTAT", "E09")),
		"E10" -> MolBarcodeSingle(MolBarcode("TACTTA", "E10")),
		"E11" -> MolBarcodeSingle(MolBarcode("TAGTAA", "E11")),
		"E12" -> MolBarcodeSingle(MolBarcode("TAGTTT", "E12")),
		"E13" -> MolBarcodeSingle(MolBarcode("TATAGA", "E13")),
		"E14" -> MolBarcodeSingle(MolBarcode("TATATC", "E14")),
		"E15" -> MolBarcodeSingle(MolBarcode("TATGAA", "E15")),
		"E16" -> MolBarcodeSingle(MolBarcode("TATGTT", "E16")),
		"E17" -> MolBarcodeSingle(MolBarcode("TATTGT", "E17")),
		"E18" -> MolBarcodeSingle(MolBarcode("TATTTG", "E18")),
		"E19" -> MolBarcodeSingle(MolBarcode("TCATAT", "E19")),
		"E20" -> MolBarcodeSingle(MolBarcode("TCATTA", "E20")),
		"E21" -> MolBarcodeSingle(MolBarcode("TCTTAA", "E21")),
		"E22" -> MolBarcodeSingle(MolBarcode("TGAAAT", "E22")),
		"E23" -> MolBarcodeSingle(MolBarcode("TGATTT", "E23")),
		"E24" -> MolBarcodeSingle(MolBarcode("TGTAAA", "E24")),
		"F01" -> MolBarcodeSingle(MolBarcode("TAAAAC", "F01")),
		"F02" -> MolBarcodeSingle(MolBarcode("TAAACA", "F02")),
		"F03" -> MolBarcodeSingle(MolBarcode("TAACAA", "F03")),
		"F04" -> MolBarcodeSingle(MolBarcode("TAACTT", "F04")),
		"F05" -> MolBarcodeSingle(MolBarcode("TAATAG", "F05")),
		"F06" -> MolBarcodeSingle(MolBarcode("TAATCT", "F06")),
		"F07" -> MolBarcodeSingle(MolBarcode("TACAAA", "F07")),
		"F08" -> MolBarcodeSingle(MolBarcode("TACATT", "F08")),
		"F09" -> MolBarcodeSingle(MolBarcode("TAGAAT", "F09")),
		"F10" -> MolBarcodeSingle(MolBarcode("TAGATA", "F10")),
		"F11" -> MolBarcodeSingle(MolBarcode("TATAAG", "F11")),
		"F12" -> MolBarcodeSingle(MolBarcode("TATACT", "F12")),
		"F13" -> MolBarcodeSingle(MolBarcode("TATCAT", "F13")),
		"F14" -> MolBarcodeSingle(MolBarcode("TATCTA", "F14")),
		"F15" -> MolBarcodeSingle(MolBarcode("TATTAC", "F15")),
		"F16" -> MolBarcodeSingle(MolBarcode("TATTCA", "F16")),
		"F17" -> MolBarcodeSingle(MolBarcode("TCAAAA", "F17")),
		"F18" -> MolBarcodeSingle(MolBarcode("TCAATT", "F18")),
		"F19" -> MolBarcodeSingle(MolBarcode("TCTAAT", "F19")),
		"F20" -> MolBarcodeSingle(MolBarcode("TCTATA", "F20")),
		"F21" -> MolBarcodeSingle(MolBarcode("TGAATA", "F21")),
		"F22" -> MolBarcodeSingle(MolBarcode("TGATAA", "F22")),
		"F23" -> MolBarcodeSingle(MolBarcode("TGTATT", "F23")),
		"F24" -> MolBarcodeSingle(MolBarcode("TGTTAT", "F24")),
		"G01" -> MolBarcodeSingle(MolBarcode("TGTTTA", "G01")),
		"G02" -> MolBarcodeSingle(MolBarcode("TTAAAG", "G02")),
		"G03" -> MolBarcodeSingle(MolBarcode("TTAATC", "G03")),
		"G04" -> MolBarcodeSingle(MolBarcode("TTACAT", "G04")),
		"G05" -> MolBarcodeSingle(MolBarcode("TTAGTT", "G05")),
		"G06" -> MolBarcodeSingle(MolBarcode("TTATAC", "G06")),
		"G07" -> MolBarcodeSingle(MolBarcode("TTATTG", "G07")),
		"G08" -> MolBarcodeSingle(MolBarcode("TTCAAT", "G08")),
		"G09" -> MolBarcodeSingle(MolBarcode("TTGAAA", "G09")),
		"G10" -> MolBarcodeSingle(MolBarcode("TTGATT", "G10")),
		"G11" -> MolBarcodeSingle(MolBarcode("TTTACA", "G11")),
		"G12" -> MolBarcodeSingle(MolBarcode("TTTAGT", "G12")),
		"G13" -> MolBarcodeSingle(MolBarcode("TTTCTT", "G13")),
		"G14" -> MolBarcodeSingle(MolBarcode("TTTGTA", "G14")),
		"G15" -> MolBarcodeSingle(MolBarcode("TTTTGA", "G15")),
		"G16" -> MolBarcodeSingle(MolBarcode("TCTTTC", "G16")),
		"G17" -> MolBarcodeSingle(MolBarcode("AGACCT", "G17")),
		"G18" -> MolBarcodeSingle(MolBarcode("AGGGAT", "G18")),
		"G19" -> MolBarcodeSingle(MolBarcode("CACCAA", "G19")),
		"G20" -> MolBarcodeSingle(MolBarcode("CAGTCA", "G20")),
		"G21" -> MolBarcodeSingle(MolBarcode("CCACAT", "G21")),
		"G22" -> MolBarcodeSingle(MolBarcode("CCGATT", "G22")),
		"G23" -> MolBarcodeSingle(MolBarcode("CTAGTG", "G23")),
		"G24" -> MolBarcodeSingle(MolBarcode("CTTCTG", "G24")),
		"H01" -> MolBarcodeSingle(MolBarcode("TTAACT", "H01")),
		"H02" -> MolBarcodeSingle(MolBarcode("TTAAGA", "H02")),
		"H03" -> MolBarcodeSingle(MolBarcode("TTACTA", "H03")),
		"H04" -> MolBarcodeSingle(MolBarcode("TTAGAA", "H04")),
		"H05" -> MolBarcodeSingle(MolBarcode("TTATCA", "H05")),
		"H06" -> MolBarcodeSingle(MolBarcode("TTATGT", "H06")),
		"H07" -> MolBarcodeSingle(MolBarcode("TTCATA", "H07")),
		"H08" -> MolBarcodeSingle(MolBarcode("TTCTAA", "H08")),
		"H09" -> MolBarcodeSingle(MolBarcode("TTGTTA", "H09")),
		"H10" -> MolBarcodeSingle(MolBarcode("TTTAAC", "H10")),
		"H11" -> MolBarcodeSingle(MolBarcode("TTTATG", "H11")),
		"H12" -> MolBarcodeSingle(MolBarcode("TTTCAA", "H12")),
		"H13" -> MolBarcodeSingle(MolBarcode("TTTTAG", "H13")),
		"H14" -> MolBarcodeSingle(MolBarcode("TTTTCT", "H14")),
		"H15" -> MolBarcodeSingle(MolBarcode("TTGGAT", "H15")),
		"H16" -> MolBarcodeSingle(MolBarcode("ACCGTA", "H16")),
		"H17" -> MolBarcodeSingle(MolBarcode("ATCGAG", "H17")),
		"H18" -> MolBarcodeSingle(MolBarcode("CAAGCT", "H18")),
		"H19" -> MolBarcodeSingle(MolBarcode("CATCAG", "H19")),
		"H20" -> MolBarcodeSingle(MolBarcode("CATGGT", "H20")),
		"H21" -> MolBarcodeSingle(MolBarcode("CGACTT", "H21")),
		"H22" -> MolBarcodeSingle(MolBarcode("CGATTG", "H22")),
		"H23" -> MolBarcodeSingle(MolBarcode("GAAGAC", "H23")),
		"H24" -> MolBarcodeSingle(MolBarcode("GATCGT", "H24")),
		"I01" -> MolBarcodeSingle(MolBarcode("GCTAGA", "I01")),
		"I02" -> MolBarcodeSingle(MolBarcode("GCTTAC", "I02")),
		"I03" -> MolBarcodeSingle(MolBarcode("GGGATT", "I03")),
		"I04" -> MolBarcodeSingle(MolBarcode("GTACAC", "I04")),
		"I05" -> MolBarcodeSingle(MolBarcode("GTTCGA", "I05")),
		"I06" -> MolBarcodeSingle(MolBarcode("TAGTGG", "I06")),
		"I07" -> MolBarcodeSingle(MolBarcode("TCTGCA", "I07")),
		"I08" -> MolBarcodeSingle(MolBarcode("TTCCTC", "I08")),
		"I09" -> MolBarcodeSingle(MolBarcode("CCAACC", "I09")),
		"I10" -> MolBarcodeSingle(MolBarcode("CCTTCC", "I10")),
		"I11" -> MolBarcodeSingle(MolBarcode("GTACCG", "I11")),
		"I12" -> MolBarcodeSingle(MolBarcode("ACCCCC", "I12")),
		"I13" -> MolBarcodeSingle(MolBarcode("ACCGGC", "I13")),
		"I14" -> MolBarcodeSingle(MolBarcode("ACGCCG", "I14")),
		"I15" -> MolBarcodeSingle(MolBarcode("ACGGGG", "I15")),
		"I16" -> MolBarcodeSingle(MolBarcode("AGCCCG", "I16")),
		"I17" -> MolBarcodeSingle(MolBarcode("AGCGGG", "I17")),
		"I18" -> MolBarcodeSingle(MolBarcode("AGGCCC", "I18")),
		"I19" -> MolBarcodeSingle(MolBarcode("AGGGGC", "I19")),
		"I20" -> MolBarcodeSingle(MolBarcode("CACCCC", "I20")),
		"I21" -> MolBarcodeSingle(MolBarcode("CACGGC", "I21")),
		"I22" -> MolBarcodeSingle(MolBarcode("CAGCCG", "I22")),
		"I23" -> MolBarcodeSingle(MolBarcode("CAGGGG", "I23")),
		"I24" -> MolBarcodeSingle(MolBarcode("CCACCG", "I24")),
		"J01" -> MolBarcodeSingle(MolBarcode("GGACAT", "J01")),
		"J02" -> MolBarcodeSingle(MolBarcode("GGCAAT", "J02")),
		"J03" -> MolBarcodeSingle(MolBarcode("GTCAAG", "J03")),
		"J04" -> MolBarcodeSingle(MolBarcode("GTGACT", "J04")),
		"J05" -> MolBarcodeSingle(MolBarcode("TCCAAC", "J05")),
		"J06" -> MolBarcodeSingle(MolBarcode("TCGAAG", "J06")),
		"J07" -> MolBarcodeSingle(MolBarcode("TTGTCC", "J07")),
		"J08" -> MolBarcodeSingle(MolBarcode("TTTGGC", "J08")),
		"J09" -> MolBarcodeSingle(MolBarcode("CTCTCC", "J09")),
		"J10" -> MolBarcodeSingle(MolBarcode("GGACCA", "J10")),
		"J11" -> MolBarcodeSingle(MolBarcode("ACCCGG", "J11")),
		"J12" -> MolBarcodeSingle(MolBarcode("ACCGCG", "J12")),
		"J13" -> MolBarcodeSingle(MolBarcode("ACGCGC", "J13")),
		"J14" -> MolBarcodeSingle(MolBarcode("ACGGCC", "J14")),
		"J15" -> MolBarcodeSingle(MolBarcode("AGCCGC", "J15")),
		"J16" -> MolBarcodeSingle(MolBarcode("AGCGCC", "J16")),
		"J17" -> MolBarcodeSingle(MolBarcode("AGGCGG", "J17")),
		"J18" -> MolBarcodeSingle(MolBarcode("AGGGCG", "J18")),
		"J19" -> MolBarcodeSingle(MolBarcode("CACCGG", "J19")),
		"J20" -> MolBarcodeSingle(MolBarcode("CACGCG", "J20")),
		"J21" -> MolBarcodeSingle(MolBarcode("CAGCGC", "J21")),
		"J22" -> MolBarcodeSingle(MolBarcode("CAGGCC", "J22")),
		"J23" -> MolBarcodeSingle(MolBarcode("CCACGC", "J23")),
		"J24" -> MolBarcodeSingle(MolBarcode("CCAGGG", "J24")),
		"K01" -> MolBarcodeSingle(MolBarcode("CCCACG", "K01")),
		"K02" -> MolBarcodeSingle(MolBarcode("CCCAGC", "K02")),
		"K03" -> MolBarcodeSingle(MolBarcode("CCCCGT", "K03")),
		"K04" -> MolBarcodeSingle(MolBarcode("CCCCTG", "K04")),
		"K05" -> MolBarcodeSingle(MolBarcode("CCCTGG", "K05")),
		"K06" -> MolBarcodeSingle(MolBarcode("CCGAGG", "K06")),
		"K07" -> MolBarcodeSingle(MolBarcode("CCGGAC", "K07")),
		"K08" -> MolBarcodeSingle(MolBarcode("CCGGCA", "K08")),
		"K09" -> MolBarcodeSingle(MolBarcode("CCGTCG", "K09")),
		"K10" -> MolBarcodeSingle(MolBarcode("CCGTGC", "K10")),
		"K11" -> MolBarcodeSingle(MolBarcode("CCTGGC", "K11")),
		"K12" -> MolBarcodeSingle(MolBarcode("CGACCC", "K12")),
		"K13" -> MolBarcodeSingle(MolBarcode("CGAGGC", "K13")),
		"K14" -> MolBarcodeSingle(MolBarcode("CGCACC", "K14")),
		"K15" -> MolBarcodeSingle(MolBarcode("CGCCCT", "K15")),
		"K16" -> MolBarcodeSingle(MolBarcode("CGCCGA", "K16")),
		"K17" -> MolBarcodeSingle(MolBarcode("CGCGCA", "K17")),
		"K18" -> MolBarcodeSingle(MolBarcode("CGCGGT", "K18")),
		"K19" -> MolBarcodeSingle(MolBarcode("CGCTGC", "K19")),
		"K20" -> MolBarcodeSingle(MolBarcode("CGGACG", "K20")),
		"K21" -> MolBarcodeSingle(MolBarcode("CGGCCA", "K21")),
		"K22" -> MolBarcodeSingle(MolBarcode("CGGCGT", "K22")),
		"K23" -> MolBarcodeSingle(MolBarcode("CGGGCT", "K23")),
		"K24" -> MolBarcodeSingle(MolBarcode("CGGGGA", "K24")),
		"L01" -> MolBarcodeSingle(MolBarcode("CCCCAC", "L01")),
		"L02" -> MolBarcodeSingle(MolBarcode("CCCCCA", "L02")),
		"L03" -> MolBarcodeSingle(MolBarcode("CCCGAG", "L03")),
		"L04" -> MolBarcodeSingle(MolBarcode("CCCGGA", "L04")),
		"L05" -> MolBarcodeSingle(MolBarcode("CCGCAG", "L05")),
		"L06" -> MolBarcodeSingle(MolBarcode("CCGCGA", "L06")),
		"L07" -> MolBarcodeSingle(MolBarcode("CCGGGT", "L07")),
		"L08" -> MolBarcodeSingle(MolBarcode("CCGGTG", "L08")),
		"L09" -> MolBarcodeSingle(MolBarcode("CCTCGG", "L09")),
		"L10" -> MolBarcodeSingle(MolBarcode("CCTGCG", "L10")),
		"L11" -> MolBarcodeSingle(MolBarcode("CGACGG", "L11")),
		"L12" -> MolBarcodeSingle(MolBarcode("CGAGCG", "L12")),
		"L13" -> MolBarcodeSingle(MolBarcode("CGCAGG", "L13")),
		"L14" -> MolBarcodeSingle(MolBarcode("CGCCAG", "L14")),
		"L15" -> MolBarcodeSingle(MolBarcode("CGCCTC", "L15")),
		"L16" -> MolBarcodeSingle(MolBarcode("CGCGAC", "L16")),
		"L17" -> MolBarcodeSingle(MolBarcode("CGCGTG", "L17")),
		"L18" -> MolBarcodeSingle(MolBarcode("CGCTCG", "L18")),
		"L19" -> MolBarcodeSingle(MolBarcode("CGGAGC", "L19")),
		"L20" -> MolBarcodeSingle(MolBarcode("CGGCAC", "L20")),
		"L21" -> MolBarcodeSingle(MolBarcode("CGGCTG", "L21")),
		"L22" -> MolBarcodeSingle(MolBarcode("CGGGAG", "L22")),
		"L23" -> MolBarcodeSingle(MolBarcode("CGGGTC", "L23")),
		"L24" -> MolBarcodeSingle(MolBarcode("CGGTCC", "L24")),
		"M01" -> MolBarcodeSingle(MolBarcode("CGGTGG", "M01")),
		"M02" -> MolBarcodeSingle(MolBarcode("CGTCCG", "M02")),
		"M03" -> MolBarcodeSingle(MolBarcode("CGTGGG", "M03")),
		"M04" -> MolBarcodeSingle(MolBarcode("CTCCCG", "M04")),
		"M05" -> MolBarcodeSingle(MolBarcode("CTGCGG", "M05")),
		"M06" -> MolBarcodeSingle(MolBarcode("CTGGCG", "M06")),
		"M07" -> MolBarcodeSingle(MolBarcode("GACCGC", "M07")),
		"M08" -> MolBarcodeSingle(MolBarcode("GACGCC", "M08")),
		"M09" -> MolBarcodeSingle(MolBarcode("GAGCGG", "M09")),
		"M10" -> MolBarcodeSingle(MolBarcode("GAGGCG", "M10")),
		"M11" -> MolBarcodeSingle(MolBarcode("GCACGG", "M11")),
		"M12" -> MolBarcodeSingle(MolBarcode("GCAGCG", "M12")),
		"M13" -> MolBarcodeSingle(MolBarcode("GCCAGG", "M13")),
		"M14" -> MolBarcodeSingle(MolBarcode("GCCCAG", "M14")),
		"M15" -> MolBarcodeSingle(MolBarcode("GCCCTC", "M15")),
		"M16" -> MolBarcodeSingle(MolBarcode("GCCGAC", "M16")),
		"M17" -> MolBarcodeSingle(MolBarcode("GCCGTG", "M17")),
		"M18" -> MolBarcodeSingle(MolBarcode("GCCTCG", "M18")),
		"M19" -> MolBarcodeSingle(MolBarcode("GCGAGC", "M19")),
		"M20" -> MolBarcodeSingle(MolBarcode("GCGCAC", "M20")),
		"M21" -> MolBarcodeSingle(MolBarcode("GCGCTG", "M21")),
		"M22" -> MolBarcodeSingle(MolBarcode("GCGGAG", "M22")),
		"M23" -> MolBarcodeSingle(MolBarcode("GCGGTC", "M23")),
		"M24" -> MolBarcodeSingle(MolBarcode("GCGTCC", "M24")),
		"N01" -> MolBarcodeSingle(MolBarcode("CGTCGC", "N01")),
		"N02" -> MolBarcodeSingle(MolBarcode("CGTGCC", "N02")),
		"N03" -> MolBarcodeSingle(MolBarcode("CTCCGC", "N03")),
		"N04" -> MolBarcodeSingle(MolBarcode("CTCGGG", "N04")),
		"N05" -> MolBarcodeSingle(MolBarcode("CTGGGC", "N05")),
		"N06" -> MolBarcodeSingle(MolBarcode("GACCCG", "N06")),
		"N07" -> MolBarcodeSingle(MolBarcode("GACGGG", "N07")),
		"N08" -> MolBarcodeSingle(MolBarcode("GAGCCC", "N08")),
		"N09" -> MolBarcodeSingle(MolBarcode("GAGGGC", "N09")),
		"N10" -> MolBarcodeSingle(MolBarcode("GCACCC", "N10")),
		"N11" -> MolBarcodeSingle(MolBarcode("GCAGGC", "N11")),
		"N12" -> MolBarcodeSingle(MolBarcode("GCCACC", "N12")),
		"N13" -> MolBarcodeSingle(MolBarcode("GCCCCT", "N13")),
		"N14" -> MolBarcodeSingle(MolBarcode("GCCCGA", "N14")),
		"N15" -> MolBarcodeSingle(MolBarcode("GCCGCA", "N15")),
		"N16" -> MolBarcodeSingle(MolBarcode("GCCGGT", "N16")),
		"N17" -> MolBarcodeSingle(MolBarcode("GCCTGC", "N17")),
		"N18" -> MolBarcodeSingle(MolBarcode("GCGACG", "N18")),
		"N19" -> MolBarcodeSingle(MolBarcode("GCGCCA", "N19")),
		"N20" -> MolBarcodeSingle(MolBarcode("GCGCGT", "N20")),
		"N21" -> MolBarcodeSingle(MolBarcode("GCGGCT", "N21")),
		"N22" -> MolBarcodeSingle(MolBarcode("GCGGGA", "N22")),
		"N23" -> MolBarcodeSingle(MolBarcode("GCGTGG", "N23")),
		"N24" -> MolBarcodeSingle(MolBarcode("GCTCCG", "N24")),
		"O01" -> MolBarcodeSingle(MolBarcode("GCTCGC", "O01")),
		"O02" -> MolBarcodeSingle(MolBarcode("GCTGCC", "O02")),
		"O03" -> MolBarcodeSingle(MolBarcode("GGAGCC", "O03")),
		"O04" -> MolBarcodeSingle(MolBarcode("GGAGGG", "O04")),
		"O05" -> MolBarcodeSingle(MolBarcode("GGCCAC", "O05")),
		"O06" -> MolBarcodeSingle(MolBarcode("GGCGAG", "O06")),
		"O07" -> MolBarcodeSingle(MolBarcode("GGCGTC", "O07")),
		"O08" -> MolBarcodeSingle(MolBarcode("GGCTCC", "O08")),
		"O09" -> MolBarcodeSingle(MolBarcode("GGGCAG", "O09")),
		"O10" -> MolBarcodeSingle(MolBarcode("GGGCCT", "O10")),
		"O11" -> MolBarcodeSingle(MolBarcode("GGGGAC", "O11")),
		"O12" -> MolBarcodeSingle(MolBarcode("GGGGCA", "O12")),
		"O13" -> MolBarcodeSingle(MolBarcode("GGGTCG", "O13")),
		"O14" -> MolBarcodeSingle(MolBarcode("GGGTGC", "O14")),
		"O15" -> MolBarcodeSingle(MolBarcode("GGTGGC", "O15")),
		"O16" -> MolBarcodeSingle(MolBarcode("GTCCCC", "O16")),
		"O17" -> MolBarcodeSingle(MolBarcode("GTGCGC", "O17")),
		"O18" -> MolBarcodeSingle(MolBarcode("GTGGCC", "O18")),
		"O19" -> MolBarcodeSingle(MolBarcode("TCCCGC", "O19")),
		"O20" -> MolBarcodeSingle(MolBarcode("TCCGGG", "O20")),
		"O21" -> MolBarcodeSingle(MolBarcode("TCGGGC", "O21")),
		"O22" -> MolBarcodeSingle(MolBarcode("TGCCCC", "O22")),
		"O23" -> MolBarcodeSingle(MolBarcode("TGGCCG", "O23")),
		"O24" -> MolBarcodeSingle(MolBarcode("TGGCGC", "O24")),
		"P01" -> MolBarcodeSingle(MolBarcode("GCTGGG", "P01")),
		"P02" -> MolBarcodeSingle(MolBarcode("GGACGC", "P02")),
		"P03" -> MolBarcodeSingle(MolBarcode("GGCACG", "P03")),
		"P04" -> MolBarcodeSingle(MolBarcode("GGCAGC", "P04")),
		"P05" -> MolBarcodeSingle(MolBarcode("GGCGCT", "P05")),
		"P06" -> MolBarcodeSingle(MolBarcode("GGCGGA", "P06")),
		"P07" -> MolBarcodeSingle(MolBarcode("GGGACC", "P07")),
		"P08" -> MolBarcodeSingle(MolBarcode("GGGAGG", "P08")),
		"P09" -> MolBarcodeSingle(MolBarcode("GGGCGA", "P09")),
		"P10" -> MolBarcodeSingle(MolBarcode("GGGCTC", "P10")),
		"P11" -> MolBarcodeSingle(MolBarcode("GGGGGT", "P11")),
		"P12" -> MolBarcodeSingle(MolBarcode("GGGGTG", "P12")),
		"P13" -> MolBarcodeSingle(MolBarcode("GGTCCC", "P13")),
		"P14" -> MolBarcodeSingle(MolBarcode("GGTGCG", "P14")),
		"P15" -> MolBarcodeSingle(MolBarcode("GTCGCG", "P15")),
		"P16" -> MolBarcodeSingle(MolBarcode("GTCGGC", "P16")),
		"P17" -> MolBarcodeSingle(MolBarcode("GTGGGG", "P17")),
		"P18" -> MolBarcodeSingle(MolBarcode("TCCCCG", "P18")),
		"P19" -> MolBarcodeSingle(MolBarcode("TCGCGG", "P19")),
		"P20" -> MolBarcodeSingle(MolBarcode("TCGGCG", "P20")),
		"P21" -> MolBarcodeSingle(MolBarcode("TGCGCG", "P21")),
		"P22" -> MolBarcodeSingle(MolBarcode("TGCGGC", "P22")),
		"P23" -> MolBarcodeSingle(MolBarcode("TGGGCC", "P23")),
		"P24" -> MolBarcodeSingle(MolBarcode("TGGGGG", "P24"))
	))

	/**
	 * Trugrade 96-well MID plates are actually quadrants of their 384-well plate so we can create them by simply
	 * taking a quadrant of the 384-well plate
 	 * @param tranQuad map of 384-well plate wells to 96-well plate wells for quadrant being taken
	 * @return map of 96 well MIDs taken from quadrant of 384 well setup
	 */
	private def getTruGradeQuadriant(tranQuad: Map[String, String]) = {
		tranQuad.map {
			case (from, to) =>
				val origFrom = mbTG384S1.contents(from)
				// Take barcodes from 384-well plate and rename them to well they'll be in on 96-well plate since
				// name is simply the well where the MID resides
				val resetFrom = MolBarcodeSingle(MolBarcode(origFrom.getSeq, to))
				to -> resetFrom
		}
	}

	// Get 96-well TruGrade barcodes (quadrants of the 384-well plate)
	// TruGrade 96-well Set1
	val mbTG96S1 = MolBarcodeContents(getTruGradeQuadriant(Transfer.q1from384))
	// TruGrade 96-well Set2
	val mbTG96S2 = MolBarcodeContents(getTruGradeQuadriant(Transfer.q2from384))
	// TruGrade 96-well Set3
	val mbTG96S3 = MolBarcodeContents(getTruGradeQuadriant(Transfer.q3from384))
	// TruGrade 96-well Set4
	val mbTG96S4 = MolBarcodeContents(getTruGradeQuadriant(Transfer.q4from384))
}
