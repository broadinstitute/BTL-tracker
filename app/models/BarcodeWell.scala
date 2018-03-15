package models
import models.initialContents.MolecularBarcodes.{MolBarcode}

/**
  * Created by amr on 12/18/2017.
  */


case class BarcodeWell (
  location: String,
  i5Contents: Option[MolBarcode],
  i7Contents: Option[MolBarcode]
)
