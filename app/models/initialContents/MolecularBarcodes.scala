package models.initialContents

import models.{Plate, Transfer, TransferWells}
import Transfer.Quad._
import models.db.DBOpers
import models.initialContents.InitialContents.ContentsMap
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONObjectID, Macros}

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
	//TODO: since name is option it should really be an option[string] but this messes up a bunch of stuff. Fix it later.
	case class MolBarcode(seq: String, name: String, _id: BSONObjectID = BSONObjectID.generate) {
		private val comp = Map('C' -> 'G','G' -> 'C','A' -> 'T','T' -> 'A')

		/**
		 * Create reverse compliment of original sequence
		 * @return original sequence complimented and reversed
		 */
		def getRevCompliment: String = {
			val complement = seq.map(comp)
			complement.reverse
		}
	}


	object MolBarcode extends DBOpers[MolBarcode]{
		protected val collectionNameKey = "mongodb.collection.barcodes"
		protected val collectionNameDefault = "barcodes"
		implicit val barcodeHandler = Macros.handler[MolBarcode]
		val reader = implicitly[BSONDocumentReader[MolBarcode]]
		val writer = implicitly[BSONDocumentWriter[MolBarcode]]
	}
	/**
	 * Nextera molecular barcode
	 * @param seq sequence
	 * @param name name
	 * @param name1 alternate name
	 */
	//TODO: since name and name1 are optional it should really be an option[string] but this messes up a bunch of stuff. Fix it later.

	class MolBarcodeNextera(seq: String, name: String, val name1: String) extends MolBarcode(seq, name)
	// Companion object with apply - can't have case class extending from case class so apply will do
	object MolBarcodeNextera {
		def apply(seq: String, name: String, name1: String) = new MolBarcodeNextera(seq,name,name1)
	}

	/**
	 * Common interface for molecular barcodes placed in wells
	 */
	trait MolBarcodeWell {
		def getName: String
		def getSeq: String
		def isNextera: Boolean
	}

	/**
	 * Molecular barcode pairs in a well
	 */
	trait MolBarcodePair extends MolBarcodeWell {
		val i5: MolBarcode
		val i7: MolBarcode
	}

	// Sequence split character
	private val seqSplit = "-"

	/**
	 * Nextera molecular barcode pairs in a well
	 * @param i5 i5 barcode
	 * @param i7 i7 barcode
	 */
	case class MolBarcodeNexteraPair(i5: MolBarcode, i7: MolBarcode) extends MolBarcodePair {
		def getName: String = "Illumina_P5-" + i5.name + "_P7-" + i7.name
		def getSeq: String = i5.seq + seqSplit + i7.seq + seqSplit
		def isNextera: Boolean = true
	}

	/**
	 * SQM molecular barcode pairs in a well
	 * @param i5 i5 barcode
	 * @param i7 i7 barcode
	 */
	case class MolBarcodeSQMPair(i5: MolBarcode, i7: MolBarcode) extends MolBarcodePair {
		def getName: String = "SQM_P5-" + i5.name + "_P7-" + i7.name
		def getSeq: String = i5.seq + seqSplit + i7.seq + seqSplit
		def isNextera: Boolean = false
	}

	/**
	 * Split up a sequence into it's individual barcodes
	 * @param seq barcode sequence (individual or pair)
	 * @return array of individual barcode sequences
	 */
	def splitSequence(seq: String): Array[String] = {
		val seqStr = if (seq.endsWith(seqSplit)) seq.substring(0, seq.length - 1) else seq
		seqStr.split(seqSplit)
	}

	/**
	 * Single molecular barcode in a well
 	 * @param m molecular barcode
	 */
	case class MolBarcodeSingle(m: MolBarcode) extends MolBarcodeWell {
		def getName: String = m.name
		def getSeq: String = m.seq
		def isNextera: Boolean = false
	}

	/**
	 * Single molecular barcode in a well
	 * @param m molecular barcode
	 */
	case class MolBarcodeNexteraSingle(m: MolBarcode) extends MolBarcodeWell {
		def getName: String = "Illumina_P7-" + m.name
		def getSeq: String = m.seq
		def isNextera: Boolean = true
	}

	/**
	 * Contents for a plate of molecular barcodes.
	 * @param contents map of well->contents
	 */
	case class MolBarcodeContents(contents: Map[String, MolBarcodeWell]) extends ContentsMap[MolBarcodeWell]

	// Create i5 Nextera Molecular barcodes
	private val mbS502 = MolBarcodeNextera("CTCTCTAT","Lexof","S502")
	private val mbS503 = MolBarcodeNextera("TATCCTCT","Wojol","S503")
	private val mbS504 = MolBarcodeNextera("AGAGTAGA","Datan","S504")
	private val mbS505 = MolBarcodeNextera("GTAAGGAG","Tadid","S505")
	private val mbS506 = MolBarcodeNextera("ACTGCATA","Copaw","S506")
	private val mbS507 = MolBarcodeNextera("AAGGAGTA","Biniw","S507")
	private val mbS508 = MolBarcodeNextera("CTAAGCCT","Ladel","S508")
	private val mbS510 = MolBarcodeNextera("CGTCTAAT","Kolaf","S510")
	private val mbS511 = MolBarcodeNextera("TCTCTCCG","Xolek","S511")
	private val mbS513 = MolBarcodeNextera("TCGACTAG","Xicod","S513")
	private val mbS515 = MolBarcodeNextera("TTCTAGCT","Zewil","S515")
	private val mbS516 = MolBarcodeNextera("CCTAGAGT","Jodat","S516")
	private val mbS517 = MolBarcodeNextera("GCGTAAGA","Piwan","S517")
	private val mbS518 = MolBarcodeNextera("CTATTAAG","Lazad","S518")
	private val mbS520 = MolBarcodeNextera("AAGGCTAT","Bipof","S520")
	private val mbS521 = MolBarcodeNextera("GAGCCTTA","Nijow","S521")
	private val mbS522 = MolBarcodeNextera("TTATGCGA","Zayen","S522")

	private val mbSxx1 = MolBarcodeNextera("AACCTCTT","Belez","Sxx")
	private val mbSxx2 = MolBarcodeNextera("AGTCACCT","Dohel","Sxx")
	private val mbSxx3 = MolBarcodeNextera("CCTCTAAC","Jolac","Sxx")
	private val mbSxx4 = MolBarcodeNextera("CTGAAGCT","Libil","Sxx")
	private val mbSxx5 = MolBarcodeNextera("GGCAATAC","Reboc","Sxx")
	private val mbSxx6 = MolBarcodeNextera("GAACGCTA","Nakew","Sxx")
	private val mbSxx7 = MolBarcodeNextera("TTAGCCAG","Zaped","Sxx")
	private val mbSxx8 = MolBarcodeNextera("TCTCGCGC","Xokep","Sxx")
	private val mbSxx9 = MolBarcodeNextera("GCAGGTTG","Paray","Sxx")
	private val mbSxx10 = MolBarcodeNextera("ATGAATTA","Fibow","Sxx")
	private val mbSxx11 = MolBarcodeNextera("CGCATATT","Kefaz","Sxx")
	private val mbSxx12 = MolBarcodeNextera("CAACTGAT","Halif","Sxx")
	private val mbSxx13 = MolBarcodeNextera("GTCTGCAC","Teyec","Sxx")
	private val mbSxx14 = MolBarcodeNextera("GCTAGCAG","Poded","Sxx")
	private val mbSxx15 = MolBarcodeNextera("TAATCCGG","Waxer","Sxx")
	private val mbSxx16 = MolBarcodeNextera("TGGTGCAT","Yiyef","Sxx")
	private val mbSxx17 = MolBarcodeNextera("GAACTTCG","Nalok","Sxx")
	private val mbSxx18 = MolBarcodeNextera("AGTAGGCA","Dodih","Sxx")
	private val mbSxx19 = MolBarcodeNextera("GGAATACG","Rafak","Sxx")
	private val mbSxx20 = MolBarcodeNextera("CAGCGATT","Hikaz","Sxx")
	private val mbSxx21 = MolBarcodeNextera("TCATGTCT","Xayol","Sxx")
	private val mbSxx22 = MolBarcodeNextera("CGACTCTC","Kalex","Sxx")
	private val mbSxx23 = MolBarcodeNextera("TTCACAGA","Zecan","Sxx")
	//Low Input Metagenomic
	private val mbSxx101 = MolBarcodeNextera("ATCTTCTC","Fezex","Sxx")
	private val mbSxx102 = MolBarcodeNextera("CAGCTCAC","Hilec","Sxx")
	private val mbSxx103 = MolBarcodeNextera("GGTTATCT","Rowol","Sxx")
	private val mbSxx104 = MolBarcodeNextera("TCCGCATA","Xepaw","Sxx")
	private val mbSxx105 = MolBarcodeNextera("TGCTTCAC","Yezec","Sxx")
	private val mbSxx106 = MolBarcodeNextera("GCTTCCTA","Poxew","Sxx")
	private val mbSxx107 = MolBarcodeNextera("GACCATCT","Nehol","Sxx")
	private val mbSxx108 = MolBarcodeNextera("CTGGTATT","Litaz","Sxx")
	private val mbSxx109 = MolBarcodeNextera("TTAATCAC","Zafec","Sxx")
	private val mbSxx110 = MolBarcodeNextera("CGCGAATA","Kenaw","Sxx")
	private val mbSxx111 = MolBarcodeNextera("GCTCACCA","Poheh","Sxx")
	private val mbSxx113 = MolBarcodeNextera("ATCCTTAA","Felob","Sxx")
	private val mbSxx114 = MolBarcodeNextera("TTCTTGGC","Zezip","Sxx")
	private val mbSxx115 = MolBarcodeNextera("CATCACTT","Hohez","Sxx")
	private val mbSxx116 = MolBarcodeNextera("CGAACTTC","Kacox","Sxx")
	private val mbSxx117 = MolBarcodeNextera("GACATTAA","Nefob","Sxx")
	private val mbSxx118 = MolBarcodeNextera("TTCACCTT","Zecez","Sxx")
	private val mbSxx119 = MolBarcodeNextera("CCAATCTG","Jafey","Sxx")
	private val mbSxx120 = MolBarcodeNextera("CGACAGTT","Kahiz","Sxx")
	private val mbSxx25 = MolBarcodeNextera("ATCGACTG","Feney","Sxx")
	private val mbSxx27 = MolBarcodeNextera("TACTCTCC","Wexoj","Sxx")
	private val mbSxx28 = MolBarcodeNextera("TGACAGCA","Yahih","Sxx")
	private val mbSxx29 = MolBarcodeNextera("GCAGGTTG","Paroy","Sxx")
	private val mbSxx30 = MolBarcodeNextera("TTCCAGCT","Zehil","Sxx")
	private val mbSxx31 = MolBarcodeNextera("TAGTTAGC","Wizap","Sxx")
	private val mbSxx32 = MolBarcodeNextera("AGCGCTAA","Depob","Sxx")
	private val mbSxx33 = MolBarcodeNextera("CGGTTCTT","Kizez","Sxx")
	private val mbSxx34 = MolBarcodeNextera("TAGCATTG","Wihoy","Sxx")
	private val mbSxx35 = MolBarcodeNextera("AATTCAAC","Boxac","Sxx")
	private val mbSxx37 = MolBarcodeNextera("GCTCTCTT","Polez","Sxx")
	private val mbSxx38 = MolBarcodeNextera("TGACTTGG","Yalor","Sxx")
	private val mbSxx39 = MolBarcodeNextera("TATGGTTC","Worox","Sxx")
	private val mbSxx40 = MolBarcodeNextera("CACTAGCC","Hewij","Sxx")
	private val mbSxx42 = MolBarcodeNextera("CTACATTG","Lahoy","Sxx")
	private val mbSxx43 = MolBarcodeNextera("GCGATTAC","Pifoc","Sxx")
	private val mbSxx44 = MolBarcodeNextera("AATTGGCC","Boyij","Sxx")
	private val mbSxx45 = MolBarcodeNextera("AATTGCTT","Boyez","Sxx")
	private val mbSxx46 = MolBarcodeNextera("TTGGTCTG","Zitey","Sxx")
	private val mbSxx47 = MolBarcodeNextera("CATCCTGG","Hojor","Sxx")
	private val mbSxx48 = MolBarcodeNextera("GGATTAAC","Razac","Sxx")
	private val mbSxx50 = MolBarcodeNextera("TCATTCGA","Xazen","Sxx")
	private val mbSxx51 = MolBarcodeNextera("GTCCAATC","Tehax","Sxx")
	private val mbSxx52 = MolBarcodeNextera("CTTGGTCA","Loroh","Sxx")
	private val mbSxx53 = MolBarcodeNextera("CCAACGCT","Jacil","Sxx")
	private val mbSxx54 = MolBarcodeNextera("TCCACTTC","Xecox","Sxx")
	private val mbSxx55 = MolBarcodeNextera("AATCTCCA","Boleh","Sxx")
	private val mbSxx57 = MolBarcodeNextera("CTGCTCCT","Lilel","Sxx")
	private val mbSxx59 = MolBarcodeNextera("GCTGATTC","Ponox","Sxx")
	private val mbSxx60 = MolBarcodeNextera("GAATCGAC","Naxic","Sxx")
	private val mbSxx62 = MolBarcodeNextera("CACGATTC","Henox","Sxx")
	private val mbSxx63 = MolBarcodeNextera("GCTCCGAT","Pojif","Sxx")
	private val mbSxx64 = MolBarcodeNextera("ACAAGTCA","Cadoh","Sxx")
	private val mbSxx65 = MolBarcodeNextera("GCTGCACT","Popal","Sxx")
	private val mbSxx67 = MolBarcodeNextera("CTGTATTC","Liwox","Sxx")
	private val mbSxx68 = MolBarcodeNextera("ATATCCGA","Faxen","Sxx")
	private val mbSxx69 = MolBarcodeNextera("TTGTCCAT","Zixef","Sxx")
	private val mbSxx70 = MolBarcodeNextera("AGTAAGTC","Dobix","Sxx")
	private val mbSxx71 = MolBarcodeNextera("GAATATCA","Nawoh","Sxx")
	private val mbSxx73 = MolBarcodeNextera("CCTGTCAT","Jotef","Sxx")
	private val mbSxx74 = MolBarcodeNextera("GACGGTTA","Nerow","Sxx")
	private val mbSxx75 = MolBarcodeNextera("CTATTAGC","Lazap","Sxx")
	private val mbSxx76 = MolBarcodeNextera("TCCAACCA","Xebeh","Sxx")
	private val mbSxx77 = MolBarcodeNextera("CTGGCTAT","Lipof","Sxx")
	private val mbSxx78 = MolBarcodeNextera("ACTGGCTC","Corex","Sxx")
	private val mbSxx79 = MolBarcodeNextera("CCATCACA","Jaxah","Sxx")
	private val mbSxx81 = MolBarcodeNextera("CACTTCAT","Hezef","Sxx")
	private val mbSxx82 = MolBarcodeNextera("CAAGCTTA","Hapow","Sxx")
	private val mbSxx83 = MolBarcodeNextera("AGGTACCA","Diweh","Sxx")
	private val mbSxx84 = MolBarcodeNextera("TCCATAAC","Xefac","Sxx")
	private val mbSxx85 = MolBarcodeNextera("GTCCTCAT","Telef","Sxx")
	private val mbSxx86 = MolBarcodeNextera("AGTACTGC","Docop","Sxx")
	private val mbSxx87 = MolBarcodeNextera("CTTGAATC","Lonax","Sxx")
	private val mbSxx88 = MolBarcodeNextera("CCAACTAA","Jacob","Sxx")
	private val mbSxx89 = MolBarcodeNextera("AATACCAT","Bocef","Sxx")
	private val mbSxx90 = MolBarcodeNextera("ACCTATGC","Cewop","Sxx")
	private val mbSxx92 = MolBarcodeNextera("CTGACATC","Licax","Sxx")
	private val mbSxx93 = MolBarcodeNextera("GCCACCAT","Pecef","Sxx")
	private val mbSxx95 = MolBarcodeNextera("TGCTATTA","Yewow","Sxx")
	private val mbSxx96 = MolBarcodeNextera("CTTCTGGC","Lolip","Sxx")
	private val mbSxx98 = MolBarcodeNextera("TACTCCAG","Wexed","Sxx")
	private val mbSxx99 = MolBarcodeNextera("ATCATACC","Fefaj","Sxx")


	// Create i7 Nextera Molecular barcodes
	private val mbN701 = MolBarcodeNextera("TAAGGCGA","Waren","N701")
	private val mbN702 = MolBarcodeNextera("CGTACTAG","Kocod","N702")
	private val mbN703 = MolBarcodeNextera("AGGCAGAA","Dihib","N703")
	private val mbN704 = MolBarcodeNextera("TCCTGAGC","Xeyap","N704")
	private val mbN705 = MolBarcodeNextera("GGACTCCT","Ralel","N705")
	private val mbN706 = MolBarcodeNextera("TAGGCATG","Wipay","N706")
	private val mbN707 = MolBarcodeNextera("CTCTCTAC","Lexoc","N707")
	private val mbN708 = MolBarcodeNextera("CAGAGAGG","Hidar","N708")
	private val mbN709 = MolBarcodeNextera("GCTACGCT","Pocil","N709")
	private val mbN710 = MolBarcodeNextera("CGAGGCTG","Karey","N710")
	private val mbN711 = MolBarcodeNextera("AAGAGGCA","Bidih","N711")
	private val mbN712 = MolBarcodeNextera("GTAGAGGA","Tanin","N712")
	private val mbN714 = MolBarcodeNextera("GCTCATGA","Pohon","N714")
	private val mbN715 = MolBarcodeNextera("ATCTCAGG","Fexar","N715")
	private val mbN716 = MolBarcodeNextera("ACTCGCTA","Cokew","N716")
	private val mbN718 = MolBarcodeNextera("GGAGCTAC","Rapoc","N718")
	private val mbN719 = MolBarcodeNextera("GCGTAGTA","Piwiw","N719")
	private val mbN720 = MolBarcodeNextera("CGGAGCCT","Kidel","N720")
	private val mbN721 = MolBarcodeNextera("TACGCTGC","Wepop","N721")
	private val mbN722 = MolBarcodeNextera("ATGCGCAG","Fiked","N722")
	private val mbN723 = MolBarcodeNextera("TAGCGCTC","Wikex","N723")
	private val mbN724 = MolBarcodeNextera("ACTGAGCG","Conik","N724")
	private val mbN726 = MolBarcodeNextera("CCTAAGAC","Jobic","N726")
	private val mbN727 = MolBarcodeNextera("CGATCAGT","Kaxat","N727")
	private val mbN728 = MolBarcodeNextera("TGCAGCTA","Yedew","N728")
	private val mbN729 = MolBarcodeNextera("TCGACGTC","Xicix","N729")
	// Barcodes used for Housekeeping (plate 1)
	private val mbNxx1 = MolBarcodeNextera("ACTTCTTC","Coxox","Nxx")
	private val mbNxx2 = MolBarcodeNextera("TGGTAACG","Yiwak","Nxx")
	private val mbNxx3 = MolBarcodeNextera("TAGATCCT","Wifel","Nxx")
	private val mbNxx4 = MolBarcodeNextera("CATCAGAC","Hohic","Nxx")
	private val mbNxx5 = MolBarcodeNextera("TTACTGTC","Zalix","Nxx")
	private val mbNxx6 = MolBarcodeNextera("GTGCGTAA","Tikob","Nxx")
	private val mbNxx7 = MolBarcodeNextera("GGCATAGG","Refar","Nxx")
	private val mbNxx8 = MolBarcodeNextera("CTATTCAA","Lezab","Nxx")
	private val mbNxx9 = MolBarcodeNextera("CAAGGCGA","Haren","Nxx")
	private val mbNxx10 = MolBarcodeNextera("CAGTTGGT","Hizit","Nxx")
	private val mbNxx11 = MolBarcodeNextera("GACGCTAT","Nepof","Nxx")
	private val mbNxx12 = MolBarcodeNextera("TCTGGACC","Xoraj","Nxx")
	private val mbNxx13 = MolBarcodeNextera("AAGGCGAC","Bipic","Nxx")
	private val mbNxx14 = MolBarcodeNextera("TGTTATAC","Yowoc","Nxx")
	private val mbNxx15 = MolBarcodeNextera("CCTAGAAT","Jodaf","Nxx")
	private val mbNxx16 = MolBarcodeNextera("TCAGCGAA","Xapib","Nxx")
	// Barcodes used for Housekeeping (plate 2)
	private val mbNxx17 = MolBarcodeNextera("AATGCGTT","Bopiz","Nxx")
	private val mbNxx18 = MolBarcodeNextera("GAGAGTTG","Nidoy","Nxx")
	private val mbNxx19 = MolBarcodeNextera("GATTACAG","Nowed","Nxx")
	private val mbNxx20 = MolBarcodeNextera("TGTGCTTA","Yopow","Nxx")
	private val mbNxx21 = MolBarcodeNextera("AGAACATT","Dacaz","Nxx")
	private val mbNxx22 = MolBarcodeNextera("TACCGCTG","Wekey","Nxx")
	private val mbNxx23 = MolBarcodeNextera("TCCTGGTC","Xeyix","Nxx")
	private val mbNxx24 = MolBarcodeNextera("CCTGGATA","Joraw","Nxx")
	private val mbNxx25 = MolBarcodeNextera("ATACCTGT","Fajot","Nxx")
	private val mbNxx26 = MolBarcodeNextera("AATGTTGG","Botor","Nxx")
	private val mbNxx27 = MolBarcodeNextera("TCGACGGC","Xicip","Nxx")
	private val mbNxx28 = MolBarcodeNextera("GGCAGATA","Redaw","Nxx")
	private val mbNxx29 = MolBarcodeNextera("GTCTTAGT","Tezat","Nxx")
	private val mbNxx30 = MolBarcodeNextera("GGAAGGCG","Radik","Nxx")
	private val mbNxx31 = MolBarcodeNextera("GGCTAGGC","Rewip","Nxx")
	private val mbNxx32 = MolBarcodeNextera("CAGCAGCA","Yeyey","Nxx")
	// Barcodes used for TCR (P7 set for entire plate)
	private val mbNxx33 = MolBarcodeNextera("TTGAGCCT", "Zidel", "Nxx")
	private val mbNxx34 = MolBarcodeNextera("CCAGTTAG", "Jatod", "Nxx")
	// Barcodes ued for MiRNA
	private val mbNxx35 = MolBarcodeNextera("GCACACGA","Pahen", "Nxx")
	private val mbNxx36 = MolBarcodeNextera("TCTGGCGA","Xoren", "Nxx")
	private val mbNxx37 = MolBarcodeNextera("CATAGCGA","Hoden", "Nxx")
	private val mbNxx38 = MolBarcodeNextera("CAGGAGCC","Hinij", "Nxx")
	private val mbNxx39 = MolBarcodeNextera("TGTCGGAT","Yokif", "Nxx")
	private val mbNxx40 = MolBarcodeNextera("ATTATGTT","Fofiz", "Nxx")
	private val mbNxx41 = MolBarcodeNextera("CCTACCAT","Jocef", "Nxx")
	private val mbNxx42 = MolBarcodeNextera("TACTTAGC","Wezap", "Nxx")
	private val mbNxx43 = MolBarcodeNextera("AACAATGG","Bebor", "Nxx")
	private val mbNxx44 = MolBarcodeNextera("TATCTGCC","Wolij", "Nxx")
	private val mbNxx45 = MolBarcodeNextera("GACCTAAC","Nelac", "Nxx")
	private val mbNxx46 = MolBarcodeNextera("GGTCCAGA","Rojan", "Nxx")
	private val mbNxx47 = MolBarcodeNextera("CAACTCTC","Halex", "Nxx")
	private val mbNxx48 = MolBarcodeNextera("CATGCTTA","Hopow", "Nxx")
	private val mbNxx49 = MolBarcodeNextera("CGTTACCA","Koweh", "Nxx")
	private val mbNxx50 = MolBarcodeNextera("CTGTAATC","Liwax", "Nxx")
	//Low Input Metagenomic
	private val mbNxx731 = MolBarcodeNextera("AAGTAGAG","Biwid","Nxx")
	private val mbNxx733 = MolBarcodeNextera("GCACATCT","Pahol","Nxx")
	private val mbNxx734 = MolBarcodeNextera("TTCGCTGA","Zepon","Nxx")
	private val mbNxx735 = MolBarcodeNextera("AGCAATTC","Debox","Nxx")
	private val mbNxx736 = MolBarcodeNextera("CACATCCT","Hefel","Nxx")
	private val mbNxx738 = MolBarcodeNextera("AAGGATGT","Binot","Nxx")
	private val mbNxx739 = MolBarcodeNextera("ACACGATC","Cakax","Nxx")
	private val mbNxx741 = MolBarcodeNextera("GTATAACA","Tawah","Nxx")
	private val mbNxx742 = MolBarcodeNextera("TGCTCGAC","Yexic","Nxx")
	private val mbNxx743 = MolBarcodeNextera("AACTTGAC","Bezic","Nxx")
	private val mbNxx744 = MolBarcodeNextera("AGTTGCTT","Doyez","Nxx")
	private val mbNxx745 = MolBarcodeNextera("TCGGAATG","Xinay","Nxx")
	private val mbNxx747 = MolBarcodeNextera("TGTTCCGA","Yoxen","Nxx")
	private val mbNxx748 = MolBarcodeNextera("AGGATCTA","Difew","Nxx")
	private val mbNxx750 = MolBarcodeNextera("CCTATGCC","Jofij","Nxx")
	private val mbNxx752 = MolBarcodeNextera("ATAGCGTC","Fapix","Nxx")
	private val mbNxx754 = MolBarcodeNextera("ATTCTAGG","Folar","Nxx")
	private val mbNxx755 = MolBarcodeNextera("CATGATCG","Honok","Nxx")
	private val mbNxx757 = MolBarcodeNextera("GACAGTAA","Nedob","Nxx")
	private val mbNxx759 = MolBarcodeNextera("TCGCCTTG","Xijoy","Nxx")
	private val mbNxx761 = MolBarcodeNextera("GAAGAAGT","Nanat","Nxx")
	private val mbNxx764 = MolBarcodeNextera("AGGTTATC","Dizax","Nxx")
	private val mbNxx765 = MolBarcodeNextera("TTACGCAC","Zakec","Nxx")
	private val mbNxx766 = MolBarcodeNextera("CCTTCGCA","Joxih","Nxx")
	private val mbNxx767 = MolBarcodeNextera("AAGACACT","Bical","Nxx")
	private val mbNxx768 = MolBarcodeNextera("ATTGTCTG","Fotey","Nxx")
	private val mbNxx769 = MolBarcodeNextera("TCCAGCAA","Xedeb","Nxx")
	private val mbNxx770 = MolBarcodeNextera("TAATGAAC","Wayac","Nxx")
	private val mbNxx771 = MolBarcodeNextera("TCCTTGGT","Xezit","Nxx")
	private val mbNxx772 = MolBarcodeNextera("GTCTGATG","Teyay","Nxx")
	private val mbNxx773 = MolBarcodeNextera("GTCATCTA","Tefew","Nxx")
	private val mbNxx774 = MolBarcodeNextera("TTGAATAG","Zibod","Nxx")
	private val mbNxx775 = MolBarcodeNextera("TCTCGGTC","Xokix","Nxx")
	private val mbNxx776 = MolBarcodeNextera("CAGCAAGG","Hihar","Nxx")
	private val mbNxx777 = MolBarcodeNextera("GAACCTAG","Najod","Nxx")
	private val mbNxx778 = MolBarcodeNextera("CCAGAGCT","Janil","Nxx")
	private val mbNxx779 = MolBarcodeNextera("AACGCATT","Bepaz","Nxx")
	private val mbNxx780 = MolBarcodeNextera("CCAACATT","Jacaz","Nxx")
	private val mbNxx783 = MolBarcodeNextera("AATGTTCT","Botol","Nxx")
	private val mbNxx784 = MolBarcodeNextera("CGCCTTCC","Keloj","Nxx")
	private val mbNxx785 = MolBarcodeNextera("GACCAGGA","Nehin","Nxx")
	private val mbNxx787 = MolBarcodeNextera("ACAGGTAT","Carof","Nxx")
	private val mbNxx789 = MolBarcodeNextera("GCCGTCGA","Peten","Nxx")
	private val mbNxx790 = MolBarcodeNextera("TAAGCACA","Wapah","Nxx")
	private val mbNxx791 = MolBarcodeNextera("ACTAAGAC","Cobic","Nxx")
	private val mbNxx792 = MolBarcodeNextera("CAGCGGTA","Hikiw","Nxx")
	private val mbNxx793 = MolBarcodeNextera("GCCTAGCC","Pewij","Nxx")
	private val mbNxx794 = MolBarcodeNextera("TATCCAGG","Wojar","Nxx")
	private val mbNxx795 = MolBarcodeNextera("AGGTAAGG","Diwar","Nxx")
	private val mbNxx796 = MolBarcodeNextera("ATTCCTCT","Fojol","Nxx")
	private val mbNxx797 = MolBarcodeNextera("GTAACATC","Tacax","Nxx")
	private val mbNxx798 = MolBarcodeNextera("TCGCTAGA","Xilan","Nxx")
	private val mbNxx799 = MolBarcodeNextera("ATTATCAA","Fofeb","Nxx")
	private val mbNxx800 = MolBarcodeNextera("CAATAGTC","Hawix","Nxx")
	private val mbNxx801 = MolBarcodeNextera("GTCCACAG","Tehed","Nxx")
	private val mbNxx802 = MolBarcodeNextera("TCTGCAAG","Xopad","Nxx")
	private val mbNxx804 = MolBarcodeNextera("CTAACTCG","Lacok","Nxx")
	private val mbNxx805 = MolBarcodeNextera("GAAGGAAG","Narad","Nxx")
	private val mbNxx806 = MolBarcodeNextera("TGTAATCA","Yoboh","Nxx")
	private val mbNxx807 = MolBarcodeNextera("ACAGTTGA","Caton","Nxx")
	private val mbNxx808 = MolBarcodeNextera("CTATGCGT","Layet","Nxx")
	private val mbNxx809 = MolBarcodeNextera("GACCGTTG","Nekoy","Nxx")
	private val mbNxx810 = MolBarcodeNextera("TTGTCTAT","Zixof","Nxx")
	private val mbNxx811 = MolBarcodeNextera("ACTGTATC","Cotax","Nxx")
	private val mbNxx812 = MolBarcodeNextera("CTGCGGAT","Likif","Nxx")
	private val mbNxx814 = MolBarcodeNextera("TTAATCAG","Zafed","Nxx")
	private val mbNxx815 = MolBarcodeNextera("AGCATGGA","Defin","Nxx")
	private val mbNxx816 = MolBarcodeNextera("CTGTGGCG","Liyik","Nxx")
	private val mbNxx817 = MolBarcodeNextera("GATATCCA","Nofeh","Nxx")
	private val mbNxx818 = MolBarcodeNextera("TTATATCT","Zawol","Nxx")
	private val mbNxx819 = MolBarcodeNextera("AGGTCGCA","Dixih","Nxx")
	private val mbNxx820 = MolBarcodeNextera("CTACCAGG","Lajar","Nxx")
	private val mbNxx821 = MolBarcodeNextera("ACCAACTG","Cebey","Nxx")
	private val mbNxx822 = MolBarcodeNextera("TGCAAGTA","Yebiw","Nxx")
	private val mbNxx823 = MolBarcodeNextera("AGGTGCGA","Diyen","Nxx")
	private val mbNxx824 = MolBarcodeNextera("CGCTATGT","Kewot","Nxx")
	private val mbNxx825 = MolBarcodeNextera("GCCGCAAC","Pepac","Nxx")
	private val mbNxx826 = MolBarcodeNextera("TGTAACTC","Yobex","Nxx")


	/**
	 * Flip (turn 180 degrees) the contents of a barcode plate - contents of upper left corner become
	 * contents of lower right corner
	 * @param in barcode plate
	 * @return barcode plate with
	 */
	private def flip(in: MolBarcodeContents) = {
		val size = in.contents.size
		val cols = size match {
			case 96 => 12
			case 384 => 24
			case _ => throw new Exception("Invalid plate size")
		}
		val last = size - 1
		val flippedContents =
			(for (indx <- 0 to last) yield {
				Plate.getWellName(cols, last - indx) -> in.contents(Plate.getWellName(cols, indx))
			}).toMap
		in.copy(contents = flippedContents)
	}

	/**
	 * Trait for MID plates made up of pairs
 	 */
	private trait MIDPairPlate {
		// Wells per row
		val wPr: Int
		// Rows per plate
		val rPp: Int
		// Fill in pair
		def getPair(rowBC: MolBarcode, colBC: MolBarcode): MolBarcodePair
		// List of well names (important to be lazy to get initialized at right time - in particular if makeSet
		// is called before it is initialized that's trouble if it's not lazy)
		lazy val wellList: List[String] = Plate.getWellList(wPr, rPp)

		/**
		 * Make a MID set
		 * @param rows list of MIDs to go across rows
		 * @param cols list of MIDs to go across cols
		 * @return map of wells to MID pairs
		 */
		def makeSet(rows: List[MolBarcode], cols: List[MolBarcode]): Map[String, MolBarcodePair] = {
			(for {row <- 0 until rPp
				  col <- 0 until wPr
			} yield {
				wellList((row * wPr) + col) -> getPair(rows(row), cols(col))
			}).toMap
		}
	}

	/**
	 * Nextera MID pair plate
	 */
	private trait MIDNexteraPairPlate extends MIDPairPlate {
		def getPair(rowBC: MolBarcode, colBC: MolBarcode) = MolBarcodeNexteraPair(i5 = rowBC, i7 = colBC)
	}

	/**
	 * HK pair plate
	 */
	private trait HKPairPlate extends MIDPairPlate {
		def getPair(rowBC: MolBarcode, colBC: MolBarcode) = MolBarcodeNexteraPair(i5 = colBC, i7 = rowBC)
	}

	/**
	 * 96-well dimensions
	 */
	private trait Plate96 {
		val wPr = 12 // 12 Wells per row
		val rPp = 8  // 8 rows per plate
	}


	/**
	 * 384-well dimensions
	 */
	private trait Plate384 {
		val wPr = 24 // 24 Wells per row
		val rPp = 16 // 8 rows per plate
	}

	/**
	 * 96-well Nextera MID plate with pairs
	 */
	private object MIDNexteraPair96 extends MIDNexteraPairPlate with Plate96

	/**
	 * 384-well Nextera MID plate with pairs
	 */
	private object MIDNexteraPair384 extends MIDNexteraPairPlate with Plate384

	/**
	 * 384-well Housekeeping MID plate with pairs
	 */
	private object HKPair384 extends HKPairPlate with Plate384

	// Set up row and column contents for Nextera paired barcodes - then making the sets is easy
	private val mbSetABRows = List(mbS502,mbS503,mbS505,mbS506,mbS507,mbS508,mbS510,mbS511)
	private val mbSetCDRows = List(mbS513,mbS515,mbS516,mbS517,mbS518,mbS520,mbS521,mbS522)
	private val mbSetACCols = List(mbN701,mbN702,mbN703,mbN704,mbN705,mbN706,mbN707,mbN710,mbN711,mbN712,mbN714,mbN715)
	private val mbSetBDCols = List(mbN716,mbN718,mbN719,mbN720,mbN721,mbN722,mbN723,mbN724,mbN726,mbN727,mbN728,mbN729)
	private val mbSetERows = List(mbS517,mbS502,mbS503,mbS504,mbS505,mbS506,mbS507,mbS508)
	private val mbSetECols = List(mbN701,mbN702,mbN703,mbN704,mbN705,mbN706,mbN707,mbN708,mbN709,mbN710,mbN711,mbN712)

	private val mbSet384ERows = List(mbS502,mbS503,mbS505,mbS506,mbS507,mbS508,mbS510,mbS511,mbS513,mbS515,mbS516,
		mbS517,mbS518,mbS520,mbS521,mbS522)
	private val mbSet384ECols = List(mbN701,mbN702,mbN703,mbN704,mbN705,mbN706,mbN707,mbN710,mbN711,mbN712,mbN714,
		mbN715,mbN716,mbN718,mbN719,mbN720,mbN721,mbN722,mbN723,mbN724,mbN726,mbN727,mbN728,mbN729)

	private val mbSet384TcellRows = List(mbSxx1,mbSxx2,mbSxx3,mbSxx4,mbSxx5,mbSxx6,mbSxx7,mbSxx8,mbSxx9,mbSxx10,mbSxx11,
		mbSxx12,mbSxx13,mbSxx14,mbSxx15,mbSxx16)
	private val mbSet384TcellColA = List(mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,
		mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33,mbNxx33)
	private val mbSet384TcellColB = List(mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,
		mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34,mbNxx34)

	private val mbSet384HKRowsA = List(mbNxx1,mbNxx2,mbNxx3,mbNxx4,mbNxx5,mbNxx6,mbNxx7,mbNxx8,mbNxx9,mbNxx10,mbNxx11,
		mbNxx12,mbNxx13,mbNxx14,mbNxx15,mbNxx16)
	private val mbSet384HKRowsB = List(mbNxx17,mbNxx18,mbNxx19,mbNxx20,mbNxx21,mbNxx22,mbNxx23,mbNxx24,mbNxx25,mbNxx26,mbNxx27,
		mbNxx28,mbNxx29,mbNxx30,mbNxx31,mbNxx32)
	private val mbSet384HKCols = List(mbSxx1,mbSxx2,mbSxx3,mbSxx4,mbSxx5,mbSxx6,mbSxx7,mbSxx8,mbSxx9,mbSxx10,mbSxx11,mbSxx12,
		mbSxx13,mbSxx14,mbSxx15,mbSxx16,mbS504,mbSxx17,mbSxx18,mbSxx19,mbSxx20,mbSxx21,mbSxx22,mbSxx23)

	// NexteraXP v2 Index Set A
	val mbSetA = MolBarcodeContents(MIDNexteraPair96.makeSet(mbSetABRows,mbSetACCols))
	// NexteraXP v2 Index Set B
	val mbSetB = MolBarcodeContents(MIDNexteraPair96.makeSet(mbSetABRows,mbSetBDCols))
	// NexteraXP v2 Index Set C
	val mbSetC = MolBarcodeContents(MIDNexteraPair96.makeSet(mbSetCDRows,mbSetACCols))
	// NexteraXP v2 Index Set D
	val mbSetD = MolBarcodeContents(MIDNexteraPair96.makeSet(mbSetCDRows,mbSetBDCols))
	// NexteraXP v2 Index Set E
	val mbSetE = MolBarcodeContents(MIDNexteraPair96.makeSet(mbSetERows,mbSetECols))
	// NexteraXP v2 Index 384 Set E
	val mbSet384A = MolBarcodeContents(MIDNexteraPair384.makeSet(mbSet384ERows,mbSet384ECols))
	// TCR Plate A
	val mbSet384TCellA = MolBarcodeContents(MIDNexteraPair384.makeSet(mbSet384TcellRows,mbSet384TcellColA))
	// TCR Plate B
	val mbSet384TCellB = MolBarcodeContents(MIDNexteraPair384.makeSet(mbSet384TcellRows,mbSet384TcellColB))
	// HK (housekeeping) Plate A
	val mbSet384HKA = MolBarcodeContents(HKPair384.makeSet(mbSet384HKRowsA,mbSet384HKCols))
	// HK (housekeeping) Plate B
	val mbSet384HKB = MolBarcodeContents(HKPair384.makeSet(mbSet384HKRowsB,mbSet384HKCols))

	// Low input Metagenomic set
	val mbSet96LIMG = MolBarcodeContents(Map(
		"A01" -> MolBarcodeNexteraPair(mbSxx25, mbNxx731),
		"B01" -> MolBarcodeNexteraPair(mbSxx14, mbNxx46),
		"C01" -> MolBarcodeNexteraPair(mbSxx27, mbNxx733),
		"D01" -> MolBarcodeNexteraPair(mbSxx28, mbNxx734),
		"E01" -> MolBarcodeNexteraPair(mbSxx29, mbNxx735),
		"F01" -> MolBarcodeNexteraPair(mbSxx30, mbNxx736),
		"G01" -> MolBarcodeNexteraPair(mbSxx31, mbNxx34),
		"H01" -> MolBarcodeNexteraPair(mbSxx32, mbNxx738),
		"A02" -> MolBarcodeNexteraPair(mbSxx33, mbNxx739),
		"B02" -> MolBarcodeNexteraPair(mbSxx34, mbNxx48),
		"C02" -> MolBarcodeNexteraPair(mbSxx35, mbNxx741),
		"D02" -> MolBarcodeNexteraPair(mbSxx23, mbNxx742),
		"E02" -> MolBarcodeNexteraPair(mbSxx37, mbNxx743),
		"F02" -> MolBarcodeNexteraPair(mbSxx38, mbNxx744),
		"G02" -> MolBarcodeNexteraPair(mbSxx39, mbNxx745),
		"H02" -> MolBarcodeNexteraPair(mbSxx40, mbNxx33),
		"A03" -> MolBarcodeNexteraPair(mbSxx1, mbNxx747),
		"B03" -> MolBarcodeNexteraPair(mbSxx42, mbNxx748),
		"C03" -> MolBarcodeNexteraPair(mbSxx43, mbNxx37),
		"D03" -> MolBarcodeNexteraPair(mbSxx44, mbNxx750),
		"E03" -> MolBarcodeNexteraPair(mbSxx45, mbNxx39),
		"F03" -> MolBarcodeNexteraPair(mbSxx46, mbNxx752),
		"G03" -> MolBarcodeNexteraPair(mbSxx47, mbNxx41),
		"H03" -> MolBarcodeNexteraPair(mbSxx48, mbNxx754),
		"A04" -> MolBarcodeNexteraPair(mbSxx11, mbNxx755),
		"B04" -> MolBarcodeNexteraPair(mbSxx50, mbNxx36),
		"C04" -> MolBarcodeNexteraPair(mbSxx51, mbNxx757),
		"D04" -> MolBarcodeNexteraPair(mbSxx52, mbNxx38),
		"E04" -> MolBarcodeNexteraPair(mbSxx53, mbNxx759),
		"F04" -> MolBarcodeNexteraPair(mbSxx54, mbNxx40),
		"G04" -> MolBarcodeNexteraPair(mbSxx55, mbNxx761),
		"H04" -> MolBarcodeNexteraPair(mbSxx13, mbNxx42),
		"A05" -> MolBarcodeNexteraPair(mbSxx57, mbNxx49),
		"B05" -> MolBarcodeNexteraPair(mbSxx7, mbNxx764),
		"C05" -> MolBarcodeNexteraPair(mbSxx59, mbNxx765),
		"D05" -> MolBarcodeNexteraPair(mbSxx60, mbNxx766),
		"E05" -> MolBarcodeNexteraPair(mbSxx2, mbNxx767),
		"F05" -> MolBarcodeNexteraPair(mbSxx62, mbNxx768),
		"G05" -> MolBarcodeNexteraPair(mbSxx63, mbNxx769),
		"H05" -> MolBarcodeNexteraPair(mbSxx64, mbNxx770),
		"A06" -> MolBarcodeNexteraPair(mbSxx65, mbNxx771),
		"B06" -> MolBarcodeNexteraPair(mbSxx17, mbNxx772),
		"C06" -> MolBarcodeNexteraPair(mbSxx67, mbNxx773),
		"D06" -> MolBarcodeNexteraPair(mbSxx68, mbNxx774),
		"E06" -> MolBarcodeNexteraPair(mbSxx69, mbNxx775),
		"F06" -> MolBarcodeNexteraPair(mbSxx70, mbNxx776),
		"G06" -> MolBarcodeNexteraPair(mbSxx71, mbNxx777),
		"H06" -> MolBarcodeNexteraPair(mbSxx12, mbNxx778),
		"A07" -> MolBarcodeNexteraPair(mbSxx73, mbNxx779),
		"B07" -> MolBarcodeNexteraPair(mbSxx74, mbNxx780),
		"C07" -> MolBarcodeNexteraPair(mbSxx75, mbNxx50),
		"D07" -> MolBarcodeNexteraPair(mbSxx76, mbNxx44),
		"E07" -> MolBarcodeNexteraPair(mbSxx77, mbNxx783),
		"F07" -> MolBarcodeNexteraPair(mbSxx78, mbNxx784),
		"G07" -> MolBarcodeNexteraPair(mbSxx79, mbNxx785),
		"H07" -> MolBarcodeNexteraPair(mbSxx5, mbNxx32),
		"A08" -> MolBarcodeNexteraPair(mbSxx81, mbNxx787),
		"B08" -> MolBarcodeNexteraPair(mbSxx82, mbNxx47),
		"C08" -> MolBarcodeNexteraPair(mbSxx83, mbNxx789),
		"D08" -> MolBarcodeNexteraPair(mbSxx84, mbNxx790),
		"E08" -> MolBarcodeNexteraPair(mbSxx85, mbNxx791),
		"F08" -> MolBarcodeNexteraPair(mbSxx86, mbNxx792),
		"G08" -> MolBarcodeNexteraPair(mbSxx87, mbNxx793),
		"H08" -> MolBarcodeNexteraPair(mbSxx88, mbNxx794),
		"A09" -> MolBarcodeNexteraPair(mbSxx89, mbNxx795),
		"B09" -> MolBarcodeNexteraPair(mbSxx90, mbNxx796),
		"C09" -> MolBarcodeNexteraPair(mbSxx6, mbNxx797),
		"D09" -> MolBarcodeNexteraPair(mbSxx92, mbNxx798),
		"E09" -> MolBarcodeNexteraPair(mbSxx93, mbNxx799),
		"F09" -> MolBarcodeNexteraPair(mbSxx22, mbNxx800),
		"G09" -> MolBarcodeNexteraPair(mbSxx95, mbNxx801),
		"H09" -> MolBarcodeNexteraPair(mbSxx96, mbNxx802),
		"A10" -> MolBarcodeNexteraPair(mbSxx10, mbNxx43),
		"B10" -> MolBarcodeNexteraPair(mbSxx98, mbNxx804),
		"C10" -> MolBarcodeNexteraPair(mbSxx99, mbNxx805),
		"D10" -> MolBarcodeNexteraPair(mbSxx3, mbNxx806),
		"E10" -> MolBarcodeNexteraPair(mbSxx101, mbNxx807),
		"F10" -> MolBarcodeNexteraPair(mbSxx102, mbNxx808),
		"G10" -> MolBarcodeNexteraPair(mbSxx103, mbNxx809),
		"H10" -> MolBarcodeNexteraPair(mbSxx104, mbNxx810),
		"A11" -> MolBarcodeNexteraPair(mbSxx105, mbNxx811),
		"B11" -> MolBarcodeNexteraPair(mbSxx106, mbNxx812),
		"C11" -> MolBarcodeNexteraPair(mbSxx107, mbNxx45),
		"D11" -> MolBarcodeNexteraPair(mbSxx108, mbNxx814),
		"E11" -> MolBarcodeNexteraPair(mbSxx109, mbNxx815),
		"F11" -> MolBarcodeNexteraPair(mbSxx110, mbNxx816),
		"G11" -> MolBarcodeNexteraPair(mbSxx111, mbNxx817),
		"H11" -> MolBarcodeNexteraPair(mbSxx21, mbNxx818),
		"A12" -> MolBarcodeNexteraPair(mbSxx113, mbNxx819),
		"B12" -> MolBarcodeNexteraPair(mbSxx114, mbNxx820),
		"C12" -> MolBarcodeNexteraPair(mbSxx115, mbNxx821),
		"D12" -> MolBarcodeNexteraPair(mbSxx116, mbNxx822),
		"E12" -> MolBarcodeNexteraPair(mbSxx117, mbNxx823),
		"F12" -> MolBarcodeNexteraPair(mbSxx118, mbNxx824),
		"G12" -> MolBarcodeNexteraPair(mbSxx119, mbNxx825),
		"H12" -> MolBarcodeNexteraPair(mbSxx120, mbNxx826)
		)
	)

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
	val mbTG96S1 = MolBarcodeContents(getTruGradeQuadriant(TransferWells.qFrom384(Q1)))
	// TruGrade 96-well Set2
	val mbTG96S2 = MolBarcodeContents(getTruGradeQuadriant(TransferWells.qFrom384(Q2)))
	// TruGrade 96-well Set3
	val mbTG96S3 = MolBarcodeContents(getTruGradeQuadriant(TransferWells.qFrom384(Q3)))
	// TruGrade 96-well Set4
	val mbTG96S4 = MolBarcodeContents(getTruGradeQuadriant(TransferWells.qFrom384(Q4)))

	// SQM Set1 (96 well plate)
	val mbSQM96S1 = MolBarcodeContents(Map(
		"A01" ->
			MolBarcodeSQMPair(MolBarcode("ATCGACTG","DualIndex_361_PondFwdPrimer"), MolBarcode("AAGTAGAG", "tagged_57")),
		"B01" ->
			MolBarcodeSQMPair(MolBarcode("GCTAGCAG","DualIndex_535_PondFwdPrimer"), MolBarcode("GGTCCAGA","tagged_726")),
		"C01" ->
			MolBarcodeSQMPair(MolBarcode("TACTCTCC","DualIndex_698_PondFwdPrimer"), MolBarcode("GCACATCT","tagged_630")),
		"D01" ->
			MolBarcodeSQMPair(MolBarcode("TGACAGCA","DualIndex_911_PondFwdPrimer"), MolBarcode("TTCGCTGA","tagged_954")),
		"E01" ->
			MolBarcodeSQMPair(MolBarcode("GCAGGTTG","DualIndex_305_PondFwdPrimer"), MolBarcode("AGCAATTC","tagged_190")),
		"F01" ->
			MolBarcodeSQMPair(MolBarcode("TTCCAGCT","DualIndex_208_PondFwdPrimer"), MolBarcode("CACATCCT","tagged_332")),
		"G01" ->
			MolBarcodeSQMPair(MolBarcode("TAGTTAGC","DualIndex_676_PondFwdPrimer"), MolBarcode("CCAGTTAG","tagged_393")),
		"H01" ->
			MolBarcodeSQMPair(MolBarcode("AGCGCTAA","DualIndex_940_PondFwdPrimer"), MolBarcode("AAGGATGT","tagged_52")),
		"A02" ->
			MolBarcodeSQMPair(MolBarcode("CGGTTCTT","DualIndex_35_PondFwdPrimer"), MolBarcode("ACACGATC","tagged_100")),
		"B02" ->
			MolBarcodeSQMPair(MolBarcode("TAGCATTG","DualIndex_324_PondFwdPrimer"), MolBarcode("CATGCTTA","tagged_375")),
		"C02" ->
			MolBarcodeSQMPair(MolBarcode("AATTCAAC","DualIndex_772_PondFwdPrimer"), MolBarcode("GTATAACA","tagged_741")),
		"D02" ->
			MolBarcodeSQMPair(MolBarcode("TTCACAGA","DualIndex_881_PondFwdPrimer"), MolBarcode("TGCTCGAC","tagged_908")),
		"E02" ->
			MolBarcodeSQMPair(MolBarcode("GCTCTCTT","DualIndex_40_PondFwdPrimer"), MolBarcode("AACTTGAC","tagged_34")),
		"F02" ->
			MolBarcodeSQMPair(MolBarcode("TGACTTGG","DualIndex_381_PondFwdPrimer"), MolBarcode("AGTTGCTT","tagged_236")),
		"G02" ->
			MolBarcodeSQMPair(MolBarcode("TATGGTTC","DualIndex_558_PondFwdPrimer"), MolBarcode("TCGGAATG","tagged_869")),
		"H02" ->
			MolBarcodeSQMPair(MolBarcode("CACTAGCC","DualIndex_715_PondFwdPrimer"), MolBarcode("TTGAGCCT","tagged_960")),
		"A03" ->
			MolBarcodeSQMPair(MolBarcode("AACCTCTT","DualIndex_41_PondFwdPrimer"), MolBarcode("TGTTCCGA","tagged_930")),
		"B03" ->
			MolBarcodeSQMPair(MolBarcode("CTACATTG","DualIndex_325_PondFwdPrimer"), MolBarcode("AGGATCTA","tagged_214")),
		"C03" ->
			MolBarcodeSQMPair(MolBarcode("GCGATTAC","DualIndex_736_PondFwdPrimer"), MolBarcode("CATAGCGA","tagged_367")),
		"D03" ->
			MolBarcodeSQMPair(MolBarcode("AATTGGCC","DualIndex_708_PondFwdPrimer"), MolBarcode("CCTATGCC","tagged_426")),
		"E03" ->
			MolBarcodeSQMPair(MolBarcode("AATTGCTT","DualIndex_43_PondFwdPrimer"), MolBarcode("TGTCGGAT","tagged_927")),
		"F03" ->
			MolBarcodeSQMPair(MolBarcode("TTGGTCTG","DualIndex_348_PondFwdPrimer"), MolBarcode("ATAGCGTC","tagged_250")),
		"G03" ->
			MolBarcodeSQMPair(MolBarcode("CATCCTGG","DualIndex_391_PondFwdPrimer"), MolBarcode("CCTACCAT","tagged_422")),
		"H03" ->
			MolBarcodeSQMPair(MolBarcode("GGATTAAC","DualIndex_765_PondFwdPrimer"), MolBarcode("ATTCTAGG","tagged_293")),
		"A04" ->
			MolBarcodeSQMPair(MolBarcode("CGCATATT","DualIndex_68_PondFwdPrimer"), MolBarcode("CATGATCG","tagged_373")),
		"B04" ->
			MolBarcodeSQMPair(MolBarcode("TCATTCGA","DualIndex_862_PondFwdPrimer"), MolBarcode("TCTGGCGA","tagged_880")),
		"C04" ->
			MolBarcodeSQMPair(MolBarcode("GTCCAATC","DualIndex_626_PondFwdPrimer"), MolBarcode("GACAGTAA","tagged_579")),
		"D04" ->
			MolBarcodeSQMPair(MolBarcode("CTTGGCTT","DualIndex_890_PondFwdPrimer"), MolBarcode("CAGGAGCC","tagged_357")),
		"E04" ->
			MolBarcodeSQMPair(MolBarcode("CCAACGCT","DualIndex_203_PondFwdPrimer"), MolBarcode("TCGCCTTG","tagged_866")),
		"F04" ->
			MolBarcodeSQMPair(MolBarcode("TCCACTTC","DualIndex_569_PondFwdPrimer"), MolBarcode("ATTATGTT","tagged_289")),
		"G04" ->
			MolBarcodeSQMPair(MolBarcode("AATCTCCA","DualIndex_914_PondFwdPrimer"), MolBarcode("GAAGAAGT","tagged_564")),
		"H04" ->
			MolBarcodeSQMPair(MolBarcode("GTCTGCAC","DualIndex_760_PondFwdPrimer"), MolBarcode("TACTTAGC","tagged_800")),
		"A05" ->
			MolBarcodeSQMPair(MolBarcode("CTGCTCCT","DualIndex_211_PondFwdPrimer"), MolBarcode("CGTTACCA","tagged_498")),
		"B05" ->
			MolBarcodeSQMPair(MolBarcode("TTAGCCAG","DualIndex_540_PondFwdPrimer"), MolBarcode("AGGTTATC","tagged_223")),
		"C05" ->
			MolBarcodeSQMPair(MolBarcode("GCTGATTC","DualIndex_572_PondFwdPrimer"), MolBarcode("TTACGCAC","tagged_938")),
		"D05" ->
			MolBarcodeSQMPair(MolBarcode("GAATCGAC","DualIndex_751_PondFwdPrimer"), MolBarcode("CCTTCGCA","tagged_438")),
		"E05" ->
			MolBarcodeSQMPair(MolBarcode("AGTCACCT","DualIndex_221_PondFwdPrimer"), MolBarcode("AAGACACT","tagged_38")),
		"F05" ->
			MolBarcodeSQMPair(MolBarcode("CACGATTC","DualIndex_573_PondFwdPrimer"), MolBarcode("ATTGTCTG","tagged_298")),
		"G05" ->
			MolBarcodeSQMPair(MolBarcode("GCTCCGAT","DualIndex_267_PondFwdPrimer"), MolBarcode("TCCAGCAA","tagged_851")),
		"H05" ->
			MolBarcodeSQMPair(MolBarcode("GCACAATT","DualIndex_67_PondFwdPrimer"), MolBarcode("TAATGAAC","tagged_786")),
		"A06" ->
			MolBarcodeSQMPair(MolBarcode("GCTGCACT","DualIndex_233_PondFwdPrimer"), MolBarcode("TCCTTGGT","tagged_861")),
		"B06" ->
			MolBarcodeSQMPair(MolBarcode("GAACTTCG","DualIndex_444_PondFwdPrimer"), MolBarcode("GTCTGATG","tagged_754")),
		"C06" ->
			MolBarcodeSQMPair(MolBarcode("CTGTATTC","DualIndex_571_PondFwdPrimer"), MolBarcode("GTCATCTA","tagged_745")),
		"D06" ->
			MolBarcodeSQMPair(MolBarcode("ATATCCGA","DualIndex_870_PondFwdPrimer"), MolBarcode("TTGAATAG","tagged_959")),
		"E06" ->
			MolBarcodeSQMPair(MolBarcode("GCGGACTT","DualIndex_281_PondFwdPrimer"), MolBarcode("TCTCGGTC","tagged_875")),
		"F06" ->
			MolBarcodeSQMPair(MolBarcode("GCGATATT","DualIndex_591_PondFwdPrimer"), MolBarcode("CAGCAAGG","tagged_352")),
		"G06" ->
			MolBarcodeSQMPair(MolBarcode("GAATATCA","DualIndex_897_PondFwdPrimer"), MolBarcode("GAACCTAG","tagged_559")),
		"H06" ->
			MolBarcodeSQMPair(MolBarcode("CAACTGAT","DualIndex_260_PondFwdPrimer"), MolBarcode("CCAGAGCT","tagged_388")),
		"A07" ->
			MolBarcodeSQMPair(MolBarcode("CCTGTCAT","DualIndex_276_PondFwdPrimer"), MolBarcode("AACGCATT","tagged_23")),
		"B07" ->
			MolBarcodeSQMPair(MolBarcode("GACGGTTA","DualIndex_773_PondFwdPrimer"), MolBarcode("CCAACATT","tagged_379")),
		"C07" ->
			MolBarcodeSQMPair(MolBarcode("CTATTAGC","DualIndex_677_PondFwdPrimer"), MolBarcode("CTGTAATC","tagged_542")),
		"D07" ->
			MolBarcodeSQMPair(MolBarcode("TCCAACCA","DualIndex_922_PondFwdPrimer"), MolBarcode("TATCTGCC","tagged_821")),
		"E07" ->
			MolBarcodeSQMPair(MolBarcode("CTGGCTAT","DualIndex_249_PondFwdPrimer"), MolBarcode("AATGTTCT","tagged_78")),
		"F07" ->
			MolBarcodeSQMPair(MolBarcode("CAGCGATT","DualIndex_58_PondFwdPrimer"), MolBarcode("CGCCTTCC","tagged_469")),
		"G07" ->
			MolBarcodeSQMPair(MolBarcode("CCATCACA","DualIndex_928_PondFwdPrimer"), MolBarcode("GACCAGGA","tagged_581")),
		"H07" ->
			MolBarcodeSQMPair(MolBarcode("GGCAATAC","DualIndex_742_PondFwdPrimer"), MolBarcode("TGCTGCTG","tagged_910")),
		"A08" ->
			MolBarcodeSQMPair(MolBarcode("CACTTCAT","DualIndex_274_PondFwdPrimer"), MolBarcode("ACAGGTAT","tagged_109")),
		"B08" ->
			MolBarcodeSQMPair(MolBarcode("CAAGCTTA","DualIndex_779_PondFwdPrimer"), MolBarcode("CAACTCTC","tagged_309")),
		"C08" ->
			MolBarcodeSQMPair(MolBarcode("AGGTACCA","DualIndex_920_PondFwdPrimer"), MolBarcode("GCCGTCGA","tagged_655")),
		"D08" ->
			MolBarcodeSQMPair(MolBarcode("TCCATAAC","DualIndex_768_PondFwdPrimer"), MolBarcode("TAAGCACA","tagged_778")),
		"E08" ->
			MolBarcodeSQMPair(MolBarcode("GTCCTCAT","DualIndex_277_PondFwdPrimer"), MolBarcode("ACTAAGAC","tagged_151")),
		"F08" ->
			MolBarcodeSQMPair(MolBarcode("AGTACTGC","DualIndex_636_PondFwdPrimer"), MolBarcode("CAGCGGTA","tagged_355")),
		"G08" ->
			MolBarcodeSQMPair(MolBarcode("CTTGAATC","DualIndex_625_PondFwdPrimer"), MolBarcode("GCCTAGCC","tagged_657")),
		"H08" ->
			MolBarcodeSQMPair(MolBarcode("CCAACTAA","DualIndex_942_PondFwdPrimer"), MolBarcode("TATCCAGG","tagged_818")),
		"A09" ->
			MolBarcodeSQMPair(MolBarcode("AATACCAT","DualIndex_283_PondFwdPrimer"), MolBarcode("AGGTAAGG","tagged_218")),
		"B09" ->
			MolBarcodeSQMPair(MolBarcode("TCAGGCTT","DualIndex_44_PondFwdPrimer"), MolBarcode("ATTCCTCT","tagged_291")),
		"C09" ->
			MolBarcodeSQMPair(MolBarcode("GAACGCTA","DualIndex_807_PondFwdPrimer"), MolBarcode("GTAACATC","tagged_732")),
		"D09" ->
			MolBarcodeSQMPair(MolBarcode("CTGACATC","DualIndex_623_PondFwdPrimer"), MolBarcode("TCGCTAGA","tagged_868")),
		"E09" ->
			MolBarcodeSQMPair(MolBarcode("GCCACCAT","DualIndex_284_PondFwdPrimer"), MolBarcode("ATTATCAA","tagged_288")),
		"F09" ->
			MolBarcodeSQMPair(MolBarcode("CGACTCTC","DualIndex_597_PondFwdPrimer"), MolBarcode("CAATAGTC","tagged_320")),
		"G09" ->
			MolBarcodeSQMPair(MolBarcode("TGCTATTA","DualIndex_784_PondFwdPrimer"), MolBarcode("GTCCACAG","tagged_747")),
		"H09" ->
			MolBarcodeSQMPair(MolBarcode("CTTCTGGC","DualIndex_646_PondFwdPrimer"), MolBarcode("TCTGCAAG","tagged_878")),
		"A10" ->
			MolBarcodeSQMPair(MolBarcode("ATGAATTA","DualIndex_787_PondFwdPrimer"), MolBarcode("AACAATGG","tagged_3")),
		"B10" ->
			MolBarcodeSQMPair(MolBarcode("TACTCCAG","DualIndex_537_PondFwdPrimer"), MolBarcode("CTAACTCG","tagged_500")),
		"C10" ->
			MolBarcodeSQMPair(MolBarcode("ATCATACC","DualIndex_723_PondFwdPrimer"), MolBarcode("GAAGGAAG","tagged_567")),
		"D10" ->
			MolBarcodeSQMPair(MolBarcode("CCTCTAAC","DualIndex_766_PondFwdPrimer"), MolBarcode("TGTAATCA","tagged_924")),
		"E10" ->
			MolBarcodeSQMPair(MolBarcode("ATCTTCTC","DualIndex_593_PondFwdPrimer"), MolBarcode("ACAGTTGA","tagged_110")),
		"F10" ->
			MolBarcodeSQMPair(MolBarcode("CAGCTCAC","DualIndex_757_PondFwdPrimer"), MolBarcode("CTATGCGT","tagged_509")),
		"G10" ->
			MolBarcodeSQMPair(MolBarcode("GGTTATCT","DualIndex_184_PondFwdPrimer"), MolBarcode("GACCGTTG","tagged_583")),
		"H10" ->
			MolBarcodeSQMPair(MolBarcode("TCCGCATA","DualIndex_824_PondFwdPrimer"), MolBarcode("TTGTCTAT","tagged_968")),
		"A11" ->
			MolBarcodeSQMPair(MolBarcode("TGCTTCAC","DualIndex_756_PondFwdPrimer"), MolBarcode("ACTGTATC","tagged_163")),
		"B11" ->
			MolBarcodeSQMPair(MolBarcode("GCTTCCTA","DualIndex_809_PondFwdPrimer"), MolBarcode("CTGCGGAT","tagged_534")),
		"C11" ->
			MolBarcodeSQMPair(MolBarcode("GACCATCT","DualIndex_188_PondFwdPrimer"), MolBarcode("GACCTAAC","tagged_584")),
		"D11" ->
			MolBarcodeSQMPair(MolBarcode("CTGGTATT","DualIndex_63_PondFwdPrimer"), MolBarcode("TTAATCAG","tagged_934")),
		"E11" ->
			MolBarcodeSQMPair(MolBarcode("TTAATCAC","DualIndex_758_PondFwdPrimer"), MolBarcode("AGCATGGA","tagged_195")),
		"F11" ->
			MolBarcodeSQMPair(MolBarcode("CGCGAATA","DualIndex_828_PondFwdPrimer"), MolBarcode("CTGTGGCG","tagged_544")),
		"G11" ->
			MolBarcodeSQMPair(MolBarcode("GCTCACCA","DualIndex_921_PondFwdPrimer"), MolBarcode("GATATCCA","tagged_616")),
		"H11" ->
			MolBarcodeSQMPair(MolBarcode("TCATGTCT","DualIndex_174_PondFwdPrimer"), MolBarcode("TTATATCT","tagged_944")),
		"A12" ->
			MolBarcodeSQMPair(MolBarcode("ATCCTTAA","DualIndex_933_PondFwdPrimer"), MolBarcode("AGGTCGCA","tagged_220")),
		"B12" ->
			MolBarcodeSQMPair(MolBarcode("TTCTTGGC","DualIndex_643_PondFwdPrimer"), MolBarcode("CTACCAGG","tagged_504")),
		"C12" ->
			MolBarcodeSQMPair(MolBarcode("CATCACTT","DualIndex_59_PondFwdPrimer"), MolBarcode("ACCAACTG","tagged_117")),
		"D12" ->
			MolBarcodeSQMPair(MolBarcode("CGAACTTC","DualIndex_570_PondFwdPrimer"), MolBarcode("TGCAAGTA","tagged_899")),
		"E12" ->
			MolBarcodeSQMPair(MolBarcode("GACATTAA","DualIndex_935_PondFwdPrimer"), MolBarcode("AGGTGCGA","tagged_222")),
		"F12" ->
			MolBarcodeSQMPair(MolBarcode("TTCACCTT","DualIndex_55_PondFwdPrimer"), MolBarcode("CGCTATGT","tagged_474")),
		"G12" ->
			MolBarcodeSQMPair(MolBarcode("CCAATCTG","DualIndex_351_PondFwdPrimer"), MolBarcode("GCCGCAAC","tagged_652")),
		"H12" ->
			MolBarcodeSQMPair(MolBarcode("CGACAGTT","DualIndex_33_PondFwdPrimer"), MolBarcode("TGTAACTC","tagged_923"))
	))


	// SQM Set1 flipped
	val mbSQM96S1flipped: MolBarcodeContents = flip(mbSQM96S1)

	/**
	 * Make a nextera single barcode set where each row is a single barcode
	 * @param bcs set of barcodes (one per row)
	 * @param width width of each ro
	 * @return collection of well->barcode
	 */
	private def makeRowBCs(bcs: List[MolBarcode], width: Int) = {
		val iter = bcs.toIterator
		val maxChar = ('A' + (bcs.length - 1)).toChar
		for {
			c <- 'A' to maxChar
			bc = iter.next
			i <- 1 to width
		} yield f"$c$i%02d" -> MolBarcodeNexteraSingle(bc)
	}

	/**
	 * Micro RNA barcode set
	 */
	val mbMiRNA =
		MolBarcodeContents(
			makeRowBCs(
				bcs = List(
					mbNxx35, mbNxx36, mbNxx37, mbNxx38, mbNxx39, mbNxx40, mbNxx41, mbNxx42,
					mbNxx43, mbNxx44, mbNxx45, mbNxx46, mbNxx47, mbNxx48, mbNxx49, mbNxx50
				),
				width = 24
			).toMap
		)
}
