package models
import models.initialContents.MolecularBarcodes.{MolBarcode, MolBarcodeWell}
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}
import scala.concurrent.Future
/**
  * Created by amr on 12/18/2017.
  */

//TODO: Review this with Thaniel, specifically the fact that i wanted to extend MolBarcodeWell so I could take advantage
//of the fact that things requiring a child type will also accept it's parent type.
case class BarcodeWell (
  location: String,
  i5Contents: Option[Future[MolBarcode]],
  i7Contents: Option[Future[MolBarcode]]
) extends MolBarcodeWell {
  //NEVER DO ANY OF THESE FROM THIS OBJECT
  def getSeq: String = ""
  override def getName: String = location
  override def isNextera: Boolean = false
}
