package controllers
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.BarcodesFile
import play.api.mvc._
import utils.MessageHandler
import utils.MessageHandler.FlashingKeys

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
                  BarcodesFile.insertBarcodesFile(file.ref.file.getCanonicalPath).map(
                    _ =>
                      FlashingKeys.setFlashingValue(
                      r = Redirect(routes.Application.index()),
                      k = FlashingKeys.Status, s = s"Barcodes: ${file.ref.file.getCanonicalPath}."
                    )
                  )
                case _ => futureBadRequest(data, "Barcode file not specified")
              }

            case _ => futureBadRequest(data, "Barcode file not specified")
          }
        }
      )
    }}
  }
}


//    Action.async {implicit request => {
//      def futureBadRequest(data: TransferFile, err: String) =
//        Future.successful(badRequest(data, err))
//      def badRequest(data: TransferFile, err: String) =
//        BadRequest(
//          views.html.transferFile(
//            TransferFile.form.fill(data).withGlobalError(err)
//          )
//        )
//
//      TransferFile.form.bindFromRequest()(request).fold(
//        formWithErrors => {
//          Future.successful(BadRequest(views.html.transferFile(
//            MessageHandler.formGlobalError(formWithErrors, MessageHandler.validationError))))
//        },
//        data => {
//          request.body.asMultipartFormData match {
//            case Some(tranFile) =>
//              tranFile.file(TransferFile.transferFileKey) match {
//                case Some(file) =>
//                  TransferFile.insertTransferFile(data, file.ref.file.getCanonicalPath)
//                    .map {
//                      case Yes((componentCount, trans)) =>
//                        val (whole, quads, slices, sliceWells, chers, cherWells, frees, inWells, outWells) =
//                          Transfer.getTransferCounts(trans)
//                        def plural(count: Int, word: String) =
//                          if (count == 1) word else word + "s"
//                        def makeLine(count: Int, desc: String, pre: String, post: String) =
//                          if (count == 0) None else Some(makeLine1(count, desc, pre, post))
//                        def makeLine1(count: Int, desc: String, pre: String, post: String) = {
//                          val preStr = if (pre.isEmpty) "" else pre + " "
//                          val postStr = if (post.isEmpty) "" else " " + post
//                          s"$count $preStr${plural(count, desc)}$postStr"
//                        }
//                        val start = makeLine1(trans.size, "transfer", "", "completed") +
//                          " (" + makeLine1(componentCount, "new component", "", "registered):")
//                        val lines = List(
//                          makeLine(whole, "component transfer", "entire", ""),
//                          makeLine(quads, "quadrant transfer", "", ""),
//                          makeLine(slices, "slice transfer", "",
//                            s"(${makeLine1(sliceWells, "well", "", "")})"
//                          ),
//                          makeLine(chers, "cherry pick transfer", "",
//                            s"(${makeLine1(cherWells, "well", "", "")})"
//                          ),
//                          makeLine(frees, "free transfer", "",
//                            s"(${makeLine1(inWells, "well", "input", "")}, " +
//                              s"${makeLine1(outWells, "well", "output", "")})"
//                          )
//                        )
//                        val counts =
//                          start + " " + lines.flatten.mkString(", ")
//                        FlashingKeys.setFlashingValue(
//                          r = Redirect(routes.Application.index()),
//                          k = FlashingKeys.Status, s = counts
//                        )
//                      case No(msg) =>
//                        badRequest(data, s"Error processing file: $msg")
//                    }
//                case _ =>
//                  futureBadRequest(data, "Transfer file must be specified")
//              }
//            case _ =>
//              futureBadRequest(data, "Transfer file must be specified")
//          }
//        }.recover {
//          case err => badRequest(data, MessageHandler.exceptionMessage(err))
//        }
//      )
//    }
//    }
//}
