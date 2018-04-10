package models.db

import models.BarcodeSet._
import models.initialContents.MolecularBarcodes._
import models.BarcodeSet
import models.db.DBBarcodeSet.WellLocation
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONHandler, Macros}
import reactivemongo.core.commands.LastError
import validations.BarcodesValidation.BarcodeWellValidations.getWellParts
import models.Plate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by amr on 12/18/2017.
  */

case class DBBarcodeSet(
                         name: String,
                         setType: String,
                         contents: Map[WellLocation, DBBarcodeWell]
                       ) {
}


object DBBarcodeSet extends DBOpers[DBBarcodeSet] {
  type WellLocation = String
  protected val collectionNameKey = "mongodb.collection.set"
  protected val collectionNameDefault = "set"
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

  val mapReader: BSONDocumentReader[Map[String, DBBarcodeWell]] =
    implicitly[BSONDocumentReader[Map[String, DBBarcodeWell]]]
  val mapWriter: BSONDocumentWriter[Map[String, DBBarcodeWell]] =
    implicitly[BSONDocumentWriter[Map[String, DBBarcodeWell]]]
  implicit val barcodeSetHandler: BSONDocumentReader[DBBarcodeSet]
    with BSONDocumentWriter[DBBarcodeSet]
    with BSONHandler[BSONDocument, DBBarcodeSet] = Macros.handler[DBBarcodeSet]
  val reader: BSONDocumentReader[DBBarcodeSet] = implicitly[BSONDocumentReader[DBBarcodeSet]]
  val writer: BSONDocumentWriter[DBBarcodeSet] = implicitly[BSONDocumentWriter[DBBarcodeSet]]

  //TODO: This needs to be made safer, will blow up if 'name' isn't in the document in DB.
  def getSetNames: Future[List[String]] = find(BSONDocument(), BSONDocument("name" -> 1))
    .map(_.map((b) => b.getAs[String]("name").get))

  /**
    * Checks if a set exists in the database.
    * @param setName the name of the set to check.
    * @return Future true if set exists, false if not.
    */
  def checkSet(setName: String): Future[Boolean] = {
    find(BSONDocument("name" -> setName), BSONDocument("name" -> 1)).map(_.nonEmpty)
  }

  def readSet(setName: String): Future[BarcodeSet] = {
//    if (dbQuery.lengthCompare(1) != 0) throw new Exception("Didn't find single barcode set")
    DBBarcodeSet.read(BSONDocument("name" -> setName))
      .map(docToSet)
      .map((bsList) => {
        if (bsList.lengthCompare(1) != 0) throw new Exception("Didn't find single barcode set")
        bsList.head
      })
  }

  def readAllSets(): Future[List[BarcodeSet]] = {
    DBBarcodeSet.read(BSONDocument()).map(docToSet)
  }

  private def docToSet(dbQuery: List[DBBarcodeSet]) =
  {
//    if (dbQuery.lengthCompare(1) != 0) throw new Exception("Didn't find single barcode set")
//    else {
    dbQuery.map((set) =>
      {
        val contents = set.contents.mapValues {
          case (barcode) =>
            set.setType match {
              case NEXTERA_PAIR => MolBarcodeNexteraPair(
                i5 = MolBarcode(seq = barcode.i5Seq.get,
                  name = barcode.i5Name.get),
                i7 = MolBarcode(seq = barcode.i7Seq.get,
                  name = barcode.i7Name.get)
              )
              case SQM_PAIR => MolBarcodeSQMPair(
                i5 = MolBarcode(seq = barcode.i5Seq.get,
                  name = barcode.i5Name.get),
                i7 = MolBarcode(seq = barcode.i7Seq.get,
                  name = barcode.i7Name.get)
              )
              case PAIR => MolBarcodePaired(
                i5 = MolBarcode(seq = barcode.i5Seq.get,
                  name = barcode.i5Name.get),
                i7 = MolBarcode(seq = barcode.i7Seq.get,
                  name = barcode.i7Name.get)
              )
              case NEXTERA_SINGLE => MolBarcodeNexteraSingle(
                m = MolBarcode(seq = barcode.i7Seq.get,
                  name = barcode.i7Name.get)
              )
              case SINGLE => MolBarcodeSingle(
                m = MolBarcode(seq = barcode.i7Seq.get,
                  name = barcode.i7Name.get)
              )
            }
        }
        BarcodeSet(name = set.name, setType = set.setType, contents = contents)
      }
    )
  }

  def writeSet(bs: BarcodeSet): Future[LastError] = {
    val dbs = DBBarcodeSet(
      name = bs.name,
      setType = bs.setType,
      contents = {
        bs.contents.map{
          case (location, well) =>
            val (row, col) = getWellParts(location).get
            val properWell = Plate.standardizeWellName(col = col, row = row.head)
            val content = well match
            {
              case paired: MolBarcodePair =>
                DBBarcodeWell(
                  i5Name = Some(paired.i5.name),
                  i5Seq = Some(paired.i5.seq),
                  i7Name = Some(paired.i7.name),
                  i7Seq = Some(paired.i7.seq))
              case single: MolBarcodeSingle =>
                DBBarcodeWell(
                  i5Name = None,
                  i5Seq = None,
                  i7Name = Some(single.m.name),
                  i7Seq = Some(single.m.seq))
            }
              properWell -> content
        }
      }
    )
    DBBarcodeSet.create(dbs)
  }
}
