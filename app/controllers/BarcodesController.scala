package controllers
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.{BarcodeSet, BarcodeWell, BarcodesFile, DBBarcodeSet}
import models.initialContents.MolecularBarcodes
import play.api.mvc._
import utils.MessageHandler
import validations.BarcodesValidation._
import models.BarcodeSet._

import scala.concurrent.Future
import models.initialContents.MolecularBarcodes._
import reactivemongo.core.commands.LastError
import utils.MessageHandler.FlashingKeys

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
      MessageHandler.addStatusFlash(request, BarcodesFile.form.fill(BarcodesFile(setName = "", setType = "")))
      )
    )
  }

  /**
    * Insert barcodes into database.
    * @param barcodeObjects A list of barcode objects.
    * @return
    */
  def insertBarcodeObjects(barcodeObjects: List[(String, MolBarcodeWell)]): Future[List[LastError]] = {
    val futuresList: List[Future[LastError]] = barcodeObjects.flatMap {
      case (_, well) => well match {
        case bc: MolBarcodeNexteraSingle =>
          List(MolBarcode.create(bc.m))
        case bc: MolBarcodeNexteraPair =>
          List(MolBarcode.create(bc.i5), MolBarcode.create(bc.i7))
        case bc: MolBarcodeSingle =>
          List(MolBarcode.create(bc.m))
        case bc: MolBarcodeSQMPair =>
          List(MolBarcode.create(bc.i5), MolBarcode.create(bc.i7))
      }
    }
    Future.sequence(futuresList)
  }

  /**
    * Create well objects from a list of barcode objects.
    * @param barcodeObjects list of MolBarcode or MolBarcodeNexteraPair objects
    * @return A list of BarcodeWell objects.
    */
  def makeSetWells(barcodeObjects: List[Option[(String, MolBarcodeWell)]]): List[BarcodeWell] = {
    barcodeObjects.map(o => {
      o.get._2 match {
        case i7Only: MolBarcode => BarcodeWell(location = o.get._1, i7Contents = Some(i7Only), i5Contents = None)
        case pair: MolBarcodeNexteraPair => BarcodeWell(location = o.get._1, i7Contents = Some(pair.i7), i5Contents = Some(pair.i5))
      }
    })
  }

  /**
    * Makes the barcode objects as either lone barcodes or paired barcodes for further processing.
    * @param bcType: The set type.
    * @param barcodesList a list of entries from a barcodes spreadsheet. Each item in list corresponds to a row in the
    *                     original sheet. So Map[String, String] is a map of key/value pairs for a single row.
    * @return a list of barcode wells. We use option but should never really have any none values here because
    *         validations would complain before we got to this point.
    */
  def makeBarcodeObjects(bcType: String, barcodesList: List[Map[String, String]]): List[(String, MolBarcodeWell)]  = {
    /**
      * Parse the barcode name from the name string from sheet (ex: Illumina_P5-Feney_P7-Biwid)
      * @param prefix: The hobbit name prefix indicating which name to extract (ex: P5, P7).
      * @param pairName: The pair name to be parsed (ex: Illumina_P5-Feney_P7-Biwid).
      * @return A 'hobbit' name parsed out of the pairName (ex: Feney).
      */
    def getName(prefix: String, pairName: Option[String]): String = {
      pairName match {
        case Some(s) => s.split("_").filter(n => n.contains(prefix)).head.replace(s"$prefix-", "")
        case None => ""
      }

    }

    // Map the barcodes to objects
    barcodesList.map(entry => {
      entry.get("Well") match {
        case Some(well) =>
          // We can see two kinds of input: Ones with i5 and 7 seqs or ones with only i7 seqs.
          if (PairedBarcodeFileHeaders.hasValidHeaders(entry) || SingleBarcodeFileHeaders.hasValidHeaders(entry)) {
            // Proceed with treating this entry as a pair
            val i7Seq = entry.get("P7 Index")
            val i5Seq = entry.get("P5 Index")
            val name = entry.get("name")
            val barcodeWell = bcType match {
              case NEXTERA_PAIR =>
                MolBarcodeNexteraPair(
                  i5 = MolBarcode(i5Seq.get, getName("P5", name)),
                  i7 = MolBarcode(i7Seq.get, getName("P7", name))
                )
              case SQM_PAIR =>
                MolBarcodeSQMPair(
                  i5 = MolBarcode(i5Seq.get, getName("P5", name)),
                  i7 = MolBarcode(i7Seq.get, getName("P7", name))
                )
                //TODO: Need to find out how nextera single hobbit names are passed in to see if we need to further
                // process name to get just hobbit name.
              case NEXTERA_SINGLE => MolBarcodeNexteraSingle(m = MolBarcode(seq = i7Seq.get, name = name.get))
              case SINGLE => MolBarcodeSingle(m = MolBarcode(seq = i7Seq.get, name = name.get))
                //TODO: Need to return error that we didn't see a valid type.
              case _ => ???
            }
            well -> barcodeWell
          } else {
            //TODO: Return an error declaring that headers are invalid.
            ???
          }
          //TODO: Return an error saying there was no valid well.
        case None => ???
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
                      // Since these are required by the form, it's okay to use get like this.
                      val setType = barData.dataParts.get("setType").get.head
                      val setName = barData.dataParts.get("setKey").get.head
                      //Convert them into objects that can be placed into wells
                      val barcodeObjects = makeBarcodeObjects(setType, barcodesList)

                      //Enter barcode objects into the DB
                      val results = insertBarcodeObjects(barcodeObjects)

                      // If results list has any LastErrors where ok is not true, put them in errors.
                      results.flatMap(r =>
                      {
//
                        val notOkay = r.filter(lastError => !lastError.ok)
                        if (notOkay.nonEmpty) {
                          val errMsgs = notOkay.map(lastError => lastError.message)
                          futureBadRequest(data, errMsgs.mkString("\n"))
                        } else {
                          val barcodeSet = BarcodeSet(
                            name = setName,
                            setType = setType,
                            contents = barcodeObjects.toMap
                          )
                          DBBarcodeSet.writeSet(barcodeSet).map(
                            _ =>
                              FlashingKeys.setFlashingValue(
                                r = Redirect(routes.Application.index()),
                                k = FlashingKeys.Status,
                                s = s"${barcodeSet.contents.size} barcode pairs added as set ${data.setName}"
                              )
                            )
                        }
                      }
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
