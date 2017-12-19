import org.scalatest.{FlatSpec, Matchers}
import validations.BarcodesValidation._

/**
  * Created by Amr on 11/28/2017.
  * Perform direct tests on validation functions in validations package
  */
class ValidationSpec extends FlatSpec with Matchers {
  private val goodEntries = List(
    Map("Well" -> "A1", "Row" -> "A", "Column" -> "01", "P7 Index" -> "AAGTAGAG", "P5 Index" -> "ATCGACTG", "name" -> "Illumina_P5-Feney_P7-Biwid"),
    Map("Well" -> "b1", "Row" -> "B", "Column" -> "01", "P7 Index" -> "ggtccaga", "P5 Index" -> "GCTAGCAG", "name" -> "Illumina_P5-Poded_P7-Rojan"),
    Map("Well" -> "C01", "Row" -> "C", "Column" -> "01", "P7 Index" -> "GCACATCT", "P5 Index" -> "TaCtCTcC", "name" -> "Illumina_P5-Wexoj_P7-Pahol")
  )
  "BarcodeFileExtension.isValidFilename" should "be true when passed filenames with valid extensions" in {
    BarcodesFileExtension.isValidFilename("foo.csv") should be (true)
    BarcodesFileExtension.isValidFilename("/this/is/a/path/foo.xls") should be (true)
    BarcodesFileExtension.isValidFilename("/another/valid/file/path/foo.xlsx") should be (true)
  }
  it should "be false when passed filenames with bad extensions" in {
    BarcodesFileExtension.isValidFilename("foo.txt") should be (false)
    BarcodesFileExtension.isValidFilename("/some/path/foo.tab") should be (false)
    BarcodesFileExtension.isValidFilename("/another/invalid/path/badfile") should be (false)
  }

  "BarcodeWellValidations" should "return true for valid wells and false for invalid ones" in {
    BarcodeWellValidations.isValidWell("A1") should be (true)
    BarcodeWellValidations.isValidWell("P09") should be (true)
    BarcodeWellValidations.isValidWell("G11") should be (true)

  }
  it should "return false when well row is not A thru P or column is not 1 thru 24" in {
    BarcodeWellValidations.isValidWell("z06") should be (false)
    BarcodeWellValidations.isValidWell("A99") should be (false)
    BarcodeWellValidations.isValidWell("Z99") should be (false)
  }

  "BarcodeSeqValidations" should "return true if seq only contains DNA bases" in {
    BarcodeSeqValidations.isValidSeq("ACCTATGC") should be (true)
    BarcodeSeqValidations.isValidSeq("CtTCTgGC") should be (true)
    BarcodeSeqValidations.isValidSeq("gatccctt") should be (true)

  }
  it should "return false if seq contains any non-DNA letters" in {
    BarcodeSeqValidations.isValidSeq("123456") should be (false)
    BarcodeSeqValidations.isValidSeq("ACCTxTGC") should be (false)
    BarcodeSeqValidations.isValidSeq("ACcTnTGC") should be (false)
  }

  it should "return true if seq is 8 characters long" in {
    BarcodeSeqValidations.isValidLength("ACCTATGC") should be (true)
    BarcodeSeqValidations.isValidLength("CtTCTgGC") should be (true)
    BarcodeSeqValidations.isValidLength("gatccctt") should be (true)
  }
  it should "return false if seq is not 8 characters long" in {
    BarcodeSeqValidations.isValidLength("ACCTA") should be (false)
    BarcodeSeqValidations.isValidLength("ACCTATGCACCTATGC") should be (false)
    BarcodeSeqValidations.isValidLength("") should be (false)
  }

  "Manufacturers" should "be true when passed valid manufacturer name" in {
    Manufacturers.isValidManufacturer("Illumina")
  }
  it should "return false when passed an invalid manufacturer name" in {
    Manufacturers.isValidManufacturer("Microsoft")
  }
  "PairedBarcodeFileHeaders" should "return true when passed a map with valid headers" in {
    val aGoodEntry = goodEntries.head
    PairedBarcodeFileHeaders.hasValidHeaders(aGoodEntry) should be (true)
  }

  "PairedBarcodeFileHeaders" should "return false when passed a map with missing headers" in {
    val aBadEntry = goodEntries.head - "Well"
    PairedBarcodeFileHeaders.hasValidHeaders(aBadEntry) should be (false)
  }

  "SingleBarcodeHeaders" should "return true when passed a map with valid headers" in {
    val aGoodEntry = goodEntries.head - "P5 Index"
    SingleBarcodeFileHeaders.hasValidHeaders(aGoodEntry) should be (true)
  }

  "SingleBarcodeHeaders" should "return false when passed a map with missing headers" in {
    val aBadEntry = goodEntries.head - "P7 Index"
    SingleBarcodeFileHeaders.hasValidHeaders(aBadEntry) should be (false)
  }

}
