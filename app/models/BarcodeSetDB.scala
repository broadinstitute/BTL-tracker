package models

import models.BarcodeWell.BarcodeWell
import models.db.DBOpers
import models.initialContents.MolecularBarcodes.{MolBarcodeNexteraPair, MolBarcodeSingle, MolBarcodeWell}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}

/**
  * Created by amr on 12/18/2017.
  */


case class BarcodeSetDB(
                     name: String,
                     contents: List[BarcodeWell]
                     ) {
  def isValidSize: Boolean = BarcodeSetDB.validSizes.contains(getSize)
  def getSize: Int = contents.size
}


object BarcodeSetDB extends DBOpers[BarcodeSetDB]{

  val PLATE96 = 96
  val PLATE384 = 384
  private val validSizes = List(PLATE96, PLATE384)
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
}

