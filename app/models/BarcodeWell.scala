package models

import models.initialContents.MolecularBarcodes.MolBarcode

/**
  * Created by amr on 12/18/2017.
  */
case class BarcodeWell (
                         location: String,
                         p5Contents: Option[Seq[MolBarcode]],
                         p7Contents: Option[Seq[MolBarcode]]
                       )

