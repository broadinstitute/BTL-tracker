import models.BarcodeSet._
import models.initialContents.MolecularBarcodes._
import org.specs2.mutable._
import controllers.BarcodesController.{insertBarcodeObjects, makeBarcodeObjects, makeSetWells}
import models.{BarcodeSet, DBBarcodeSet}
import models.db.BarcodeSetCollection.db

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import play.api.test._
import play.api.test.Helpers._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument


class WriteHardcodedSets extends Specification {


	val hcSets = List(mbSet96LIMG)
	      "LIMG should be written" in {
	        running(TestServer(3333)) {
						val limg = mbSet96LIMG.contents
						val limgBS = BarcodeSet(
							name = "Low Input Metegenomic",
							setType = BarcodeSet.NEXTERA_PAIR,
							contents = limg
						)
						val result = Await.result(DBBarcodeSet.writeSet(limgBS), Duration(10, SECONDS))
	          result.ok mustEqual true
	           }
	        }
}