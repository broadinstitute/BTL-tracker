import models.BarcodeSet.BarcodeSet
import models.initialContents.MolecularBarcodes.MolBarcode
import org.specs2.mutable._
import controllers.BarcodesController.{insertBarcodeObjects, makeBarcodeObjects, makeSetWells}
import models.db.BarcodeSetCollection.db
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import play.api.test._
import play.api.test.Helpers._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument

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
  private val barcodeObjects = makeBarcodeObjects(goodEntries)
  private val barcodeWells = makeSetWells(barcodeObjects)
  private val set = BarcodeSet(name = setName, contents = barcodeWells)

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
      // Template for unit tests
//      "" in {
//        running(TestServer(3333)) {
//          0 mustEqual 0
//           }
//        }
      "be created in DB" in {
        running(TestServer(3333)) {
          //Create Barcodes
          val result = insertBarcodeObjects(barcodeObjects)
          val notOk = result.filter(lastError => !lastError.ok)
          notOk.size mustEqual 0
        }
      }
      "and have a set created for them" in {
        running(TestServer(3333)) {
          val result = Await.result(BarcodeSet.create(set), Duration(5, SECONDS))
          result.ok mustEqual true
        }
      }
      "and have their barcode IDs contained in the set" in {
        running(TestServer(3333)) {
          val setList = Await.result(BarcodeSet.read(BSONDocument("name" -> setName)), Duration(5, SECONDS))
          //Get data from DB
          val dbObjectIDs = setList.head.contents.map( w => w.i5Contents.get) ::: setList.head.contents.map(w => w.i7Contents.get)
          // Get original data
          val originalIDs = barcodeObjects.map(o => {
            o.get._2 match {
              case Left(l) =>
                val b = Await.result(MolBarcode.read(BSONDocument("seq" -> l.seq)), Duration(5, SECONDS))
                (b.head._id, b.head._id)
              case Right(r) =>
                val b1 = Await.result(MolBarcode.read(BSONDocument("seq" -> r.i7.seq)), Duration(5, SECONDS))
                val b2 = Await.result(MolBarcode.read(BSONDocument("seq" -> r.i5.seq)), Duration(5, SECONDS))
                (b1.head._id, b2.head._id)
            }
          })
          val originalIDsList = originalIDs.map(_._1) ++ originalIDs.map(_._2)
          // The object IDs we get from mongodb must equal the IDs contained in the barcode objects.
          sameAs(dbObjectIDs, originalIDsList) mustEqual true

          //A negative test to make sure sameAs will return false when two lists don't match
          sameAs(dbObjectIDs, List(1,2,3)) mustEqual false
          }
        }
      // This should always be last test
      "and finally be deleted from DB." in {
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

          //Delete and test deletion of BarcodeSet contents
          Await.result(BarcodeSet.delete(set), Duration(5, SECONDS)).ok mustEqual true

          // Drop and test drop barcodes collection.
          Await.result(db.collection[BSONCollection]("barcodes").drop(), Duration(5, SECONDS)) mustEqual true

          // Drop and test drop of sets collection
          Await.result(db.collection[BSONCollection]("sets").drop(), Duration(5, SECONDS)) mustEqual true

        }
      }
    }
  }

}
