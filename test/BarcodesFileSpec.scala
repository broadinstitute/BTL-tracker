import models.BarcodesFile.validateEntry
import org.broadinstitute.spreadsheets.{CellSheet, HeaderSheet}
import org.broadinstitute.spreadsheets.Utils.getCSVFileData
import validations.BarcodesValidation.BarcodesFileExtension
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by amr on 11/30/2017.
  * Perform barcode validations on a concrete example file.
  */
class BarcodesFileSpec extends FlatSpec with Matchers{
  // Setup sheet data for a good sheet
  private val sheetToVals = (sh: CellSheet) => new HeaderSheet(sh)
  private val file = getClass.getResource("barcodes_test.csv").toString.replace("file:","")
  private val sheet = {
    getCSVFileData(file, sheetToVals)
  }
  private val barcodesList = (new sheet.RowValueIter).toList

  // Setup sheet data for a bad sheet
  private val badFile = getClass.getResource("barcodes_test_bad_data.csv").toString.replace("file:","")
  private val badSheet = {
    getCSVFileData(badFile, sheetToVals)
  }
  private val badBarcodesList = (new badSheet.RowValueIter).toList

  // A good map for sheetless testing.
  private val goodEntries = List(
    Map("Well" -> "A1", "Row" -> "A", "Column" -> "01", "P7 Index" -> "AAGTAGAG", "P5 Index" -> "ATCGACTG", "name" -> "Illumina_P5-Feney_P7-Biwid"),
    Map("Well" -> "b1", "Row" -> "B", "Column" -> "01", "P7 Index" -> "ggtccaga", "P5 Index" -> "GCTAGCAG", "name" -> "Illumina_P5-Poded_P7-Rojan"),
    Map("Well" -> "C01", "Row" -> "C", "Column" -> "01", "P7 Index" -> "GCACATCT", "P5 Index" -> "TaCtCTcC", "name" -> "Illumina_P5-Wexoj_P7-Pahol")
  )

  // A bad map for sheetless testing
  private val badEntries = List(
    // Bad well
    Map("Well" -> "Z25", "Row" -> "A", "Column" -> "01", "P7 Index" -> "AAGTAGAG", "P5 Index" -> "ATCGACTG", "name" -> "Illumina_P5-Feney_P7-Biwid"),
    // Bad seq content
    Map("Well" -> "B1", "Row" -> "B", "Column" -> "01", "P7 Index" -> "xGTCCAGA", "P5 Index" -> "GCTAGCAG", "name" -> "Illumina_P5-Poded_P7-Rojan"),
    // Bad seq length
    Map("Well" -> "C1", "Row" -> "C", "Column" -> "01", "P7 Index" -> "CATCT", "P5 Index" -> "TACTCTCC", "name" -> "Illumina_P5-Wexoj_P7-Pahol")
  )

  "validateEntry" should "return true for all entries of goodEntries" in {
		goodEntries.foreach(entry => validateEntry(entry).forall(v => v.isEmpty) shouldBe true)
	}

	"validateEntry" should "return false for all entries in badEntries" in {
		badEntries.foreach(entry => validateEntry(entry).forall(v => v.isEmpty) shouldBe false)
	}

  "barcodes_test.csv" should "validate" in {
    BarcodesFileExtension.isValidFilename(file) should be (true)
  }

  "barcodes_test.csv contents" should "pass validateEntry" in {
    val validationResult = barcodesList.map(entry => validateEntry(entry))
		validationResult.foreach(result => result.forall(v => v.isEmpty) shouldBe true)
  }

  "barcodes_test_bad_data.csv contents" should "fail validateEntry" in {
		val validationResult = badBarcodesList.flatMap(entry => validateEntry(entry))
		print(validationResult)
		validationResult.head.get should startWith ("A111")
  }

}

