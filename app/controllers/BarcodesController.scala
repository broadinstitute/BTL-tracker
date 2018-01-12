package controllers
import models.BarcodeSet.BarcodeSet
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
    * @return //TODO: Probably a better way than appending to a mutable list here but don't know how at the moment.
    */
  def insertBarcodeObjects(barcodeObjects: List[Option[(String, Either[MolBarcode, MolBarcodeNexteraPair] with Product with Serializable)]]): List[LastError] = {
    var errors = new ListBuffer[LastError]()
    barcodeObjects.map {
      case Some(b) => b._2 match {
        case Left(l) =>
          val res = Await.result(MolBarcode.create(l), 10.seconds)
          errors.append(res)
        case Right(r)=>
          val r1 = Await.result(MolBarcode.create(r.i5), 10.seconds)
          val r2 = Await.result(MolBarcode.create(r.i7), 10.seconds)
          errors.append(r1)
          errors.append(r2)
      }
      case None => None
    }
    errors.toList
  }

  /**
    * Create well objects from a list of barcode objects.
    * @param barcodeObjects list of MolBarcode or MolBarcodeNexteraPair objects
    * @return A list of BarcodeWell objects.
    */
  def makeSetWells(barcodeObjects: List[Option[(String, Either[MolBarcode, MolBarcodeNexteraPair] with Product with Serializable)]]): List[BarcodeWell] = {
    barcodeObjects.map(o => {
      o.get._2 match {
        case Left(i7Only) => BarcodeWell(location = o.get._1, i7Contents = Some(i7Only._id), i5Contents = None)
        case Right(pair) => BarcodeWell(location = o.get._1, i7Contents = Some(pair.i7._id), i5Contents = Some(pair.i5._id))
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
                      val errors = results.filter(p => !p.ok)
                      //TODO: Can evaluate errors here where if size isn't 0, their was a problem with a barcode.

                      //Make wells out of them.
                      val setWells = makeSetWells(barcodeObjects)

                      //Make a set out of the barcodes
                      val set = BarcodeSet(name = data.setName,
                        contents = setWells
                      )
                      //Add the set to DB
                      val response = BarcodeSet.create(set)
                      //TODO: I want to tell the user if the set already exists in the database.
                      // Having this blocking code here accomplishes that but not in an elegant way. I would prefer to use
                      // the messaging in the GUI to do this.
                      Await.result(response, 5.seconds)
//                      BarcodeSet.create(set).onComplete {
//                        case Success(s) =>
//                          if (s.ok) {
//                            // operation worked, can wrap things up.
//                            Future(FlashingKeys.setFlashingValue(
//                              r = Redirect(routes.Application.index()),
//                              k = FlashingKeys.Status, s = s"${set.contents.size} barcodes added as set ${data.setName}"
//                              )
//                            )
//                          } else {
//                            //operation failed, notify user
//                            futureBadRequest(data, s.err.getOrElse("Unable to add set to database."))
//                          }
//                        case Failure(f) => futureBadRequest(data,  "Barcode Set not created for an unknown reason.")
//                      }
//                      Await.result(response, 5.seconds) match {
//                        case e: Exception => futureBadRequest(data, e.message)
//                        case _ => None
//                      }
                      //TODO: I don't get why I can't remove this since the code is duplicated in the if (s.ok) block
                      // and the else statement returns futureBadRequest. I feel like all the return cases are handled.
                      Future(FlashingKeys.setFlashingValue(
                        r = Redirect(routes.Application.index()),
                        k = FlashingKeys.Status, s = s"${set.contents.size} barcode pairs added as set ${data.setName}"
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
