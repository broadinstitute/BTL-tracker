package models
import models.initialContents.MolecularBarcodes.{MolBarcode, MolBarcodeWell}
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}
import scala.concurrent.Future
/**
  * Created by amr on 12/18/2017.
  */

//TODO: Review this with Thaniel
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


object BarcodeWell {
  protected val collectionNameKey = "mongodb.collection.wells"
  protected val collectionNameDefault = "wells"
  implicit val barcodeSetHandler = Macros.handler[BarcodeWell]
  val reader = implicitly[BSONDocumentReader[BarcodeWell]]
  val writer = implicitly[BSONDocumentWriter[BarcodeWell]]
}
