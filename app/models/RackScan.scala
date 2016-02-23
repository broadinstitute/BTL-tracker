package models

import models.db.{ TrackerCollection, DBOpers }
import models.initialContents.InitialContents
import reactivemongo.bson.{ BSONDocumentWriter, BSONDocumentReader, Macros, BSONDocument }
import org.broadinstitute.LIMStales.sampleRacks.{
	RackScan => SampleRackScan,
	RackTube => SampleRackTube,
	BarcodedContentList,
	BarcodedContent
}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Persist the results of a rack scan consisting of the 2D barcodes and positions of tubes in a rack.
 * Created by nnovod on 1/5/16.
 */

/**
 * Tube that's in a rack
 *
 * @param bc tube barcode
 * @param pos position of tube within rack
 */
case class RackTube(bc: String, pos: String) extends BarcodedContent {
	val barcode = bc
}

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
	 *
	 * @param rt LIMStales RackTube
	 * @return our RackTube
	 */
	implicit def sampleRackTubeToRackTube(rt: SampleRackTube): RackTube = RackTube(bc = rt.barcode, pos = rt.pos)
}

/**
 * Rack holding tubes
 *
 * @param barcode barcode of rack
 * @param contents list of tubes in rack
 */
case class RackScan(barcode: String, contents: List[RackTube]) extends BarcodedContentList[RackTube]

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

	// Barcode key for rack in mongo
	private val barcodeKey = "barcode"

	/**
	 * Implicit conversion from LIMStales RackScan to our RackScan.  They are pretty much the same but we need
	 * to declare our own to make a companion object that can do BSON conversions
	 *
	 * @param rs LIMStales RackScan
	 * @return our RackScan
	 */
	implicit def sampleRackScanToRackScan(rs: SampleRackScan): RackScan =
		RackScan(rs.barcode, rs.contents.map((rt) => RackTube.sampleRackTubeToRackTube(rt)))

	/**
	 * Find a rack based on barcode
	 *
	 * @param bc barcode of rack being looked for
	 * @return list of racks found (should only be one or none)
	 */
	def findRack(bc: String) = read(BSONDocument(barcodeKey -> bc))

	//@TODO - Eliminate this (do it all async) once it's decided exactly when the rack scan will be read
	//Also copy over (without project) the rack scans in the Jira DB as well as setting initial contents on Racks

	import scala.concurrent.Await
	import scala.concurrent.duration._
	def findRackSync(bc: String) = {
		val f = findRack(bc)
		try {
			val res = Await.result(f, Duration(5000, MILLISECONDS))
			(res, None)
		} catch {
			case e: Exception => (List.empty, Some(e.getLocalizedMessage))
		}
	}

	/**
	 * Replace rack entry in DB if it's there, otherwise insert a new entry.
	 *
	 * @param rack rack to be inserted
	 * @return upsert status
	 */
	def insertOrReplace(rack: RackScan) = upsert(BSONDocument(barcodeKey -> rack.barcode), rack)

	//@TODO Eliminate sync version of this
	def getABTubesSync(ids: List[String]) = {
		val f = getABTubes(ids)
		try {
			Await.result(f, Duration(5000, MILLISECONDS))
		} catch {
			case e: Exception => (List.empty, Some(e.getLocalizedMessage))
		}
	}

	/**
	 * Get antibody tubes.
	 *
	 * @param ids list of ids for wanted antibody tubes
	 * @return (list of components found with ids, error message if there were problems)
	 */
	def getABTubes(ids: List[String]) =
		TrackerCollection.findIds(ids).map((tubes) => {
			// Get objects from bson
			val rackContents = ComponentFromJson.bsonToComponents(tubes)
			// Get list of ids found
			val rackContentsIds = rackContents.map(_.id)
			// See if there were any ids not found
			val notFound = ids.diff(rackContentsIds)
			if (notFound.isEmpty) {
				// Check that all the components found are tubes with antibodies
				// Make list of (components that are not tubes, tubes that do not contain an antibody)
				val tubeErrs = rackContents.flatMap {
					case t: Tube =>
						if (t.initialContent.isEmpty ||
							!InitialContents.ContentType.isAntibody(t.initialContent.get))
							List((None, Some(t.id)))
						else
							List.empty
					case c =>
						List((Some(c.id), None))
				}
				// If there are no errors return list we got without an error, otherwise make error string
				if (tubeErrs.isEmpty)
					(rackContents, None)
				else {
					val notTubes = tubeErrs.flatMap {
						case (Some(t), _) => List(t)
						case _ => List.empty
					}
					val notABs = tubeErrs.flatMap {
						case (_, Some(t)) => List(t)
						case _ => List.empty
					}
					val notTubesErr =
						if (notTubes.isEmpty)
							""
						else
							"Scan entries not tubes: " + notTubes.mkString(", ")
					val notABsErr =
						if (notABs.isEmpty)
							""
						else
							(if (notTubesErr.isEmpty) "" else "; ") +
								"Scan entries do not contain an antibody: " +
								notABs.mkString(", ")
					(rackContents, Some(notTubesErr + notABsErr))
				}
			} else {
				(rackContents, Some("Tubes from scan not registered: " + notFound.mkString(", ")))
			}
		})

}
