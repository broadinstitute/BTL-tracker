package controllers

import java.io.FileOutputStream

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

		val headerPositions = HeadersToValues(inFile, 0, headers, Map.empty[String, (List[String], List[String])])
		val sheet = headerPositions.getSheet.get
		val workBook = sheet.getWorkbook

		def setValues[T](fields: Map[String, T], setCell: (Cell, T) => Unit) =
			fields.foreach {
				case (header, value) => headerPositions.getHeaderLocation(header) match {
					case Some((r, c)) =>
						val row = sheet.getRow(r + 1) match {
							case null => sheet.createRow(r + 1)
							case rowFound => rowFound
						}
						val cell = row.getCell(c) match {
							case null => row.createCell(c)
							case cellFound => cellFound
						}
						setCell(cell, value)
				}
			}

		@tailrec
		def processResults(index: Int, results: Iterator[MergeResult], errs: List[String]) : List[String] = {
			if (results.hasNext) {
				val result = results.next
				val ezPassResults = result.bsp match {
					case Some(bsp) =>
						val strOptionFields = getBspFields(bsp).flatMap {
							case (k, Some(str)) => List(k -> str)
							case _ => List.empty
						}
						val strFields = strOptionFields ++ getMidFields(result.mid) ++ constantFields
						val intFields = getCalcIntFields(index, libSize, libVol)
						val floatFields = getCalcFloatFields(libConcentration)
						setValues[String](strFields, (cell, value) => cell.setCellValue(value))
						setValues[Int](intFields, (cell, value) => cell.setCellValue(value))
						setValues[Float](floatFields, (cell, value) => cell.setCellValue(value))
						List.empty
					case None => List("No sample found")
				}
				processResults(index + 1, results, errs ++ ezPassResults)
			} else errs
		}

		import java.io.File
		TransferContents.getContents(component).map {
			case Some(contents) => contents.wells.get(TransferContents.oneWell) match {
				case Some(resultSet) =>
					val errs = processResults(1, resultSet.toIterator, contents.errs)
					val outFile = new FileOutputStream(File.createTempFile("RIP", ".xlsx"))
					workBook.write(outFile)
					outFile.close()
					errs
				case None => List(s"$component is a multi-well component") ++ contents.errs
			}
			case None => List(s"$component has no contents")
		}
	}

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

	private case class MidInfo(seq: String, name: String, kit: String)
	private def getMidSeq(mids: MidInfo) = mids.seq
	private def getMidName(mids: MidInfo) = mids.name
	private def getMidKit(mids: MidInfo) = mids.kit
	private val	midFields : Map[String,(MidInfo) => String] =
		Map("Molecular Barcode Sequence" -> getMidSeq,
		"Molecular Barcode Name" -> getMidName,
		"Illumina or 454 Kit Used" -> getMidKit
	)
	private def getMidFields(mids: Set[MergeMid]) = {
		val allMids = mids.foldLeft(("", "", ""))((soFar, mid) =>
			(soFar._1 + mid.sequence, soFar._2 + "-" + mid.name, {
				val nextKit = if (mid.isNextera) "Nextera" else "Illumina"
				if (soFar._3.isEmpty || soFar._3 == nextKit) nextKit else "Nextera/Illumina"
			})
		)
		val mi = MidInfo(allMids._1, allMids._2, allMids._3)
		midFields.map{
			case (k, v) => k -> v(mi)
		}
	}

	private val constantFields =
		Map("Sequencing Technology (Illumina/454/TechX Internal Other)" -> "Illumina",
			"Single/Double Stranded (S/D)" -> "D",
			"Pooled" -> "Y"
		)

	private def getIndex(index: Int, libSize: Int, libVol: Int) = index
	private def getInsertSize(index: Int, libSize: Int, libVol: Int) = libSize - 126
	private def getLibSize(index: Int, libSize: Int, libVol: Int) = libSize
	private def getLibVol(index: Int, libSize: Int, libVol: Int) = libVol
	private val calcIntFields: Map[String, (Int, Int, Int) => Int] =
		Map("Sample Number" -> getIndex,
		"Insert Size Range bp. (i.e the library size without adapters)" -> getInsertSize,
		"Library Size Range bp. (i.e. the insert size plus adapters)" -> getLibSize,
		"Total Library Volume (ul)" -> getLibVol // Integer, warning if under 20
	)
	private def getCalcIntFields(index: Int, libSize: Int, libVol: Int) =
		calcIntFields.map {
			case (k, v) => k -> v(index, libSize, libVol)
		}

	private def getLibConcentration(libConcentration: Float) = libConcentration
	private val calcFloatFields: Map[String, (Float) => Float] =
		Map("Total Library Concentration (ng/ul)" -> getLibConcentration)
	private def getCalcFloatFields(libConcentration: Float) =
		calcFloatFields.map {
			case (k, v) => k -> v(libConcentration)
		}

	val headers =
		(bspMap.keys ++ midFields.keys ++ constantFields.keys ++ calcIntFields.keys ++ calcFloatFields.keys).toList

	private val unknown = "Unknown"
	private val unKnownFields = Map(
		"Funding Source" -> "Get from Jira parent ticket",
		"Coverage (# Lanes/Sample)" -> "Get from Jira parent ticket",
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
