import models.BarcodeSet.BarcodeSet
import models.initialContents.MolecularBarcodes.MolBarcode
import org.specs2.mutable._
import controllers.BarcodesController.{makeBarcodeObjects, makeSetWells, insertBarcodeObjects}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import play.api.test._
import play.api.test.Helpers._

/**
  * Created by amr on 12/4/2017.
  * A set of tests for db operations related to barcodes.
  */
class BarcodeOperationsSpec extends Specification{
  // This forces Specification to run the tests sequentially which is what we want here. If we don't, can run into
  // issue where testserver shuts down while another test is trying to access server because they run in parallel
  // by default.
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

  "Good barcodes" should {
    {
      "be created in DB" in {
        running(TestServer(3333)) {
          //Create Barcodes
          val result = insertBarcodeObjects(barcodeObjects)
          val notOk = result.filter(lastError => !lastError.ok)
          notOk.size mustEqual 0
        }
      }
      // This should always be last test
      "be deleted from DB" in {
        running(TestServer(3333)) {
          val result2 = barcodeObjects.map(b => {
            b.get._2 match {
              case Left(l) => MolBarcode.delete(l)
              case Right(r) =>
                MolBarcode.delete(r.i7)
                MolBarcode.delete(r.i5)
            }
          })
          val notOk2 = result2.filter(futureLE => !Await.result(futureLE, Duration(5, SECONDS)).ok)
          notOk2.size mustEqual 0
        }
      }
    }
  }
}
