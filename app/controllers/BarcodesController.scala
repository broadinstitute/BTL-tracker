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
                    BarcodesFile.insertBarcodesFile(file.ref.file.getCanonicalPath).map(
                      _ =>
                        FlashingKeys.setFlashingValue(
                          r = Redirect(routes.Application.index()),
                          k = FlashingKeys.Status, s = s"Barcodes: ${file.ref.file.getCanonicalPath}."
                        )
                    )
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
