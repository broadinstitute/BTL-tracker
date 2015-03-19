package controllers

import controllers.TransferContents.{MergeResult, MergeMid, MergeBsp}
import org.broadinstitute.spreadsheets.HeadersToValues
import org.apache.poi.ss.usermodel._

import scala.annotation.tailrec

/**
 * Created by nnovod on 3/10/15.
 * Methods to make an EZPass
 */
object MakeEZPass {

	def makeEZPass(inFile: String, component: String, libSize: Int, libVol: Int, libConcentration: Float) = {

		val headers = HeadersToValues(inFile, 0, List("ID"), Map.empty[String, (List[String], List[String])])
		val sheet = headers.getSheet.get
		val workBook = sheet.getWorkbook

		@tailrec
		def processResults(index: Int, results: Iterator[MergeResult], errs: List[String]) : List[String] = {
			if (results.hasNext) {
				val result = results.next
				val bspFields = result.bsp match {
					case Some(bsp) =>
						val bspFields = getBspFields(bsp)
						val midFields = getMidFields(result.mid)
						val constFields = getConstantFields
						val calcIntFields = getCalcIntFields(index, libSize, libVol)
						val calcFloatFields = getCalcFloatFields(libConcentration)
						List.empty
					case None => List("No sample found")
				}
				processResults(index + 1, results, errs ++ bspFields)
			} else errs
		}

		TransferContents.getContents(component).map {
			case Some(contents) => contents.wells.get(TransferContents.oneWell) match {
				case Some(resultSet) => processResults(1, resultSet.toIterator, List.empty)
				case None =>
			}
			case None =>
		}
	}

	val unknown = "Unknown"

	private def getProject(bsp: MergeBsp) = Some(bsp.project)
	private def getProjectDescription(bsp: MergeBsp) = bsp.projectDescription
	private def getGssrSample(bsp: MergeBsp) = bsp.gssrSample
	private def getCollabSample(bsp: MergeBsp) = bsp.collabSample
	private def getIndividual(bsp: MergeBsp) = bsp.individual
	private def getLibrary(bsp: MergeBsp) = bsp.library
	private def getSampleTube(bsp: MergeBsp) = Some(bsp.sampleTube)
	private val bspMap : Map[String, (MergeBsp) => Option[String]]=
		Map("Additional Sample Information" -> getProject, // Jira ticket
			"Project Title Description (e.g. MG1655 Jumping Library Dev.)" -> getProjectDescription, // Ticket summary
			"Source Sample GSSR ID" -> getGssrSample,
			"Collaborator Sample ID" -> getCollabSample,
			"Individual Name (aka Patient ID, Required for human subject samples)" -> getIndividual,
			"Library Name (External Collaborator Library ID)" -> getLibrary,
			"Sample Tube Barcode" -> getSampleTube)

	private def getBspFields(bsp: MergeBsp) = bspMap.map{
		case (k, v) => k -> v(bsp)
	}

	private def getMidFields(mids: Set[MergeMid]) = {
		val allMids = mids.foldLeft(("", "", ""))((soFar, mid) =>
			(soFar._1 + mid.sequence, soFar._1 + "-" + mid.name, {
				val nextKit = if (mid.isNextera) "Nextera" else "Illumina"
				if (soFar._3.isEmpty || soFar._3 == nextKit) nextKit else "Nextera/Illumina"
			})
		)
		Map("Molecular Barcode Sequence" -> allMids._1,
			"Molecular Barcode Name" -> allMids._2,
			"Illumina or 454 Kit Used" -> allMids._3
		)
	}

	private def getConstantFields =
		Map("Sequencing Technology (Illumina/454/TechX Internal Other)" -> "Illumina",
			"Single/Double Stranded (S/D)" -> "D",
			"Pooled" -> "Y"
		)

	private def getCalcIntFields(index: Int, libSize: Int, libVol: Int) =
		Map("Sample Number" -> index,
			"Insert Size Range bp. (i.e the library size without adapters)" -> (libSize - 126),
			"Library Size Range bp. (i.e. the insert size plus adapters)" -> libSize,
			"Total Library Volume (ul)" -> libVol // Integer, warning if under 20
		)

	private def getCalcFloatFields(libConcentration: Float) =
		Map("Total Library Concentration (ng/ul)" -> libConcentration)

	private val unKnownFields = Map(
		"Funding Source" -> "Jira parent ticket",
		"Coverage (# Lanes/Sample)" -> "Jira parent ticket",
		"Strain" -> unknown,
		"Sex (for non-human samples only)" -> unknown,
		"Cell Line"	-> unknown,
		"Tissue Type" -> unknown,
		"Library Type (see dropdown)" -> unknown,
		"Data Analysis Type (see dropdown)" -> unknown,
		"Reference Sequence" -> unknown,
		"GSSR # of Bait Pool (If submitting a hybrid selection library, please provide GSSR of bait pool used in experiment in order to properly run Hybrid Selection pipeline analyses)" -> unknown,
		"Jump Size (kb) if applicable" -> unknown,
		"Restriction Enzyme if applicable" -> "Later, for RRBS",
		"Molecular barcode Plate ID" -> unknown,
		"Molecular barcode Plate well ID" -> unknown,
		"Approved By" -> unknown,
		"Requested Completion Date" -> "ASAP",
		"Data Submission (Yes, Yes Later, or No)" -> unknown,
		"Additional Assembly and Analysis Information" -> unknown)
}
