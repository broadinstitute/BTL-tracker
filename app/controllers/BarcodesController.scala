package controllers
import models.DBBarcodeSet.BarcodeSet
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.BarcodesFile
import models.BarcodeWell.BarcodeWell
import play.api.mvc._
import utils.MessageHandler
import utils.MessageHandler.FlashingKeys
import validations.BarcodesValidation._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import models.initialContents.MolecularBarcodes._
import reactivemongo.core.commands.LastError

import scala.collection.mutable.ListBuffer

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
    * Insert barcodes into database.
    * @param barcodeObjects A list of barcode objects.
    * @return
    */
  def insertBarcodeObjects(barcodeObjects: List[Option[(String, Either[MolBarcode, MolBarcodeNexteraPair] with Product with Serializable)]]): Future[List[LastError]] = {
    val futuresList: List[Future[LastError]] = barcodeObjects.flatMap {
      case Some(b) => b._2 match {
        case Left(l) =>
          List(MolBarcode.create(l))
        case Right(r)=>
          List(MolBarcode.create(r.i5), MolBarcode.create(r.i7))
      }
      case None => List(Future.failed(new Exception("No barcodes found.")))
    }
    Future.sequence(futuresList)
  }

  /**
    * Create well objects from a list of barcode objects.
    * @param barcodeObjects list of MolBarcode or MolBarcodeNexteraPair objects
    * @return A list of BarcodeWell objects.
    */
  def makeSetWells(barcodeObjects: List[Option[(String, Either[MolBarcode, MolBarcodeNexteraPair] with Product with Serializable)]]): List[BarcodeWell] = {
    barcodeObjects.map(o => {
      o.get._2 match {
        case Left(i7Only) => BarcodeWell(location = o.get._1, i7Contents = Some(i7Only), i5Contents = None)
        case Right(pair) => BarcodeWell(location = o.get._1, i7Contents = Some(pair.i7), i5Contents = Some(pair.i5))
      }
    })
  }

  /**
    * Makes the barcode objects as either lone barcodes or paired barcodes for further processing.
    * @param barcodesList a list of entries from a barcodes spreadsheet. Each item in list corresponds to a row in the
    *                     original sheet.
    * @return a list of barcode wells. We use option but should never really have any none values here because
    *         validations would complain before we got to this point.
    */
  //TODO: We should use MolBarcodePair instead of MolbarcodeNexteraPair
  def makeBarcodeObjects(barcodesList: List[Map[String, String]]): List[Option[(String, Either[MolBarcode, MolBarcodeNexteraPair] with Product with Serializable)]] = {
    /**
      * Parse the barcode name from the name string from sheet (ex: Illumina_P5-Feney_P7-Biwid)
      * @param barcodeType: The hobbit name prefix indicating which name to extract (ex: P5, P7).
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

    // Map the barcodes to objects
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
                val pair = makePair(i7, i5, Some(n))
                Some(Tuple2(well, Right(pair)))

              case Seq(Some(i7), None, Some(n)) =>
                val i7Barcode = MolBarcode(seq = i7, name = getName("P7", n))
                Some(Tuple2(well, Left(i7Barcode)))

              case Seq(Some(i7), Some(i5), None) =>
                val anonPair = makePair(i7, i5, None)
                Some(Tuple2(well, Right(anonPair)))
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

                      //Convert them into objects that can be placed into wells
                      val barcodeObjects = makeBarcodeObjects(barcodesList)

                      //Enter barcode objects into the DB
                      val results = insertBarcodeObjects(barcodeObjects)

                      // If results list has any LastErrors where ok is not true, put them in errors.
                      results.flatMap(r => {
                        val notOkay = r.filter(lastError => !lastError.ok)
                        if (notOkay.nonEmpty) {
                          val errMsgs = notOkay.map(lastError => lastError.message)
                          futureBadRequest(data, errMsgs.mkString("\n"))
                        } else {
                          //Make wells out of them.
                          val setWells = makeSetWells(barcodeObjects)

                          //Make a set out of the barcodes
                          val set = BarcodeSet(name = data.setName,
                            contents = setWells
                          )
                          //Add the set to DB
                          BarcodeSet.create(set).map(
                            //TODO: Examine lastError to determine if the set already exists.
                            _ =>
                              FlashingKeys.setFlashingValue(
                                r = Redirect(routes.Application.index()),
                                k = FlashingKeys.Status, s = s"${set.contents.size} barcode pairs added as set ${data.setName}"
                            )
                          )
                        }
                      })
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
