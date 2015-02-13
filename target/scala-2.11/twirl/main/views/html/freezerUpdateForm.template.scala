
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

/***************************************************************************
* Screen to update an existing component                                   *
* @param freezerForm form used to gather values - must already have id set *
***************************************************************************/
object freezerUpdateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[Form[Freezer],String,Option[Component.HiddenFields],play.twirl.api.HtmlFormat.Appendable] {

  /***************************************************************************
* Screen to update an existing component                                   *
* @param freezerForm form used to gather values - must already have id set *
***************************************************************************/
  def apply/*5.2*/(freezerInput: Form[Freezer], id: String, hiddenFields: Option[Component.HiddenFields]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*5.89*/("""
"""),format.raw/*7.1*/("""
"""),format.raw/*8.70*/("""
"""),_display_(/*9.2*/mainComponent("Freezer")/*9.26*/{_display_(Seq[Any](format.raw/*9.27*/("""
    """),_display_(/*10.6*/componentIntro("Freezer", freezerInput)),format.raw/*10.45*/("""
    """),format.raw/*11.114*/("""
    """),_display_(/*12.6*/form(routes.FreezerController.updateFreezerFromForm(id), 'style -> "display:inline;")/*12.91*/ {_display_(Seq[Any](format.raw/*12.93*/("""
        """),_display_(/*13.10*/component(freezerInput, true, Component.ComponentType.Freezer, hiddenFields)),format.raw/*13.86*/("""
	    """),_display_(/*14.7*/inputText(freezerInput(Freezer.addressKey), '_label -> "Address")),format.raw/*14.72*/("""
	    """),_display_(/*15.7*/inputText(freezerInput(Freezer.temperatureKey), '_label -> "Temperature (Celsius)")),format.raw/*15.90*/("""
	    """),_display_(/*16.7*/updateButtons()),format.raw/*16.22*/("""
    """)))}),format.raw/*17.6*/("""
	"""),_display_(/*18.3*/homeOption(Seq.empty)),format.raw/*18.24*/("""
""")))}),format.raw/*19.2*/("""
"""))}
  }

  def render(freezerInput:Form[Freezer],id:String,hiddenFields:Option[Component.HiddenFields]): play.twirl.api.HtmlFormat.Appendable = apply(freezerInput,id,hiddenFields)

  def f:((Form[Freezer],String,Option[Component.HiddenFields]) => play.twirl.api.HtmlFormat.Appendable) = (freezerInput,id,hiddenFields) => apply(freezerInput,id,hiddenFields)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/freezerUpdateForm.scala.html
                  HASH: 3b9315d3d82257e9b739c52600d5af88b604a9e3
                  MATRIX: 1168->309|1358->396|1385->414|1413->484|1440->486|1472->510|1510->511|1542->517|1602->556|1636->670|1668->676|1762->761|1802->763|1839->773|1936->849|1969->856|2055->921|2088->928|2192->1011|2225->1018|2261->1033|2297->1039|2326->1042|2368->1063|2400->1065
                  LINES: 25->5|28->5|29->7|30->8|31->9|31->9|31->9|32->10|32->10|33->11|34->12|34->12|34->12|35->13|35->13|36->14|36->14|37->15|37->15|38->16|38->16|39->17|40->18|40->18|41->19
                  -- GENERATED --
              */
          