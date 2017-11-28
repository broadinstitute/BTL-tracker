import org.scalatest.{FlatSpec, Matchers}
import validations.BarcodesValidation._

/**
  * Created by Amr on 11/28/2017.
  */
class ValidationSpec extends FlatSpec with Matchers {
  "BarcodeFileExtension.isValidFilename" should "be true when passed filenames with valid extensions" in {
    BarcodesFileExtension.isValidFilename("foo.csv") should be (true)
    BarcodesFileExtension.isValidFilename("/this/is/a/path/foo.xls") should be (true)
    BarcodesFileExtension.isValidFilename("/another/valid/file/path/foo.xlsx") should be (true)
  }
  it should "be false when passed filenames with bad extensions" in {
    BarcodesFileExtension.isValidFilename("foo.txt") should be (false)
    BarcodesFileExtension.isValidFilename("foo.tab") should be (false)
  }

}
