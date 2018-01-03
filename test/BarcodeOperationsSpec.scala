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
        //TODO: Add good barcodes and verify they've been added to the DB either via response analysis or querying for them.
        val response = goodBarcodes.map(b => MolBarcode.create(b))

      }
      "not be duplicated in the database" in {
        //TODO: Try to add same barcodes and query DB to make sure we don't have duplicates.
      }
    }
  }
  // do the various db operations on them
  // test that what was expected happened.

}
