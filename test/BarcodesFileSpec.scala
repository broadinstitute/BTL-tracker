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
  // Setup sheet data
  private val sheetToVals = (sh: CellSheet) => new HeaderSheet(sh)
  private val file = getClass.getResource("barcodes_test.csv").toString.replace("file:","")
  private val sheet = {
    getCSVFileData(file, sheetToVals)
  }
  private val barcodesList = (new sheet.RowValueIter).toList
//  private val result = barcodesList.map()
  "barcodes_test.csv" should "validate" in {
    BarcodesFileExtension.isValidFilename(file) should be (true)
  }
  "barcodes_test.csv contents" should "pass validateEntry" in {
    val validationResult = barcodesList.map(entry => validateEntry(entry))
    val result = validationResult.head.unzip
    print(result)
  }
}

