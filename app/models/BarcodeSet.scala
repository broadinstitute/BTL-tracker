package models

import models.BarcodeWell.BarcodeWell
import models.db.DBOpers
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}

/**
  * Created by amr on 12/18/2017.
  */
object BarcodeSet {
  case class BarcodeSet(
                       name: String,
                       contents: List[BarcodeWell]
                       )

  object BarcodeSet extends DBOpers[BarcodeSet]{
    protected val collectionNameKey = "mongodb.collection.sets"
    protected val collectionNameDefault = "sets"
    implicit val barcodeSetHandler = Macros.handler[BarcodeSet]
    val reader = implicitly[BSONDocumentReader[BarcodeSet]]
    val writer = implicitly[BSONDocumentWriter[BarcodeSet]]
  }
}
