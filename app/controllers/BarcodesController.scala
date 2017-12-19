package controllers

import models.BarcodeSet.BarcodeSet
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.{BarcodeWell, BarcodesFile}
import play.api.mvc._
import utils.MessageHandler
import utils.MessageHandler.FlashingKeys
import validations.BarcodesValidation._
import scala.concurrent.Future
import models.initialContents.MolecularBarcodes._
/**
  * Created by amr on 11/13/2017.
  */

/**
  *
  */
object BarcodesController extends Controller {


  /**
    * Display view for the barcodes form.
    * @return
    */
  def displayBarcodesView(): Action[AnyContent] = Action { request =>
    Ok(views.html.barcodesFile(
      MessageHandler.addStatusFlash(request, BarcodesFile.form.fill(BarcodesFile(setName = "")))
      )
    )
  }

  /**
    * Make the wells for a particular set of barcodes provided the wells exist.
    * @param barcodesList a list of entries from a barcodes spreadsheet. Each item in list corresponds to a row in the
    *                     original sheet.
    * @return a list of barcode wells. We use option but should never really have any none values here because
    *         validations would complain before we got to this point.
    */
  def makeSetWells(barcodesList: List[Map[String, String]]):List[Option[BarcodeWell]] = {
    /**
      * Parse the barcode name from the name string from sheet (ex: Illumina_P5-Feney_P7-Biwid)
      * @param barcodeType: The hobbit name prefix indicating if it is the P5 or P7 name (ex: P5, P7).
      * @param pairName: The pair name to be parsed (ex: Illumina_P5-Feney_P7-Biwid).
      * @return A 'hobbit' name parsed out of the pairName (ex: Feney).
      */
    def getName(barcodeType: String, pairName: String): String = {
      pairName.split("_").filter(n => n.contains(barcodeType)).head.replace(s"$barcodeType-", "")
    }

    /**
      * Make a paired barcode object.
      * @param i7seq: the i7 barcode sequence.
      * @param i5seq: the i5 barcode sequence.
      * @param pairName: the pairName.
      * @return A MolBarcodeNexteraPair object.
      */
    def makePair(i7seq: String, i5seq: String, pairName: Option[String]): MolBarcodeNexteraPair = {
      // Add these barcodes to barcode collection, but only if they don't exist already
      pairName match {
        case Some(n) =>
          MolBarcodeNexteraPair(
            i5 = MolBarcode(i5seq, getName("P5", n)),
            i7 = MolBarcode(i7seq, getName("P7", n))
          )
        case none =>
          //TODO: Would prefer to be able to use None for name but needs significant refactoring of all barcode objects.
          MolBarcodeNexteraPair(
            i5 = MolBarcode(i5seq, ""),
            i7 = MolBarcode(i7seq, "")
          )
      }
    }
    // MakeSetWells calls
    barcodesList.map(entry => {
      entry.get("Well") match {
        case Some(well) =>
          // We can see two kinds of input: Ones with i5 and 7 seqs or ones with only i7 seqs.
          if (PairedBarcodeFileHeaders.hasValidHeaders(entry) || SingleBarcodeFileHeaders.hasValidHeaders(entry)) {
            // Proceed with treating this entry as a pair
            val i7Seq = entry.get("P7 Index")
            val i5Seq = entry.get("P5 Index")
            val pairName = entry.get("name")
            Seq(i7Seq, i5Seq, pairName) match {
              // We have a i7 barcode, i5 barcode, and a name
              case Seq(Some(i7), Some(i5), Some(n)) =>
                val pair = makePair(i7Seq.get, i5Seq.get, pairName)
                Some(BarcodeWell(
                  location = well,
                  i5Contents = Some(pair.i5),
                  i7Contents = Some(pair.i7)
                ))
              case Seq(Some(i7), None, Some(n)) =>
                Some(BarcodeWell(
                  location = well,
                  i5Contents = None,
                  i7Contents = Some(MolBarcode(seq = i7, name = getName("P7", n)))
                ))

              case Seq(Some(i7), Some(i5), None) =>
                val anonPair = makePair(i7Seq.get, i5Seq.get, None)
                Some(BarcodeWell(
                  location = well,
                  i5Contents = Some(anonPair.i5),
                  i7Contents = Some(anonPair.i7)
                ))
              case _ => None
            }
          } else {
            None
          }
        case None => None
      }
    }
    )
  }

  /**
    * Upload barcodes to the database.
    * @return
    */
  def upload(): Action[AnyContent] = {
    Action.async { implicit request => {
      def futureBadRequest(data: BarcodesFile, err: String) =
        Future.successful(badRequest(data, err))

      def badRequest(data: BarcodesFile, err: String) =
        BadRequest(
          views.html.barcodesFile(
            BarcodesFile.form.fill(data).withGlobalError(err)
          )
        )
      BarcodesFile.form.bindFromRequest()(request).fold(
        formWithErrors => {
          Future.successful(BadRequest(
            views.html.barcodesFile(
              MessageHandler.formGlobalError(formWithErrors, MessageHandler.validationError)
              )
            )
          )
        },
        data => {
          request.body.asMultipartFormData match {
            case Some(barData) =>
              barData.file(BarcodesFile.fileKey) match {
                case Some(file) =>
                  if (BarcodesFileExtension.isValidFilename(file.filename)) {
                    val result = BarcodesFile.barcodesFileToSheet(file.ref.file.getCanonicalPath)
                    val errors = result._2
                    if (errors.isEmpty) {
                      //Get the list of barcodes
                      val barcodesList = result._1
                      //Make wells out of them
                      val setWells = makeSetWells(barcodesList)
                      //Make a set out of the barcodes
                      val set = BarcodeSet(name = data.setName,
                        contents = setWells.map(w => w.get)
                      )
                      //Add barcodes to the database and count how many added successfully.
                      //TODO: This will catch when a key already exists for example but it's not properly showing the Exception in GUI
                      val insertionResults = set.contents.map(well => {
                        try {
                          MolBarcode.create(well.i5Contents.get)
                          MolBarcode.create(well.i7Contents.get)
                        } catch {
                          case e: Exception => futureBadRequest(data, e.getMessage)
                        }
                      })

                      //TODO: The flashing message is no longer showing up on screen.
                      Future(FlashingKeys.setFlashingValue(
                        r = Redirect(routes.Application.index()),
                        k = FlashingKeys.Status, s = s"$insertionResults barcodes added as set $set."
                      )
                    )
                    } else {
                      val errorString = result._2.unzip._2.flatten.mkString("<br>")
                      futureBadRequest(data, errorString)
                    }
                  } else {
                    futureBadRequest(data, "Barcode file is not an acceptable format.")
                  }
                case _ => futureBadRequest(data, "Barcode file not specified")
              }
            case _ => futureBadRequest(data, "Barcode file not specified")
          }
        }
      )
    }}
  }
}
