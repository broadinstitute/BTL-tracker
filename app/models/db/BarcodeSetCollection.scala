package models.db

import play.api.Play
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

/**
  * Created by Osiris on 1/17/2018.
  */
object BarcodeSetCollection extends Controller with MongoController{
  /**
    * Get tracker collection name.  We use a def instead of a val to avoid hot-reloading problems.
    * @return collection name
    */
  private def barcodeSetCollectionName =
    Play.current.configuration.getString("mongodb.collection.set").getOrElse("set")

  /**
    * Get collection to do JSON mongo operations.  We use a def instead of a val to avoid hot-reloading problems.
    * @return collection that uses JSON for input/output of barcodeset data
    */
  private def barcodeSetCollection: JSONCollection = db.collection[JSONCollection](barcodeSetCollectionName)
}
