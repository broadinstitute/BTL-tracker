import models.initialContents.MolecularBarcodes.MolBarcode
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import reactivemongo.core.commands.LastError

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
  private val goodBarcodes = List(
    MolBarcode("AAGTAGAG", "Biwid"),
    MolBarcode("ggtccaga", "Rojan"),
    MolBarcode("GCACATCT", "Pahol"),
    MolBarcode("TTCGCTGA", "Zepon"),
    MolBarcode("AGCAATTC", "Debox"),
    MolBarcode("CACATCCT", "Hefel"),
    MolBarcode("CCAGTTAG", "Jatod"),
    MolBarcode("AAGGATGT", "Binot"),
    MolBarcode("ACACGATC", "Cakax"),
    MolBarcode("CATGCTTA", "Hopow")
  )
  running(TestServer(3333)) {
    "Good barcodes" should {
      {
        "be created in a local database" in {
          running(TestServer(3333)) {
            // Insert all the barcodes in the database and return the results.
            val results = goodBarcodes.map(b => Await.result(MolBarcode.create(b), Duration.Inf))
            // Get all the LastErrors for each creation so we can see if they all worked.
            val res = results.map(r => r.ok)
            // If they all worked, false should not be contained in res.
            res.contains(false) shouldEqual false
          }
        }
        "be deleted from the local database" in {
          running(TestServer(3333)) {
            val results = goodBarcodes.map(b => Await.result(MolBarcode.delete(b), Duration.Inf))
            val res = results.map(r => r.ok)
            res.contains(false) shouldEqual false
          }
        }
      }
    }
  }
}
