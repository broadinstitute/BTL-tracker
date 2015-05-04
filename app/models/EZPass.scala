package models

import java.io.{FileOutputStream, File}

import models.TransferContents.{MergeMid, MergeBsp, MergeResult}
import models.project.SquidProject
import org.apache.poi.ss.usermodel.Cell
import org.broadinstitute.spreadsheets.HeadersToValues
import play.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Format, Json}
import play.api.data.format.Formats._

import scala.annotation.tailrec

/**
 * Module to use for form to create an EZPass.
 * Created by nnovod on 3/20/15.
 */

/**
 * EZPASS parameters to be taken from form
 * @param component component to make EZPASS for
 * @param libSize library insert size including adapters
 * @param libVol library volume (ul)
 * @param libConcentration library concentration (ng/ul)
 * @return list of errors
 */
case class EZPass(component: String, libSize: Int, libVol: Int, libConcentration: Float)
object EZPass {
	val idKey = "id"
	val libSizeKey = "libSize"
	val libVolKey = "libVol"
	val libConcKey = "libConc"
	val form =
		Form(mapping(
			idKey -> nonEmptyText,
			libSizeKey -> number,
			libVolKey -> number(min=20),
			libConcKey -> of[Float]
		)(EZPass.apply)(EZPass.unapply))

	/**
	 * Formatter for going to/from and validating Json
	 */
	implicit val ezpassFormat : Format[EZPass] = Json.format[EZPass]

	/**
	 * Interface to use to create an EZPass.
	 * @tparam T context data passed between calls
	 */
	trait SetEZPassData[T] {
		/**
		 * Initialize what's needed to handle the output data
		 * @param component ID of component EZPass is being made for
		 * @param fileHeaders headers we want to set data for in EZPass
		 * @return context to be used in other interface calls
		 */
		def initData(component: String, fileHeaders: List[String]): T

		/**
		 * Set fields for a new row of EZPass data.
		 * @param context context being kept for setting EZPass data
		 * @param strData strings to be set for next spreadsheet entry (fieldName -> data)
		 * @param intData integers to be set for next spreadsheet entry (fieldName -> data)
		 * @param floatData floating points to be set for next spreadsheet entry (fieldName -> data)
		 * @param index index of entry (1-based)
		 * @return context to continue operations
		 */
		def setFields(context: T, strData: Map[String, String], intData: Map[String, Int],
					  floatData: Map[String, Float], index: Int): T

		/**
		 * All done processing EZPass data.
		 * @param context context kept for handling EZPass data
		 * @param samplesFound # of samples found
		 * @param errs list of errors found
		 * @return (optional conclusion, list of errors)
		 */
		def allDone(context: T,  samplesFound: Int, errs: List[String]): (Option[String], List[String])
	}

	/**
	 * Context information to remember while writing out data to an EZPass spreadsheet.
	 * @param headersToValues contains locations of headers in spreadsheet and current spreadsheet state
	 * @param component component that EZPass is being done for
	 */
	case class DataSource(headersToValues: HeadersToValues, component: String)

	/**
	 * Implementation of setEZPassData to write out EZPass data to a new spreadsheet.
	 */
	object writeEZPassData extends SetEZPassData[DataSource] {
		/**
		 * Initialize what's needed to write output to a new EZPass spreadsheet.
		 * @param component ID of component EZPass is being made for
		 * @param fileHeaders headers we want to set data for in EZPass
		 * @return context containing spreadsheet information for setting sample information in spreadsheet
		 */
		def initData(component: String, fileHeaders: List[String]) = {
			val inFile = Play.application().path().getCanonicalPath + "/conf/data/EZPass.xlsx"
			DataSource(
				HeadersToValues(inFile, 0, fileHeaders, Map.empty[String, (List[String], List[String])]), component)
		}

		/**
		 * Set the fields for a row in the spreadsheet.  We retrieve all the values and set them in the proper row
		 * (based on index) under the proper headers.
		 * @param context spreadsheet information
		 * @param strData strings to be set for next spreadsheet entry (fieldName -> data)
		 * @param intData integers to be set for next spreadsheet entry (fieldName -> data)
		 * @param floatData floating points to be set for next spreadsheet entry (fieldName -> data)
		 * @param index index of entry (1-based)
		 * @return context with spreadsheet information to keep using
		 */
		def setFields(context: DataSource, strData: Map[String, String], intData: Map[String, Int],
					  floatData: Map[String, Float], index: Int): DataSource = {
			/*
			 * Set values in spreadsheet cells
			 * @param fields fields to set (header name -> value to set)
			 * @param index sample number
			 * @param setCell callback to set cell value
			 * @tparam T type of cell value
			 */
			def setValues[T](context: DataSource, fields: Map[String, T], index: Int, setCell: (Cell, T) => Unit) = {
				val headerParameters = context.headersToValues
				val sheet = headerParameters.getSheet.get
				fields.foreach {
					case (header, value) => headerParameters.getHeaderLocation(header) match {
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
			}
			// Set the values into the spreadsheet
			setValues[String](context, strData, index, (cell, value) => cell.setCellValue(value))
			setValues[Int](context, intData, index, (cell, value) => cell.setCellValue(value))
			setValues[Float](context, floatData, index, (cell, value) => cell.setCellValue(value))
			context
		}

		/**
		 * We're all done setting data into the EZPass.  If any samples present then write out a new EZPass and shut
		 * things down.  The output file is a temp file that is ready to be uploaded to the user.
		 * @param context context kept for handling EZPass data
		 * @param samplesFound # of samples found
		 * @param errs list of errors found
		 * @return (path of output file, list of errors)
		 */
		def allDone(context: DataSource, samplesFound: Int, errs: List[String]) = {
			context.headersToValues.getSheet match {
				case Some(sheet) =>
					if (samplesFound != 0) {
						// Create temporary file and write new EZPASS there
						val tFile = File.createTempFile("TRACKER_", ".xlsx")
						val outFile = new FileOutputStream(tFile)
						sheet.getWorkbook.write(outFile)
						outFile.close()
						(Some(tFile.getCanonicalPath), errs)
					} else
						(None, List(s"No samples found") ++ errs)
				case None =>
					(None, errs)
			}
		}
	}

	/**
	 * Make an EZPASS file.
	 * @param component ID of component to make EZPASS for
	 * @param libSize library insert size including adapters
	 * @param libVol library volume (ul)
	 * @param libConcentration library concentration (ng/ul)
	 * @tparam T type of context kept while setting data
	 * @return list of errors
	 */
	def makeEZPass[T](setData: SetEZPassData[T], component: String,
					  libSize: Int, libVol: Int, libConcentration: Float) = {
		/*
		 * Process the results of looking at transfers into wanted component.
		 * @param setContext context kept for handling EZpass
 		 * @param index sample number
		 * @param results results from transfers
		 * @param samplesFound # of bsp samples found so far
		 * @return (context, total # of samples found)
		 */
		@tailrec
		def processResults(setContext: T, index: Int, results: Iterator[MergeResult], samplesFound: Int): (T, Int) = {
			if (results.hasNext) {
				val result = results.next()
				val ezPassResults = result.bsp match {
					case Some(bsp) =>
						// Get bsp and squid optional values
						// Put together optional string values (leave out those that are not set)
						val strOptionFields = getBspFields(bsp).flatMap {
							case (k, Some(str)) => List(k -> str)
							case _ => List.empty
						}
						// Get string, integer and floating point values
						val strFields = strOptionFields ++ getMidFields(result.mid) ++
							getCalcStrFields(component) ++ constantFields
						val intFields = getCalcIntFields(index, libSize, libVol)
						val floatFields = getCalcFloatFields(libConcentration)
						// Found sample
						(setData.setFields(setContext, strFields, intFields, floatFields, index), 1)
					// Sample not found
					case None => (setContext, 0)
				}
				// Go get next sample
				processResults(ezPassResults._1, index + 1, results, samplesFound + ezPassResults._2)
			} else (setContext, samplesFound)
		}

		// Go initialize collector and get context for completing output of data
		val context = setData.initData(component, headers)

		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		// Go get contents from transfers into specified component
		TransferContents.getContents(component).map {
			// Should just be one well (i.e., tube) of contents
			case Some(contents) => contents.wells.get(TransferContents.oneWell) match {
				case Some(resultSet) =>
					// Go put results into the EZPASS
					val samplesFound = processResults(context, 1, resultSet.toIterator, 0)
					// Finish things up
					setData.allDone(samplesFound._1, samplesFound._2, contents.errs)
				// No results - must not be a tube
				case None => setData.allDone(context, 0, List(s"$component is a multi-well component") ++ contents.errs)
			}
			// No contents
			case None => setData.allDone(context, 0, List(s"$component has no contents"))
		}.recover{
			case e: Exception => setData.allDone(context, 0, List(s"Exception: ${e.getLocalizedMessage}"))
		}
	}

	// Values from bsp data - methods to retrieve specific values
	private def getProject(bsp: MergeBsp) = Some(bsp.project)
	private def getProjectDescription(bsp: MergeBsp) = bsp.projectDescription
	private def getGssrSample(bsp: MergeBsp) = bsp.gssrSample
	private def getCollabSample(bsp: MergeBsp) = bsp.collabSample
	private def getIndividual(bsp: MergeBsp) = bsp.individual
	private def getLibrary(bsp: MergeBsp) = bsp.library
	// Values from Squid
	private def getSquidProject(bsp: MergeBsp) = bsp.gssrSample match {
		case Some(gssr) => SquidProject.findProjNameByBarcode(gssr)
		case _ => None
	}

	// private def getSampleTube(bsp: MergeBsp) = Some(bsp.sampleTube)
	// Map of headers to methods to retrieve bsp values
	private val bspMap : Map[String, (MergeBsp) => Option[String]]=
		Map("Additional Sample Information" -> getProject, // Jira ticket
			"Project Title Description (e.g. MG1655 Jumping Library Dev.)" -> getProjectDescription, // Ticket summary
			"Source Sample GSSR ID" -> getGssrSample,
			"Collaborator Sample ID" -> getCollabSample,
			"Individual Name (aka Patient ID, Required for human subject samples)" -> getIndividual,
			"Library Name (External Collaborator Library ID)" -> getLibrary,
			"SQUID Project" -> getSquidProject)

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
			(soFar._1 + mid.sequence, if (soFar._2.isEmpty) mid.name else soFar._2 + "+" + mid.name, {
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

	// String values - methods to retreive values
	private def getTubeBarcode(id: String) = id
	// Map of headers to methods to retrieve values
	private val calcStrFields: Map[String, (String) => String] =
		Map("Sample Tube Barcode" -> getTubeBarcode)

	/**
	 * Retrieve string values.
	 * @param id ID for EZPASS tube
	 */
	private def getCalcStrFields(id: String) =
		calcStrFields.map {
			case (k, v) => k -> v(id)
		}

	// All the headers we want to set
	private val headers =
		(bspMap.keys ++ midFields.keys ++ constantFields.keys ++
			calcIntFields.keys ++ calcFloatFields.keys ++ calcStrFields.keys).toList

	// EZPASS fields we don't know how to fill in yet
	private val unknown = "Unknown"
	private val unKnownFields = Map(
		"Funding Source" -> "Get from Jira parent ticket",
		"Coverage (# Lanes/Sample)" -> "Get from Jira parent ticket",
		"Virtual GSSR ID" -> "Assigned via SQUID",
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
