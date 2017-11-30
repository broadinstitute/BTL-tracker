package validations


/**
  * Created by amr on 11/27/2017.
  */
object BarcodesValidation{

  def validateBarcode(well: String, seq: String): Boolean = {
    BarcodeWell.isValidWell(well) && BarcodeSeq.isValidSeq(seq)
  }

  object BarcodesFileExtension extends Enumeration {
    type BarcodesFileExtension = Value
    val csv, xls, xlsx = Value

    def isValidFilename(filename: String): Boolean ={
      val ext = filename.split("\\.").last
      BarcodesFileExtension.values.exists(_.toString == ext)
    }
  }

  object Manufacturers extends Enumeration{
    type manufacturer = Value
    val Illumina = Value

    def isValidManufacturer(m: String): Boolean = {
      Manufacturers.values.exists(_.toString == m)
    }
  }

  object BarcodeWell {
    def isValidRow(r: String): Boolean = {
      if (r.toUpperCase >= "A" && r.toUpperCase <= "P") true
      else false
    }

    def isValidColumn(c: Int): Boolean = {
      if (c >= 1 && c <= 24) true
      else false
    }

    def isValidWell(w: String): Boolean = {
      val row: String = w.replaceAll("[^A-Za-z]", "")
      val col: Int = w.replaceAll("[^0-9]+", "").toInt
      isValidRow(row) && isValidColumn(col)
    }
  }

  object BarcodeSeq {
    def isValidSeq(s: String): Boolean = {
      s.toUpperCase().matches("[ATCG]+")
    }

    def isValidLength(s: String): Boolean = {
      s.length() == 8
    }
  }
}

