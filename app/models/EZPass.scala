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
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Module for creating an EZPass.
 * Created by nnovod on 3/20/15.
 */

/**
 * EZPASS parameters to be taken from form
 * @param component component to make EZPASS for
 * @param libSize library insert size including adapters
 * @param libVol library volume (ul)
 * @param libConcentration library concentration (ng/ul)
 * @param fileName name to give to EZPass
 */
case class EZPass(component: String, libSize: Int, libVol: Int, libConcentration: Float, fileName: String)

/**
 * EZPass creation logic
 */
object EZPass {
	// Form keys
	val idKey = "id"
	val libSizeKey = "libSize"
	val libVolKey = "libVol"
	val libConcKey = "libConc"
	val fileName = "fileName"
	// Form with mapping to object and validation
	val form =
		Form(mapping(
			idKey -> nonEmptyText,
			libSizeKey -> number,
			libVolKey -> number(min=20),
			libConcKey -> of[Float],
			fileName -> nonEmptyText
		)(EZPass.apply)(EZPass.unapply))

	/**
	 * Formatter for going to/from and validating Json
	 */
	implicit val ezpassFormat : Format[EZPass] = Json.format[EZPass]

	/**
	 * Interface to use to create an EZPass.
	 * @tparam T context data passed between calls
	 * @tparam R result returned by allDone call
	 */
	trait SetEZPassData[T, R] {
		/**
		 * Initialize what's needed to handle the output data
		 * @param component ID of component EZPass is being made for
		 * @param fileHeaders keys for data to set in EZPass
		 * @return context to be used in other interface calls
		 */
		def initData(component: String, fileHeaders: List[String]): T

		/**
		 * Set fields for a new row of EZPass data.  The fields being set were included in the fieldHeaders list
		 * supplied in the initData call.
		 * @param context our context being kept for setting EZPass data
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
		 * @param context our context kept for handling EZPass data
		 * @param samplesFound # of samples found
		 * @param errs list of errors found
		 * @return (result, list of errors)
		 */
		def allDone(context: T,  samplesFound: Int, errs: List[String]): Future[(R, List[String])]
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
	object WriteEZPassData extends SetEZPassData[DataSource, Option[String]] {
		/**
		 * Initialize what's needed to write output to a new EZPass spreadsheet.
		 * @param component ID of component EZPass is being made for
		 * @param fileHeaders keys for data to set in EZPass
		 * @return context containing spreadsheet information for setting sample information in spreadsheet
		 */
		def initData(component: String, fileHeaders: List[String]) = {
			val inFile = Play.application().path().getCanonicalPath + "/conf/data/EZPass.xlsx"
			DataSource(
				HeadersToValues(inFile, 0, fileHeaders, Map.empty[String, (List[String], List[String])]), component)
		}

		/**
		 * Set the fields for a row in the spreadsheet.  Retrieve all the values and set them in the proper row
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
		 * All done setting data into the EZPass.  If any samples present then write out a new EZPass and shut things
		 * down.  The output file is a temp file that is ready to be uploaded to the user.
		 * @param context context kept for handling EZPass data
		 * @param samplesFound # of samples found
		 * @param errs list of errors found
		 * @return (path of output file, list of errors)
		 */
		def allDone(context: DataSource, samplesFound: Int, errs: List[String]) = {
			Future {
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
	}

	// Types of Data returned for EZPass with project creation
	// Future to obtain LSid - optional lsid and optional exception if future completes with error
	private type FutureLSID = Future[(Option[String], Option[Exception])]
	// Data saved row-by-row - All values (maps of headers to strings, integers or floats) and futures to get LSIDs
	private type EZPassData = (Map[String, String], Map[String, Int], Map[String, Float], FutureLSID)
	// Final data - Map of values (will include squid project)
	private type EZPassFinalData = (Map[String, String], Map[String, Int], Map[String, Float])

	/**
	 * Context saved between calls of preliminary pass when making an EZPass with projects.
	 * @param fileHeaders keys for data to set in EZPass
	 * @param data data collected so far
	 */
	case class EZPassSaved(fileHeaders: List[String], data: List[EZPassData])

	/**
	 * Final output from first pass when making an EZPass with a project.
	 * @param fileHeaders keys for data to set in EZPass
	 * @param data data to set in EZPass
	 */
	case class EZPassProjectData(fileHeaders: List[String], data: List[EZPassFinalData])

	// Labels used for fields in EZPass and fetching of squid project name
	private val gssrBarcodeLabel = "Source Sample GSSR ID"
	private val squidProjectLabel = "SQUID Project"

	/**
	 * Retrieving EZPASS with project data is a bit convoluted because retrieving squid project names one-by-one is
	 * extremely slow (about 1 second per project).  To avoid this delay the project names are retrieved via squid in
	 * batches, which is much faster (4 parallel requests for 24 projects each complete in about 3 seconds), after an
	 * initial pass through all the EZPass data.  So here's how it goes:
	 * 1)For each row (setFields method) save the data found for that row and start up a future to retrieve the LSID
	 * for that row.  The LSID is needed to retrieve the project name for the row later.
	 * 2)Once all the rows are done (allDone method) wait for all the LSIDs to be retrieved and then send off a small
	 * number of parallel requests to get the project names associated with the LSIDs.
	 * 3)After the projects are retrieved put them into the proper rows data and return with all the data set,
	 * including the project.
	 */
	object WriteEZPassWithProject extends SetEZPassData[EZPassSaved, EZPassProjectData] {
		/**
		 * Initialize what's needed to handle the output data
		 * @param component ID of component EZPass is being made for
		 * @param fileHeaders keys for data to set in EZPass
		 * @return context to be used in other interface calls
		 */
		def initData(component: String, fileHeaders: List[String]) = EZPassSaved(fileHeaders, List.empty)

		/**
		 * Set fields for a new row of EZPass data.  Data passed in is saved and a future is started up to request
		 * from Squid the LSID for the sample.
		 * @param context context being kept for setting EZPass data
		 * @param strData strings to be set for next spreadsheet entry (fieldName -> data)
		 * @param intData integers to be set for next spreadsheet entry (fieldName -> data)
		 * @param floatData floating points to be set for next spreadsheet entry (fieldName -> data)
		 * @param index index of entry (1-based)
		 * @return context to continue operations
		 */
		def setFields(context: EZPassSaved, strData: Map[String, String], intData: Map[String, Int],
					  floatData: Map[String, Float], index: Int) =
			EZPassSaved(context.fileHeaders, context.data :+
				(strData, intData, floatData,
					Future {
						strData.get(gssrBarcodeLabel) match {
							case Some(str) =>
								try {
									(SquidProject.findSampleByBarcode(str), None)
								} catch {
									case e: Exception => (None, Some(e))
								}
							case _ => (None, None)
						}
					}.recover{ case e: Exception => (None, Some(e)) }
					)
			)

		/**
		 * Fold together a list of futures into a single future.  When that completes make lists of results and errors.
		 * @param futures futures to complete via fold
		 * @param errsSoFar list of errors found so far
		 * @param getErr callback to set error message from an exception
		 * @param emptyResult callback to set empty result if global exception occurs
		 * @tparam T type of data being collected
		 * @return lists of results from completion of futures and any exceptions that occurred
		 */
		private def doFold[T](futures: List[Future[(T, Option[Exception])]],
							  errsSoFar: List[String],
							  getErr: (Exception) => String,
							  emptyResult: () => T) : Future[(List[T], List[String])]= {
			if (futures.isEmpty) Future.successful((List.empty, errsSoFar)) else {
				// Fold the futures together into a list of tuples containing (T if found, exception if error)
				val futFold = Future.fold(futures)(List.empty[(T, Option[Exception])])(
					(soFar, next) => soFar :+ (next._1, next._2)
				).recover { case e: Exception => List((emptyResult(), Some(e))) }
				// Now take result of fold and separate out results list and errors
				futFold.map((result) => {
					result.foldLeft((List.empty[T], errsSoFar)){
						case ((soFarTs, soFarErrs), (nextT, nextErr)) =>
							// Add error if there was one
							val errs = nextErr match {
								case Some(e) => soFarErrs :+ getErr(e)
								case None => soFarErrs
							}
							// Add to results and return errors
							(soFarTs :+ nextT, errs)
					}
				})
			}
		}

		/**
		 * Finish retrieving EZPass values.  Project names are retrieved here in batches.  The batch size is
		 * determined by the number of requests Squid can handle simultaneously.  Given that number each of the
		 * requests query for approximately the same number of projects, using the LSIDs previously retrieved as input.
		 * @param context context kept for handling EZPass data
		 * @param samplesFound # of samples found
		 * @param errs list of errors found
		 * @return (EZPassProjectData, list of errors)
		 */
		def allDone(context: EZPassSaved, samplesFound: Int, errs: List[String]) = {
			val numRequests = 4 // Number of project request to do in parallel
			// Get a list of all the futures used to retrieve LSIDs
			val futures = context.data.map {
				case (_, _, _, fut) => fut
			}
			// First get all the lsids (from slew of futures).  Once that completes use the LSIDs to get the projects
			val lsidFutures = doFold[Option[String]](futures, errs,
				(e) => s"Error retrieving LSIDs: ${e.getLocalizedMessage}", () => None)
			val futureResults = lsidFutures.flatMap {
				case (lsidList, lsidErrs) =>
					// Get project names
					val projectToGet = lsidList.flatten
					// Find slice size to retrieve projects in groups
					val sliceSize = projectToGet.size / numRequests +
						(if (projectToGet.size % numRequests != 0) 1 else 0)
					// Start up fetching of projects in futures (empty check needed because grouped doesn't like 0)
					val projFutures =
						if (projectToGet.isEmpty) List(Future.successful(Map.empty[String, String], None))
						else {
							(for (projs <- projectToGet.grouped(sliceSize))
								yield Future {
									try {
										(SquidProject.findProjectsByLSIDs(projs), None)
									} catch {
										case e: Exception =>
											(Map.empty[String, String], Some(e))
									}
								}).toList
						}
					// Fold the project gathering futures together and wait for the results
					doFold[Map[String, String]](projFutures, lsidErrs,
						(e) => s"Error retrieving project: ${e.getLocalizedMessage}", () => Map.empty[String, String])
			}
			// Once project requests complete put the maps from each request together into one map and put the results
			// into EZPass data being collected
			futureResults.map {
				case (projsList, projErrs) =>
					// Combine project maps
					val projs = projsList.reduce(_ ++ _)
					// Now make all the entries include the projects retrieved
					val rows = context.data.map {
						case (strs, ints, floats, _) =>
							// If gssrBarcode exists and we got a project for it then add project to string values found
							strs.get(gssrBarcodeLabel) match {
								case Some(bc) => projs.get(bc) match {
									case Some(proj) => (strs + (squidProjectLabel -> proj), ints, floats)
									case _ => (strs, ints, floats)
								}
								case _ => (strs, ints, floats)
							}
					}
					// Return all the data found as well as the errors
					(EZPassProjectData(context.fileHeaders, rows), projErrs)
			}
		}

	}
	
	/**
	 * Make an EZPASS file.  First get the contents of the target component, via a parsing of the graph of components
	 * that were transferred into the target component.  Next callback to the caller via the SetEZPassData trait
	 * to let the caller handle the data as wanted.
	 * @param setData callbacks to use to create EZPass
	 * @param component ID of component to make EZPASS for
	 * @param libSize library insert size including adapters
	 * @param libVol library volume (ul)
	 * @param libConcentration library concentration (ng/ul)
	 * @tparam T type of context kept for setData
	 * @tparam R type returned
	 * @return (R data, list of errors)
	 */
	def makeEZPass[T, R](setData: SetEZPassData[T, R], component: String,
						 libSize: Int, libVol: Int, libConcentration: Float) : Future[(R, List[String])] = {
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
			// If all done then return with context kept and # of samples found
			if (results.isEmpty) (setContext, samplesFound)
			else {
				// Go get next row of data and process it
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
				processResults(ezPassResults._1, index + ezPassResults._2, results, samplesFound + ezPassResults._2)
			}
		}

		// Go initialize collector and get context for completing output of data
		val context = setData.initData(component, headers)

		import play.api.libs.concurrent.Execution.Implicits.defaultContext
		// Go get contents from transfers into specified component
		TransferContents.getContents(component).flatMap {
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
		}.recoverWith{
			case e: Exception =>
				setData.allDone(context, 0, List(s"Exception: ${e.getLocalizedMessage}"))
		}
	}

	/**
	 * Make the EZPass with data already calculated.  Simply report to the caller what has been found.
	 * @param setData callbacks to use to create EZPass
	 * @param component component EZPass is being created for
	 * @param data data already found for EZPass
	 * @param errs errors found during previous calculation of data
	 * @tparam T context to be used during EZPass creation
	 * @tparam R result returned by EZPass creation
	 * @return (R, list of errors)
	 */
	private def makeEZPassFromData[T, R](setData: SetEZPassData[T, R], component: String,
										 data: EZPassProjectData, errs: List[String]) = {
		// initData to start EZPass creation, setFields once for each row and finish up (allDone)
		val setContext = setData.initData(component, data.fileHeaders)
		@tailrec
		// Recursive fellow to set all the fields till done
		def setFields(context: T, data: List[EZPassFinalData], index: Int) : T = {
			data match {
				case Nil => context
				case head :: tail =>
					val next = setData.setFields(context, head._1, head._2, head._3, index)
					setFields(next, tail, index+1)
			}
		}
		// Set rows
		val finalContext = setFields(setContext, data.data, 1)
		// Finish up
		setData.allDone(finalContext, data.data.size, errs)
	}

	/**
	 * Make an EZPass file with project information.  This requires a few passes - first get all the data except
	 * the project information.  This first pass also starts up asynchronous activity to fetch the LSIDs for samples.
	 * The next pass retrieves the projects (as a few Squid calls) based on the LSIDs retrieved.  Finally, after the
	 * project data is retrieved, call back to the caller with the data retrieved.
	 * @param setData callbacks to use when creating EZPass
	 * @param component ID of component to make EZPass for
	 * @param libSize library insert size including adapters
	 * @param libVol library volume (ul)
	 * @param libConcentration library concentration (ng/ul)
	 * @tparam T type of context kept while setting data
	 * @tparam R type returned makeEZPass
	 * @return (R data, list of errors)
	 */
	def makeEZPassWithProject[T, R](setData: SetEZPassData[T, R], component: String,
									libSize: Int, libVol: Int, libConcentration: Float) = {
		// First get the EZPass with the project data
		makeEZPass(WriteEZPassWithProject, component, libSize, libVol, libConcentration).flatMap {
			// Then map results using input EZPass creator
			case (projData, errs) =>
				makeEZPassFromData(setData, component, projData, errs)
		}
	}

	// Values from bsp data - methods to retrieve specific values
	private def getProject(bsp: MergeBsp) = Some(bsp.project)
	private def getProjectDescription(bsp: MergeBsp) = bsp.projectDescription
	private def getGssrSample(bsp: MergeBsp) = bsp.gssrSample
	private def getCollabSample(bsp: MergeBsp) = bsp.collabSample
	private def getIndividual(bsp: MergeBsp) = bsp.individual
	private def getLibrary(bsp: MergeBsp) = bsp.library
	// private def getSampleTube(bsp: MergeBsp) = Some(bsp.sampleTube)
	// Map of headers to methods to retrieve bsp values
	private val bspMap : Map[String, (MergeBsp) => Option[String]]=
		Map("Additional Sample Information" -> getProject, // Jira ticket
			"Project Title Description (e.g. MG1655 Jumping Library Dev.)" -> getProjectDescription, // Ticket summary
			gssrBarcodeLabel -> getGssrSample,
			"Collaborator Sample ID" -> getCollabSample,
			"Individual Name (aka Patient ID, Required for human subject samples)" -> getIndividual,
			"Library Name (External Collaborator Library ID)" -> getLibrary,
			squidProjectLabel -> ((bsp: MergeBsp) => None)) // If project is requested it's picked up later

	/**
	 * Get bsp fields - go through bsp map and return new map with fetched values
	 * @param bsp bsp data
	 * @return map of headers to values
	 */
	private def getBspFields(bsp: MergeBsp) = bspMap.map {
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
		// Combine all the MIDs (should normally be only one but allow for more)
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

	// All the headers to be set in the EZPass
	private val headers =
		(bspMap.keys ++ midFields.keys ++ constantFields.keys ++
			calcIntFields.keys ++ calcFloatFields.keys ++ calcStrFields.keys).toList

	// EZPASS fields we don't know how to fill in yet
	/*
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
	*/
}
