package models

import controllers.BarcodesController.BadRequest
import org.broadinstitute.spreadsheets.{CellSheet, HeaderSheet}
import org.broadinstitute.spreadsheets.Utils.{getCSVFileData, getSheetData, isSpreadSheet}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import utils.{MessageHandler, No, Yes, YesOrNo}
import views.html.defaultpages.badRequest
import validations.BarcodesValidation._

import scala.concurrent.Future

/**
  * Created by amr on 11/15/2017.
  */
case class BarcodesFile (setName: Option[String])

object BarcodesFile {
  val fileKey = "barcodesFile"
  val setName = ""
  val form =
    Form(mapping(setName -> optional(text))(BarcodesFile.apply)(BarcodesFile.unapply))

  /** validateEntry
    * A function to validate each cell in a  row/entry in a sheet.
    * @param entry a row of a sheet.
    * @return A list of tuples containing the validity tuple of each cell in the row.
    */
  def validateEntry(entry: Map[String, String]): List[(Boolean, Option[String])] = {

    /** validateCell
      * Inner function for validating a specific cell's content.
      * @param cellKey they key in the entry. Used for error messages only.
      * @param cell the Option[String] representing the cell (ex: a get call on a map).
      * @param func the validation function to use.
      * @return A list of tuples, with each tuple representing the validation result, and if the result is false,
      *         a string returning the invalid data.
      */
    def validateCell(cellKey: String, cell: Option[String], func:(String) => Boolean): (Boolean, Option[String]) = {
      cell match {
        case Some(cellContents)  =>
          val result = func(cellContents)
          if (result) (result, None)
          else (result, Some(s"$cellContents is not a valid ${cellKey.toLowerCase()}."))
        case None => (false, Some(s"$cellKey not found."))
      }
    }
    // Populate the list of row/entry cell validations using validateCell.
    List(
      validateCell("Well", entry.get("Well"), BarcodeWell.isValidWell),
      validateCell("P7 Index", entry.get("P7 Index"), BarcodeSeq.isValidSeq),
      validateCell("P5 Index", entry.get("P5 Index"), BarcodeSeq.isValidSeq),
      validateCell("P7 Index", entry.get("P7 Index"), BarcodeSeq.isValidLength),
      validateCell("P5 Index", entry.get("P5 Index"), BarcodeSeq.isValidLength)
    )
  }

  /**
    * Validates a barcode file while also turning it into a sheet
    * @param file the path to the file
    * @return A tuple of Future and errors.
    */
  def insertBarcodesFile(file: String): (Future[YesOrNo[Int]], List[(Boolean, Option[String])]) = {
    /**
      * Gets the file and turns it into a HeaderSheet object.
      * @return HeaderSheet object.
      */
    def getFile = {
      val sheetToVals = (sh: CellSheet) => new HeaderSheet(sh)
      if (isSpreadSheet(file)) {
        // TODO: getSheetData currently doesn't work with excel files properly at least in windows.
        // Workaround is to paste csv data in notepad and save it, which strips weird characters from the data.
        getSheetData(file, 0, sheetToVals)
      }
      else getCSVFileData(file, sheetToVals)
      }
    val sheet = getFile
    val barcodesList = (new sheet.RowValueIter).toList
    //This creates a List(of sheet rows) of lists(of row data) of tuples (cell validation and message)
    val validationResults = barcodesList.map(entry => validateEntry(entry))
    val errors = validationResults.flatten.filter(p => !p._1)
    (Future.successful(Yes(0)), errors)
  }
}
