package models

import models.TransferContents.{MergeResult, MergeSample}
import models.initialContents.MolecularBarcodes
import org.broadinstitute.LIMStales.sampleRacks.SampleMapEntry

/**
 * Create a walkup sequencing sheet.
 * Created by nnovod on 2/1/16.
 */
object Walkup {
	// Spreadsheet headers
	private val sampleName = "SampleName"
	private val bc1 = "IndexBarcode1"
	private val bc2 = "IndexBarcode2"
	private val headerLocs = Array(sampleName, bc1, bc2)

	/**
	 * Make a spreadsheet with walk up sequencing instructions.
	 *
	 * @param samples samples to be put in spreadsheet
	 * @return (name of file created, if it went ok, errors found)
	 */
	def makeWUS(samples: Set[MergeResult]) = {
		/**
		 * Get sample id and position
		 * @param mergeSample optional sample from merge results
		 * @return optional (id, position)
		 */
		def getId(mergeSample: Option[MergeSample]) =
			mergeSample match {
				case Some(sample) =>
					Some((sample.origCollabID.getOrElse(sample.sampleTube), sample.pos))
				case None => None
			}

		/**
		 * Get sample name for walkup sequencing sheet
		 * @param id initial sample id
		 * @param pos sample position
		 * @param ab optional antibody
		 * @return sample id to use in walkup sequencing sheet
		 */
		def getSampleStr(id: String, pos: String, ab: Option[String]) = {
			SampleMapEntry.fixStr(
				in = id,
				append = SampleMapEntry.getExtra(pos = pos, ab = ab),
				maxLen = 50
			)
		}

		/**
		 * Get sample name for walkup sequencing sheet
		 * @param mergeSample optional sample from merge results
		 * @param ab optional antibody
		 * @return sample id to use in walkup sequencing sheet (blank if no merge sample)
		 */
		def getSampleId(mergeSample: Option[MergeSample], ab: Option[String]) = {
			getId(mergeSample) match {
				case Some((sampleId, pos)) => getSampleStr(sampleId, pos, ab)
				case None => ""
			}
		}


		// Group by unique barcodes
		val uniqueMids = samples.groupBy((mergeResult) => {
			mergeResult.mid.headOption match {
				case Some(mid) =>
					val mids = MolecularBarcodes.splitSequence(mid.sequence)
					if (mids.isEmpty) ("", "")
					else if (mids.size == 1) (mids(0), "")
					else (mids(1), mids(0)) // p7 barcode comes first in WUS
				case None => ("", "")
			}
		})

		// Make csv file
		spreadsheets.Utils.setCSVValues(
			headers = headerLocs, input = uniqueMids,
			getValues = (inp: ((String, String), Set[MergeResult]), headers) => {
				val ((mid1, mid2), mergeResults) = inp
				// Get string we'll use for sample
				val sample =
					if (mergeResults.isEmpty) ""
					else if (mergeResults.size == 1)
						getSampleId(mergeResults.head.sample, mergeResults.head.antibody.headOption)
					else {
						// If multiple samples with same barcode(s) then first sort them by sample name and position
						val sortedResults = mergeResults.toArray.sortBy(mergeResult => {
							getId(mergeResult.sample) match {
								case Some((sampleId, pos)) => sampleId + "_" + pos
								case None => ""
							}
						})
						// Get first and last of samples
						val first = sortedResults.head
						val last = sortedResults.last
						// Now make name as firstSample_pos_lastSample_pos unless firstSample and lastSample names
						// are the same - then simply sampleName_firstPos_lastPos
						getId(first.sample) match {
							case Some((sampleId, pos)) =>
								getId(last.sample) match {
									case Some((sampleId1, pos1)) =>
										val start = sampleId + "_" + pos +
											(if (sampleId == sampleId1) "" else "_" + sampleId1)
										getSampleStr(start, pos1, sortedResults.head.antibody.headOption)
									case None => getSampleId(first.sample, first.antibody.headOption)
								}
							case None => getSampleId(last.sample, last.antibody.headOption)
						}
					}
				// If no sample name then skip it, otherwise set next line in spreadsheet with sample name and MIDs
				if (sample.isEmpty)
					None
				else {
					// Return array of values found, ordered by headers
					Some(headers.map((s) =>
						if (s == sampleName) sample
						else if (s == bc1) mid1
						else if (s == bc2) mid2
						else throw new Exception("Unknown WUS header")))
				}
			},
			noneMsg = "No samples found")
	}
}
