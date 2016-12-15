package models

import models.TransferContents.{MergeResult, MergeSample}
import models.initialContents.MolecularBarcodes
import org.broadinstitute.LIMStales.sampleRacks.SampleMapEntry
import utils.SampleNames

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
		// Group by unique barcodes (p7, p5, error)
		val uniqueMids = samples.groupBy((mergeResult) => {
			val err = if (mergeResult.mid.size > 1) "ERROR: More than one barcode set found" else ""
			mergeResult.mid.headOption match {
				case Some(mid) =>
					val mids = MolecularBarcodes.splitSequence(mid.sequence)
					if (mids.isEmpty) ("", "", err)
					else if (mids.size == 1) (mids(0), "", err)
					else if (mids.size == 2) (mids(1), mids(0), err) // p7 barcode comes first in WUS
					else (mids(1), mids(0), {
						val errHeader = if (err.isEmpty) "ERROR:" else ";"
						err + errHeader + " More than two barcodes found in set"
					})
				case None => ("", "", err)
			}
		})

		// Make csv file
		spreadsheets.Utils.setCSVValues(
			headers = headerLocs, input = uniqueMids,
			getValues = (inp: ((String, String, String), Set[MergeResult]), headers) => {
				val ((mid1, mid2, err), mergeResults) = inp
				// Get string we'll use for sample
				val sample =
					if (mergeResults.isEmpty) ""
					else if (mergeResults.size == 1)
						SampleNames.getSampleId(
							mergeSample = mergeResults.head.sample,
							ab = mergeResults.head.antibody.headOption,
							fetchID = SampleNames.getWalkupSampleName,
							maxLen = SampleNames.walkupMaxSampleNameLen
						)
					else {
						val (name, _) = SampleNames.getMergedSampleName(
							mergeResults = mergeResults,
							fetchID = SampleNames.getWalkupSampleName,
							maxLen = SampleNames.walkupMaxSampleNameLen
						)
						name
					}
				// If no sample name then skip it, otherwise set next line in spreadsheet with sample name and MIDs
				if (sample.isEmpty)
					List.empty
				else {
					// Return array of values found, ordered by headers
					val bcs = headers.map((s) =>
						if (s == sampleName) sample
						else if (s == bc1) mid1
						else if (s == bc2) mid2
						else throw new Exception("Unknown WUS header"))
					List(if (err.isEmpty) bcs else bcs :+ err)
				}
			},
			noneMsg = "No samples found")
	}
}
