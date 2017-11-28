package validations


/**
  * Created by amr on 11/27/2017.
  */
object BarcodesValidation{

  object BarcodesFileExtension extends Enumeration {
    type BarcodesFileExtension = Value
    val csv, xls, xlsx = Value

    def isValidFilename(filename: String): Boolean ={
      val ext = filename.split("\\.").last
      BarcodesFileExtension.values.exists(_.toString == ext)
    }
  }

  object BarcodeWell {
    def isValidRow(r: String): Boolean = {
      ???
    }

    def isValidColumn(c: Int): Boolean = {
      ???
    }

    def isValidWell(w: String): Boolean = {
      ???
    }
  }

  object BarcodeSeq {
    def isValidSeq(s: String): Boolean = {
      ???
    }
  }




}



object Manufacturers extends Enumeration {
  type manufacturer = Value
  val Illumina = Value
}