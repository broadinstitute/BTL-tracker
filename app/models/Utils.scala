package models

/**
 * Created by nnovod on 5/21/15.
 */
object Utils {
	/**
	 * Regular expression to use to split around white space or commas
	 */
	private val splitRegExp = """[\s,\n]""".r

	/**
	 * Get list of IDs.  IDs can be separated by white space (space, tab, carriage return) or commas.
	 * @param inp one or more IDs
	 * @return array of IDs found
	 */
	def getIDs(inp: String) = splitRegExp.split(inp).filter(_.length != 0)
}
