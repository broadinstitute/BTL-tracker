
package views.html

import play.twirl.api._
import play.twirl.api.TemplateMagic._

import play.api.templates.PlayMagic._
import models._
import controllers._
import play.api.i18n._
import play.api.mvc._
import play.api.data._
import views.html._

/**/
object homeOption extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Seq[scala.Tuple2[Call, String]],play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(others: Seq[(Call, String)]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*1.31*/("""
"""),_display_(/*3.2*/others/*3.8*/.map/*3.12*/{(call) =>_display_(Seq[Any](format.raw/*3.22*/("""
	"""),_display_(/*4.3*/form(call._1, 'style -> "display:inline;")/*4.45*/ {_display_(Seq[Any](format.raw/*4.47*/("""
		"""),format.raw/*5.3*/("""<input type="submit" value=""""),_display_(/*5.32*/call/*5.36*/._2),format.raw/*5.39*/("""" style="display:inline;">
	""")))}),format.raw/*6.3*/("""
""")))}),format.raw/*7.2*/("""
"""),format.raw/*8.1*/("""<br><br>
"""),_display_(/*9.2*/form(routes.Application.index(), 'style -> "display:inline;")/*9.63*/ {_display_(Seq[Any](format.raw/*9.65*/("""
	"""),format.raw/*10.2*/("""<input type="submit" value="Home" style="display:inline;">
""")))}),format.raw/*11.2*/("""
"""))}
  }

  def render(others:Seq[scala.Tuple2[Call, String]]): play.twirl.api.HtmlFormat.Appendable = apply(others)

  def f:((Seq[scala.Tuple2[Call, String]]) => play.twirl.api.HtmlFormat.Appendable) = (others) => apply(others)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Jan 29 22:21:42 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/homeOption.scala.html
                  HASH: 3241fabca5814af579a52532affc02dc4401f23a
                  MATRIX: 535->1|667->30|694->49|707->55|719->59|766->69|794->72|844->114|883->116|912->119|967->148|979->152|1002->155|1060->184|1091->186|1118->187|1153->197|1222->258|1261->260|1290->262|1380->322
                  LINES: 19->1|22->1|23->3|23->3|23->3|23->3|24->4|24->4|24->4|25->5|25->5|25->5|25->5|26->6|27->7|28->8|29->9|29->9|29->9|30->10|31->11
                  -- GENERATED --
              */
          