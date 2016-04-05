package utils

/**
 * Trait to return either a value or an error string.
 * Created by nnovod on 4/4/16.
 */
trait YesOrNo[+T] {
	def getYes: T
	def getYesOption: Option[T]
	def getNo: String
	def getNoOption: Option[String]
	val isYes: Boolean
	val isNo: Boolean
	def fold[S](yes: T => S, no: (String) => S) =
		if (isYes) yes(getYes) else no(getNo)
}

case class Yes[T](value: T) extends YesOrNo[T] {
	def getYes = value
	def getYesOption = Some(value)
	def getNo = None.get
	def getNoOption = None
	val isYes = true
	val isNo = false
}

case class No(err: String) extends YesOrNo[Nothing] {
	def getYes = None.get
	def getYesOption = None
	def getNo = err
	def getNoOption = Some(err)
	val isYes = false
	val isNo = true
}
