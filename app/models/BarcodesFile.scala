package models

import org.broadinstitute.spreadsheets.{CellSheet, HeaderSheet}
import org.broadinstitute.spreadsheets.Utils.{getCSVFileData, getSheetData, isSpreadSheet}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import utils.{MessageHandler, Yes, YesOrNo}
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

  def validateEntry(entry: Map[String, String]): List[Tuple2[Boolean, String]] = {
    //Validate barcode well, map boolean result
    //Validate barcode seq
    //TODO: Need to work on this as it isn't working properly now. Run BarcodesFileSpec to see why.
    List(
      (BarcodeWell.isValidWell(entry.getOrElse("Well", "A:1")), "isValidWell"),
      (BarcodeSeq.isValidLength(entry.getOrElse("P7 Index", "P7 seq missing or is of nonstandard length")), "P7 isValidLength"),
      (BarcodeSeq.isValidLength(entry.getOrElse("P5 Index", "P7 seq missing or is of nonstandard length")), "P5 isValidLength"),
      (BarcodeSeq.isValidSeq(entry.getOrElse("P7 Index", "P7 seq missing or contains non-DNA characters")), "P5 isValidSeq"),
      (BarcodeSeq.isValidSeq(entry.getOrElse("P5 Index", "P7 seq missing or contains non-DNA characters")), "P7 isValidSeq")
    )
  }

  def insertBarcodesFile(file: String): Future[YesOrNo[Int]] = {

    def getFile = {
      val sheetToVals = (sh: CellSheet) => new HeaderSheet(sh)
      if (isSpreadSheet(file)) {
        //TODO: getSheetData currently doesn't work with excel files properly at least in windows.
        getSheetData(file, 0, sheetToVals)
      }
      else getCSVFileData(file, sheetToVals)
      }
    val sheet = getFile
    val barcodesList = (new sheet.RowValueIter).toList
    val validationResults = barcodesList.map(entry => validateEntry(entry))

    //TODO remove this when done.
    Future.successful(Yes(0))
  }
}
