package models

import org.broadinstitute.spreadsheets.{CellSheet, HeaderSheet}
import org.broadinstitute.spreadsheets.Utils.{getCSVFileData, getSheetData, isSpreadSheet}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import utils.{MessageHandler, Yes, YesOrNo}
import views.html.defaultpages.badRequest

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
    println(barcodesList)
    //TODO validate/cleanse data file.
    //TODO process barcodesList to add each entry to database
    //TODO remove this when done.
    Future.successful(Yes(0))
  }
  //TODO: parse barcodes file
  //TODO: Return number of barcodes added
}
