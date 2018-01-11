import models.initialContents.MolecularBarcodes.MolBarcode

private val goodBarcodes = List(
  MolBarcode("AAGTAGAG", "Biwid"),
  MolBarcode("ggtccaga", "Rojan"),
  MolBarcode("GCACATCT", "Pahol"),
  MolBarcode("TTCGCTGA", "Zepon"),
  MolBarcode("AGCAATTC", "Debox"),
  MolBarcode("CACATCCT", "Hefel"),
  MolBarcode("CCAGTTAG", "Jatod"),
  MolBarcode("AAGGATGT", "Binot"),
  MolBarcode("ACACGATC", "Cakax"),
  MolBarcode("CATGCTTA", "Hopow")
)

val response = goodBarcodes.map(b => MolBarcode.create(b))