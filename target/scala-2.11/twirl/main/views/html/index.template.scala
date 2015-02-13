
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

/***********************************
* Home page                        *
***********************************/
object index extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template0[play.twirl.api.HtmlFormat.Appendable] {

  /***********************************
* Home page                        *
***********************************/
  def apply/*4.2*/():play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*4.4*/("""
"""),format.raw/*6.1*/("""
"""),format.raw/*7.56*/("""
"""),_display_(/*8.2*/main("Welcome to Tracker")/*8.28*/{_display_(Seq[Any](format.raw/*8.29*/("""
    """),format.raw/*9.5*/("""<link rel="shortcut icon" type="image/png" href=""""),_display_(/*9.55*/routes/*9.61*/.Assets.at("images/icon.png")),format.raw/*9.90*/("""">
""")))}/*10.2*/{_display_(Seq[Any](format.raw/*10.3*/("""
	"""),format.raw/*11.2*/("""<h1>Welcome to Tracker</h1>
	"""),_display_(/*12.3*/form(controllers.routes.Application.find(), 'style -> "display:inline;")/*12.75*/ {_display_(Seq[Any](format.raw/*12.77*/("""
		"""),format.raw/*13.3*/("""<input type="submit" value="Find/Update Component" style="display:inline;">
	""")))}),format.raw/*14.3*/("""
	"""),_display_(/*15.3*/form(controllers.routes.Application.add(), 'style -> "display:inline;")/*15.74*/ {_display_(Seq[Any](format.raw/*15.76*/("""
		"""),format.raw/*16.3*/("""<input type="submit" value="Add Component" style="display:inline;">
	""")))}),format.raw/*17.3*/("""
""")))}),format.raw/*18.2*/("""
"""))}
  }

  def render(): play.twirl.api.HtmlFormat.Appendable = apply()

  def f:(() => play.twirl.api.HtmlFormat.Appendable) = () => apply()

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/index.scala.html
                  HASH: 3cf86f9b732ee2e9b92cf2263040337fd37f2367
                  MATRIX: 710->112|814->114|841->132|869->188|896->190|930->216|968->217|999->222|1075->272|1089->278|1138->307|1160->311|1198->312|1227->314|1283->344|1364->416|1404->418|1434->421|1542->499|1571->502|1651->573|1691->575|1721->578|1821->648|1853->650
                  LINES: 23->4|26->4|27->6|28->7|29->8|29->8|29->8|30->9|30->9|30->9|30->9|31->10|31->10|32->11|33->12|33->12|33->12|34->13|35->14|36->15|36->15|36->15|37->16|38->17|39->18
                  -- GENERATED --
              */
          