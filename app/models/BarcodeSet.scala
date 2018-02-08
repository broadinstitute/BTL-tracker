package models

import models.DBBarcodeSet.WellLocation
import models.initialContents.InitialContents.ContentsMap
import models.initialContents.MolecularBarcodes.MolBarcodeWell

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


//  def readSet: Future[List[BarcodeSet]] = DBBarcodeSet.readSet
  def writeSet (bs:BarcodeSet) = DBBarcodeSet.writeSet(bs)
}