package models.InitialContents

import formats.CustomFormats._
import play.api.libs.json.{Json,Format}
import InitialContents._

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
		extends Contents[MolBarcodeWell]

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

	private val mbSetAA01 = MolBarcodeWell("A01",mbS502,mbN701)
	private val mbSetAB01 = MolBarcodeWell("B01",mbS503,mbN701)
	private val mbSetAC01 = MolBarcodeWell("C01",mbS505,mbN701)
	private val mbSetAD01 = MolBarcodeWell("D01",mbS506,mbN701)
	private val mbSetAE01 = MolBarcodeWell("E01",mbS507,mbN701)
	private val mbSetAF01 = MolBarcodeWell("F01",mbS508,mbN701)
	private val mbSetAG01 = MolBarcodeWell("G01",mbS510,mbN701)
	private val mbSetAH01 = MolBarcodeWell("H01",mbS511,mbN701)
	private val mbSetAA02 = MolBarcodeWell("A02",mbS502,mbN702)
	private val mbSetAB02 = MolBarcodeWell("B02",mbS503,mbN702)
	private val mbSetAC02 = MolBarcodeWell("C02",mbS505,mbN702)
	private val mbSetAD02 = MolBarcodeWell("D02",mbS506,mbN702)
	private val mbSetAE02 = MolBarcodeWell("E02",mbS507,mbN702)
	private val mbSetAF02 = MolBarcodeWell("F02",mbS508,mbN702)
	private val mbSetAG02 = MolBarcodeWell("G02",mbS510,mbN702)
	private val mbSetAH02 = MolBarcodeWell("H02",mbS511,mbN702)
	private val mbSetAA03 = MolBarcodeWell("A03",mbS502,mbN703)
	private val mbSetAB03 = MolBarcodeWell("B03",mbS503,mbN703)
	private val mbSetAC03 = MolBarcodeWell("C03",mbS505,mbN703)
	private val mbSetAD03 = MolBarcodeWell("D03",mbS506,mbN703)
	private val mbSetAE03 = MolBarcodeWell("E03",mbS507,mbN703)
	private val mbSetAF03 = MolBarcodeWell("F03",mbS508,mbN703)
	private val mbSetAG03 = MolBarcodeWell("G03",mbS510,mbN703)
	private val mbSetAH03 = MolBarcodeWell("H03",mbS511,mbN703)
	private val mbSetAA04 = MolBarcodeWell("A04",mbS502,mbN704)
	private val mbSetAB04 = MolBarcodeWell("B04",mbS503,mbN704)
	private val mbSetAC04 = MolBarcodeWell("C04",mbS505,mbN704)
	private val mbSetAD04 = MolBarcodeWell("D04",mbS506,mbN704)
	private val mbSetAE04 = MolBarcodeWell("E04",mbS507,mbN704)
	private val mbSetAF04 = MolBarcodeWell("F04",mbS508,mbN704)
	private val mbSetAG04 = MolBarcodeWell("G04",mbS510,mbN704)
	private val mbSetAH04 = MolBarcodeWell("H04",mbS511,mbN704)
	private val mbSetAA05 = MolBarcodeWell("A05",mbS502,mbN705)
	private val mbSetAB05 = MolBarcodeWell("B05",mbS503,mbN705)
	private val mbSetAC05 = MolBarcodeWell("C05",mbS505,mbN705)
	private val mbSetAD05 = MolBarcodeWell("D05",mbS506,mbN705)
	private val mbSetAE05 = MolBarcodeWell("E05",mbS507,mbN705)
	private val mbSetAF05 = MolBarcodeWell("F05",mbS508,mbN705)
	private val mbSetAG05 = MolBarcodeWell("G05",mbS510,mbN705)
	private val mbSetAH05 = MolBarcodeWell("H05",mbS511,mbN705)
	private val mbSetAA06 = MolBarcodeWell("A06",mbS502,mbN706)
	private val mbSetAB06 = MolBarcodeWell("B06",mbS503,mbN706)
	private val mbSetAC06 = MolBarcodeWell("C06",mbS505,mbN706)
	private val mbSetAD06 = MolBarcodeWell("D06",mbS506,mbN706)
	private val mbSetAE06 = MolBarcodeWell("E06",mbS507,mbN706)
	private val mbSetAF06 = MolBarcodeWell("F06",mbS508,mbN706)
	private val mbSetAG06 = MolBarcodeWell("G06",mbS510,mbN706)
	private val mbSetAH06 = MolBarcodeWell("H06",mbS511,mbN706)
	private val mbSetAA07 = MolBarcodeWell("A07",mbS502,mbN707)
	private val mbSetAB07 = MolBarcodeWell("B07",mbS503,mbN707)
	private val mbSetAC07 = MolBarcodeWell("C07",mbS505,mbN707)
	private val mbSetAD07 = MolBarcodeWell("D07",mbS506,mbN707)
	private val mbSetAE07 = MolBarcodeWell("E07",mbS507,mbN707)
	private val mbSetAF07 = MolBarcodeWell("F07",mbS508,mbN707)
	private val mbSetAG07 = MolBarcodeWell("G07",mbS510,mbN707)
	private val mbSetAH07 = MolBarcodeWell("H07",mbS511,mbN707)
	private val mbSetAA08 = MolBarcodeWell("A08",mbS502,mbN710)
	private val mbSetAB08 = MolBarcodeWell("B08",mbS503,mbN710)
	private val mbSetAC08 = MolBarcodeWell("C08",mbS505,mbN710)
	private val mbSetAD08 = MolBarcodeWell("D08",mbS506,mbN710)
	private val mbSetAE08 = MolBarcodeWell("E08",mbS507,mbN710)
	private val mbSetAF08 = MolBarcodeWell("F08",mbS508,mbN710)
	private val mbSetAG08 = MolBarcodeWell("G08",mbS510,mbN710)
	private val mbSetAH08 = MolBarcodeWell("H08",mbS511,mbN710)
	private val mbSetAA09 = MolBarcodeWell("A09",mbS502,mbN711)
	private val mbSetAB09 = MolBarcodeWell("B09",mbS503,mbN711)
	private val mbSetAC09 = MolBarcodeWell("C09",mbS505,mbN711)
	private val mbSetAD09 = MolBarcodeWell("D09",mbS506,mbN711)
	private val mbSetAE09 = MolBarcodeWell("E09",mbS507,mbN711)
	private val mbSetAF09 = MolBarcodeWell("F09",mbS508,mbN711)
	private val mbSetAG09 = MolBarcodeWell("G09",mbS510,mbN711)
	private val mbSetAH09 = MolBarcodeWell("H09",mbS511,mbN711)
	private val mbSetAA10 = MolBarcodeWell("A10",mbS502,mbN712)
	private val mbSetAB10 = MolBarcodeWell("B10",mbS503,mbN712)
	private val mbSetAC10 = MolBarcodeWell("C10",mbS505,mbN712)
	private val mbSetAD10 = MolBarcodeWell("D10",mbS506,mbN712)
	private val mbSetAE10 = MolBarcodeWell("E10",mbS507,mbN712)
	private val mbSetAF10 = MolBarcodeWell("F10",mbS508,mbN712)
	private val mbSetAG10 = MolBarcodeWell("G10",mbS510,mbN712)
	private val mbSetAH10 = MolBarcodeWell("H10",mbS511,mbN712)
	private val mbSetAA11 = MolBarcodeWell("A11",mbS502,mbN714)
	private val mbSetAB11 = MolBarcodeWell("B11",mbS503,mbN714)
	private val mbSetAC11 = MolBarcodeWell("C11",mbS505,mbN714)
	private val mbSetAD11 = MolBarcodeWell("D11",mbS506,mbN714)
	private val mbSetAE11 = MolBarcodeWell("E11",mbS507,mbN714)
	private val mbSetAF11 = MolBarcodeWell("F11",mbS508,mbN714)
	private val mbSetAG11 = MolBarcodeWell("G11",mbS510,mbN714)
	private val mbSetAH11 = MolBarcodeWell("H11",mbS511,mbN714)
	private val mbSetAA12 = MolBarcodeWell("A12",mbS502,mbN715)
	private val mbSetAB12 = MolBarcodeWell("B12",mbS503,mbN715)
	private val mbSetAC12 = MolBarcodeWell("C12",mbS505,mbN715)
	private val mbSetAD12 = MolBarcodeWell("D12",mbS506,mbN715)
	private val mbSetAE12 = MolBarcodeWell("E12",mbS507,mbN715)
	private val mbSetAF12 = MolBarcodeWell("F12",mbS508,mbN715)
	private val mbSetAG12 = MolBarcodeWell("G12",mbS510,mbN715)
	private val mbSetAH12 = MolBarcodeWell("H12",mbS511,mbN715)

	val mbSetA = MolBarcodeContents(contentIDs(ContentType.NexteraSetA),ContentType.NexteraSetA,
		List(mbSetAA01,mbSetAB01,mbSetAC01,mbSetAD01,mbSetAE01,mbSetAF01,mbSetAG01,mbSetAH01,
			mbSetAA02,mbSetAB02,mbSetAC02,mbSetAD02,mbSetAE02,mbSetAF02,mbSetAG02,mbSetAH02,
			mbSetAA03,mbSetAB03,mbSetAC03,mbSetAD03,mbSetAE03,mbSetAF03,mbSetAG03,mbSetAH03,
			mbSetAA04,mbSetAB04,mbSetAC04,mbSetAD04,mbSetAE04,mbSetAF04,mbSetAG04,mbSetAH04,
			mbSetAA05,mbSetAB05,mbSetAC05,mbSetAD05,mbSetAE05,mbSetAF05,mbSetAG05,mbSetAH05,
			mbSetAA06,mbSetAB06,mbSetAC06,mbSetAD06,mbSetAE06,mbSetAF06,mbSetAG06,mbSetAH06,
			mbSetAA07,mbSetAB07,mbSetAC07,mbSetAD07,mbSetAE07,mbSetAF07,mbSetAG07,mbSetAH07,
			mbSetAA08,mbSetAB08,mbSetAC08,mbSetAD08,mbSetAE08,mbSetAF08,mbSetAG08,mbSetAH08,
			mbSetAA09,mbSetAB09,mbSetAC09,mbSetAD09,mbSetAE09,mbSetAF09,mbSetAG09,mbSetAH09,
			mbSetAA10,mbSetAB10,mbSetAC10,mbSetAD10,mbSetAE10,mbSetAF10,mbSetAG10,mbSetAH10,
			mbSetAA11,mbSetAB11,mbSetAC11,mbSetAD11,mbSetAE11,mbSetAF11,mbSetAG11,mbSetAH11,
			mbSetAA12,mbSetAB12,mbSetAC12,mbSetAD12,mbSetAE12,mbSetAF12,mbSetAG12,mbSetAH12))

	private val mbSetBA01 = MolBarcodeWell("A01",mbS502,mbN716)
	private val mbSetBB01 = MolBarcodeWell("B01",mbS503,mbN716)
	private val mbSetBC01 = MolBarcodeWell("C01",mbS505,mbN716)
	private val mbSetBD01 = MolBarcodeWell("D01",mbS506,mbN716)
	private val mbSetBE01 = MolBarcodeWell("E01",mbS507,mbN716)
	private val mbSetBF01 = MolBarcodeWell("F01",mbS508,mbN716)
	private val mbSetBG01 = MolBarcodeWell("G01",mbS510,mbN716)
	private val mbSetBH01 = MolBarcodeWell("H01",mbS511,mbN716)
	private val mbSetBA02 = MolBarcodeWell("A02",mbS502,mbN718)
	private val mbSetBB02 = MolBarcodeWell("B02",mbS503,mbN718)
	private val mbSetBC02 = MolBarcodeWell("C02",mbS505,mbN718)
	private val mbSetBD02 = MolBarcodeWell("D02",mbS506,mbN718)
	private val mbSetBE02 = MolBarcodeWell("E02",mbS507,mbN718)
	private val mbSetBF02 = MolBarcodeWell("F02",mbS508,mbN718)
	private val mbSetBG02 = MolBarcodeWell("G02",mbS510,mbN718)
	private val mbSetBH02 = MolBarcodeWell("H02",mbS511,mbN718)
	private val mbSetBA03 = MolBarcodeWell("A03",mbS502,mbN719)
	private val mbSetBB03 = MolBarcodeWell("B03",mbS503,mbN719)
	private val mbSetBC03 = MolBarcodeWell("C03",mbS505,mbN719)
	private val mbSetBD03 = MolBarcodeWell("D03",mbS506,mbN719)
	private val mbSetBE03 = MolBarcodeWell("E03",mbS507,mbN719)
	private val mbSetBF03 = MolBarcodeWell("F03",mbS508,mbN719)
	private val mbSetBG03 = MolBarcodeWell("G03",mbS510,mbN719)
	private val mbSetBH03 = MolBarcodeWell("H03",mbS511,mbN719)
	private val mbSetBA04 = MolBarcodeWell("A04",mbS502,mbN720)
	private val mbSetBB04 = MolBarcodeWell("B04",mbS503,mbN720)
	private val mbSetBC04 = MolBarcodeWell("C04",mbS505,mbN720)
	private val mbSetBD04 = MolBarcodeWell("D04",mbS506,mbN720)
	private val mbSetBE04 = MolBarcodeWell("E04",mbS507,mbN720)
	private val mbSetBF04 = MolBarcodeWell("F04",mbS508,mbN720)
	private val mbSetBG04 = MolBarcodeWell("G04",mbS510,mbN720)
	private val mbSetBH04 = MolBarcodeWell("H04",mbS511,mbN720)
	private val mbSetBA05 = MolBarcodeWell("A05",mbS502,mbN721)
	private val mbSetBB05 = MolBarcodeWell("B05",mbS503,mbN721)
	private val mbSetBC05 = MolBarcodeWell("C05",mbS505,mbN721)
	private val mbSetBD05 = MolBarcodeWell("D05",mbS506,mbN721)
	private val mbSetBE05 = MolBarcodeWell("E05",mbS507,mbN721)
	private val mbSetBF05 = MolBarcodeWell("F05",mbS508,mbN721)
	private val mbSetBG05 = MolBarcodeWell("G05",mbS510,mbN721)
	private val mbSetBH05 = MolBarcodeWell("H05",mbS511,mbN721)
	private val mbSetBA06 = MolBarcodeWell("A06",mbS502,mbN722)
	private val mbSetBB06 = MolBarcodeWell("B06",mbS503,mbN722)
	private val mbSetBC06 = MolBarcodeWell("C06",mbS505,mbN722)
	private val mbSetBD06 = MolBarcodeWell("D06",mbS506,mbN722)
	private val mbSetBE06 = MolBarcodeWell("E06",mbS507,mbN722)
	private val mbSetBF06 = MolBarcodeWell("F06",mbS508,mbN722)
	private val mbSetBG06 = MolBarcodeWell("G06",mbS510,mbN722)
	private val mbSetBH06 = MolBarcodeWell("H06",mbS511,mbN722)
	private val mbSetBA07 = MolBarcodeWell("A07",mbS502,mbN723)
	private val mbSetBB07 = MolBarcodeWell("B07",mbS503,mbN723)
	private val mbSetBC07 = MolBarcodeWell("C07",mbS505,mbN723)
	private val mbSetBD07 = MolBarcodeWell("D07",mbS506,mbN723)
	private val mbSetBE07 = MolBarcodeWell("E07",mbS507,mbN723)
	private val mbSetBF07 = MolBarcodeWell("F07",mbS508,mbN723)
	private val mbSetBG07 = MolBarcodeWell("G07",mbS510,mbN723)
	private val mbSetBH07 = MolBarcodeWell("H07",mbS511,mbN723)
	private val mbSetBA08 = MolBarcodeWell("A08",mbS502,mbN724)
	private val mbSetBB08 = MolBarcodeWell("B08",mbS503,mbN724)
	private val mbSetBC08 = MolBarcodeWell("C08",mbS505,mbN724)
	private val mbSetBD08 = MolBarcodeWell("D08",mbS506,mbN724)
	private val mbSetBE08 = MolBarcodeWell("E08",mbS507,mbN724)
	private val mbSetBF08 = MolBarcodeWell("F08",mbS508,mbN724)
	private val mbSetBG08 = MolBarcodeWell("G08",mbS510,mbN724)
	private val mbSetBH08 = MolBarcodeWell("H08",mbS511,mbN724)
	private val mbSetBA09 = MolBarcodeWell("A09",mbS502,mbN726)
	private val mbSetBB09 = MolBarcodeWell("B09",mbS503,mbN726)
	private val mbSetBC09 = MolBarcodeWell("C09",mbS505,mbN726)
	private val mbSetBD09 = MolBarcodeWell("D09",mbS506,mbN726)
	private val mbSetBE09 = MolBarcodeWell("E09",mbS507,mbN726)
	private val mbSetBF09 = MolBarcodeWell("F09",mbS508,mbN726)
	private val mbSetBG09 = MolBarcodeWell("G09",mbS510,mbN726)
	private val mbSetBH09 = MolBarcodeWell("H09",mbS511,mbN726)
	private val mbSetBA10 = MolBarcodeWell("A10",mbS502,mbN727)
	private val mbSetBB10 = MolBarcodeWell("B10",mbS503,mbN727)
	private val mbSetBC10 = MolBarcodeWell("C10",mbS505,mbN727)
	private val mbSetBD10 = MolBarcodeWell("D10",mbS506,mbN727)
	private val mbSetBE10 = MolBarcodeWell("E10",mbS507,mbN727)
	private val mbSetBF10 = MolBarcodeWell("F10",mbS508,mbN727)
	private val mbSetBG10 = MolBarcodeWell("G10",mbS510,mbN727)
	private val mbSetBH10 = MolBarcodeWell("H10",mbS511,mbN727)
	private val mbSetBA11 = MolBarcodeWell("A11",mbS502,mbN728)
	private val mbSetBB11 = MolBarcodeWell("B11",mbS503,mbN728)
	private val mbSetBC11 = MolBarcodeWell("C11",mbS505,mbN728)
	private val mbSetBD11 = MolBarcodeWell("D11",mbS506,mbN728)
	private val mbSetBE11 = MolBarcodeWell("E11",mbS507,mbN728)
	private val mbSetBF11 = MolBarcodeWell("F11",mbS508,mbN728)
	private val mbSetBG11 = MolBarcodeWell("G11",mbS510,mbN728)
	private val mbSetBH11 = MolBarcodeWell("H11",mbS511,mbN728)
	private val mbSetBA12 = MolBarcodeWell("A12",mbS502,mbN729)
	private val mbSetBB12 = MolBarcodeWell("B12",mbS503,mbN729)
	private val mbSetBC12 = MolBarcodeWell("C12",mbS505,mbN729)
	private val mbSetBD12 = MolBarcodeWell("D12",mbS506,mbN729)
	private val mbSetBE12 = MolBarcodeWell("E12",mbS507,mbN729)
	private val mbSetBF12 = MolBarcodeWell("F12",mbS508,mbN729)
	private val mbSetBG12 = MolBarcodeWell("G12",mbS510,mbN729)
	private val mbSetBH12 = MolBarcodeWell("H12",mbS511,mbN729)

	val mbSetB = MolBarcodeContents(contentIDs(ContentType.NexteraSetB),ContentType.NexteraSetB,
		List(mbSetBA01,mbSetBB01,mbSetBC01,mbSetBD01,mbSetBE01,mbSetBF01,mbSetBG01,mbSetBH01,
			mbSetBA02,mbSetBB02,mbSetBC02,mbSetBD02,mbSetBE02,mbSetBF02,mbSetBG02,mbSetBH02,
			mbSetBA03,mbSetBB03,mbSetBC03,mbSetBD03,mbSetBE03,mbSetBF03,mbSetBG03,mbSetBH03,
			mbSetBA04,mbSetBB04,mbSetBC04,mbSetBD04,mbSetBE04,mbSetBF04,mbSetBG04,mbSetBH04,
			mbSetBA05,mbSetBB05,mbSetBC05,mbSetBD05,mbSetBE05,mbSetBF05,mbSetBG05,mbSetBH05,
			mbSetBA06,mbSetBB06,mbSetBC06,mbSetBD06,mbSetBE06,mbSetBF06,mbSetBG06,mbSetBH06,
			mbSetBA07,mbSetBB07,mbSetBC07,mbSetBD07,mbSetBE07,mbSetBF07,mbSetBG07,mbSetBH07,
			mbSetBA08,mbSetBB08,mbSetBC08,mbSetBD08,mbSetBE08,mbSetBF08,mbSetBG08,mbSetBH08,
			mbSetBA09,mbSetBB09,mbSetBC09,mbSetBD09,mbSetBE09,mbSetBF09,mbSetBG09,mbSetBH09,
			mbSetBA10,mbSetBB10,mbSetBC10,mbSetBD10,mbSetBE10,mbSetBF10,mbSetBG10,mbSetBH10,
			mbSetBA11,mbSetBB11,mbSetBC11,mbSetBD11,mbSetBE11,mbSetBF11,mbSetBG11,mbSetBH11,
			mbSetBA12,mbSetBB12,mbSetBC12,mbSetBD12,mbSetBE12,mbSetBF12,mbSetBG12,mbSetBH12))

	private val mbSetCA01 = MolBarcodeWell("A01",mbS513,mbN701)
	private val mbSetCB01 = MolBarcodeWell("B01",mbS515,mbN701)
	private val mbSetCC01 = MolBarcodeWell("C01",mbS516,mbN701)
	private val mbSetCD01 = MolBarcodeWell("D01",mbS517,mbN701)
	private val mbSetCE01 = MolBarcodeWell("E01",mbS518,mbN701)
	private val mbSetCF01 = MolBarcodeWell("F01",mbS520,mbN701)
	private val mbSetCG01 = MolBarcodeWell("G01",mbS521,mbN701)
	private val mbSetCH01 = MolBarcodeWell("H01",mbS522,mbN701)
	private val mbSetCA02 = MolBarcodeWell("A02",mbS513,mbN702)
	private val mbSetCB02 = MolBarcodeWell("B02",mbS515,mbN702)
	private val mbSetCC02 = MolBarcodeWell("C02",mbS516,mbN702)
	private val mbSetCD02 = MolBarcodeWell("D02",mbS517,mbN702)
	private val mbSetCE02 = MolBarcodeWell("E02",mbS518,mbN702)
	private val mbSetCF02 = MolBarcodeWell("F02",mbS520,mbN702)
	private val mbSetCG02 = MolBarcodeWell("G02",mbS521,mbN702)
	private val mbSetCH02 = MolBarcodeWell("H02",mbS522,mbN702)
	private val mbSetCA03 = MolBarcodeWell("A03",mbS513,mbN703)
	private val mbSetCB03 = MolBarcodeWell("B03",mbS515,mbN703)
	private val mbSetCC03 = MolBarcodeWell("C03",mbS516,mbN703)
	private val mbSetCD03 = MolBarcodeWell("D03",mbS517,mbN703)
	private val mbSetCE03 = MolBarcodeWell("E03",mbS518,mbN703)
	private val mbSetCF03 = MolBarcodeWell("F03",mbS520,mbN703)
	private val mbSetCG03 = MolBarcodeWell("G03",mbS521,mbN703)
	private val mbSetCH03 = MolBarcodeWell("H03",mbS522,mbN703)
	private val mbSetCA04 = MolBarcodeWell("A04",mbS513,mbN704)
	private val mbSetCB04 = MolBarcodeWell("B04",mbS515,mbN704)
	private val mbSetCC04 = MolBarcodeWell("C04",mbS516,mbN704)
	private val mbSetCD04 = MolBarcodeWell("D04",mbS517,mbN704)
	private val mbSetCE04 = MolBarcodeWell("E04",mbS518,mbN704)
	private val mbSetCF04 = MolBarcodeWell("F04",mbS520,mbN704)
	private val mbSetCG04 = MolBarcodeWell("G04",mbS521,mbN704)
	private val mbSetCH04 = MolBarcodeWell("H04",mbS522,mbN704)
	private val mbSetCA05 = MolBarcodeWell("A05",mbS513,mbN705)
	private val mbSetCB05 = MolBarcodeWell("B05",mbS515,mbN705)
	private val mbSetCC05 = MolBarcodeWell("C05",mbS516,mbN705)
	private val mbSetCD05 = MolBarcodeWell("D05",mbS517,mbN705)
	private val mbSetCE05 = MolBarcodeWell("E05",mbS518,mbN705)
	private val mbSetCF05 = MolBarcodeWell("F05",mbS520,mbN705)
	private val mbSetCG05 = MolBarcodeWell("G05",mbS521,mbN705)
	private val mbSetCH05 = MolBarcodeWell("H05",mbS522,mbN705)
	private val mbSetCA06 = MolBarcodeWell("A06",mbS513,mbN706)
	private val mbSetCB06 = MolBarcodeWell("B06",mbS515,mbN706)
	private val mbSetCC06 = MolBarcodeWell("C06",mbS516,mbN706)
	private val mbSetCD06 = MolBarcodeWell("D06",mbS517,mbN706)
	private val mbSetCE06 = MolBarcodeWell("E06",mbS518,mbN706)
	private val mbSetCF06 = MolBarcodeWell("F06",mbS520,mbN706)
	private val mbSetCG06 = MolBarcodeWell("G06",mbS521,mbN706)
	private val mbSetCH06 = MolBarcodeWell("H06",mbS522,mbN706)
	private val mbSetCA07 = MolBarcodeWell("A07",mbS513,mbN707)
	private val mbSetCB07 = MolBarcodeWell("B07",mbS515,mbN707)
	private val mbSetCC07 = MolBarcodeWell("C07",mbS516,mbN707)
	private val mbSetCD07 = MolBarcodeWell("D07",mbS517,mbN707)
	private val mbSetCE07 = MolBarcodeWell("E07",mbS518,mbN707)
	private val mbSetCF07 = MolBarcodeWell("F07",mbS520,mbN707)
	private val mbSetCG07 = MolBarcodeWell("G07",mbS521,mbN707)
	private val mbSetCH07 = MolBarcodeWell("H07",mbS522,mbN707)
	private val mbSetCA08 = MolBarcodeWell("A08",mbS513,mbN710)
	private val mbSetCB08 = MolBarcodeWell("B08",mbS515,mbN710)
	private val mbSetCC08 = MolBarcodeWell("C08",mbS516,mbN710)
	private val mbSetCD08 = MolBarcodeWell("D08",mbS517,mbN710)
	private val mbSetCE08 = MolBarcodeWell("E08",mbS518,mbN710)
	private val mbSetCF08 = MolBarcodeWell("F08",mbS520,mbN710)
	private val mbSetCG08 = MolBarcodeWell("G08",mbS521,mbN710)
	private val mbSetCH08 = MolBarcodeWell("H08",mbS522,mbN710)
	private val mbSetCA09 = MolBarcodeWell("A09",mbS513,mbN711)
	private val mbSetCB09 = MolBarcodeWell("B09",mbS515,mbN711)
	private val mbSetCC09 = MolBarcodeWell("C09",mbS516,mbN711)
	private val mbSetCD09 = MolBarcodeWell("D09",mbS517,mbN711)
	private val mbSetCE09 = MolBarcodeWell("E09",mbS518,mbN711)
	private val mbSetCF09 = MolBarcodeWell("F09",mbS520,mbN711)
	private val mbSetCG09 = MolBarcodeWell("G09",mbS521,mbN711)
	private val mbSetCH09 = MolBarcodeWell("H09",mbS522,mbN711)
	private val mbSetCA10 = MolBarcodeWell("A10",mbS513,mbN712)
	private val mbSetCB10 = MolBarcodeWell("B10",mbS515,mbN712)
	private val mbSetCC10 = MolBarcodeWell("C10",mbS516,mbN712)
	private val mbSetCD10 = MolBarcodeWell("D10",mbS517,mbN712)
	private val mbSetCE10 = MolBarcodeWell("E10",mbS518,mbN712)
	private val mbSetCF10 = MolBarcodeWell("F10",mbS520,mbN712)
	private val mbSetCG10 = MolBarcodeWell("G10",mbS521,mbN712)
	private val mbSetCH10 = MolBarcodeWell("H10",mbS522,mbN712)
	private val mbSetCA11 = MolBarcodeWell("A11",mbS513,mbN714)
	private val mbSetCB11 = MolBarcodeWell("B11",mbS515,mbN714)
	private val mbSetCC11 = MolBarcodeWell("C11",mbS516,mbN714)
	private val mbSetCD11 = MolBarcodeWell("D11",mbS517,mbN714)
	private val mbSetCE11 = MolBarcodeWell("E11",mbS518,mbN714)
	private val mbSetCF11 = MolBarcodeWell("F11",mbS520,mbN714)
	private val mbSetCG11 = MolBarcodeWell("G11",mbS521,mbN714)
	private val mbSetCH11 = MolBarcodeWell("H11",mbS522,mbN714)
	private val mbSetCA12 = MolBarcodeWell("A12",mbS513,mbN715)
	private val mbSetCB12 = MolBarcodeWell("B12",mbS515,mbN715)
	private val mbSetCC12 = MolBarcodeWell("C12",mbS516,mbN715)
	private val mbSetCD12 = MolBarcodeWell("D12",mbS517,mbN715)
	private val mbSetCE12 = MolBarcodeWell("E12",mbS518,mbN715)
	private val mbSetCF12 = MolBarcodeWell("F12",mbS520,mbN715)
	private val mbSetCG12 = MolBarcodeWell("G12",mbS521,mbN715)
	private val mbSetCH12 = MolBarcodeWell("H12",mbS522,mbN715)

	val mbSetC = MolBarcodeContents(contentIDs(ContentType.NexteraSetC),ContentType.NexteraSetC,
		List(mbSetCA01,mbSetCB01,mbSetCC01,mbSetCD01,mbSetCE01,mbSetCF01,mbSetCG01,mbSetCH01,
			mbSetCA02,mbSetCB02,mbSetCC02,mbSetCD02,mbSetCE02,mbSetCF02,mbSetCG02,mbSetCH02,
			mbSetCA03,mbSetCB03,mbSetCC03,mbSetCD03,mbSetCE03,mbSetCF03,mbSetCG03,mbSetCH03,
			mbSetCA04,mbSetCB04,mbSetCC04,mbSetCD04,mbSetCE04,mbSetCF04,mbSetCG04,mbSetCH04,
			mbSetCA05,mbSetCB05,mbSetCC05,mbSetCD05,mbSetCE05,mbSetCF05,mbSetCG05,mbSetCH05,
			mbSetCA06,mbSetCB06,mbSetCC06,mbSetCD06,mbSetCE06,mbSetCF06,mbSetCG06,mbSetCH06,
			mbSetCA07,mbSetCB07,mbSetCC07,mbSetCD07,mbSetCE07,mbSetCF07,mbSetCG07,mbSetCH07,
			mbSetCA08,mbSetCB08,mbSetCC08,mbSetCD08,mbSetCE08,mbSetCF08,mbSetCG08,mbSetCH08,
			mbSetCA09,mbSetCB09,mbSetCC09,mbSetCD09,mbSetCE09,mbSetCF09,mbSetCG09,mbSetCH09,
			mbSetCA10,mbSetCB10,mbSetCC10,mbSetCD10,mbSetCE10,mbSetCF10,mbSetCG10,mbSetCH10,
			mbSetCA11,mbSetCB11,mbSetCC11,mbSetCD11,mbSetCE11,mbSetCF11,mbSetCG11,mbSetCH11,
			mbSetCA12,mbSetCB12,mbSetCC12,mbSetCD12,mbSetCE12,mbSetCF12,mbSetCG12,mbSetCH12))

	private val mbSetDA01 = MolBarcodeWell("A01",mbS513,mbN716)
	private val mbSetDB01 = MolBarcodeWell("B01",mbS515,mbN716)
	private val mbSetDC01 = MolBarcodeWell("C01",mbS516,mbN716)
	private val mbSetDD01 = MolBarcodeWell("D01",mbS517,mbN716)
	private val mbSetDE01 = MolBarcodeWell("E01",mbS518,mbN716)
	private val mbSetDF01 = MolBarcodeWell("F01",mbS520,mbN716)
	private val mbSetDG01 = MolBarcodeWell("G01",mbS521,mbN716)
	private val mbSetDH01 = MolBarcodeWell("H01",mbS522,mbN716)
	private val mbSetDA02 = MolBarcodeWell("A02",mbS513,mbN718)
	private val mbSetDB02 = MolBarcodeWell("B02",mbS515,mbN718)
	private val mbSetDC02 = MolBarcodeWell("C02",mbS516,mbN718)
	private val mbSetDD02 = MolBarcodeWell("D02",mbS517,mbN718)
	private val mbSetDE02 = MolBarcodeWell("E02",mbS518,mbN718)
	private val mbSetDF02 = MolBarcodeWell("F02",mbS520,mbN718)
	private val mbSetDG02 = MolBarcodeWell("G02",mbS521,mbN718)
	private val mbSetDH02 = MolBarcodeWell("H02",mbS522,mbN718)
	private val mbSetDA03 = MolBarcodeWell("A03",mbS513,mbN719)
	private val mbSetDB03 = MolBarcodeWell("B03",mbS515,mbN719)
	private val mbSetDC03 = MolBarcodeWell("C03",mbS516,mbN719)
	private val mbSetDD03 = MolBarcodeWell("D03",mbS517,mbN719)
	private val mbSetDE03 = MolBarcodeWell("E03",mbS518,mbN719)
	private val mbSetDF03 = MolBarcodeWell("F03",mbS520,mbN719)
	private val mbSetDG03 = MolBarcodeWell("G03",mbS521,mbN719)
	private val mbSetDH03 = MolBarcodeWell("H03",mbS522,mbN719)
	private val mbSetDA04 = MolBarcodeWell("A04",mbS513,mbN720)
	private val mbSetDB04 = MolBarcodeWell("B04",mbS515,mbN720)
	private val mbSetDC04 = MolBarcodeWell("C04",mbS516,mbN720)
	private val mbSetDD04 = MolBarcodeWell("D04",mbS517,mbN720)
	private val mbSetDE04 = MolBarcodeWell("E04",mbS518,mbN720)
	private val mbSetDF04 = MolBarcodeWell("F04",mbS520,mbN720)
	private val mbSetDG04 = MolBarcodeWell("G04",mbS521,mbN720)
	private val mbSetDH04 = MolBarcodeWell("H04",mbS522,mbN720)
	private val mbSetDA05 = MolBarcodeWell("A05",mbS513,mbN721)
	private val mbSetDB05 = MolBarcodeWell("B05",mbS515,mbN721)
	private val mbSetDC05 = MolBarcodeWell("C05",mbS516,mbN721)
	private val mbSetDD05 = MolBarcodeWell("D05",mbS517,mbN721)
	private val mbSetDE05 = MolBarcodeWell("E05",mbS518,mbN721)
	private val mbSetDF05 = MolBarcodeWell("F05",mbS520,mbN721)
	private val mbSetDG05 = MolBarcodeWell("G05",mbS521,mbN721)
	private val mbSetDH05 = MolBarcodeWell("H05",mbS522,mbN721)
	private val mbSetDA06 = MolBarcodeWell("A06",mbS513,mbN722)
	private val mbSetDB06 = MolBarcodeWell("B06",mbS515,mbN722)
	private val mbSetDC06 = MolBarcodeWell("C06",mbS516,mbN722)
	private val mbSetDD06 = MolBarcodeWell("D06",mbS517,mbN722)
	private val mbSetDE06 = MolBarcodeWell("E06",mbS518,mbN722)
	private val mbSetDF06 = MolBarcodeWell("F06",mbS520,mbN722)
	private val mbSetDG06 = MolBarcodeWell("G06",mbS521,mbN722)
	private val mbSetDH06 = MolBarcodeWell("H06",mbS522,mbN722)
	private val mbSetDA07 = MolBarcodeWell("A07",mbS513,mbN723)
	private val mbSetDB07 = MolBarcodeWell("B07",mbS515,mbN723)
	private val mbSetDC07 = MolBarcodeWell("C07",mbS516,mbN723)
	private val mbSetDD07 = MolBarcodeWell("D07",mbS517,mbN723)
	private val mbSetDE07 = MolBarcodeWell("E07",mbS518,mbN723)
	private val mbSetDF07 = MolBarcodeWell("F07",mbS520,mbN723)
	private val mbSetDG07 = MolBarcodeWell("G07",mbS521,mbN723)
	private val mbSetDH07 = MolBarcodeWell("H07",mbS522,mbN723)
	private val mbSetDA08 = MolBarcodeWell("A08",mbS513,mbN724)
	private val mbSetDB08 = MolBarcodeWell("B08",mbS515,mbN724)
	private val mbSetDC08 = MolBarcodeWell("C08",mbS516,mbN724)
	private val mbSetDD08 = MolBarcodeWell("D08",mbS517,mbN724)
	private val mbSetDE08 = MolBarcodeWell("E08",mbS518,mbN724)
	private val mbSetDF08 = MolBarcodeWell("F08",mbS520,mbN724)
	private val mbSetDG08 = MolBarcodeWell("G08",mbS521,mbN724)
	private val mbSetDH08 = MolBarcodeWell("H08",mbS522,mbN724)
	private val mbSetDA09 = MolBarcodeWell("A09",mbS513,mbN726)
	private val mbSetDB09 = MolBarcodeWell("B09",mbS515,mbN726)
	private val mbSetDC09 = MolBarcodeWell("C09",mbS516,mbN726)
	private val mbSetDD09 = MolBarcodeWell("D09",mbS517,mbN726)
	private val mbSetDE09 = MolBarcodeWell("E09",mbS518,mbN726)
	private val mbSetDF09 = MolBarcodeWell("F09",mbS520,mbN726)
	private val mbSetDG09 = MolBarcodeWell("G09",mbS521,mbN726)
	private val mbSetDH09 = MolBarcodeWell("H09",mbS522,mbN726)
	private val mbSetDA10 = MolBarcodeWell("A10",mbS513,mbN727)
	private val mbSetDB10 = MolBarcodeWell("B10",mbS515,mbN727)
	private val mbSetDC10 = MolBarcodeWell("C10",mbS516,mbN727)
	private val mbSetDD10 = MolBarcodeWell("D10",mbS517,mbN727)
	private val mbSetDE10 = MolBarcodeWell("E10",mbS518,mbN727)
	private val mbSetDF10 = MolBarcodeWell("F10",mbS520,mbN727)
	private val mbSetDG10 = MolBarcodeWell("G10",mbS521,mbN727)
	private val mbSetDH10 = MolBarcodeWell("H10",mbS522,mbN727)
	private val mbSetDA11 = MolBarcodeWell("A11",mbS513,mbN728)
	private val mbSetDB11 = MolBarcodeWell("B11",mbS515,mbN728)
	private val mbSetDC11 = MolBarcodeWell("C11",mbS516,mbN728)
	private val mbSetDD11 = MolBarcodeWell("D11",mbS517,mbN728)
	private val mbSetDE11 = MolBarcodeWell("E11",mbS518,mbN728)
	private val mbSetDF11 = MolBarcodeWell("F11",mbS520,mbN728)
	private val mbSetDG11 = MolBarcodeWell("G11",mbS521,mbN728)
	private val mbSetDH11 = MolBarcodeWell("H11",mbS522,mbN728)
	private val mbSetDA12 = MolBarcodeWell("A12",mbS513,mbN729)
	private val mbSetDB12 = MolBarcodeWell("B12",mbS515,mbN729)
	private val mbSetDC12 = MolBarcodeWell("C12",mbS516,mbN729)
	private val mbSetDD12 = MolBarcodeWell("D12",mbS517,mbN729)
	private val mbSetDE12 = MolBarcodeWell("E12",mbS518,mbN729)
	private val mbSetDF12 = MolBarcodeWell("F12",mbS520,mbN729)
	private val mbSetDG12 = MolBarcodeWell("G12",mbS521,mbN729)
	private val mbSetDH12 = MolBarcodeWell("H12",mbS522,mbN729)

	val mbSetD = MolBarcodeContents(contentIDs(ContentType.NexteraSetD),ContentType.NexteraSetD,
		List(mbSetDA01,mbSetDB01,mbSetDC01,mbSetDD01,mbSetDE01,mbSetDF01,mbSetDG01,mbSetDH01,
			mbSetDA02,mbSetDB02,mbSetDC02,mbSetDD02,mbSetDE02,mbSetDF02,mbSetDG02,mbSetDH02,
			mbSetDA03,mbSetDB03,mbSetDC03,mbSetDD03,mbSetDE03,mbSetDF03,mbSetDG03,mbSetDH03,
			mbSetDA04,mbSetDB04,mbSetDC04,mbSetDD04,mbSetDE04,mbSetDF04,mbSetDG04,mbSetDH04,
			mbSetDA05,mbSetDB05,mbSetDC05,mbSetDD05,mbSetDE05,mbSetDF05,mbSetDG05,mbSetDH05,
			mbSetDA06,mbSetDB06,mbSetDC06,mbSetDD06,mbSetDE06,mbSetDF06,mbSetDG06,mbSetDH06,
			mbSetDA07,mbSetDB07,mbSetDC07,mbSetDD07,mbSetDE07,mbSetDF07,mbSetDG07,mbSetDH07,
			mbSetDA08,mbSetDB08,mbSetDC08,mbSetDD08,mbSetDE08,mbSetDF08,mbSetDG08,mbSetDH08,
			mbSetDA09,mbSetDB09,mbSetDC09,mbSetDD09,mbSetDE09,mbSetDF09,mbSetDG09,mbSetDH09,
			mbSetDA10,mbSetDB10,mbSetDC10,mbSetDD10,mbSetDE10,mbSetDF10,mbSetDG10,mbSetDH10,
			mbSetDA11,mbSetDB11,mbSetDC11,mbSetDD11,mbSetDE11,mbSetDF11,mbSetDG11,mbSetDH11,
			mbSetDA12,mbSetDB12,mbSetDC12,mbSetDD12,mbSetDE12,mbSetDF12,mbSetDG12,mbSetDH12))

	/**
	 * Formatter for going to/from and validating Json
	 */
	// Supply our custom enum Reader and Writer for content type enum
	implicit val contentTypeFormat: Format[ContentType.ContentType] = enumFormat(ContentType)
	implicit val molBarcodeFormatter = Json.format[MolBarcode]
	implicit val molBarcodeWellFormatter = Json.format[MolBarcodeWell]
	implicit val molBarcodeContentsFormatter = Json.format[MolBarcodeContents]

}
