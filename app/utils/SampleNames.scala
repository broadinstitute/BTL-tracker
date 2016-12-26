package utils

import models.Transfer.Quad
import models.TransferContents.{MergeResult, MergeSample}
import models.TransferWells
import org.broadinstitute.LIMStales.sampleRacks.SampleMapEntry

/**
 * Some methods to make sample names.  The names are used for walkup sequencing sheets and EZPasses.
 * Created by nnovod on 11/29/16.
 */
object SampleNames {
	// Maximum lengths for sample names
	val walkupMaxSampleNameLen = 50
	val ezPassMaxSampleNameLen = 512

	/**
	 * Get walkup sheet sample name for a single sample
	 * @param ms sample info
	 * @return (collaboratorID if there; otherwise the tube ID, sample position)
	 */
	def getWalkupSampleName(ms: MergeSample): (String, String) = (ms.origCollabID.getOrElse(ms.sampleTube), ms.pos)

	/**
	 * Get EZPass sample name for a single sample
	 * @param ms sample info
	 * @return (sampleID if there; otherwise the tube ID, sample position)
	 */
	def getEZPassSampleName(ms: MergeSample): (String, String) = (ms.sampleID.getOrElse(ms.sampleTube), ms.pos)

	/**
	 * Get sample id and position
	 * @param mergeSample optional sample from merge results
	 * @param fetchID given a MergeSample return (sampleID, samplePosition)
	 * @return optional (id, position)
	 */
	def getId(mergeSample: Option[MergeSample], fetchID: (MergeSample) => (String, String)): Option[(String, String)] =
		mergeSample match {
			case Some(sample) =>
				Some(fetchID(sample))
			case None => None
		}

	/**
	 * Get sample name with appended position and antibody, maximun length and fixup of ugly characters.
	 * @param id initial sample id
	 * @param pos sample position
	 * @param ab optional antibody
	 * @param maxLen maximum length for sample string
	 * @return sample id to use
	 */
	def getSampleStr(id: String, pos: String, ab: Option[String], maxLen: Int): String = {
		SampleMapEntry.fixStr(
			in = id,
			append = SampleMapEntry.getExtra(pos = pos, ab = ab),
			maxLen = maxLen
		)
	}

	/**
	 * Get sample name using callback for sample ID
	 * @param mergeSample optional sample from merge results
	 * @param ab optional antibody
	 * @param fetchID callback, given a MergeSample, returns (sampleID, samplePosition)
	 * @param maxLen maximum length for sample string
	 * @return sample id to use (blank if no merge sample)
	 */
	def getSampleId(mergeSample: Option[MergeSample], ab: Option[String],
					fetchID: (MergeSample) => (String, String), maxLen: Int): String = {
		getId(mergeSample, fetchID) match {
			case Some((sampleId, pos)) => getSampleStr(sampleId, pos, ab, maxLen)
			case None => ""
		}
	}

	/**
	 * Get sample name for a set of samples - we sort the samples by sampleID/position and then make a name combining
	 * the first and last sampleIds and positions
	 * @param mergeResults samples to be merged
	 * @param fetchID given a MergeSample return (sampleID, samplePosition)
	 * @param maxLen maximum length for sample string
	 * @return sample id to use in walkup sequencing sheet (blank if no merge sample)
	 */
	def getMergedSampleName(mergeResults: Set[MergeResult],
							fetchID: (MergeSample) => (String, String), maxLen: Int): (String, Option[MergeResult]) = {
		if (mergeResults.isEmpty)
			("", None)
		else if (mergeResults.size == 1)
			(SampleNames.getSampleId(
				mergeSample = mergeResults.head.sample,
				ab = mergeResults.head.antibody.headOption,
				fetchID = fetchID,
				maxLen = maxLen
			), mergeResults.headOption)
		else {
			// If multiple samples with same barcode(s) then first sort them by sample name and position
			val sortedResults = mergeResults.toArray.sortBy(mergeResult => {
				SampleNames.getId(
					mergeSample = mergeResult.sample,
					fetchID = fetchID
				) match {
					case Some((sampleId, pos)) => sampleId + "_" + pos
					case None => ""
				}
			})
			// Get first and last of samples
			val first = sortedResults.head
			val last = sortedResults.last
			// Now make name as firstSample_pos_lastSample_pos unless firstSample and lastSample names
			// are the same - then simply sampleName_firstPos_lastPos
			SampleNames.getId(
				mergeSample = first.sample,
				fetchID = fetchID
			) match {
				case Some((sampleId, pos)) =>
					// Get name for first sample
					val firstId = SampleNames.getSampleStr(
						id = sampleId, pos = pos, ab = first.antibody.headOption,
						maxLen = maxLen
					)
					SampleNames.getId(
						mergeSample = last.sample,
						fetchID = fetchID
					) match {
						case Some((sampleId1, pos1)) =>
							// If initial sample names are the same
							//  If wells are first and last well of quadrant and no antibodies are involved then
							//  make name sampleId_quadrant else just append position and antibody
							//  of second sample to first sample name
							// else append together complete first and second sample names
							if (sampleId == sampleId1) {
								// See if wells start and end quadrant
								val qFound =
									TransferWells.quadFirstAndLast.find {
										case (_, wells) => wells._1 == pos && wells._2 == pos1
									}
								// If no antibodies and we start and end quadrant then set quadrant as "position"
								if (first.antibody.isEmpty && last.antibody.isEmpty && qFound.isDefined) {
									(SampleNames.getSampleStr(
										id = sampleId, pos = Quad.shortStr(qFound.get._1), ab = None, maxLen = maxLen
									), Some(first))
								} else {
									// Otherwise add last position to id gotten for first sample
									(SampleNames.getSampleStr(
										id = firstId, pos = pos1, ab = last.antibody.headOption,
										maxLen = maxLen
									), Some(first))
								}
							} else
								// Put together two ids from different plates
								(firstId + "_" +
									SampleNames.getSampleStr(
										id = sampleId1, pos = pos1, ab = last.antibody.headOption,
										maxLen = maxLen
									), Some(first))
						case None => (firstId, Some(first))
					}
				case None =>
					(SampleNames.getSampleId(
						mergeSample = last.sample, ab = last.antibody.headOption,
						fetchID = fetchID,
						maxLen = maxLen
					), Some(last))
			}
		}
	}
}
