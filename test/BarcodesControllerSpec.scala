import models.initialContents.MolecularBarcodes.MolBarcode
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by amr on 12/4/2017.
  * A set of tests for db operations related to barcodes.
  */
class BarcodesControllerSpec extends FlatSpec with Matchers{
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
  //TODO: test makeSetWells
  //TODO: test upload()
  "" should "" in {
    val result = goodBarcodes.map( b => MolBarcode.create(b))
    println(result)
  }
  // do the various db operations on them
  // test that what was expected happened.

}
