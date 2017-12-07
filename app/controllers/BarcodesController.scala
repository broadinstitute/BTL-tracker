package controllers
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.BarcodesFile
import play.api.mvc._
import utils.{MessageHandler, Yes}
import utils.MessageHandler.FlashingKeys
import validations.BarcodesValidation._
import models.db.{ TrackerCollection, DBOpers }
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
    * Get the barcodes file
    *
    * @return
    */

  def displayBarcodesView(): Action[AnyContent] = Action { request =>
    Ok(views.html.barcodesFile(
      MessageHandler.addStatusFlash(request, BarcodesFile.form.fill(BarcodesFile(setName = None)))
      )
    )
  }

  def add() = {}

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
            case Some(barFile) =>
              barFile.file(BarcodesFile.fileKey) match {
                case Some(file) =>
                  if (BarcodesFileExtension.isValidFilename(file.filename)) {
                    val result = BarcodesFile.barcodesFileToSheet(file.ref.file.getCanonicalPath)
                    val errors = result._3
                    if (errors.isEmpty) {
                      val barcodesList = result._2
                      barcodesList.map(entry => {
                        //TODO: Need to figure out how I'm going to insert data into DB
                        /**
                          * Option 1: Pass the barcodesList data out to here and add it to the DB at this level. This
                          * allows me to keep the current architecture where we only do DB operation if result._2.isEmpty.
                          *
                          * Option 2: Do the DB operations in insertBarcodesFile, which means I need to figure out
                          * how to pass a Future 'unsuccessful' back out of that method that will trigger the app
                          * to show the errors on the web interface for the user.
                          */
                        //TODO: Trying option 1. Here we are entering the data into database into two collections at same time.
                        // Need to insert name and sequences into barcodes collection. Each barcode will need a unique
                        // id or can use the mongo object ID as unique ID.
                        // Need to insert set name and wells into set collection.
                        // ID of barcode in barcode set should appear in the corresponding wells of the set.

                        entry.get("Well") match {
                          case Some(w) =>
                            // We can see two kinds of input: Ones with i5 and 7 seqs or ones with only i7 seqs.
                            if (PairedBarcodeFileHeaders.hasValidHeaders(entry) || SingleBarcodeFileHeaders.hasValidHeaders(entry)) {
                                // Proceed with treating this entry as a pair
                              val i7Seq = entry.get("P7 Index")
                              val i5Seq = entry.get("P5 Index")
                              val pairName = entry.get("name")
                              Seq(i7Seq, i5Seq, pairName) match {
                                case Seq(Some(i7), Some(i5), Some(n)) =>
                                  //TODO: Add these barcodes to barcode collection, but only if they don't exist already
                                  val i5Barcode = MolBarcode(i5, n.split("_").filter(n => n.contains("P5")).head.replace("P5-", ""))
                                  val i7Barcode = MolBarcode(i7, n.split("_").filter(n => n.contains("P7")).head.replace("P7-", ""))
                                  MolBarcode.create(i5Barcode)
                                  //TODO: Add this to the set in set collection
                                  MolBarcodeNexteraPair(
                                    i5 = i5Barcode,
                                    i7 = i7Barcode
                                  )

                                  Future.successful(Yes(0))

                                case Seq(Some(i7), None, Some(n)) =>
                                  //TODO: Add these barcodes to the barcode collection but only if it doesn't exist already.
                                  val i7Barcode = MolBarcode(i7, n.replace("P7-", ""))
                                  //TODO: Add this to the set in set collection
                                  MolBarcodeNexteraSingle(i7Barcode)
                                  Future.successful(Yes(0))

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
                      result._1.map(
                        _ =>
                          FlashingKeys.setFlashingValue(
                            r = Redirect(routes.Application.index()),
                            k = FlashingKeys.Status, s = s"Barcodes: ${file.ref.file.getCanonicalPath}."
                          )
                      )
                    } else {
                      //TODO: Figure out how to make <br> show up in the barcodesFile html.
                      // Currently the <br> gets converted to 'lt' and 'gt' text so the <br> doesn't do what it's supposed to do.
                      val errorString = result._3.unzip._2.flatten.mkString("<br>")
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
