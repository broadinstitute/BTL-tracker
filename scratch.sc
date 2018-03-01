import scala.collection.immutable.ListMap

val errors = ListMap(1 -> List("a", "b", "c"), 3 -> List("x", "y", "z"))

val str = errors.mkString("<br>")
println(str)