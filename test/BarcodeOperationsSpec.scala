import models.BarcodeSet._
import models.initialContents.MolecularBarcodes._
import org.specs2.mutable._
import controllers.BarcodesController.makeBarcodeObjects
import models.{BarcodeSet, DBBarcodeSet}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import play.api.test._
import play.api.test.Helpers._


/**
  * Created by amr on 12/4/2017.
  * A set of tests for db operations related to barcodes.
  */
class BarcodeOperationsSpec extends Specification {
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
  private val setName = "BarcodeOperationsSpec"
  private val barcodeObjects = makeBarcodeObjects(barcodesList = goodEntries, bcType = NEXTERA_PAIR)
//  private val barcodeWells = makeSetWells(barcodeObjects)
  private val set = BarcodeSet(name = setName, contents = barcodeObjects.toMap, setType = NEXTERA_PAIR)

  // Credit to Luigi Plinge for sameAs method for testing if two traversables have same contents.
  // https://stackoverflow.com/questions/7434762/comparing-collection-contents-with-scalatest/24243753
  def sameAs[A](c: Traversable[A], d: Traversable[A]): Boolean =
    if (c.isEmpty) d.isEmpty
    else {
      val (e, f) = d span (c.head !=)
      if (f.isEmpty) false else sameAs(c.tail, e ++ f.tail)
    }

  "Good barcodes" should {
    {
      "a set should be created in DB" in {
        running(TestServer(3333)) {
          val result = Await.result(DBBarcodeSet.writeSet(set), Duration(5, SECONDS))
          result.ok mustEqual true
        }
      }
      "and original barcodes match queried barcodes" in {
        running(TestServer(3333)) {
          val setList = Await.result(DBBarcodeSet.readSet(setName), Duration(5, SECONDS))
          setList mustEqual set
        }
      }
      // This should always be last test
      "and finally be deleted from DB." in {
        running(TestServer(3333)) {
          val result2 = barcodeObjects.map{
            case (_, bw: MolBarcodeSQMPair) =>
              MolBarcode.delete(bw.i5)
              MolBarcode.delete(bw.i7)
            case (_, bw: MolBarcodeNexteraPair) =>
              MolBarcode.delete(bw.i5)
              MolBarcode.delete(bw.i7)
            case (_, bw: MolBarcodeNexteraSingle) =>
              MolBarcode.delete(bw.m)
            case (_, bw: MolBarcodeSingle) =>
              MolBarcode.delete(bw.m)
          }

          val notOk2 = result2.filter(futureLE => !Await.result(futureLE, Duration(5, SECONDS)).ok)
          notOk2.size mustEqual 0

          //Delete and test deletion of BarcodeSet contents
          Await.result(DBBarcodeSet.deleteByKey("name", setName), Duration(5, SECONDS)).ok mustEqual true
        }
      }
    }
  }
}
