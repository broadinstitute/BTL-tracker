package models

import Transfer.{Quad, Slice}

/**
 * Well manipulation for transfers.
 * Created by nnovod on 7/8/15.
 */
object TransferWells {
	// Rows and columns
	private val row1 = 'A'
	private val col1 = 1
	private val rowLast96 = 'H'
	private val colLast96 = 12
	private val rowsPer96 = rowLast96 - row1 + 1
	private val colsPer96 = colLast96 - col1 + 1
	private val rowLast384 = 'P'
	private val colLast384 = 24
	private val rowsPer384 = rowLast384 - row1 + 1
	private val colsPer384 = colLast384 - col1 + 1
	private val wellsPer96 = rowsPer96 * colsPer96
	private val wellsPer384 = rowsPer384 * colsPer384
	private val wellRegexp = """^([A-Z])(\d+)$""".r

	/**
	  * Make an index from a well string
	  * @param well well (A01, A02, ...)
	  * @param colsPerRow # of columns per row in welled component
	  * @return index for well (A01 = 0, A02 = 1, ...)
	  */
	private def wellToIdx(well: String, colsPerRow: Int) = {
		well.toUpperCase() match {
			case wellRegexp(row, col) => ((row.last - row1) * colsPerRow) + (col.toInt - 1)
			case _ => throw new Exception(s"Invalid well $well")
		}
	}

	/**
	  * Make an index from a well string for a 96-welled component
	  * @param well well (A01, A02, ...)
	  * @return index for well (A01 = 0, A02 = 1, ...)
	  */
	def make96IdxFromWellStr(well: String) = wellToIdx(well, colsPer96)

	/**
	  * Make an index from a well string for a 384-welled component
	  * @param well well (A01, A02, ...)
	  * @return index for well (A01 = 0, A02 = 1, ...)
	  */
	def make384IdxFromWellStr(well: String) = wellToIdx(well, colsPer384)

	import Quad._
	/**
	 * Make a well mapping going between a quadrant of a 384 well plate and a 96 well plate.
	 * @param qtr which quarter to move to/from
	 * @param toTuple callback to make a tuple of source well -> target well
	 * @return map going between a quadrant of a 384 well plate and a 96 well plate
	 */
	private def qToQ(qtr: Quad, toTuple: (Char, Int, (Int, Int)) => (String, String)) = {
		// Get relative x/y coordinates within each group of 2x2 wells to quadrants
		val q = qtr match {
			case Q1 => (0, 0)
			case Q2 => (0, 1)
			case Q3 => (1, 0)
			case Q4 => (1, 1)
		}
		// Make map
		(for {x <- row1 to rowLast96
			  y <- col1 to colLast96}
			yield toTuple(x, y, q)).toMap
	}

	/**
	 * Make string for well within quadrant in 384 well plate
	 * @param x row coordinate of matching well in 96-well plate
	 * @param y column coordinate of matching well in 96-well plate
	 * @param q relative coordinates to wanted well with each 2x2 set of wells to wanted quadrant
	 * @return well description (letter and 2-digit number - e.g., A01)
	 */
	private def well384fromQWell(x: Char, y: Int, q: (Int, Int)) =
		wellStr((((x - row1) * 2) + row1 + q._1).toChar, ((y - col1) * 2) + col1 + q._2)

	/**
	 * Make string for well in plate
	 * @param x row coordinate of well
	 * @param y column coordinate of well
	 * @return well description (letter and 2-digit number - e.g., A01)
	 */
	private def wellStr(x: Char, y: Int) = f"$x$y%02d"

	/**
	 * Make a well string (e.g., "A01") from an index (e.g., 0).  It is assumed that indicies go across first
	 * (e.g., 0 -> "A01", 1 -> "A02", ...)
	 * @param i index to wanted well
	 * @param colsPerRow # of columns per row on component
	 * @return well label (e.g., "A01")
	 */
	private def makeWellStrFromIndex(i: Int, colsPerRow: Int) =
		wellStr((row1 + i/colsPerRow).toChar, col1 + i%colsPerRow)

	/**
	 * Make a well string (e.g., "A01") from an index (e.g., 0) for a 96-well plate
	 * @param i index to wanted well
	 * @return well label (e.g., "A01")
	 */
	private def make96WellStrFromIndex(i: Int) = makeWellStrFromIndex(i, colsPer96)

	/**
	 * Make a well string (e.g., "A01") from an index (e.g., 0) for a 384-well plate
	 * @param i index to wanted well
	 * @return well label (e.g., "A01")
	 */
	private def make384WellStrFromIndex(i: Int) = makeWellStrFromIndex(i, colsPer384)

	/**
	 * Make map of 96-well plate to quadrant of 384-well plate
	 * @param qtr quadrant of 384-well plate
	 * @return map of 96-well plate wells to target quadrant wells in 384-well plate
	 */
	private def q96to384(qtr: Quad) = qToQ(qtr, (x, y, q) => wellStr(x, y) -> well384fromQWell(x, y, q))

	/**
	 * Make map of 384-well plate quadrant to 96-well plate
	 * @param qtr quadrant of 384-well plate
	 * @return map of quadrant of 384-well plate wells to target 96-well plate wells
	 */
	private def q384to96(qtr: Quad) = qToQ(qtr, (x, y, q) => well384fromQWell(x, y, q) -> wellStr(x, y))

	// Well mappings between 96-well plate and 384-well plate quadrants - can be used to see what well quadrant
	// transfers come from and are going to
	lazy val qTo384 = Map(Q1 -> q96to384(Q1), Q2 -> q96to384(Q2), Q3 -> q96to384(Q3), Q4 -> q96to384(Q4))
	lazy val qFrom384 = Map(Q1 -> q384to96(Q1), Q2 -> q384to96(Q2), Q3 -> q384to96(Q3), Q4 -> q384to96(Q4))
	// Make 384 to 384 map of maps: (fromQ, toQ) -> (originalWells -> destinationWells)
	lazy val q384to384map =
		(for {
			qFrom <- Quad.values.toIterable
			qTo <- Quad.values.toIterable
		} yield {
				val from = qFrom384(qFrom)
				val to = qTo384(qTo)
				// Going from original source well to final destination well - 96 in middle to calculate quadrant well
				(qFrom, qTo) -> from.map {
					case (k, v) => k -> to(v)
				}
			}).toMap

	/**
	  * Make map of entire plate pointing to itself
	  * @param size size of plate
	  * @param wellStrFromIndex callback to make well name from index
	  * @return map of entire plate of wells pointing to themselves
	  */
	private def makeEntireMap(size: Int, wellStrFromIndex: (Int) => String) =
		(0 to size-1).map((i) => {
			val well = wellStrFromIndex(i)
			well -> well
		}).toMap

	/**
	  * Map of wells for 96-well plate pointing to themselves (A01->A01, ..., H12->H12)
	  */
	lazy val entire96to96wells = makeEntireMap(wellsPer96, make96WellStrFromIndex)

	/**
	  * Map of wells for 384-well plate pointing to themselves (A01->A01, ..., P24->P24)
	  */
	lazy val entire384to384wells = makeEntireMap(wellsPer384, make384WellStrFromIndex)

	import Slice._

	/**
	 * Map of fixed slices to wells.
	 */
	private lazy val slices = {
		// 24 well slice - 3 columns going down of 96-well plate
		def slice24(y: Int) = (y, row1, 3, 8)
		// 48 well slice - 6 columns going down of 96-well plate
		def slice48(y: Int) = (y, row1, 6, 8)
		// Make list of wanted wells
		def makeRange(limits: (Int, Char, Int, Int)) = {
			val xRange = limits._2 until (limits._2 + limits._4).toChar
			val yRange = limits._1 until (limits._1 + limits._3)
			List.tabulate[String](xRange.size * yRange.size)(i => {
				wellStr(xRange(i/yRange.size), yRange(i%yRange.size))
			})
		}
		// Make map
		Map(S1 -> makeRange(slice24(1)),
			S2 -> makeRange(slice24(4)),
			S3 -> makeRange(slice24(7)),
			S4 -> makeRange(slice24(10)),
			S5 -> makeRange(slice48(1)),
			S6 -> makeRange(slice48(7))
		)
	}

	/**
	 * Make a list of wells included in a wanted slice of a 96-well plate.
	 * @param slice slice wanted
	 * @param cherries list of indicies (A01 is 0, A02 is 1, ...) to individual wells picked if cherry picking
	 * @return list containing only wells from wanted slice
	 */
	private def slice96(slice: Slice, cherries: Option[List[Int]]) = {
		slice match {
			case CP => cherries match {
				case Some(picked) =>
					picked.map(make96WellStrFromIndex)
				case _ =>
					throw new Exception("Cherry picking slice without cherries")
			}
			case _ => slices(slice)
		}
	}

	/**
	 * Make map of wells pointing to themselves - some applications want a map of input wells to output wells for
	 * any all transfers being done so we need this self map when there are no quadrants involved.
	 * @param wellList list of wells to make a map of wells to point to themselves
	 */
	private def makeSelfWellMap(wellList: List[String]) = wellList.map((w) => w -> w).toMap

	/**
	 * Get a map of wells for a slice of a 96-well plate going into a 96-well plate
	 * @param slice slice of plate wanted
	 * @param cherries list of indicies (A01 is 0, A02 is 1, ...) to individual wells picked if cherry picking
	 * @return map of wells to well including only wells from slice
	 */
	private def slice96to96(slice: Slice, cherries: Option[List[Int]]) = makeSelfWellMap(slice96(slice, cherries))

	/**
	 * Get a map of wells for a slice of a 96-well plate going into a 384-well plate
	 * @param quad quadrant of plate to take slice to
	 * @param slice slice of quadrant wanted
	 * @param cherries list of indicies (A01 is 0, A02 is 1, ...) to individual wells picked if cherry picking
	 * @return map of source 96-plate wells to 384-plate wells
	 */
	private def slice96to384(quad: Quad, slice: Slice, cherries: Option[List[Int]]) = {
		// Get wells of quadrant wanted (slice of 96 well quadrant)
		val sliceWells = slice96(slice, cherries)
		// Get map of wells from slice in quadrant
		qTo384(quad).filter {
			case (w, _) => sliceWells.contains(w)
		}
	}

	/**
	 * Get a map of wells for a slice of a 384-well plate going into a 96-well plate
	 * @param quad quadrant of plate to take slice from
	 * @param slice slice of quadrant wanted
	 * @param cherries list of indicies (A01 is 0, A02 is 1, ...) to individual wells picked if cherry picking
	 * @return map of source 384-plate wells to 96-plate wells
	 */
	private def slice384to96(quad: Quad, slice: Slice, cherries: Option[List[Int]]) = {
		// Get wells of quadrant wanted (slice of 96 well quadrant)
		val sliceWells = slice96(slice, cherries)
		// Get map of wells from slice in quadrant
		qFrom384(quad).filter {
			case (_, w) => sliceWells.contains(w)
		}
	}

	/**
	 * For quadrant slices, creates a map of (quadrant,slice) -> (originalWell -> targetWell).  Should not be used
	 * for cherry picking slices
	 * @param slicer callback to get a mapping of original wells to target wells
	 * @return map, keyed by quadrant and slice, to values that are maps of slice's original wells to target wells
	 */
	private def getQuadSliceMap(slicer: (Quad.Quad, Slice.Slice, Option[List[Int]]) => Map[String, String]) =
		(for {
			s <- Slice.values.toIterable if s != CP
			q <- Quad.values.toIterable
		} yield {
				(q, s) -> slicer(q, s, None)
			}).toMap

	// Make map of maps: (quadrant,slice) -> (originalWells -> destinationWells)
	// Not for cherry picking slices
	private lazy val slice96to384map = getQuadSliceMap(slice96to384)
	private lazy val slice384to96map = getQuadSliceMap(slice384to96)

	/**
	 * Get a map of wells for a slice of a 96-well plate going into a 384-well plate.
	 * @param quad quadrant of plate to put slice into
	 * @param slice slice of quadrant wanted
	 * @param cherries list of indicies (A01 is 0, A02 is 1, ...) to individual wells picked within quadrant
	 * @return map of well to well including only wells selected
	 */
	def slice96to384wells(quad: Quad, slice: Slice, cherries: Option[List[Int]]) =
		if (slice != CP) slice96to384map(quad, slice) else {
			val cherryWells = slice96(slice,cherries)
			val quadWells = qTo384(quad)
			quadWells.filter {
				case (inWell, _) =>
					cherryWells.contains(inWell)
			}
		}

	/**
	 * Get a map of wells for a slice of a 384-well plate going into a 96-well plate.
	 * @param quad quadrant of plate to take slice from
	 * @param slice slice of quadrant wanted
	 * @param cherries list of indicies (A01 is 0, A02 is 1, ...) to individual wells picked within quadrant
	 * @return map of well to well including only wells selected
	 */
	def slice384to96wells(quad: Quad, slice: Slice, cherries: Option[List[Int]]) =
		if (slice != CP) slice384to96map(quad, slice) else {
			val cherryWells = slice96(slice,cherries)
			val quadWells = qFrom384(quad)
			quadWells.filter {
				case (_, outWell) =>
					cherryWells.contains(outWell)
			}
		}

	// Make 384 to 384 map of maps: (fromQ, toQ, slice) -> (originalWells -> destinationWells)
	// Not for cherry picking slices
	private lazy val slice384to384map =
		(for {
			s <- Slice.values.toIterable if s != CP
			qFrom <- Quad.values.toIterable
			qTo <- Quad.values.toIterable
		} yield {
				val from = slice384to96map(qFrom, s)
				val to = slice96to384map(qTo, s)
				// Going from original source well to final destination well - 96 in middle to calculate quadrant/slice
				(qFrom, qTo, s) -> from.map {
					case (k, v) => k -> to(v)
				}
			}).toMap

	/**
	 * Get a map of wells for a slice of a 384-well plate going into a 384-well plate.
	 * @param fromQ quadrant of plate to take slice from
	 * @param toQ quadrant of plate to take slice to
	 * @param slice slice of quadrant wanted
	 * @param cherries list of indicies (A01 is 0, A02 is 1, ...) to individual wells picked within quadrant
	 * @return map of well to well including only wells selected
	 */
	def slice384to384wells(fromQ: Quad, toQ: Quad, slice: Slice, cherries: Option[List[Int]]) =
	// If not cherry picking then get fixed map of quadrant to quadrant transfer
		if (slice != CP) slice384to384map(fromQ, toQ, slice) else {
			// Cherry picking - get wells that are in quadrant being transferred and then only take wells cherry picked
			// First get cherry picked wells labels - picked wells are within quadrant (i.e., 96-well plate coordinates)
			val cherryWells = slice96(slice, cherries)
			// Next get map of input wells to where wells are located in quadrant (e.g., cherry picked locations)
			val quadWells = qFrom384(fromQ)
			// Now get map of unfiltered quad to quad transfer and then use filter to only include cherry picked wells
			val wells = q384to384map(fromQ, toQ)
			wells.filter {
				case (inWell, _) =>
					quadWells.get(inWell) match {
						case Some(w) => cherryWells.contains(w) // Is well within quadrant one we want?
						case _ => false // Should never get here
					}
			}
		}

	/**
	 * Get a map of wells for cherry picked 384-well component
	 * @param cherries list of indicies (A01 is 0, A02 is 1, ...) to individual wells picked if cherry picking
	 * @return map of wells to well including only wells from slice
	 */
	def slice384to384wells(slice: Slice, cherries: Option[List[Int]]) =
		if (slice != CP || cherries.isEmpty) Map.empty[String, String] else makeSelfWellMap(index384toWell(cherries.get))

	/**
	 * Convert list of indicies into 384-well plate into well names (e.g., "A01")
	 * @param cherries indicies to wells selected
	 * @return well names (e.g., "A01")
	 */
	private def index384toWell(cherries: List[Int]) = cherries.map(make384WellStrFromIndex)

	// Make 96 to 96 map of maps: slice -> (originalWells -> destinationWells) - no quadrants are needed for 96-wells
	// Not for cherry picking slices
	private lazy val slice96to96map =
		Slice.values.toIterable.filterNot(_ == CP).map((value) => value -> slice96to96(value, None)).toMap

	/**
	 * Get a map of wells for a slice of a 96-well plate going into a 96-well plate
	 * @param slice slice of plate wanted
	 * @param cherries list of indicies (A01 is 0, A02 is 1, ...) to individual wells picked if cherry picking
	 * @return map of wells to well including only wells from slice
	 */
	def slice96to96wells(slice: Slice, cherries: Option[List[Int]]) =
		if (slice != CP) slice96to96map(slice) else slice96to96(slice, cherries)
}
