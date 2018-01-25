package models

import models.initialContents.MolecularBarcodes.MolBarcodeWell

/**
  * Created by amr on 1/25/2018.
  */
case class BarcodeSet(
                         name: String,
                         contents: List[MolBarcodeWell]
                       ) {
  def isValidSize: Boolean = BarcodeSet.validSizes.contains(getSize)
  def getSize: Int = contents.size
}

object BarcodeSet {
  val PLATE96 = 96
  val PLATE384 = 384
  private val validSizes = List(PLATE96, PLATE384)

  def getSets = BarcodeSetDB.getSets
}