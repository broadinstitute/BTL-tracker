
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

/**********************************************************************************
* Screen to update an existing component                                          *
* @param tubeForm form used to gather values for plate - must already have id set *
**********************************************************************************/
object plateUpdateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[Form[Plate],String,Option[Component.HiddenFields],play.twirl.api.HtmlFormat.Appendable] {

  /**********************************************************************************
* Screen to update an existing component                                          *
* @param tubeForm form used to gather values for plate - must already have id set *
**********************************************************************************/
  def apply/*5.2*/(plateInput: Form[Plate], id: String, hiddenFields: Option[Component.HiddenFields]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Plate

Seq[Any](format.raw/*5.85*/("""
"""),format.raw/*8.1*/("""
"""),format.raw/*9.68*/("""
"""),_display_(/*10.2*/mainComponent("Plate")/*10.24*/{_display_(Seq[Any](format.raw/*10.25*/("""
    """),_display_(/*11.6*/componentIntro("Plate", plateInput)),format.raw/*11.41*/("""
    """),format.raw/*12.120*/("""
    """),_display_(/*13.6*/form(routes.PlateController.updatePlateFromForm(id), 'style -> "display:inline;")/*13.87*/ {_display_(Seq[Any](format.raw/*13.89*/("""
        """),_display_(/*14.10*/component(plateInput, true, Component.ComponentType.Plate, hiddenFields)),format.raw/*14.82*/("""
"""),format.raw/*15.81*/("""
"""),format.raw/*16.80*/("""
	    """),_display_(/*17.7*/select(field = plateInput(Plate.layoutKey),
		    options = options(ContainerDivisions.divisionTypes), '_label -> "Layout")),format.raw/*18.80*/("""
	    """),_display_(/*19.7*/updateButtons()),format.raw/*19.22*/("""
    """)))}),format.raw/*20.6*/("""
	"""),_display_(/*21.3*/homeOption(Seq.empty)),format.raw/*21.24*/("""
""")))}),format.raw/*22.2*/("""
"""))}
  }

  def render(plateInput:Form[Plate],id:String,hiddenFields:Option[Component.HiddenFields]): play.twirl.api.HtmlFormat.Appendable = apply(plateInput,id,hiddenFields)

  def f:((Form[Plate],String,Option[Component.HiddenFields]) => play.twirl.api.HtmlFormat.Appendable) = (plateInput,id,hiddenFields) => apply(plateInput,id,hiddenFields)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/plateUpdateForm.scala.html
                  HASH: 79bda6058237590356579cf7d001a96d513cac96
                  MATRIX: 1220->337|1426->420|1453->459|1481->527|1509->529|1540->551|1579->552|1611->558|1667->593|1701->713|1733->719|1823->800|1863->802|1900->812|1993->884|2022->965|2051->1045|2084->1052|2228->1175|2261->1182|2297->1197|2333->1203|2362->1206|2404->1227|2436->1229
                  LINES: 25->5|29->5|30->8|31->9|32->10|32->10|32->10|33->11|33->11|34->12|35->13|35->13|35->13|36->14|36->14|37->15|38->16|39->17|40->18|41->19|41->19|42->20|43->21|43->21|44->22
                  -- GENERATED --
              */
          