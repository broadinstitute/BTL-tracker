package models

import models.db.DBOpers
import reactivemongo.bson.{BSONDocumentWriter, BSONDocumentReader, Macros, BSONDocument}
import org.broadinstitute.LIMStales.sampleRacks.{RackScan => SampleRackScan, RackTube => SampleRackTube}
/**
  * Created by nnovod on 1/5/16.
  */

/**
  * Tube that's in a rack
  * @param barcode tube barcode
  * @param pos position of tube within rack
  */
case class RackTube(barcode: String, pos: String)

/**
  * Companion object to define BSON converters
  */
object RackTube {
	/**
	  * To convert between RackTubes and BSON
	  */
	implicit val rackTubeHandler = Macros.handler[RackTube]

	/**
	  * Implicit conversion from LIMStales RackTube to our RackTube.  They are pretty much the same but we need
	  * to declare our own to make a companion object that can do BSON conversions
	  * @param rt LIMStales RackTube
	  * @return our RackTube
	  */
	implicit def sampleRackTubeToRackTube(rt: SampleRackTube) = RackTube(rt.barcode, rt.pos)
}

/**
  * Rack holding tubes
  * @param barcode barcode of rack
  * @param contents list of tubes in rack
  */
case class RackScan(barcode: String, contents: List[RackTube])

/**
  * Companion object to define BSON converters
  */
object RackScan extends DBOpers[RackScan] {
	/**
	  * Configuration key for collection
	  */
	protected val collectionNameKey = "mongodb.collection.rack"

	/**
	  * Default collection name if none found in configuration
	  */
	protected val collectionNameDefault = "rack"

	/**
	  * To convert between RackScan and BSON
	  */
	implicit val rackScanHandler = Macros.handler[RackScan]

	// Define reader and writer for RackScan used by DB operations.
	// implicitly searches for implicit values of type specified.  Here it finds values just defined in rackScanHandler.
	val reader = implicitly[BSONDocumentReader[RackScan]]
	val writer = implicitly[BSONDocumentWriter[RackScan]]

	/**
	  * Implicit conversion from LIMStales RackScan to our RackScan.  They are pretty much the same but we need
	  * to declare our own to make a companion object that can do BSON conversions
	  * @param rs LIMStales RackScan
	  * @return our RackScan
	  */
	implicit def sampleRackScanToRackScan(rs: SampleRackScan) =
		RackScan(rs.barcode, rs.contents.map((rt) => RackTube.sampleRackTubeToRackTube(rt)))

	/**
	  * Find a rack based on barcode
	  * @param bc barcode of rack being looked for
	  * @return list of racks found (should only be one or none)
	  */
	def findRack(bc: String) = read(BSONDocument("barcode" -> bc))

	/**
	  * Replace rack entry in DB if it's there, otherwise insert a new entry.
	  * @param rack rack to be inserted
	  * @return upsert status
	  */
	def insertOrReplace(rack: RackScan) = upsert(BSONDocument("barcode" -> rack.barcode), rack)
}
