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

	/**
	 * Make an EZPASS file.
	 * @param inFile template EZPASS
	 * @param component component to make EZPASS for
	 * @param libSize library insert size including adapters
	 * @param libVol library volume (ul)
	 * @param libConcentration library concentration (ng/ul)
	 * @return list of errors
	 */
	def makeEZPass(inFile: String, component: String, libSize: Int, libVol: Int, libConcentration: Float) = {
		// Open up template EZPASS and get locations of wanted headers
		val headerPositions = HeadersToValues(inFile, 0, headers, Map.empty[String, (List[String], List[String])])
		// Get poi sheet and workbook for EZPASS
		val sheet = headerPositions.getSheet.get
		val workBook = sheet.getWorkbook

		/**
		 * Set values in spreadsheet cells
		 * @param fields fields to set (header name -> value to set)
		 * @param index sample number
		 * @param setCell callback to set cell value
		 * @tparam T type of cell value
		 */
		def setValues[T](fields: Map[String, T], index: Int, setCell: (Cell, T) => Unit) =
			fields.foreach {
				case (header, value) => headerPositions.getHeaderLocation(header) match {
					case Some((r, c)) =>
						val row = sheet.getRow(r + index) match {
							case null => sheet.createRow(r + index)
							case rowFound => rowFound
						}
						val cell = row.getCell(c) match {
							case null => row.createCell(c)
							case cellFound => cellFound
						}
						setCell(cell, value)
					case _ =>
				}
			}

		/**
		 * Process the results of looking at transfers into wanted component.
 		 * @param index sample number
		 * @param results results from transfers
		 * @param errs errors found so far
		 * @return update list of errors
		 */
		@tailrec
		def processResults(index: Int, results: Iterator[MergeResult], errs: List[String]) : List[String] = {
			if (results.hasNext) {
				val result = results.next()
				val ezPassResults = result.bsp match {
					case Some(bsp) =>
						// Get optional string values (leave out those that are not set)
						val strOptionFields = getBspFields(bsp).flatMap {
							case (k, Some(str)) => List(k -> str)
							case _ => List.empty
						}
						// Get string, integer and floating point values
						val strFields = strOptionFields ++ getMidFields(result.mid) ++ constantFields
						val intFields = getCalcIntFields(index, libSize, libVol)
						val floatFields = getCalcFloatFields(libConcentration)
						// Now go set the values into the spreadsheet
						setValues[String](strFields, index, (cell, value) => cell.setCellValue(value))
						setValues[Int](intFields, index, (cell, value) => cell.setCellValue(value))
						setValues[Float](floatFields, index, (cell, value) => cell.setCellValue(value))
						// No errors
						List.empty
					case None => List("No sample found")
				}
				// Go get next sample
				processResults(index + 1, results, errs ++ ezPassResults)
			} else errs
		}

		import java.io.File
		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		// Go get contents from transfers into specified component
		TransferContents.getContents(component).map {
			// Should just be one well (i.e., tube) of contents
			case Some(contents) => contents.wells.get(TransferContents.oneWell) match {
				case Some(resultSet) =>
					// Go put results into the EZPASS
					val errs = processResults(1, resultSet.toIterator, contents.errs)
					// Create temporary file and write new EZPASS there
					val tFile = File.createTempFile("TRACKER_", ".xlsx")
					val outFile = new FileOutputStream(tFile)
					workBook.write(outFile)
					outFile.close()
					(Some(tFile.getCanonicalPath), errs)
				case None => (None, List(s"$component is a multi-well component") ++ contents.errs)
			}
			case None => (None, List(s"$component has no contents"))
		}
	}

	// Values from bsp data - methods to retrieve specific values
	private def getProject(bsp: MergeBsp) = Some(bsp.project)
	private def getProjectDescription(bsp: MergeBsp) = bsp.projectDescription
	private def getGssrSample(bsp: MergeBsp) = bsp.gssrSample
	private def getCollabSample(bsp: MergeBsp) = bsp.collabSample
	private def getIndividual(bsp: MergeBsp) = bsp.individual
	private def getLibrary(bsp: MergeBsp) = bsp.library
	private def getSampleTube(bsp: MergeBsp) = Some(bsp.sampleTube)
	// Map of headers to methods to retrieve value
	private val bspMap : Map[String, (MergeBsp) => Option[String]]=
		Map("Additional Sample Information" -> getProject, // Jira ticket
			"Project Title Description (e.g. MG1655 Jumping Library Dev.)" -> getProjectDescription, // Ticket summary
			"Source Sample GSSR ID" -> getGssrSample,
			"Collaborator Sample ID" -> getCollabSample,
			"Individual Name (aka Patient ID, Required for human subject samples)" -> getIndividual,
			"Library Name (External Collaborator Library ID)" -> getLibrary,
			"Sample Tube Barcode" -> getSampleTube)

	/**
	 * Get bsp fields - go through bsp map and return new map with fetched values
	 * @param bsp bsp data
	 * @return map of headers to values
	 */
	private def getBspFields(bsp: MergeBsp) = bspMap.map{
		case (k, v) => k -> v(bsp)
	}

	// Values from MIDs - methods to retreive values
	private case class MidInfo(seq: String, name: String, kit: String)
	private def getMidSeq(mids: MidInfo) = mids.seq
	private def getMidName(mids: MidInfo) = mids.name
	private def getMidKit(mids: MidInfo) = mids.kit
	// Map of headers to methods to retrieve value
	private val	midFields : Map[String,(MidInfo) => String] =
		Map("Molecular Barcode Sequence" -> getMidSeq,
		"Molecular Barcode Name" -> getMidName,
		"Illumina or 454 Kit Used" -> getMidKit
	)

	/**
	 * Get MID fields - go through MID map and return new map with fetched values
	 * @param mids data for MIDs
	 * @return map of headers to values
	 */
	private def getMidFields(mids: Set[MergeMid]) = {
		// Combine all the MIDs (should normally be only one but we allow for more)
		val allMids = mids.foldLeft(("", "", ""))((soFar, mid) =>
			(soFar._1 + mid.sequence, if (soFar._2.isEmpty) mid.name else soFar._2 + "-" + mid.name, {
				val nextKit = if (mid.isNextera) "Nextera" else "Illumina"
				if (soFar._3.isEmpty || soFar._3 == nextKit) nextKit else "Nextera/Illumina"
			})
		)
		val mi = MidInfo(seq = allMids._1, name = allMids._2, kit = allMids._3)
		midFields.map{
			case (k, v) => k -> v(mi)
		}
	}

	// Map for contant field headers to constant values
	private val constantFields =
		Map("Sequencing Technology (Illumina/454/TechX Internal Other)" -> "Illumina",
			"Single/Double Stranded (S/D)" -> "D",
			"Requested Completion Date" -> "ASAP",
			"Pooled" -> "Y"
		)

	// Integer values - methods to retrieve values
	private def getIndex(index: Int, libSize: Int, libVol: Int) = index
	private def getInsertSize(index: Int, libSize: Int, libVol: Int) = libSize - 126
	private def getLibSize(index: Int, libSize: Int, libVol: Int) = libSize
	private def getLibVol(index: Int, libSize: Int, libVol: Int) = libVol
	// Map of headers to methods to retrieve values
	private val calcIntFields: Map[String, (Int, Int, Int) => Int] =
		Map("Sample Number" -> getIndex,
		"Insert Size Range bp. (i.e the library size without adapters)" -> getInsertSize,
		"Library Size Range bp. (i.e. the insert size plus adapters)" -> getLibSize,
		"Total Library Volume (ul)" -> getLibVol // Integer, warning if under 20
	)

	/**
	 * Retrieve integer values
	 * @param index sample number
	 * @param libSize library size
	 * @param libVol library volume
	 * @return map of headers to values
	 */
	private def getCalcIntFields(index: Int, libSize: Int, libVol: Int) =
		calcIntFields.map {
			case (k, v) => k -> v(index, libSize, libVol)
		}

	// Floating point values - methods to retreive values
	private def getLibConcentration(libConcentration: Float) = libConcentration
	// Map of headers to methods to retrieve values
	private val calcFloatFields: Map[String, (Float) => Float] =
		Map("Total Library Concentration (ng/ul)" -> getLibConcentration)

	/**
	 * Retrieve floating point values.
	 * @param libConcentration library concentration
	 * @return map of headers to values
	 */
	private def getCalcFloatFields(libConcentration: Float) =
		calcFloatFields.map {
			case (k, v) => k -> v(libConcentration)
		}

	// All the headers we want to set
	val headers =
		(bspMap.keys ++ midFields.keys ++ constantFields.keys ++ calcIntFields.keys ++ calcFloatFields.keys).toList

	// EZPASS fields we don't know how to fill in yet
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
		"Data Submission (Yes, Yes Later, or No)" -> unknown,
		"Additional Assembly and Analysis Information" -> unknown)
}
