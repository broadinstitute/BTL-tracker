package models

import models.BarcodeWell.BarcodeWell
import models.db.DBOpers
import models.initialContents.MolecularBarcodes.{MolBarcode, MolBarcodeNexteraPair, MolBarcodeSingle, MolBarcodeWell}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}

import scala.concurrent.Future

/**
  * Created by amr on 12/18/2017.
  */

//TODO: Might want to consider using enum for set type.
case class DBBarcodeSet(
                     name: String,
                     setType: String,
                     contents: List[DBBarcodeWell]
                     )


object DBBarcodeSet extends DBOpers[DBBarcodeSet] {


  protected val collectionNameKey = "mongodb.collection.sets"
  protected val collectionNameDefault = "sets"
  implicit val barcodeSetHandler = Macros.handler[DBBarcodeSet]
  val reader = implicitly[BSONDocumentReader[DBBarcodeSet]]
  val writer = implicitly[BSONDocumentWriter[DBBarcodeSet]]


  def readSets =
    DBBarcodeSet.read(BSONDocument())
      .map(barcodeSet => barcodeSet.map(
        bs =>
          bs.name -> bs.contents.map(bw => {
            MolBarcode.read(BSONDocument("name" -> bw.i7Contents.get))
              .map( bcList =>
                bcList.head)
              .map(i7bc => {
              bw.i5Contents match {
                //TODO: need to have another value in DB indicating what type of set this is (Nextera, SQM, etc..)
                case Some(i5) =>
                  MolBarcode.read(BSONDocument("name" -> i5))
                    .map( bcList =>
                      bcList.head)
                    .map({ i5bc =>
                    MolBarcodeNexteraPair(i5 = i5bc, i7 = i7bc)
                  })
                case None => MolBarcodeSingle(m = i7bc)
              }
            })
          })
      ))

  def writeSet(bs: BarcodeSet) = {
    val dbs = DBBarcodeSet(
      name = bs.name,
      setType = bs.setType,
      contents = ???

    )
  }
}

