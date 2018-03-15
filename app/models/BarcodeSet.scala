package models

import models.db.DBBarcodeSet.WellLocation
import models.initialContents.InitialContents.ContentsMap
import models.initialContents.MolecularBarcodes.MolBarcodeWell
import models.db.DBBarcodeSet

import scala.concurrent.Future

/**
  * Created by amr on 1/25/2018.
  */
case class BarcodeSet(
                         name: String,
                         setType: String,
                         contents: Map[WellLocation, MolBarcodeWell]
                       ) extends ContentsMap[MolBarcodeWell] {
  def isValidSize: Boolean = BarcodeSet.validSizes.contains(getSize)
  def getSize: Int = contents.size
}

object BarcodeSet {
  val PLATE96 = 96
  val PLATE384 = 384
  private val validSizes = List(PLATE96, PLATE384)
  val NEXTERA_PAIR = "NexteraPair"
  val SQM_PAIR = "SQMPair"
  val NEXTERA_SINGLE = "NexteraSingle"
  val SINGLE = "Single"
  val setTypes = List(NEXTERA_PAIR, NEXTERA_SINGLE, SINGLE, SQM_PAIR)


  def readSet (name: String): Future[BarcodeSet] = DBBarcodeSet.readSet(name)
  def writeSet (bs:BarcodeSet) = DBBarcodeSet.writeSet(bs)
}