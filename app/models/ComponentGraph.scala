package models

import models.Transfer.Quad._
import scalax.collection.Graph
import scalax.collection.edge.Implicits._
import ContainerDivisions.Division._

import scalax.collection.edge.LkDiEdge

//import scalax.collection.edge.LkDiEdge

/**
 * @author Nathaniel Novod
 *         Date: 3/3/15
 *         Time: 4:15 PM
 */
object ComponentGraph {

	import scalax.collection.edge.LBase.LEdgeImplicits
	object MyImplicit extends LEdgeImplicits[TransferEdge]
	import MyImplicit._

	val r = Rack("R",None,None,List.empty,None,None, DIM8x12)
	val p = Plate("P",None,None,List.empty,None,None,DIM8x12)
	val t = Tube("T",None,None,List.empty,None,None)
	import scala.language.implicitConversions
	def asComponent[C <: Component](c: C) = c.asInstanceOf[Component]
	implicit def rackToComponent(r: Rack) = r.asInstanceOf[Component]
	implicit def plateToComponent(p: Plate) = p.asInstanceOf[Component]
	implicit def tubeToComponent(t: Tube) = t.asInstanceOf[Component]
	case class TransferEdge(fromQuad: Option[Quad],toQuad: Option[Quad])

	val eL = Seq((asComponent(r) ~+#> asComponent(p))(TransferEdge(None,Some(Q1))),
		(asComponent(r) ~+#> asComponent(p))(TransferEdge(None,Some(Q2))),
		(asComponent(r) ~+#> asComponent(p))(TransferEdge(None,Some(Q2))),
		(asComponent(p) ~+#> asComponent(t))(TransferEdge(Some(Q1),None)))
	val g = Graph(eL: _*)
	println("p: " + n(p).diPredecessors)
	println("r: " + n(r).diPredecessors)

	def n(outer: Component): g.NodeT = g.get(outer)
}