package models

import models.TransferContents.MergeTotalContents
import models.db.{ TrackerCollection, DBOpers }
import reactivemongo.bson.{ BSONDocumentWriter, BSONDocumentReader, Macros, BSONDocument }
import org.broadinstitute.LIMStales.sampleRacks.{
	RackScan => SampleRackScan,
	RackTube => SampleRackTube,
	BarcodedContentList,
	BarcodedContent
}
import scala.concurrent.Future
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
		RackScan(barcode = rs.barcode, contents = rs.contents.map((rt) => RackTube.sampleRackTubeToRackTube(rt)))

	/**
	 * Find a rack based on barcode
	 *
	 * @param bc barcode of rack being looked for
	 * @return list of racks found (should only be one or none)
	 */
	def findRack(bc: String) = read(BSONDocument(barcodeKey -> bc))

	//@TODO - Eliminate this (do it all async) once it's decided exactly when the rack scan will be read

	import scala.concurrent.Await
	import scala.concurrent.duration._
	def findRackSync(bc: String) = {
		val f = findRack(bc)
		try {
			val res = Await.result(awaitable = f, atMost = Duration(5000, MILLISECONDS))
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
	def insertOrReplace(rack: RackScan) = upsert(selector = BSONDocument(barcodeKey -> rack.barcode), entry = rack)

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
	 * @param ids list of ids for wanted antibody tubes
	 * @return (list of components found with antibodies, error message if there were problems)
	 */
	def getABTubes(ids: List[String]) = {
		getTubes(ids = ids,
			setContents = (tube, contents) => {
				if (contents.errs.nonEmpty)
					(tube -> Set.empty[String],
						Some(f"Errors retrieving ${tube.id} contents: ${contents.errs.mkString(",")}"))
				else if (contents.wells.isEmpty)
					(tube -> Set.empty[String], None)
				else if (contents.wells.size != 1)
					(tube -> Set.empty[String], Some(f"${tube.id} not single well component"))
				else if (contents.wells.head._2.isEmpty)
					(tube -> Set.empty[String], None)
				else if (contents.wells.head._2.flatMap(_.bsp).nonEmpty)
					(tube -> Set.empty[String], Some(f"${tube.id} contains samples with antibodies"))
				else if (contents.wells.head._2.flatMap(_.mid).nonEmpty)
					(tube -> Set.empty[String], Some(f"${tube.id} contains MIDs with antibodies"))
				else
					(tube -> contents.wells.head._2.flatMap(_.antibody), None)
			}
		)
	}

	/**
	 * Get contents of a set of tubes, presumably coming from a rack.
	 * @param ids list of ids for wanted tubes
	 * @param setContents callback to set contents for tube
	 * @tparam T type of contents
	 * @return (list of tubes found with returned contents, error message if there were problems)
	 */
	def getTubes[T](ids: List[String], setContents: (Tube, MergeTotalContents) => (T, Option[String])) =
		TrackerCollection.findIds(ids).flatMap((tubes) => {
			// Get objects from bson
			val rackContents = ComponentFromJson.bsonToComponents(tubes)
			// Get list of ids found
			val rackContentsIds = rackContents.map(_.id)
			// See if there were any ids not found
			val notFound = ids.diff(rackContentsIds)
			// Make error message of unfound tubes
			val tubesNotFound =
				if (notFound.isEmpty) None else Some("Tubes from scan not registered: " + notFound.mkString(", "))
			// Check that all the components found are tubes
			// Make list of (contents, error)
			// Get list of futures
			val contentFutures : List[Future[(Option[T], Option[String])]] =
				rackContents.map {
					case t: Tube =>
						// It's a tube - go (via future) find the contents of the tube
						TransferContents.getContents(t.id).map {
							case Some(contents) =>
								val (res, err) = setContents(t, contents)
								(Some(res), err)
							case _ => (None, Some(f"Entry ${t.id} has no contents"))
						}
					case c =>
						Future.successful((None, Some(f"Entry ${c.id} not a tube")))
				}

			// Wait for futures to complete and then look for errors
			Future.sequence(contentFutures).map((contents) => {
				// Get what we found
				val finalContents = contents.flatMap(_._1)
				// Get all errrors recorded
				val contentsErrs = contents.flatMap(_._2)
				val errs = tubesNotFound match {
					case Some(tnf) => tnf :: contentsErrs
					case None => contentsErrs
				}
				// Return what we've got
				(finalContents,
					if (errs.isEmpty) None else Some(errs.mkString("; ")))
			})
		})

}
