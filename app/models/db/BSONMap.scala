package models.db

import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONString}
import models.DBBarcodeSet.DBWell
/**
  * Created by amr on 2/1/2018.
  */
	trait BSONMap[T] {
    implicit def MapReader[V](implicit vr: BSONDocumentReader[V]): BSONDocumentReader[Map[String, V]] = new BSONDocumentReader[Map[String, V]] {
      def read(bson: BSONDocument): Map[String, V] = {
        val elements = bson.elements.map { tuple =>
          // assume that all values in the document are BSONDocuments
          tuple._1 -> vr.read(tuple._2.seeAsTry[BSONDocument].get)
        }
        elements.toMap
      }
    }

    implicit def MapWriter[V](implicit vw: BSONDocumentWriter[V]): BSONDocumentWriter[Map[String, V]] = new BSONDocumentWriter[Map[String, V]] {
      def write(map: Map[String, V]): BSONDocument = {
        val elements = map.toStream.map { tuple =>
          tuple._1 -> vw.write(tuple._2)
        }
        BSONDocument(elements)
      }
    }
}

object DBMapWellPtr extends BSONMap[DBWell]{
  implicit val read = MapReader
  implicit val write = MapWriter

}

