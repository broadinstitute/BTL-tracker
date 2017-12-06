package controllers
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.BarcodesFile
import play.api.mvc._
import utils.MessageHandler
import utils.MessageHandler.FlashingKeys
import validations.BarcodesValidation._
import scala.concurrent.Future

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
                    val result = BarcodesFile.insertBarcodesFile(file.ref.file.getCanonicalPath)
                    if (result._2.isEmpty) {

                      result._1.map(
                        _ =>
                          //TODO: Need to figure out how I'm going to insert data into DB
                          /**
                            * Option 1: Pass the barcodesList data out to here and add it to the DB at this level. This
                            * allows me to keep the current architecture where we only do DB operation if result._2.isEmpty.
                            *
                            * Option 2: Do the DB operations in insertBarcodesFile, which means I need to figure out
                            * how to pass a Future 'unsuccessful' back out of that method that will trigger the app
                            * to show the errors on the web interface for the user.
                            */
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
