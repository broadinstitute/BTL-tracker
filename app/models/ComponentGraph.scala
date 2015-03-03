package models

import models.Transfer.Quad._
import scalax.collection.Graph
import scalax.collection.edge.Implicits._
import ContainerDivisions.Division._

//import scalax.collection.edge.LkDiEdge

/**
 * @author Nathaniel Novod
 *         Date: 3/3/15
 *         Time: 4:15 PM
 */
object ComponentGraph {

	case class TransferEdge(fromQuad: Option[Quad],toQuad: Option[Quad])

	import scalax.collection.edge.LBase.LEdgeImplicits
	object MyImplicit extends LEdgeImplicits[TransferEdge]
	import MyImplicit._

	val r = Rack("R",None,None,List.empty,None,None, DIM8x12)
	val p = Plate("P",None,None,List.empty,None,None,DIM8x12)
	val t = Tube("T",None,None,List.empty,None,None)
	import scala.language.implicitConversions
	implicit def toRackToComp(r: Rack) = r.asInstanceOf[Component]
	implicit def toPlateToComp(p: Plate) = p.asInstanceOf[Component]
	implicit def toTubeToComp(t: Tube) = t.asInstanceOf[Component]
	val eL = Seq((r.asInstanceOf[Component] ~+#> p.asInstanceOf[Component])(TransferEdge(None,Some(Q1))),(p.asInstanceOf[Component] ~+#> t.asInstanceOf[Component])(TransferEdge(Some(Q1),None)))
	val g = Graph(eL: _*)
	println("p: " + n(p).diPredecessors)
	println("r: " + n(r).diPredecessors)

	def n(outer: Component): g.NodeT = g.get(outer)
}