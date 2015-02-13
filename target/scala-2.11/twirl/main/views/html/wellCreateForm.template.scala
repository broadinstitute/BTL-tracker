
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

/******************************************************
* Screen to create a component                        *
* @param wellForm form used to gather values for well *
******************************************************/
object wellCreateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[Well],play.twirl.api.HtmlFormat.Appendable] {

  /******************************************************
* Screen to create a component                        *
* @param wellForm form used to gather values for well *
******************************************************/
  def apply/*5.2*/(wellInput: Form[Well]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*5.25*/("""
"""),format.raw/*7.1*/("""
"""),format.raw/*8.67*/("""
"""),_display_(/*9.2*/mainComponent("Create Well")/*9.30*/{_display_(Seq[Any](format.raw/*9.31*/("""
    """),_display_(/*10.6*/componentIntro("Create Well", wellInput)),format.raw/*10.46*/("""
    """),format.raw/*11.114*/("""
    """),_display_(/*12.6*/form(routes.WellController.createWellFromForm(), 'style -> "display:inline;")/*12.83*/ {_display_(Seq[Any](format.raw/*12.85*/("""
        """),_display_(/*13.10*/component(wellInput, false, Component.ComponentType.Well)),format.raw/*13.67*/("""
        """),_display_(/*14.10*/inputText(wellInput(Location.locationKey), '_label -> "Location")),format.raw/*14.75*/("""
        """),_display_(/*15.10*/inputText(wellInput(Container.contentKey), '_label -> "Content")),format.raw/*15.74*/("""
	    """),_display_(/*16.7*/inputText(wellInput(Well.columnKey), '_label -> "Column")),format.raw/*16.64*/("""
	    """),_display_(/*17.7*/inputText(wellInput(Well.rowKey), '_label -> "Row")),format.raw/*17.58*/("""
	    """),_display_(/*18.7*/createButtons()),format.raw/*18.22*/("""
    """)))}),format.raw/*19.6*/("""
	"""),_display_(/*20.3*/homeOption(Seq.empty)),format.raw/*20.24*/("""
""")))}),format.raw/*21.2*/("""
"""))}
  }

  def render(wellInput:Form[Well]): play.twirl.api.HtmlFormat.Appendable = apply(wellInput)

  def f:((Form[Well]) => play.twirl.api.HtmlFormat.Appendable) = (wellInput) => apply(wellInput)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Jan 30 21:45:23 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/wellCreateForm.scala.html
                  HASH: caad126a2448ab66f3752af130704d095c8eaed1
                  MATRIX: 956->225|1082->248|1109->266|1137->333|1164->335|1200->363|1238->364|1270->370|1331->410|1365->524|1397->530|1483->607|1523->609|1560->619|1638->676|1675->686|1761->751|1798->761|1883->825|1916->832|1994->889|2027->896|2099->947|2132->954|2168->969|2204->975|2233->978|2275->999|2307->1001
                  LINES: 25->5|28->5|29->7|30->8|31->9|31->9|31->9|32->10|32->10|33->11|34->12|34->12|34->12|35->13|35->13|36->14|36->14|37->15|37->15|38->16|38->16|39->17|39->17|40->18|40->18|41->19|42->20|42->20|43->21
                  -- GENERATED --
              */
          