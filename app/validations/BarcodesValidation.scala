package validations


/**
  * Created by amr on 11/27/2017.
  */

//TODO: Need to update BarcodesValidation to distinguish between 96 and 384 well plates. Also make this better for handling different types of bad wells.
//TODO: Add comments
object BarcodesValidation{

  def validateBarcode(well: String, seq: String): Boolean = {
    BarcodeWellValidations.isValidWell(well) && BarcodeSeqValidations.isValidSeq(seq)
  }

  object BarcodesFileExtension extends Enumeration {
    type BarcodesFileExtension = Value
    val csv, xls, xlsx = Value

    def isValidFilename(filename: String): Boolean ={
      val ext = filename.split("\\.").last
      BarcodesFileExtension.values.exists(_.toString == ext)
    }
  }

  object PairedBarcodeFileHeaders extends Enumeration {
    type PairedBarcodeFileHeaders = Value
    val p7_seq = Value("P7 Index")
    val p5_seq = Value("P5 Index")
    val well = Value("Well")
    val name = Value("name")

    def hasValidHeaders(entry: Map[String, String]): Boolean = {
      val entryKeys = entry.keys.map(k => k.toLowerCase).toSet
      val enumValuesAsStrings = PairedBarcodeFileHeaders.values.map(v => v.toString.toLowerCase)
      entryKeys.subsetOf(enumValuesAsStrings)
    }
  }

  object SingleBarcodeFileHeaders extends Enumeration {
    type SingleBarcodeFileHeaders = Value
    val p7_seq = Value("P7 Index")
    val well = Value("Well")
    val name = Value("name")

    def hasValidHeaders(entry: Map[String, String]): Boolean = {
      val entryKeys = entry.keys.toSet
      val enumValuesAsStrings = SingleBarcodeFileHeaders.values.map(v => v.toString)
      enumValuesAsStrings.subsetOf(entryKeys)
    }
  }

  object Manufacturers extends Enumeration{
    type manufacturer = Value
    val Illumina = Value

    def isValidManufacturer(m: String): Boolean = {
      Manufacturers.values.exists(_.toString == m)
    }
  }

  object BarcodeWellValidations {

    def isValidWell(w: String): Boolean = {
      val row: String = w.replaceAll("[^A-Za-z]", "")
      val col: Int = w.replaceAll("[^0-9]+", "").toInt
      isValidRow(row) && isValidColumn(col)
    }

    def isValidRow(r: String): Boolean = {
      if (r.toUpperCase >= "A" && r.toUpperCase <= "P") true
      else false
    }

    def isValidColumn(c: Int): Boolean = {
      if (c >= 1 && c <= 24) true
      else false
    }

    def getWellParts(w: String): Option[(String, Int)] = {
      val r = """([A-Za-z])(\d+)""".r
      w match {
        case r(s, i) => Some((s, i.toInt))
        case _ => None
      }
    }
  }

  object BarcodeSeqValidations {
    def isValidSeq(s: String): Boolean = {
      s.toUpperCase().matches("[ATCG]+")
    }

    def isValidLength(s: String): Boolean = {
      s.length() == 8
    }
  }
}

