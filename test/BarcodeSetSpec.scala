import models.DBBarcodeSet.BarcodeSet
import org.specs2.mutable._
import controllers.BarcodesController.{makeBarcodeObjects, makeSetWells}
import models.db.BarcodeSetCollection.db

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import play.api.test._
import play.api.test.Helpers._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument


/**
  * Created by amr on 1/16/2018.
  */
class BarcodeSetSpec extends Specification{
  override def is = args(sequential = true) ^ super.is
  private val goodEntries = List(
    Map("Well" -> "A1", "Row" -> "A", "Column" -> "01", "P7 Index" -> "AAGTAGAG", "P5 Index" -> "ATCGACTG", "name" -> "Illumina_P5-Feney_P7-Biwid"),
    Map("Well" -> "b1", "Row" -> "B", "Column" -> "01", "P7 Index" -> "ggtccaga", "P5 Index" -> "GCTAGCAG", "name" -> "Illumina_P5-Poded_P7-Rojan"),
    Map("Well" -> "C01", "Row" -> "C", "Column" -> "01", "P7 Index" -> "GCACATCT", "P5 Index" -> "TaCtCTcC", "name" -> "Illumina_P5-Wexoj_P7-Pahol")
  )
  private val barcodeObjects = makeBarcodeObjects(goodEntries)
  private val barcodeWells = makeSetWells(barcodeObjects)
  val sets = List(
    BarcodeSet(name = "set1", contents = barcodeWells),
    BarcodeSet(name = "set2", contents = barcodeWells),
    BarcodeSet(name = "set3", contents = barcodeWells),
    BarcodeSet(name = "set4", contents = barcodeWells),
    BarcodeSet(name = "set5", contents = barcodeWells)
  )
  object test extends Enumeration {
    type set = Value
  }
  "All BarcodeSets in set list" should {
    // Template for unit tests
    //      "" in {
    //        running(TestServer(3333)) {
    //          0 mustEqual 0
    //           }
    //        }

    "be created in DB" in {
      running(TestServer(3333))
      {
        // db.sets.createIndex({"name":1}, {unique:true, dropDups:true})
        val index: Index = Index(
          key = Seq(("name", IndexType.Ascending)),
          unique = true,
          dropDups = true
        )
        db.collection[BSONCollection]("sets").indexesManager.ensure(index)
        val results = sets.map(set => Await.result(BarcodeSet.create(set), Duration(5, SECONDS)).ok)
        results must not contain false

         }
      }
    "have one retrievable from DB" in {
      running(TestServer(3333)) {
        //TODO: Having trouble figuring out how to get the contents of the collection. 
        val query = BSONDocument("name" -> "set1")
        val result = Await.result(BarcodeSet.read(query), Duration(5, SECONDS))
        result.head.name mustEqual "set1"
      }
    }
    "have all retrievable from DB" in {
      running(TestServer(3333)) {
        val query = BSONDocument()
        val result = Await.result(BarcodeSet.read(query), Duration(5, SECONDS))
        result.size mustEqual 5

      }
    }
    "be removed in DB" in {
      running(TestServer(3333)) {
        val result = sets.map(s => Await.result(BarcodeSet.delete(s), Duration(5, SECONDS)).ok)
        result must not contain false
      }
    }
  }
}
