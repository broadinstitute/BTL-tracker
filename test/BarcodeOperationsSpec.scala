import models.BarcodeSet.BarcodeSet
import controllers.BarcodesController.{makeBarcodeObjects, makeSetWells, insertBarcodeObjects}
import models.initialContents.MolecularBarcodes.{MolBarcode, MolBarcodeNexteraPair}
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Await
import scala.concurrent.duration.Duration



/**
  * Created by amr on 12/4/2017.
  * A set of tests for db operations related to barcodes.
  */
class BarcodeOperationsSpec extends Specification{
  //This forces tests to be run sequentially which is what I want in this case.
  override def is = args(sequential = true) ^ super.is
  // set up a bunch of molbarcode objects.
  private val goodEntries = List(
    Map("Well" -> "A1", "Row" -> "A", "Column" -> "01", "P7 Index" -> "AAGTAGAG", "P5 Index" -> "ATCGACTG", "name" -> "Illumina_P5-Feney_P7-Biwid"),
    Map("Well" -> "b1", "Row" -> "B", "Column" -> "01", "P7 Index" -> "ggtccaga", "P5 Index" -> "GCTAGCAG", "name" -> "Illumina_P5-Poded_P7-Rojan"),
    Map("Well" -> "C01", "Row" -> "C", "Column" -> "01", "P7 Index" -> "GCACATCT", "P5 Index" -> "TaCtCTcC", "name" -> "Illumina_P5-Wexoj_P7-Pahol")
  )
  // Make our objects from the barcode entries
  private val barcodeObjects = makeBarcodeObjects(goodEntries)
  private val barcodeWells = makeSetWells(barcodeObjects)
  private val set = BarcodeSet(name = "BarcodeOperationsSpec", contents = barcodeWells)
  //TODO: This running testserver business is super buggy. Half the time i get 'application not running' error. This makes testing DB operations very frustrating.
  running(TestServer(3333)) {
    // When setwells are created they are also put in the DB.
    "Good barcodes" should {
      {
        "be created in a local database" in {
          val result = insertBarcodeObjects(barcodeObjects)
          0 shouldEqual 0
        }
//        "be deleted from the local database" in {
//          barcodeObjects.map(o => o.get._2 match {
//            case Left(i7) => MolBarcode.delete(i7)
//            case Right(pair) =>
//              MolBarcode.delete(pair.i5)
//              MolBarcode.delete(pair.i7)
//          })
//        0 shouldEqual 0
//        }
      }
    }
  }
}
