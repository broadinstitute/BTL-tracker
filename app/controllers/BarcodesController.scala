package controllers
import models.BarcodeSet.BarcodeSet
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.{BarcodeWell, BarcodesFile}
import play.api.mvc._
import utils.{MessageHandler, Yes}
import utils.MessageHandler.FlashingKeys
import validations.BarcodesValidation._
import models.db.{DBOpers, TrackerCollection}

import scala.concurrent.Future
import models.initialContents.MolecularBarcodes._
import reactivemongo.bson
import reactivemongo.bson.BSONDocument
/**
  * Created by amr on 11/13/2017.
  */

/**
  *
  */
object BarcodesController extends Controller {

  /**
    * Get the barcodes file
    *
    * @return
    */

  def displayBarcodesView(): Action[AnyContent] = Action { request =>
    Ok(views.html.barcodesFile(
      MessageHandler.addStatusFlash(request, BarcodesFile.form.fill(BarcodesFile(setName = "")))
      )
    )
  }

  def getSetContents(barcodesList: List[Map[String, String]]):List[BarcodeWell] = {
    barcodesList.map(entry => {???})
  }

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
                  //TODO: Use this to start making the sets.

                  if (BarcodesFileExtension.isValidFilename(file.filename)) {
                    val result = BarcodesFile.barcodesFileToSheet(file.ref.file.getCanonicalPath)
                    val errors = result._2
                    if (errors.isEmpty) {
                      val barcodesList = result._1
                      val set = BarcodeSet(name = data.setName,
                        //TODO: Need to figure out how I'm going to populate contents
                        contents = getSetContents()
                      )
                      barcodesList.map(entry => {

                        entry.get("Well") match {
                          case Some(w) =>
                            // We can see two kinds of input: Ones with i5 and 7 seqs or ones with only i7 seqs.
                            if (PairedBarcodeFileHeaders.hasValidHeaders(entry) || SingleBarcodeFileHeaders.hasValidHeaders(entry)) {
                                // Proceed with treating this entry as a pair
                              val i7Seq = entry.get("P7 Index")
                              val i5Seq = entry.get("P5 Index")
                              val pairName = entry.get("name")
                              Seq(i7Seq, i5Seq, pairName) match {
                                  // We have a i7 barcode, i5 barcode, and a name
                                case Seq(Some(i7), Some(i5), Some(n)) =>
                                  // Add these barcodes to barcode collection, but only if they don't exist already
                                  val i5Name = n.split("_").filter(n => n.contains("P5")).head.replace("P5-", "")
                                  val i7Name = n.split("_").filter(n => n.contains("P7")).head.replace("P7-", "")
                                  val i5Barcode = MolBarcode(i5, i5Name)
                                  val i7Barcode = MolBarcode(i7, i7Name)
                                  MolBarcode.create(i5Barcode)
                                  MolBarcode.create(i7Barcode)
                                  //TODO: Add this to the set in set collection
                                  MolBarcodeNexteraPair(
                                    i5 = i5Barcode,
                                    i7 = i7Barcode
                                  )
                                  Future.successful(Yes(0))
                                //TODO: We have a i7 barcode only and a name
                                case Seq(Some(i7), None, Some(n)) =>
                                  //TODO: Add these barcodes to the barcode collection but only if it doesn't exist already.
                                  val i7Barcode = MolBarcode(i7, n.replace("P7-", ""))
                                  MolBarcode.create(i7Barcode)
                                  //TODO: Add this to the set in set collection
                                  MolBarcodeNexteraSingle(i7Barcode)
                                  Future.successful(Yes(0))
                                //TODO: We have both barcodes but no names for them
                                case Seq(Some(i7), Some(i5), None) => ???

                                case _ => futureBadRequest(data, "")
                              }
                            } else {
                              futureBadRequest(data,
                                s"Well $w data doesn't conform to header expectations."
                              )
                            }
                          case None => futureBadRequest(data, "Well data missing from this entry.")
                        }
                        }
                      )
                        Future(FlashingKeys.setFlashingValue(
                          r = Redirect(routes.Application.index()),
                          k = FlashingKeys.Status, s = s"Barcodes: ${file.ref.file.getCanonicalPath}."
                        )
                      )
                    } else {
                      //TODO: Figure out how to make <br> show up in the barcodesFile html.
                      // Currently the <br> gets converted to 'lt' and 'gt' text so the <br> doesn't do what it's supposed to do.
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
