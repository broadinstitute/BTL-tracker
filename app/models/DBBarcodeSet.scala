package models
import models.db.{BSONMap, DBOpers}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros}
import reactivemongo.core.commands.LastError
import models.DBBarcodeSet.{DBWell, WellLocation}
import models.initialContents.MolecularBarcodes._

import scala.concurrent.Future

/**
  * Created by amr on 12/18/2017.
  */

//TODO: Might want to consider using enum for set type.
case class DBBarcodeSet(
                         name: String,
                         setType: String,
                         contents: Map[WellLocation, DBWell]
                       ) {
}


object DBBarcodeSet extends DBOpers[DBBarcodeSet] {
  type DBWell = DBBarcodeWell
  type WellLocation = String
  protected val collectionNameKey = "mongodb.collection.sets"
  protected val collectionNameDefault = "sets"
  implicit object BSONMap extends BSONDocumentWriter[Map[String, DBBarcodeWell]] with BSONDocumentReader[Map[String, DBBarcodeWell]] {

    def read(bson: BSONDocument): Map[String, DBBarcodeWell] = {
      val elements = bson.elements.map {
        // assume that all values in the document are BSONDocuments
        case (key, value) => key -> DBBarcodeWell.reader.read(value.seeAsTry[BSONDocument].get)
      }
      elements.toMap
    }


    def write(map: Map[String, DBBarcodeWell]): BSONDocument = {
      val elements = map.toStream.map {
        case (key, value) => key -> DBBarcodeWell.writer.write(value)
      }
      BSONDocument(elements)
    }
  }
  val mapReader = implicitly[BSONDocumentReader[Map[String, DBWell]]]
  val mapWriter = implicitly[BSONDocumentWriter[Map[String, DBWell]]]
  implicit val barcodeSetHandler = Macros.handler[DBBarcodeSet]
  val reader = implicitly[BSONDocumentReader[DBBarcodeSet]]
  val writer = implicitly[BSONDocumentWriter[DBBarcodeSet]]

  //TODO: Review this with Thaniel
  def getSetNames: Future[List[DBBarcodeSet]] = read(BSONDocument())

  def readSet(setName: String): Future[BarcodeSet] = {

    DBBarcodeSet.read(BSONDocument("name" -> setName))
      .map((x: List[DBBarcodeSet]) =>
      {
        if (x.size != 1) throw new Exception("Didn't find single barcode set")
        else {
          val set = x.head
          val contents = set.contents.mapValues {
            case (barcode) =>
              set.setType match {
                case "MolBarcodeNexteraPair" => MolBarcodeNexteraPair(
                  i5 = MolBarcode(seq = barcode.i5Seq.get,
                    name = barcode.i5Name.get),
                  i7 = MolBarcode(seq = barcode.i7Seq.get,
                    name = barcode.i7Name.get)
                )
                case "MolBarcodeSQMPair" => MolBarcodeSQMPair(
                  i5 = MolBarcode(seq = barcode.i5Seq.get,
                    name = barcode.i5Name.get),
                  i7 = MolBarcode(seq = barcode.i7Seq.get,
                    name = barcode.i7Name.get)
                )
                case "MolBarcodeNexteraSingle" => MolBarcodeNexteraSingle(
                  m = MolBarcode(seq = barcode.i7Seq.get,
                    name = barcode.i7Name.get)
                )
                case "MolBarcodeSingle" => MolBarcodeSingle(
                  m = MolBarcode(seq = barcode.i7Seq.get,
                    name = barcode.i7Name.get)
                )
              }
              }
          BarcodeSet(name = set.name, setType = set.setType, contents = contents)
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
