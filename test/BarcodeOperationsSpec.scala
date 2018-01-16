import models.initialContents.MolecularBarcodes.MolBarcode
import org.scalatest.{FlatSpec, Matchers}
import org.specs2.mutable._

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import play.api.test._
import play.api.test.Helpers._

import scala.util.{Failure, Success}


/**
  * Created by amr on 12/4/2017.
  * A set of tests for db operations related to barcodes.
  */
class BarcodeOperationsSpec extends Specification{
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

  "Good barcodes" should {

    {

      "be added to the database" in {
        running(TestServer(3333)) {
          val response = goodBarcodes.map(b => MolBarcode.create(b))
          0 mustEqual 0
        }
      }
    }

  }
  // do the various db operations on them
  // test that what was expected happened.

}
