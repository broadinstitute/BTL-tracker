package formats

import play.api.libs.json._
import play.api.libs.json.JsString

/**
 * Special type formatters to get fields into (deserialize via a reader) and out (serialize via a writer) of play JSON.
 */
object CustomFormats {
	/**
	 * Deserializer for Enumeration types.
	 *
	 * {{{
	 * (Json \ "status").as(enum(Status))
	 * }}}
	 * @param enum enumeration
	 * @tparam E type of enumeration
	 * @return enumerated value
	 */
	def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
		def reads(json: JsValue) = json match {
			case JsString(s) =>
				try {
					JsSuccess(enum.withName(s))
				} catch {
					case e: java.util.NoSuchElementException => JsError("Enumeration expected")
				}
			case _ => JsError("String expected")
		}
	}

	/**
	 * Serializer - simply make string of value
	 * @tparam E type of enumeration
	 * @return json string
	 */
	def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
		def writes(v: E#Value): JsValue = JsString(v.toString)
	}

	/**
	 * Make a formatter for (de)serialization.  Simply point to reader and writer.
	 *
	 * @param enum enumeration
	 * @tparam E enumeration type
	 * @return formatter for doing (de)serialization to/from forms
	 */
	def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
		Format(enumReads(enum), enumWrites)
	}

}
