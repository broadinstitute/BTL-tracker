package controllers

import models.Component.ComponentType
import play.api.Play
import play.api.data.Form
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.Redirect

/**
 * Created 2/13/15.
 */
object Errors {

	/**
	 * Validation error report
	 */
	val validationError = "Data entry error - see below"

	/**
	 * Regular expression to find duplicate key in database exception
	 */
	private val dbExc = """DatabaseException.*duplicate key.*dup key.*\{.*"(.*)".*\}.*""".r

	// One to find no primary node is available
	private val dbNotFound = """.*No primary node.*""".r

	// Start and end of error message indicating attempt to enter component a second time
	private val dataEntryErr =
		"Attempt to enter duplicate data failed because the operation was previously done for component "

	/**
	 * Parse exception to get nicer error message
	 * @param e thrown exception
	 * @return string for exception
	 */
	def exceptionMessage(e: Throwable) = {
		val msg = e.getLocalizedMessage
		msg match {
			case dbExc(key) => dataEntryErr + key
			case dbNotFound() => "Database " +
				Play.current.configuration.getString("mongodb.uri").getOrElse("unknown") + " not available"
			case _ => msg
		}
	}

	/**
	 * Values to be put into "flash".  Flash values are implemented by play by making temporary cookies to keep flash
	 * value across a single request.  They are to be used very judiciously since cookies can overlap between requests
	 * and it's not clear if/when they are returned to the server.  They're really only effective to set a status
	 * message in a request's redirect result - then the redirected request can pick up the status from flash.
	 */
	object FlashingKeys extends Enumeration {
		type FlashingKeysType = Value
		val Status = Value

		/**
		 * Set a flashing value in a request's result
		 * @param r result for http request
		 * @param k key for flashing type to be set
		 * @param s value to be set for flashing key
		 * @return new result with flashing value now set
		 */
		def setFlashingValue(r: Result, k: FlashingKeysType, s: String) = r.flashing(k.toString -> s)

		/**
		 * Get a request's flashing value
		 * @param r request
		 * @param k key for wanted flashing value
		 * @return optional value found
		 */
		def getFlashingValue(r: Request[_], k: FlashingKeysType) = r.flash.get(k.toString)
	}

	/**
	 * Add flash to form - any "status" in the requests "flash" is set as a global error in the form.  Flash is used
	 * to pick up any errors set by a previous request redirected to the current request containing the input form.
	 * @param request http request
	 * @param data form
	 * @return form with optional flash "status" added as a global error
	 */
	def addStatusFlash[C](request: Request[_], data: Form[C]): Form[C] =
		FlashingKeys.getFlashingValue(request,FlashingKeys.Status).map(data.withGlobalError(_)).getOrElse(data)

	/**
	 * Create string with list of component type(s) we wanted.
	 *
	 * @param id Item not found
	 * @param componentType optional type of component searched for
	 * @return string showing component type(s) we were looking for
	 */
	def notFoundComponentMessage(id: String,componentType: List[ComponentType.ComponentType]) = {
		val itemType = if (componentType.isEmpty) "component" else componentType.map(_.toString).mkString(" or ")
		s"Can not find $itemType $id"
	}

	/**
	 * Default not found redirection - we redirect to add page flashing what we didn't find.
	 * @param id Item not found
	 * @param componentType optional type of component searched for
	 * @return redirect to add page
	 */
	def notFoundRedirect(id: String,componentType: List[ComponentType.ComponentType]) = {
		val r = Redirect(routes.Application.add)
		FlashingKeys.setFlashingValue(r,FlashingKeys.Status,
			notFoundComponentMessage(id,componentType) + " - you can add the component now")
	}

	/**
	 * Method to take messages and put them on the form
	 *
	 * @param msgs map of error messages in form of fieldName->errorMessage (no fieldname for global error)
	 * @return form to display with messages
	 */
	def setMessages[I](msgs: Map[Option[String],String], form: Form[I]) = {
		// Add messages to form - getting a new form each time a message is added
		msgs.foldLeft(form) {
			case (f, (Some(k),m)) => f.withError(k,m)
			case (f, (None,m)) => f.withGlobalError(m)
		}
	}

	/**
	 * Method to take errors and put them on the form
	 *
	 * @param msgs map of error messages in form of fieldName->errorMessage
	 * @return form to display and set with errors
	 */
	def setFailureMsgs[I](msgs: Map[Option[String],String], form: Form[I]) = {
		// Make form with data filled in and errors set with fields
		val formWithFieldErrors = setMessages(msgs, form)
		// See if global error already exists
		val isGlobalErrors = msgs.exists(_._1 == None)
		// Add global error in form - if other errors set are not global they will be field specific later in form
		formWithFieldErrors.withGlobalError("Operation unsuccessful - fix errors" +
			(if (!isGlobalErrors) " below" else ""))
	}

	/**
	 * Method to take errors and put them on a form filled with data
	 * @param msgs map of error messages in form of fieldName->errorMessage
	 * @param data data to fill in form
	 * @return form to display, filled with data and set with errors
 	 */
	def fillAndSetFailureMsgs[I](msgs: Map[Option[String],String], form: Form[I], data: I) = {
		setFailureMsgs(msgs, form.fill(data))
	}
}
