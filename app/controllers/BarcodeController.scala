package controllers
import java.nio.file.{Path, Paths}

import play.api.mvc._

/**
  * Created by amr on 11/13/2017.
  */

/**
  *
  */
object BarcodeController extends Controller{

  /**
    * Get the barcodes file
    * @return
    */
  def upload() = Action(parse.multipartFormData) { request =>
    request.body.file("barcodes").map { barcode_file =>

      // only get the last part of the filename
      // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
      val filename = Paths.get(barcode_file.filename).getFileName
      Ok(process(filename))
    }.getOrElse {
      Redirect(routes.Application.index()).flashing(
        "error" -> "Missing file")
    }
  }

  def process(file: Path) = {}
}
