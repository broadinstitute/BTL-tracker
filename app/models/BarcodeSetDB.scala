package models

import models.BarcodeWell.BarcodeWell
import models.db.DBOpers
import models.initialContents.MolecularBarcodes.{MolBarcode, MolBarcodeNexteraPair, MolBarcodeSingle, MolBarcodeWell}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}

/**
  * Created by amr on 12/18/2017.
  */


case class BarcodeSetDB(
                     name: String,
                     contents: List[BarcodeWell]
                     )


object BarcodeSetDB extends DBOpers[BarcodeSetDB]{


  protected val collectionNameKey = "mongodb.collection.sets"
  protected val collectionNameDefault = "sets"
  implicit val barcodeSetHandler = Macros.handler[BarcodeSetDB]
  val reader = implicitly[BSONDocumentReader[BarcodeSetDB]]
  val writer = implicitly[BSONDocumentWriter[BarcodeSetDB]]

  def getSets =
    BarcodeSetDB.read(BSONDocument())
      .map(barcodeSet => barcodeSet.map(
        bs =>
          bs.name -> bs.contents.map(bw => {
            val i7 = bw.i7Contents.get
            bw.i5Contents match {
                //TODO: need to have another value in DB indicating what type of set this is (Nextera, SQM, etc..)
              case Some(i5) => MolBarcodeNexteraPair(i5 = i5, i7 = i7)
              case None => MolBarcodeSingle(m = i7)
            }
          })
      ))

  def putSet(bs: BarcodeSet) = {
    MolBarcode.read
  }
}

