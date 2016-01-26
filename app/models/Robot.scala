package models

import Robot._
import models.db.TrackerCollection
import models.initialContents.InitialContents
import models.initialContents.InitialContents.ContentType
import models.project.JiraProject
import org.broadinstitute.LIMStales.sampleRacks.BSPTube
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by nnovod on 1/12/16.
  */
case class Robot(robotType: RobotType.RobotType) {
	def makeABPlate(abRack: String, abPlate: String, bspRack: String) = {
		TrackerCollection.findIds(List(abRack, abPlate, bspRack)).flatMap((ids) => {
			// Map bson to components (someday direct mapping should be possible but too painful for now)
			val components = ComponentFromJson.bsonToComponents(ids)
			// Find components
			val abRackComponent = components.find(_.id == abRack)
			val abPlateComponent = components.find(_.id == abPlate)
			val bspRackComponent = components.find(_.id == bspRack)
			// Exit if any errors - otherwise go make plate instructions
			(abRackComponent, abPlateComponent, bspRackComponent) match {
				case (Some(abR: Rack), Some(abP: Plate), Some(bspR: Rack)) =>
					if (abR.initialContent.isEmpty || abR.initialContent.get != ContentType.ABtubes)
						throw new Exception(s"Antibody rack $abRack initial content not set to antibody tubes")
					if (bspR.initialContent.isEmpty || bspR.initialContent.get != ContentType.BSPtubes)
						throw new Exception(s"BSP rack $bspRack initial content not set to BSP tubes")
					robotType match {
						case RobotType.HAMILTON => makeHamiltonABPlate(abR, abP, bspR)
						case _ => throw new Exception("Invalid Robot Type")
					}
				case (None, _, _) => throw new Exception(s"Antibody rack $abRack not registered")
				case (_, None, _) => throw new Exception(s"Antibody plate $abPlate not registered")
				case (_, _, None) => throw new Exception(s"BSP rack $bspRack not registered")
				case (Some(c), _, _) if !c.isInstanceOf[Rack] =>
					throw new Exception(s"Antibody rack $abRack is not a rack")
				case (_, Some(c), _) if !c.isInstanceOf[Plate] =>
					throw new Exception(s"Antibody plate $abPlate is not a plate")
				case (_, _, Some(c)) if !c.isInstanceOf[Rack] =>
					throw new Exception(s"BSP rack $bspRack is not a rack")
			}
		}).recover {
			case e => List((None, Some(s"Error making antibody plate: ${e.getLocalizedMessage}")))
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
	  *
	  * @param volume volumne to transfer
	  * @param abType antibody type
	  * @param rackPos tube position in rack
	  * @param platePos well position in plate
	  * @param tubeID antibody tube barcode
	  */
	case class ABTubeToPlate(volume: Float, abType: InitialContents.ContentType.ContentType,
							 rackPos: String, platePos: String, tubeID: String)

	/**
	  * Make the robot instructions for transferring antibodies (from tubes in a rack) to wells in a plate using a
	  * Hamilton robot.  The placement of the antibodies in the plate is based on the original sample information from
	  * bsp that links an antibody with each sample.  The instructions are returned as a list of tuples with two
	  * optional components:
	  * (transfer amount, antibody, antibody source tube position, antibody plate destination position, rack id) and
	  * an error message.  Note either one but not both parts of the tuple can be None.
	  *
	  * @param abRack rack containing antibody tubes used as source for transfers
	  * @param abPlate destination plate for antibodies
	  * @param bspRack BSP sample rack containing original sample information
	  * @return ((transfer amount, ab type, ab source tube position, ab plate destination position, rack id), error)
	  */
	private def makeHamiltonABPlate(abRack: Rack, abPlate: Plate, bspRack: Rack) = {

		def checkRackScan(id: String, rs: List[RackScan]) =
			if (rs.isEmpty)
				Some(s"Rack scan not found for $id")
			else if (rs.head.contents.isEmpty)
				Some(s"Rack scan empty for $id")
			else if (rs.size != 1)
				Some(s"Multiple rack scans found for $id")
			else
				None

		/*
		 * Make the robot instructions for transferring antibodies (from tubes in a rack) to wells in a plate.  The
		 * placement of the antibodies in the plate is based on the original sample information from bsp that links
		 * an antibody with each sample.
		 *
		 * @param abContainers antibody tubes found in rack
		 * @param bspTubes bsp info for sample tubes
		 * @param abTubes antibody tubes barcode/position from rack scan
		 * @param sampleTubesScan sample tubes barcode/position from bsp rack scan
		 * @return (optional Transfer instructions, optional error message) - one or other should be set
		 */
		def makeInstructions(abContainers: List[Component], bspTubes: List[BSPTube],
							 abTubes: List[RackTube], sampleTubesScan: List[RackScan]) = {
			// Make sure we've only got antibody tubes in the container list
			val abRackTubes = abContainers.flatMap{
				case t: Tube if t.initialContent.isDefined &&
					ContentType.isAntibody(t.initialContent.get) => List(t)
				case _ => List.empty
			}
			// Map maps to do efficient searches when looping through sample tubes
			val abRackTubesMap =
				abRackTubes.map((t) => t.initialContent.get.toString -> t).toMap
			val bspTubesMap = bspTubes.map((t) => t.barcode -> t).toMap
			val abTubesMap = abTubes.map((t) => t.barcode -> t).toMap
			// Go through sample tubes found from scan
			val sampleTubes = sampleTubesScan.head.contents
			sampleTubes.map((sampleTube) =>
			{
				val sampleBarcode = sampleTube.barcode
				// Find sample tube in Jira BSP tubes
				bspTubesMap.get(sampleBarcode) match {
					case Some(bspTube) =>
						bspTube.antiBody match {
							// Find antibody tube wanted for sample
							case Some(bspAB) =>
								abRackTubesMap.get(bspAB) match {
									case (Some(abRackTube)) =>
										val abType = abRackTube.initialContent.get
										// Find antibody position in rack
										abTubesMap.get(abRackTube.id) match {
											case Some(abPos) =>
												val abRackPos = abPos.pos
												val abPlatePos = sampleTube.pos
												// Get volume for antibody
												InitialContents.antibodyMap.get(abType) match {
													// We've done it
													// Create robot instruction for transfer of ab from Rack to Plate
													case Some(abInfo) =>
														(Some(ABTubeToPlate(abInfo.volume, abType,
															abRackPos, abPlatePos, abRackTube.id)), None)
													case None => (None, Some(s"Antibody ${abType.toString} not found"))
												}
											case None => (None,
												Some(s"Antibody tube ${abRackTube.id} not found in rack scan"))
										}
									case None => (None, Some(s"Antibody $bspAB not found in antibody tubes in rack"))
								}
							case None => (None, Some(s"No antibody specified for sample tube ${bspTube.barcode}"))
						}
					case None => (None, Some(s"Couldn't find sample tube $sampleBarcode"))
				}
			})
		}

		// Get BSP rack data
		JiraProject.getBspIssueCollection(bspRack.id) match {
			case (_, Some(err)) =>
				// Error getting bsp data
				Future.successful(List((None, Some(err))))
			case (bspData, _) =>
				// Check out that bsp data is there
				if (bspData.isEmpty)
					Future.successful(List((None, Some(s"Jira BSP issue not found for rack ${bspRack.id}"))))
				else if (bspData.head.list.isEmpty || bspData.head.list.head.contents.isEmpty)
					Future.successful(List((None, Some(s"Jira BSP data empty for ${bspRack.id}"))))
				else {
					// Get BSP tube data
					val bspTubes = bspData.flatMap((issueList) => issueList.list.flatMap(_.contents))
					// Get scan of BSP rack
					val sampleTubesScanFuture = RackScan.findRack(bspRack.id)
					// Get scan of antibody rack
					val abTubesScanFuture = RackScan.findRack(abRack.id)
					// Wait for async activity to complete and then take next step
					Future.sequence(List(sampleTubesScanFuture, abTubesScanFuture)).flatMap {
						case (List(sampleTubesScan, abTubesScan)) =>
							// Check out results of retrieving rack scans
							(checkRackScan(abRack.id, abTubesScan), checkRackScan(bspRack.id, sampleTubesScan)) match {
								case (Some(err), Some(err1)) =>
									Future.successful(List((None, Some(err + ";" + err1))))
								case (Some(err), _) =>
									Future.successful(List((None, Some(err))))
								case (_, Some(err)) =>
									Future.successful(List((None, Some(err))))
								case _ =>
									// Rack scans look good - now go get contents of ab tubes
									val abTubes = abTubesScan.head.contents
									val abTubeBarcodes = abTubes.map(_.barcode)
									RackScan.getABTubes(abTubeBarcodes).map {
										// Error found - go report it
										case (_, Some(err)) => List((None, Some(err)))
										// We've got all the data - go make the instructions
										case (abContainers, _) =>
											makeInstructions(abContainers, bspTubes, abTubes, sampleTubesScan)
									}
							}
					}
				}
		}
	}

	/**
	  * Make a list of transfers from a list of robot instructions to make a antibody plate
	  *
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
		tubesMap.map{
			case (tube, wells) =>
				// Get wells as indicies
				val wellIdxs =
					wells.map((t) => {
						div match {
							case ContainerDivisions.Division.DIM8x12 => TransferWells.make96IdxFromWellStr(t.platePos)
							case ContainerDivisions.Division.DIM16x24 => TransferWells.make384IdxFromWellStr(t.platePos)
						}
					})
				// Create transfer from tube to wells in plate
				Transfer(from = tube, to = plate, fromQuad = None, toQuad = None, project = project,
					slice = Some(Transfer.Slice.CP), cherries = Some(wellIdxs), isTubeToMany = true)
		}.toList
	}

}
