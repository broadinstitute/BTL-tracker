package models

import models.initialContents.MolecularBarcodes.MolBarcodeWell

import scala.concurrent.Future

/**
  * Created by amr on 1/25/2018.
  */
case class BarcodeSet(
                         name: String,
                         setType: String,
                         contents: Map[String, MolBarcodeWell]
                       ) {
  def isValidSize: Boolean = BarcodeSet.validSizes.contains(getSize)
  def getSize: Int = contents.size
}

object BarcodeSet {
  val PLATE96 = 96
  val PLATE384 = 384
  private val validSizes = List(PLATE96, PLATE384)

//  def readSet: Future[List[BarcodeSet]] = DBBarcodeSet.readSet
  def writeSet (bs:BarcodeSet) = DBBarcodeSet.writeSet(bs)
}