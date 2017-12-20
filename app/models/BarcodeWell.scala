package models

import models.db.DBOpers
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONObjectID, Macros}

/**
  * Created by amr on 12/18/2017.
  */

object BarcodeWell {

  /**
    * An object representing the well in a barcode set(aka plate).
    * @param location Well location.
    * @param i5Contents The BSONObjectID of the i5 barcode stored in the database.
    * @param i7Contents The BSONObjectID of the i7 barcode stored in the database.
    */
  case class BarcodeWell(
                          location: String,
                          i5Contents: Option[BSONObjectID],
                          i7Contents: Option[BSONObjectID]
                        )

  object BarcodeWell extends DBOpers[BarcodeWell] {
    protected val collectionNameKey = "mongodb.collection.wells"
    protected val collectionNameDefault = "wells"
    implicit val barcodeSetHandler = Macros.handler[BarcodeWell]
    val reader = implicitly[BSONDocumentReader[BarcodeWell]]
    val writer = implicitly[BSONDocumentWriter[BarcodeWell]]
  }

}