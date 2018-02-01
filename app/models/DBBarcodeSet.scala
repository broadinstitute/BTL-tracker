package models
import models.db.DBOpers
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}
import reactivemongo.core.commands.LastError
import models.DBBarcodeSet.{DBWellPtr, WellLocation}
import scala.concurrent.Future

/**
  * Created by amr on 12/18/2017.
  */

//TODO: Might want to consider using enum for set type.
case class DBBarcodeSet(
                     name: String,
                     setType: String,
                     contents: Map[WellLocation, DBWellPtr]
                     ) {
}


object DBBarcodeSet extends DBOpers[DBBarcodeSet] {
  type DBWellPtr = String
  type WellLocation = String
  protected val collectionNameKey = "mongodb.collection.sets"
  protected val collectionNameDefault = "sets"
  import db.BSONMap
  implicit val mapperHandler = new BSONMap[DBWellPtr]
  val mapReader = implicitly[BSONDocumentReader[BSONMap[DBWellPtr]]]
  val mapWriter = implicitly[BSONDocumentWriter[BSONMap[DBWellPtr]]]
  implicit val barcodeSetHandler = Macros.handler[DBBarcodeSet]
  val reader = implicitly[BSONDocumentReader[DBBarcodeSet]]
  val writer = implicitly[BSONDocumentWriter[DBBarcodeSet]]

  //TODO: Review this with Thaniel

  def getSetNames: Future[List[String]] = {
    read(BSONDocument())
    //      .map( dbBarcodeSetList => dbBarcodeSetList.map( set => set.name))
  }

  def readSet(setName: String): Future[BarcodeSet] = {
    DBBarcodeSet.read(BSONDocument("name" -> setName)).map(x => {
      if (x.size != 1) throw new Exception("invalid set name")
      else {
        val set = x.head
        set.
          set.contents.map(ptr => {

        })


      }
    })
  }

  def writeSet(bs: BarcodeSet): Future[LastError] = {

    //    val dbs = DBBarcodeSet(
    //      name = bs.name,
    //      setType = bs.setType,
    //      contents = {
    //        bs.contents.map(well => {
    //          val i5Content = well._2. match {
    //            case Some(i5bc) => Some(i5bc.name)
    //            case None => None
    //          }
    //          DBBarcodeWell(i5Contents = i5Content, i7Contents = Some(well.i7Contents.get.name), location = well.location)
    //        }
    //        )
    //      }
    //    )
    //    DBBarcodeSet.create(???)
    //  }
    ???
  }
}
