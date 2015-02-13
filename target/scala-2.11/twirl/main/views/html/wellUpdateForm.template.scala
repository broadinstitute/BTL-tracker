
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

/*********************************************************************************
* Screen to update an existing component                                         *
* @param wellForm form used to gather values for well - must already have id set *
*********************************************************************************/
object wellUpdateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[Form[Well],String,Option[Component.HiddenFields],play.twirl.api.HtmlFormat.Appendable] {

  /*********************************************************************************
* Screen to update an existing component                                         *
* @param wellForm form used to gather values for well - must already have id set *
*********************************************************************************/
  def apply/*5.2*/(wellInput: Form[Well], id: String, hiddenFields: Option[Component.HiddenFields]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*5.83*/("""
"""),format.raw/*7.1*/("""
"""),format.raw/*8.67*/("""
"""),_display_(/*9.2*/mainComponent("Well")/*9.23*/{_display_(Seq[Any](format.raw/*9.24*/("""
    """),_display_(/*10.6*/componentIntro("Well", wellInput)),format.raw/*10.39*/("""
    """),format.raw/*11.114*/("""
    """),_display_(/*12.6*/form(routes.WellController.updateWellFromForm(id), 'style -> "display:inline;")/*12.85*/ {_display_(Seq[Any](format.raw/*12.87*/("""
        """),_display_(/*13.10*/component(wellInput, true, Component.ComponentType.Well, hiddenFields)),format.raw/*13.80*/("""
        """),_display_(/*14.10*/inputText(wellInput(Location.locationKey), '_label -> "Location")),format.raw/*14.75*/("""
        """),_display_(/*15.10*/inputText(wellInput(Container.contentKey), '_label -> "Content")),format.raw/*15.74*/("""
	    """),_display_(/*16.7*/inputText(wellInput(Well.columnKey), '_label -> "Column")),format.raw/*16.64*/("""
	    """),_display_(/*17.7*/inputText(wellInput(Well.rowKey), '_label -> "Row")),format.raw/*17.58*/("""
	    """),_display_(/*18.7*/updateButtons()),format.raw/*18.22*/("""
    """)))}),format.raw/*19.6*/("""
	"""),_display_(/*20.3*/homeOption(Seq.empty)),format.raw/*20.24*/("""
""")))}),format.raw/*21.2*/("""
"""))}
  }

  def render(wellInput:Form[Well],id:String,hiddenFields:Option[Component.HiddenFields]): play.twirl.api.HtmlFormat.Appendable = apply(wellInput,id,hiddenFields)

  def f:((Form[Well],String,Option[Component.HiddenFields]) => play.twirl.api.HtmlFormat.Appendable) = (wellInput,id,hiddenFields) => apply(wellInput,id,hiddenFields)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/wellUpdateForm.scala.html
                  HASH: a60c895e6eb0d59ea5961f719a7dffce31620b54
                  MATRIX: 1210->333|1394->414|1421->432|1449->499|1476->501|1505->522|1543->523|1575->529|1629->562|1663->676|1695->682|1783->761|1823->763|1860->773|1951->843|1988->853|2074->918|2111->928|2196->992|2229->999|2307->1056|2340->1063|2412->1114|2445->1121|2481->1136|2517->1142|2546->1145|2588->1166|2620->1168
                  LINES: 25->5|28->5|29->7|30->8|31->9|31->9|31->9|32->10|32->10|33->11|34->12|34->12|34->12|35->13|35->13|36->14|36->14|37->15|37->15|38->16|38->16|39->17|39->17|40->18|40->18|41->19|42->20|42->20|43->21
                  -- GENERATED --
              */
          