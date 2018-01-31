package models
import scala.concurrent.ExecutionContext.Implicits.global
import models.db.DBOpers
import models.initialContents.MolecularBarcodes.{MolBarcode, MolBarcodeWell}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}
import reactivemongo.core.commands.LastError

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

  //TODO: Review this with Thaniel
  def readSets: Future[List[BarcodeSet]] = {
    def getBarcode(bcName: String): Future[MolBarcode] = {
      MolBarcode.read(BSONDocument("name" -> bcName)).map(x => x.head)
    }
    def getSetContents(contents: List[DBBarcodeWell]): List[BarcodeWell] = {
      contents.map(dbBarcodeWell => {
        //If we have i5Contents
        if (dbBarcodeWell.i5Contents.nonEmpty) {
          BarcodeWell(
            location = dbBarcodeWell.location,
            i5Contents = Some(getBarcode(dbBarcodeWell.i5Contents.get)),
            i7Contents = Some(getBarcode(dbBarcodeWell.i7Contents.get))
          )
        } else {
          BarcodeWell(
            location = dbBarcodeWell.location,
            i5Contents = None,
            i7Contents = Some(getBarcode(dbBarcodeWell.i7Contents.get))
          )
        }
      })
    }
    DBBarcodeSet.read(BSONDocument()).map( dbBarcodeSetList =>
      dbBarcodeSetList.map(
        dbBarcodeSet =>
        BarcodeSet(
          name = dbBarcodeSet.name,
          setType = dbBarcodeSet.setType,
          contents = getSetContents(dbBarcodeSet.contents)
        )
      )
    )
  }

  def writeSet(bs: BarcodeSet): Future[LastError] = {
    def getBarcodeName(bc: Future[MolBarcode]): String = {

    }
//TODO: Figure out how we can get rid of the .results here to handle the futures rath
    val dbs = DBBarcodeSet(
      name = bs.name,
      setType = bs.setType,
      contents = {
        bs.contents.map(well => {
          DBBarcodeWell(i7Contents = Some(getBarcodeName(well.i7Contents.get)), i5Contents = Some(getBarcodeName(well.i5Contents.get)), location = well.location)
//          well.i5Contents match {
//            case Some(i5bc) => well.i7Contents.get.map(i7bc => DBBarcodeWell(i5Contents = Some(i5bc.result(???).name), location = well.location, i7Contents = Some(i7bc.name))
//            case None => well.i7Contents.get.map(i7bc => DBBarcodeWell(i5Contents = None, location = well.location, i7Contents = Some(i7bc.name))).result(???)
//          }
        })
      }
    )
    DBBarcodeSet.create(dbs)
  }
}

