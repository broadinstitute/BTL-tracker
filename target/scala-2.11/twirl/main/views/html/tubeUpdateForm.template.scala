
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
* @param tubeForm form used to gather values for tube - must already have id set *
*********************************************************************************/
object tubeUpdateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[Form[Tube],String,Option[Component.HiddenFields],play.twirl.api.HtmlFormat.Appendable] {

  /*********************************************************************************
* Screen to update an existing component                                         *
* @param tubeForm form used to gather values for tube - must already have id set *
*********************************************************************************/
  def apply/*5.2*/(tubeInput: Form[Tube], id: String, hiddenFields: Option[Component.HiddenFields]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*5.83*/("""
"""),format.raw/*7.1*/("""
"""),format.raw/*8.67*/("""
"""),_display_(/*9.2*/mainComponent("Tube")/*9.23*/{_display_(Seq[Any](format.raw/*9.24*/("""
	"""),_display_(/*10.3*/componentIntro("Tube", tubeInput)),format.raw/*10.36*/("""
    """),format.raw/*11.119*/("""
    """),_display_(/*12.6*/form(routes.TubeController.updateTubeFromForm(id), 'style -> "display:inline;")/*12.85*/ {_display_(Seq[Any](format.raw/*12.87*/("""
        """),_display_(/*13.10*/component(tubeInput, true, Component.ComponentType.Tube, hiddenFields)),format.raw/*13.80*/("""
        """),_display_(/*14.10*/inputText(tubeInput(Location.locationKey), '_label -> "Location")),format.raw/*14.75*/("""
        """),_display_(/*15.10*/inputText(tubeInput(Container.contentKey), '_label -> "Content")),format.raw/*15.74*/("""
	    """),_display_(/*16.7*/updateButtons()),format.raw/*16.22*/("""
    """)))}),format.raw/*17.6*/("""
	"""),_display_(/*18.3*/homeOption(Seq.empty)),format.raw/*18.24*/("""
""")))}),format.raw/*19.2*/("""
"""))}
  }

  def render(tubeInput:Form[Tube],id:String,hiddenFields:Option[Component.HiddenFields]): play.twirl.api.HtmlFormat.Appendable = apply(tubeInput,id,hiddenFields)

  def f:((Form[Tube],String,Option[Component.HiddenFields]) => play.twirl.api.HtmlFormat.Appendable) = (tubeInput,id,hiddenFields) => apply(tubeInput,id,hiddenFields)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/tubeUpdateForm.scala.html
                  HASH: 90d0297d5e33b8273eae5bacf3a9bd3985036181
                  MATRIX: 1210->333|1394->414|1421->432|1449->499|1476->501|1505->522|1543->523|1572->526|1626->559|1660->678|1692->684|1780->763|1820->765|1857->775|1948->845|1985->855|2071->920|2108->930|2193->994|2226->1001|2262->1016|2298->1022|2327->1025|2369->1046|2401->1048
                  LINES: 25->5|28->5|29->7|30->8|31->9|31->9|31->9|32->10|32->10|33->11|34->12|34->12|34->12|35->13|35->13|36->14|36->14|37->15|37->15|38->16|38->16|39->17|40->18|40->18|41->19
                  -- GENERATED --
              */
          