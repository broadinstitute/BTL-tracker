package validations

/**
  * Created by amr on 11/27/2017.
  */
object BarcodesInputFile extends Enumeration{

  type BarcodesInputFile = Value
  val csv = Value

  def isBarcodesFile(f: BarcodesInputFile): Boolean = {
    f == csv
  }
}
