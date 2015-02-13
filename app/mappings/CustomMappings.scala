package mappings

import play.api.data.format.Formatter
import play.api.data.Mapping
import play.api.data.Forms
import play.api.data.FormError

/**
 * Custom mappings to get special type fields into and out of play forms
 */
object CustomMappings {

	/**
	 * Constructs a simple mapping for a text field (mapped as `scala.Enumeration`)
	 *
	 * For example:
	 * {{{
	 *   Form("status" -> enum(Status))
	 * }}}
	 *
	 * @param enum the Enumeration#Value
	 */
	def enum[E <: Enumeration](enum: E): Mapping[E#Value] = Forms.of(enumFormatter(enum))

	/**
	 * Default formatter for `scala.Enumeration`
	 */
	def enumFormatter[E <: Enumeration](enum: E): Formatter[E#Value] = new Formatter[E#Value] {
		def bind(key: String, data: Map[String, String]) = {
			play.api.data.format.Formats.stringFormat.bind(key, data).right.flatMap { s =>
				scala.util.control.Exception.allCatch[E#Value]
					.either(enum.withName(s))
					.left.map(e => Seq(FormError(key, "invalid enumerated value", Nil)))
			}
		}
		def unbind(key: String, value: E#Value) = Map(key -> value.toString)
	}
}