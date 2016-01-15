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
					if (abR.initialContent != ContentType.antiBodies)
						throw new Exception("Antibody rack initial content not set to antibody tubes")
					if (abR.initialContent != ContentType.BSPtubes)
						throw new Exception("BSP rack initial content not set to BSP tubes")
					robotType match {
						case RobotType.HAMILTON => makeHamiltonABPlate(abR, abP, bspR)
						case _ => throw new Exception("Invalid Robot Type")
					}
				case (None, _, _) => throw new Exception("Antibody rack not registered")
				case (_, None, _) => throw new Exception("Antibody plate not registered")
				case (_, _, None) => throw new Exception("BSP rack not registered")
				case (Some(c), _, _) if !c.isInstanceOf[Rack] => throw new Exception("Antibody rack is not a rack")
				case (_, Some(c), _) if !c.isInstanceOf[Plate] => throw new Exception("Antibody plate is not a plate")
				case (_, _, Some(c)) if !c.isInstanceOf[Rack] => throw new Exception("BSP rack is not a rack")
			}
		}).recover {
			case e => List((None, Some(e.getLocalizedMessage)))
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

	private def makeHamiltonABPlate(abRack: Rack, abPlate: Plate, bspRack: Rack) = {

		def checkRackScan(id: String, rs: List[RackScan]) =
			if (rs.isEmpty)
				Some("Rack scan not found for " + id)
			else if (rs.head.contents.isEmpty)
				Some("Rack scan empty for " + id)
			else if (rs.size != 1)
				Some("Multiple rack scans found for " + id)
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
		 * @return (optional Transfer instruction, optional error message) - one or other should be set
		 */
		def makeInstructions(abContainers: List[Component], bspTubes: List[BSPTube],
							 abTubes: List[RackTube],sampleTubesScan: List[RackScan]) = {
			val abRackTubes = abContainers.flatMap{
				case t: Tube if (t.initialContent.isDefined &&
					ContentType.isAntibody(t.initialContent.get)) => List(t)
				case _ => List.empty
			}
			// We can finally get to work - we've got all the data we need
			// Map maps to do efficient searches when looping through sample tubes
			val abRackTubesMap =
				abRackTubes.map((t) => t.initialContent.get.toString -> t).toMap
			val bspTubesMap = bspTubes.map((t) => t.barcode -> t).toMap
			val abTubesMap = abTubes.map((t) => t.barcode -> t).toMap
			// Go through sample tubes found from scan
			val sampleTubes = sampleTubesScan.head.contents
			sampleTubes.map((sampleTube) => {
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
														(Some((abInfo.volume, abType,
															abRackPos, abPlatePos, abRackTube.id)), None)
													case None => (None, Some("Antibody not found"))
												}
											case None => (None, Some("Antibody tube not found in rack scan"))
										}
									case None => (None, Some("Antibody tube not found in ab tubes from rack"))
								}
							case None => (None, Some("No AB specified in bsp tube"))
						}
					case None => (None, Some("Couldn't find match in bsp tubes"))
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
					Future.successful(List((None, Some("Jira BSP issue not found for rack " + bspRack.id))))
				else if (bspData.head.list.isEmpty || bspData.head.list.head.contents.isEmpty)
					Future.successful(List((None, Some("Jira BSP data empty for  " + bspRack.id))))
				else {
					val bspTubes = bspData.head.list.head.contents
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
										case (abContainers, _) =>
											makeInstructions(abContainers, bspTubes, abTubes, sampleTubesScan)
									}
							}
					}
				}
		}
	}

}
