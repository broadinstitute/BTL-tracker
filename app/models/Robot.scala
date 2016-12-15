package models

import Robot._
import models.TransferContents.MergeTotalContents
import models.db.{ TransferCollection, TrackerCollection }
import models.initialContents.InitialContents
import models.initialContents.InitialContents.ContentType
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.{No, Yes}
import scala.concurrent.Future

/**
 * Instructions are made for the robots to follow.
 * Created by nnovod on 1/12/16.
 */
case class Robot(robotType: RobotType.RobotType) {
	/**
	 * Make transfers to create an antibody plate for a plate of samples.
	 * @param abRack rack containing antibody tubes
	 * @param abPlate plate to be made with antibodies for samples
	 * @param sampleContainer container with samples
	 * @return (transfers, error)
	 */
	def makeABPlate(abRack: String, abPlate: String, sampleContainer: String) = {
		def checkRacks(abR: Rack) = {
			if (abR.initialContent.isEmpty || abR.initialContent.get != ContentType.ABtubes)
				throw new Exception(s"Antibody rack $abRack initial content not set to antibody tubes")
		}
		def makePlate(contents: MergeTotalContents, abR: Rack, abP: Plate, sampleC: Component with ContainerDivisions) = {
			(robotType match {
				case RobotType.HAMILTON => makeHamiltonABPlate(contents = contents,
					abRack = abR, abPlate = abP, sampleContainer = sampleC)
				case _ => throw new Exception("Invalid Robot Type")
			}).map((t) => (Some(ABTrans(abRack = abR, abPlate = abP, sampleContainer = sampleC, trans = t)), None))
		}

		TransferContents.getContents(sampleContainer).flatMap {
			case None => throw new Exception(s"$sampleContainer has no contents")
			case Some(contents) if contents.errs.nonEmpty =>
				throw new Exception(s"Error getting contents for $sampleContainer: ${contents.errs.mkString("; ")}")
			case Some(contents) if contents.wells.nonEmpty =>
				// Check if anything already transferred into plate
				val transIntoABPlate = TransferCollection.getSourceIDs(abPlate)
				// Get components for racks and plate
				val components = TrackerCollection.findIds(List(abRack, abPlate, sampleContainer))
				Future.sequence(List(transIntoABPlate, components)).flatMap(f = (docs) => {
					if (docs.head.nonEmpty) throw new Exception(s"Transfers already done into antibody plate $abPlate")
					val ids = docs.tail.head
					// Map bson to components (someday direct mapping should be possible but too painful for now)
					val components = ComponentFromJson.bsonToComponents(ids)
					// Find components
					val abRackComponent = components.find(_.id == abRack)
					val sampleComponent = components.find(_.id == sampleContainer)
					val abPlateComponent = components.find(_.id == abPlate)
					// Exit if any errors - otherwise go make plate instructions
					(abRackComponent, abPlateComponent, sampleComponent) match {
						case (Some(abR: Rack), Some(abP: Plate), Some(sampleC: Component with ContainerDivisions)) =>
							// Check that racks are ok
							checkRacks(abR)
							// Make plate instructions
							makePlate(contents = contents, abR = abR, abP = abP, sampleC = sampleC)
						case (None, _, _) => throw new Exception(s"Antibody rack $abRack not registered")
						case (_, _, None) => throw new Exception(s"Sample container $sampleContainer not registered")
						case (Some(abR: Rack), None, Some(sampleC: Component with ContainerDivisions)) =>
							// Check that antibody rack ok
							checkRacks(abR)
							// Register antibody plate not there yet and then make plate instructions
							val plateDesc = s"${sampleC.component.toString} $sampleContainer"
							val abP =
								Plate(id = abPlate,
									description = Some(s"Antibody plate generated for sample $plateDesc"),
									project = sampleC.project, tags = List.empty, locationID = None,
									initialContent = None, layout = sampleC.layout)
							TrackerCollection.insertComponent(data = abP,
								onSuccess = (_) => abP, onFailure = throw _)
								.flatMap(makePlate(contents, abR, _, sampleC))
						case (Some(c), _, _) if !c.isInstanceOf[Rack] =>
							throw new Exception(s"Antibody rack $abRack is not a rack")
						case (_, Some(c), _) if !c.isInstanceOf[Plate] =>
							throw new Exception(s"Antibody plate $abPlate is not a plate")
						case (_, _, Some(c)) if !c.isInstanceOf[Component with ContainerDivisions] =>
							throw new Exception(s"Sample container $sampleContainer is not a valid container type")
					}
				})
		}.recover {
			case e => (None, Some(s"Error making antibody plate: ${e.getLocalizedMessage}"))
		}
	}
}

import scala.concurrent.Future
object Robot {
	/**
	 * Enumeration for all division types
	 */
	object RobotType extends Enumeration {
		type RobotType = Value
		val HAMILTON = Value("Hamilton")
	}

	/**
	 * Transfer of antibody tube (in rack) to position in plate being made
	 * @param volume volume to transfer
	 * @param abType antibody type
	 * @param rackPos tube position in rack
	 * @param platePos well position in plate
	 * @param tubeID antibody tube barcode
	 */
	case class ABTubeToPlate(volume: Int, abType: InitialContents.ContentType.ContentType,
		rackPos: String, platePos: String, tubeID: String)

	/**
	 * Antibody rack->plate transfer info.
	 * @param abRack source ab rack
	 * @param abPlate destination ab plate
	 * @param sampleContainer destination sample plate
	 * @param trans tube->rack transfers along with error messages
	 */
	case class ABTrans(abRack: Rack, abPlate: Plate, sampleContainer: Component with ContainerDivisions,
		trans: List[(Option[ABTubeToPlate], Option[String])])

	/**
	 * Make the robot instructions for transferring antibodies (from tubes in a rack) to wells in a plate using a
	 * Hamilton robot.  The placement of the antibodies in the plate is based on the original sample information from
	 * bsp that links an antibody with each sample.  The instructions are returned as a list of tuples with two
	 * optional components:
	 * (transfer amount, antibody, antibody source tube position, antibody plate destination position, rack id) and
	 * an error message.  Note either one but not both parts of the tuple can be None.
	 * @param contents contents of sample container
	 * @param abRack rack containing antibody tubes used as source for transfers
	 * @param abPlate destination plate for antibodies
	 * @param sampleContainer BSP sample rack containing original sample information
	 * @return ((transfer amount, ab type, ab source tube position, ab plate destination position, rack id), error)
	 */
	private def makeHamiltonABPlate(contents: MergeTotalContents, abRack: Rack, abPlate: Plate,
		sampleContainer: Component with ContainerDivisions) = {

		/*
		 * Make the robot instructions for transferring antibodies (from tubes in a rack) to wells in a plate.  The
		 * placement of the antibodies in the plate is based on the original sample information from bsp that links
		 * an antibody with each sample.
		 *
		 * @param abContainers antibody tubes found in rack with associated antibodies
		 * @param abTubes antibody tubes barcode/position from rack scan
		 * @return (optional Transfer instructions, optional error message) - one or other should be set
		 */
		def makeInstructions(abContainers: List[(Component, Set[String])], abTubes: List[RackTube]) = {
			// Make maps to do efficient searches when looping through samples
			val abRackTubesMap =
				abContainers.flatMap{
					case (c, ab) if ab.nonEmpty =>
						Some(ab.head -> c)
					case _ => None
				}.toMap
			val abTubesMap = abTubes.map((t) => t.barcode -> t).toMap
			contents.wells.toList.map {
				case (well, sample) =>
					if (sample.isEmpty)
						(None, Some(s"No sample found in well $well of ${sampleContainer.id}"))
					else if (sample.size != 1)
						(None, Some(s"Multiple samples found in well $well of ${sampleContainer.id}"))
					else {
						sample.head.sample match {
							case None =>
								(None, Some(s"No sample assigned to well $well of ${sampleContainer.id}"))
							case Some(bsp) =>
								bsp.antibody match {
									case None =>
										(None, Some(s"No antibody assigned to sample tube ${bsp.sampleTube}"))
									case Some(ab) =>
										// Find antibody tube in rack
										abRackTubesMap.get(ab) match {
											case (Some(abRackTube)) =>
												val abType = ContentType.withName(ab)
												// Find antibody position in rack
												abTubesMap.get(abRackTube.id) match {
													case Some(abPos) =>
														val abRackPos = abPos.pos
														val abPlatePos = well
														// Get volume for antibody
														InitialContents.antibodyMap.get(abType) match {
															// We've done it
															// Create instruction for transfer of ab from Rack to Plate
															case Some(abInfo) =>
																(Some(ABTubeToPlate(volume = abInfo.volume,
																	abType = abType, rackPos = abRackPos,
																	platePos = abPlatePos, tubeID = abRackTube.id)),
																	None)
															case None =>
																(None, Some(s"Antibody ${abType.toString} not found"))
														}
													case None =>
														(None, Some(
															s"Antibody tube ${abRackTube.id} not found in rack scan"))
												}
											case None =>
												(None, Some(s"Antibody $ab not found in antibody tubes in rack"))
										}
								}
						}
					}
			}
		}

		// Get scan of antibody rack
		RackScan.findRack(abRack.id).flatMap((abTubesScan) => {
			// Check out results of retrieving rack scans
			RackScan.checkRackScan(id = abRack.id, rs = abTubesScan) match {
				case No(err) =>
					Future.successful(List((None, Some(err))))
				case Yes(abTubes) =>
					// Rack scans look good - now go get contents of ab tubes
					val abTubeBarcodes = abTubes.contents.map(_.barcode)
					RackScan.getABTubes(abTubeBarcodes).map {
						// Error found - go report it
						case (_, Some(err)) => List((None, Some(err)))
						// We've got all the data - go make the instructions
						case (containers, _) =>
							// Make sure none of the tubes have multiple antibodies
							val errList = containers.flatMap {
								case (c, abs) if abs.size > 1 =>
									Some(s"Multiple antibodies ${abs.mkString(",")} found in ${c.id}")
								case _ => None
							}
							// If no multiples found then go make instructions, otherwise report error
							if (errList.isEmpty)
									makeInstructions(abContainers = containers, abTubes = abTubes.contents)
							else
									List((None, Some(errList.mkString("; "))))
					}
			}
		})
	}

	/**
	 * Make a list of transfers from a list of robot instructions to make a antibody plate
	 * @param plate antibody plate ID
	 * @param tubeToPlateList list of tube to plate transfers done on robot
	 * @param project optional project to associate with the transfer
	 * @param div plate division (96 or 384 well plate)
	 * @return tube to plate transfers done on robot
	 */
	def makeABTransfers(plate: String, tubeToPlateList: List[ABTubeToPlate], project: Option[String],
		div: ContainerDivisions.Division.Division) = {
		// Group transfers by tube
		val tubesMap = tubeToPlateList.groupBy(_.tubeID)
		// Map tubes into transfers
		tubesMap.map {
			case (tube, wells) =>
				// Get destination wells
				val wellPos = wells.map(_.platePos)
				// Create transfer from tube to wells in plate
				Transfer.fromTubeTransfer(tube = tube, plate = plate, wells = wellPos, div = div, proj = project)
		}.toList
	}

	// Regular expression for start of well position with a zero (leaves letter part as first group member)
	private val positionR = """([A-Za-z])0""".r

	/**
	 * Make a csv file with instructions for the robot to transfer antibodies from a rack of antibody tubes to
	 * a plate.
	 * @param trans transfers wanted
	 * @return (name of file created if it went ok, errors found)
	 */
	def makeABSpreadSheet(trans: List[ABTubeToPlate]) = {
		// Spreadsheet headers
		val vol = "Volume"
		val abType = "AB Name"
		val abLoc = "Source AB Well Location"
		val destLoc = "Destination Working Plate Well Location"
		// Array of all headers, ordered by column #
		val headers = Array(abType, vol, abLoc, destLoc)
		// Method to take a position and format it for robot (without leading zero)
		def robotPosition(pos: String) = positionR.replaceAllIn(pos, "$1")
		// Map of functions to retrieve values for a single tube to plate transfer
		// Note positions must have leading "0"s stripped out (e.g., A01 must be set as A1).  Apparently the robots
		// are picky about that.
		val retrieveValue = Map[String, (ABTubeToPlate) => String](
			vol -> (_.volume.toString),
			abType -> (_.abType.toString),
			abLoc -> ((inp) => robotPosition(inp.rackPos)),
			destLoc -> ((inp) => robotPosition(inp.platePos)))

		// Go make csv file
		spreadsheets.Utils.setCSVValues(headers = headers, input = trans,
			// Get individual values via retrieveValue functions called with tubeToPlate parameter
			getValues =
				(tubeToPlate: ABTubeToPlate, valsToGet) => List(valsToGet.map(retrieveValue(_)(tubeToPlate))),
			noneMsg = "No antibodies found")
	}
}
