package utils

/**
 * Trait to return either a value or an error string.  This is extended to be Yes or No: A successful operation with
 * an associated return value (Yes) or an error (No).  Note we need covariance for T to support No, which is
 * YesOrNo[Nothing].  Nothing is a subtype of all other types so a YesOrNo[DataType] can be represented by a No
 * (a YesOrNo[Nothing]) only if type T is covariant.
 * Created by nnovod on 4/4/16.
 */
sealed trait YesOrNo[+T] {
	/**
	 * Get success (Yes) return value.  Should only be called if known that Yes is set.
	 * @return value stored
	 */
	def getYes: T

	/**
	 * Get success (Yes) return value as option.  Returns None if Yes value not set.
	 * @return optional value stored
	 */
	def getYesOption: Option[T]

	/**
	 * Get error (No) value.  Should only be called if known that operation completed with an error.
	 * @return error string
	 */
	def getNo: String

	/**
	 * Get error (No) value returned as option.  Returns None if No value not set.
	 * @return optional error string
	 */
	def getNoOption: Option[String]

	/**
	 * Did operation complete successfully (Yes value is set)?
	 */
	val isYes: Boolean

	/**
	 * Did operation complete with an error (No value is set)?
	 */
	val isNo: Boolean

	/**
	 * Callback to retrieve either success (Yes) or failure (No) value and make a single result type.
	 * @param yes function to call with Yes value
	 * @param no function to call with No value
	 * @tparam S return type for yes and no functions
	 * @return value set by yes or no function
	 */
	def fold[S](yes: T => S, no: (String) => S) =
		if (isYes) yes(getYes) else no(getNo)
}

/**
 * Representation of a successful operation.
 * @param value value returned by operation
 * @tparam T type of value
 */
case class Yes[T](value: T) extends YesOrNo[T] {
	/**
	 * Return success (Yes) value - for Yes there's always one there
	 * @return value stored
	 */
	def getYes = value

	/**
	 * Return optional success (Yes) value - for Yes it's always Some(value)
	 * @return optional value stored
	 */
	def getYesOption = Some(value)

	/**
	 * Return error (No) value - for Yes there's none there
	 * @return error string
	 */
	def getNo = None.get

	/**
	 * Return optional error (No) value - for Yes it's always None
	 * @return optional error string
	 */
	def getNoOption = None

	/**
	 * Are we Yes?  Yes we are
	 */
	val isYes = true

	/**
	 * Are we No? No we're not.
	 */
	val isNo = false
}

/**
 * Representation of an unsuccessful operation.
 * @param err error message
 */
case class No(err: String) extends YesOrNo[Nothing] {
	/**
	 * Success value - nothing there for No - this should never be called
	 * @return nothing
	 */
	def getYes = None.get

	/**
	 * Get optional success value - None there for No.
	 * @return optional value stored
	 */
	def getYesOption = None

	/**
	 * Get error message.  One always there for No.
	 * @return error string
	 */
	def getNo = err

	/**
	 * Get optional error message.  Always there for No.
	 * @return optional error string
	 */
	def getNoOption = Some(err)

	/**
	 * Are we Yes?  No.
	 */
	val isYes = false

	/**
	 * Are we No? Yes.
	 */
	val isNo = true
}
