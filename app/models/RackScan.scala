package models

import models.TransferContents.{MergeResult, MergeTotalContents}
import models.db.{ TrackerCollection, DBOpers }
import utils.{Yes, No}
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
	 * @param rt LIMStales RackTube
	 * @return our RackTube
	 */
	implicit def sampleRackTubeToRackTube(rt: SampleRackTube): RackTube = RackTube(bc = rt.barcode, pos = rt.pos)
}

/**
 * Rack holding tubes
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
	 * @param rs LIMStales RackScan
	 * @return our RackScan
	 */
	implicit def sampleRackScanToRackScan(rs: SampleRackScan): RackScan =
		RackScan(barcode = rs.barcode, contents = rs.contents.map((rt) => RackTube.sampleRackTubeToRackTube(rt)))

	/**
	 * Find a rack based on barcode
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
	 * Check out scan found for an individual rack.  If all ok return contents.
	 * @param id id for wanted rack
	 * @param rs rack scan found for id
	 * @return YesOrNo(RackScan)
	 */
	def checkRackScan(id: String, rs: List[RackScan]) =
		if (rs.isEmpty)
			No(s"Rack scan not found for $id")
		else if (rs.head.contents.isEmpty)
			No(s"Rack scan empty for $id")
		else if (rs.size != 1)
			No(s"Multiple rack scans found for $id")
		else
			Yes(rs.head)

	/**
	 * Replace rack entry in DB if it's there, otherwise insert a new entry.
	 * @param rack rack to be inserted
	 * @return upsert status
	 */
	def insertOrReplace(rack: RackScan) = upsert(selector = BSONDocument(barcodeKey -> rack.barcode), entry = rack)

	//@TODO Eliminate sync versions
	def getABTubesSync(ids: List[String]) = {
		val f = getABTubes(ids)
		try {
			Await.result(f, Duration(5000, MILLISECONDS))
		} catch {
			case e: Exception => (List.empty, Some(e.getLocalizedMessage))
		}
	}

	def getTubeContentsSync(ids: List[String]) = {
		val f = getTubeContents(ids)
		try {
			Await.result(f, Duration(5000, MILLISECONDS))
		} catch {
			case e: Exception => (List.empty, Some(e.getLocalizedMessage))
		}
	}


	/**
	 * Check if contents found are vaild for a tube
	 * @param tube tube we're checking
	 * @param contents contents found in tube
	 * @return optional error message
	 */
	private def checkTube(tube: Tube, contents: MergeTotalContents) = {
		if (contents.errs.nonEmpty)
			Some(s"Errors retrieving ${tube.id} contents: ${contents.errs.mkString(",")}")
		else if (contents.wells.size > 1)
			Some(s"${tube.id} not single well component")
		else None
	}

	/**
	 * See if there is anything found in contents
	 * @param contents contents found
	 * @return true if something found in the contents
	 */
	private def isContentsEmpty(contents: MergeTotalContents) =
		!contents.wells.exists {
			case (_, results) => results.nonEmpty
		}

	/**
	 * Get antibody tubes.
	 * @param ids list of ids for wanted antibody tubes
	 * @return (list of components found with antibodies, error message if there were problems)
	 */
	def getABTubes(ids: List[String]) = {
		// Get tubes and check for lots of errors when setting contents
		getTubes[(Tube, Set[String])](ids = ids,
			setContents = (tube, contents) => {
				checkTube(tube, contents) match {
					case None =>
						if (isContentsEmpty(contents))
							(tube -> Set.empty[String], None)
						else if (contents.wells.head._2.flatMap(_.sample).nonEmpty)
							(tube -> Set.empty[String], Some(s"${tube.id} contains samples with antibodies"))
						else if (contents.wells.head._2.flatMap(_.mid).nonEmpty)
							(tube -> Set.empty[String], Some(s"${tube.id} contains MIDs with antibodies"))
						else
							(tube -> contents.wells.head._2.flatMap(_.antibody), None)
					case err =>
						(tube -> Set.empty[String], err)
				}
			}
		)
	}

	/**
	 * Get tube contents
	 * @param ids list of ids for tubes
	 * @return (list components found with optional contents, error message if there are problems)
	 */
	def getTubeContents(ids: List[String]) = {
		// Get tubes and check for lots of errors when setting contents
		getTubes[(Tube, Set[MergeResult])](ids = ids,
			setContents = (tube, contents) => {
				checkTube(tube, contents) match {
					case None =>
						if (isContentsEmpty(contents))
							(tube -> Set.empty[MergeResult], None)
						else
							(tube -> contents.wells.head._2, None)
					case err =>
						(tube -> Set.empty[MergeResult], err)
				}
			}
		)
	}

	/**
	 * From a list of IDs, presumably from a rack, find which are registered and which are tubes.
	 * @param ids list of ids for wanted tubes
	 * @return (list of tubes found, list of non-tube components found, list of ids not found)
	 */
	def findTubes(ids: List[String]) =
		TrackerCollection.findIds(ids).map((rackContents) => {
			// Get list of ids found
			val rackContentsIds = rackContents.map(_.id)
			// See if there were any ids not found
			val notFound = ids.diff(rackContentsIds)
			// Separate tubes from non-tubes
			val isOrNotATube = rackContents.groupBy {
				case _ : Tube => true
				case _ => false
			}
			(isOrNotATube.getOrElse(true, List.empty).map(_.asInstanceOf[Tube]),
				isOrNotATube.getOrElse(false, List.empty), notFound)
		})

	/**
	 * Get contents of a set of tubes, presumably coming from a rack.
	 * @param ids list of ids for wanted tubes
	 * @param setContents callback to set contents for tube
	 * @tparam T type of contents
	 * @return (list of tubes found with returned contents, error message if there were problems)
	 */
	def getTubes[T](ids: List[String], setContents: (Tube, MergeTotalContents) => (T, Option[String])) =
		findTubes(ids).flatMap {
			case (rackContents, notTubes, notFound) =>
				// Get list of futures to get contents
				val contentFutures : List[Future[(Option[T], Option[String])]] =
					rackContents.map((t) =>
						// Go (via future) find the contents of the tube
						TransferContents.getContents(t.id).map {
							case Some(contents) =>
								val (res, err) = setContents(t, contents)
								(Some(res), err)
							case _ => (None, Some(s"Entry ${t.id} has no contents"))
						}
					)
				// Wait for futures to complete and then look for errors
				Future.sequence(contentFutures).map((contents) => {
					// Get tube contents we found
					val finalContents = contents.flatMap(_._1)
					// Get all errrors recorded
					val contentsErrs = contents.flatMap(_._2)
					val notFoundErrs = s"Tubes from scan not registered: ${notFound.mkString(", ")}"
					val notTubesErrs = s"Entries not tubes: ${notTubes.map(_.id).mkString(", ")}"
					val errs =
						if (notFound.isEmpty && notTubes.isEmpty)
							contentsErrs
						else if (notFound.nonEmpty && notTubes.nonEmpty)
							notFoundErrs :: notTubesErrs :: contentsErrs
						else if (notFound.nonEmpty)
							notFoundErrs :: contentsErrs
						else
							notTubesErrs :: contentsErrs
					// Return what we've got
					(finalContents,
						if (errs.isEmpty) None else Some(errs.mkString("; ")))
				})
		}

	/**
	 * Retrieve rack scans.  For each id either an error or valid scan of the rack's tubes is returned
	 * @param racks rack ids for which to retrieve scans
	 * @return (id -> YesOrNo(RackScan)
	 */
	def getRackContents(racks: List[String]) = {
		val racksFound = racks.map(findRack)
		Future.sequence(racksFound).map((scans) => {
			// Get ids iterator (what futures was based on as well)
			val idIter = racks.toIterator
			// Get map of results (id -> (Option(RackScan), Option(error))
			scans.map((rs) => {
				val id = idIter.next()
				id -> checkRackScan(id, rs)
			}).toMap
		})
	}

}
