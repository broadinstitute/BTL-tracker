package models

import models.db.DBOpers
import models.initialContents.MolecularBarcodes.MolBarcode
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONObjectID, Macros}

/**
  * Created by amr on 12/18/2017.
  */

case class BarcodeWell(
                        location: String,
                        i5Contents: Option[MolBarcode],
                        i7Contents: Option[MolBarcode]
                      )


object BarcodeWell  {
  protected val collectionNameKey = "mongodb.collection.wells"
  protected val collectionNameDefault = "wells"
  implicit val barcodeSetHandler = Macros.handler[BarcodeWell]
  val reader = implicitly[BSONDocumentReader[BarcodeWell]]
  val writer = implicitly[BSONDocumentWriter[BarcodeWell]]
}
////TODO: remove the parent BarcodeWell objects not necessary. See BarcodeSet for example.
//object BarcodeWell {
//
//  /**
//    * An object representing the well in a barcode set(aka plate).
//    * @param location Well location.
//    * @param i5Contents The BSONObjectID of the i5 barcode stored in the database.
//    * @param i7Contents The BSONObjectID of the i7 barcode stored in the database.
//    */
//  // TODO, never let i5Contents and i7Contents outside of BarcodeWell. Could do this by making the case class private.
//  // Anything that  consumes the BarcodeWell object should convert the i5/i7Contents into a BarcodeObject immediately.
//  case class BarcodeWell(
//                          location: String,
//                          i5Contents: Option[MolBarcode],
//                          i7Contents: Option[MolBarcode]
//                        )
//
//  object BarcodeWell extends DBOpers[BarcodeWell] {
//    protected val collectionNameKey = "mongodb.collection.wells"
//    protected val collectionNameDefault = "wells"
//    implicit val barcodeSetHandler = Macros.handler[BarcodeWell]
//    val reader = implicitly[BSONDocumentReader[BarcodeWell]]
//    val writer = implicitly[BSONDocumentWriter[BarcodeWell]]
//  }
//
//}