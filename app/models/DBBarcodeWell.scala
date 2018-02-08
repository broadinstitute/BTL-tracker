package models

import models.db.DBOpers
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}

/**
  * Created by Amr on 1/26/2018.
  */
case class DBBarcodeWell (
                           i5Name: Option[String],
                           i7Name: Option[String],
                           i5Seq: Option[String],
                           i7Seq: Option[String]
)

object DBBarcodeWell extends DBOpers[DBBarcodeWell] {
  protected val collectionNameKey = "mongodb.collection.wells"
  protected val collectionNameDefault = "wells"
  implicit val barcodeWellHandler = Macros.handler[DBBarcodeWell]
  val reader = implicitly[BSONDocumentReader[DBBarcodeWell]]
  val writer = implicitly[BSONDocumentWriter[DBBarcodeWell]]
}