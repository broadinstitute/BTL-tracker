import java.io.InputStream

import models.BarcodesFile.validateEntry
import org.broadinstitute.spreadsheets.{CellSheet, HeaderSheet}
import org.broadinstitute.spreadsheets.Utils.{getCSVFileData, getSheetData, isSpreadSheet}
import validations.BarcodesValidation.{BarcodeSeq, BarcodeWell, BarcodesFileExtension, Manufacturers}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.Files
import play.api.mvc.MultipartFormData
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
    validateEntry(goodEntries.head).forall(p => p._1) should be (true)
    validateEntry(goodEntries.last).forall(p => p._1) should be (true)
    validateEntry(goodEntries(1)).forall(p => p._1) should be (true)
  }

  "validateEntry" should "return false for all entreis in badEntries" in {
    validateEntry(badEntries.head).forall(p => p._1) should be (false)
    validateEntry(badEntries.last).forall(p => p._1) should be (false)
    validateEntry(badEntries(1)).forall(p => p._1) should be (false)
  }

  "barcodes_test.csv" should "validate" in {
    BarcodesFileExtension.isValidFilename(file) should be (true)
  }

  "barcodes_test.csv contents" should "pass validateEntry" in {
    val validationResult = barcodesList.map(entry => validateEntry(entry))
    validationResult.head.unzip._1.forall(p => p) should be (true)
    val errors = validationResult.flatten.filter(p => !p._1)
    errors.size should be (0)
  }

  "barcodes_test_bad_data.csv contents" should "fail validateEntry" in {
    val badValidationResult = badBarcodesList.map(entry => validateEntry(entry))
    val errors = badValidationResult.flatten.filter(p => !p._1)
    badValidationResult.head.unzip._1.forall(p => p) should be (false)
    errors.size should be (7)
  }
}

