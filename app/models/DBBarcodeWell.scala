package models

import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}

/**
  * Created by Amr on 1/26/2018.
  */
case class DBBarcodeWell (
                           location: String,
                           i5Contents: Option[String],
                           i7Contents: Option[String]
)

object DBBarcodeWell {
  protected val collectionNameKey = "mongodb.collection.wells"
  protected val collectionNameDefault = "wells"
  implicit val barcodeSetHandler = Macros.handler[DBBarcodeWell]
  val reader = implicitly[BSONDocumentReader[DBBarcodeWell]]
  val writer = implicitly[BSONDocumentWriter[DBBarcodeWell]]
}