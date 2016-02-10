package models

import models.TransferContents.MergeResult
import models.initialContents.MolecularBarcodes

/**
 * Create a walkup sequencing sheet.
 * Created by nnovod on 2/1/16.
 */
object Walkup {
	/**
	 * Make a spreadsheet with walk up sequencing instructions.
	 *
	 * @param samples samples to be put in spreadsheet
	 * @return (name of file created, if it went ok, errors found)
	 */
	def makeWUS(samples: Set[MergeResult]) = {
		// Spreadsheet headers
		val sampleName = "sampleName"
		val bc1 = "IndexBarcode1"
		val bc2 = "IndexBarcode2"
		val headerLocs = Array(sampleName, bc1, bc2)

		// Make csv file
		spreadsheets.Utils.setCSVValues(
			headers = headerLocs, input = samples,
			getValues = (inp: MergeResult, headers) => {
				// Get sample name
				val sample = inp.bsp match {
					case Some(bsp) => bsp.collabSample.getOrElse("")
					case None => ""
				}
				// If no sample name then skip it, otherwise set next line in spreadsheet with sample name and MIDs
				if (sample.isEmpty)
					None
				else {
					val (mid1, mid2) = inp.mid.headOption match {
						case Some(mid) =>
							val mids = MolecularBarcodes.splitSequence(mid.sequence)
							if (mids.isEmpty) ("", "")
							else if (mids.size == 1) (mids(0), "")
							else (mids(0), mids(1))
						case None => ("", "")
					}
					Some(headers.map((s) =>
						if (s == sampleName) sample
						else if (s == bc1) mid1
						else if (s == bc2) mid2
						else throw new Exception("Unknown WUS header")))
				}
			},
			noneMsg = "No sample names found")
	}
}
