
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

/*************************************************************************************
* Screen to update an existing component                                             *
* @param sampleForm form used to gather values for sample - must already have id set *
*************************************************************************************/
object sampleUpdateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[Form[Sample],String,Option[Component.HiddenFields],play.twirl.api.HtmlFormat.Appendable] {

  /*************************************************************************************
* Screen to update an existing component                                             *
* @param sampleForm form used to gather values for sample - must already have id set *
*************************************************************************************/
  def apply/*5.2*/(sampleInput: Form[Sample], id: String, hiddenFields: Option[Component.HiddenFields]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*5.87*/("""
"""),format.raw/*7.1*/("""
"""),format.raw/*8.69*/("""
"""),_display_(/*9.2*/mainComponent("Sample")/*9.25*/{_display_(Seq[Any](format.raw/*9.26*/("""
    """),_display_(/*10.6*/componentIntro("Sample", sampleInput)),format.raw/*10.43*/("""
    """),format.raw/*11.114*/("""
    """),_display_(/*12.6*/form(routes.SampleController.updateSampleFromForm(id), 'style -> "display:inline;")/*12.89*/ {_display_(Seq[Any](format.raw/*12.91*/("""
        """),_display_(/*13.10*/component(sampleInput, true, Component.ComponentType.Sample, hiddenFields)),format.raw/*13.84*/("""
        """),_display_(/*14.10*/inputText(sampleInput(Location.locationKey), '_label -> "Location")),format.raw/*14.77*/("""
	    """),_display_(/*15.7*/inputText(sampleInput(Sample.externalIDKey), '_label -> "External ID")),format.raw/*15.77*/("""
	    """),_display_(/*16.7*/updateButtons()),format.raw/*16.22*/("""
    """)))}),format.raw/*17.6*/("""
	"""),_display_(/*18.3*/homeOption(Seq.empty)),format.raw/*18.24*/("""
""")))}),format.raw/*19.2*/("""
"""))}
  }

  def render(sampleInput:Form[Sample],id:String,hiddenFields:Option[Component.HiddenFields]): play.twirl.api.HtmlFormat.Appendable = apply(sampleInput,id,hiddenFields)

  def f:((Form[Sample],String,Option[Component.HiddenFields]) => play.twirl.api.HtmlFormat.Appendable) = (sampleInput,id,hiddenFields) => apply(sampleInput,id,hiddenFields)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/sampleUpdateForm.scala.html
                  HASH: 3d101364a14430c148352b478470016237a62a7a
                  MATRIX: 1246->349|1434->434|1461->452|1489->521|1516->523|1547->546|1585->547|1617->553|1675->590|1709->704|1741->710|1833->793|1873->795|1910->805|2005->879|2042->889|2130->956|2163->963|2254->1033|2287->1040|2323->1055|2359->1061|2388->1064|2430->1085|2462->1087
                  LINES: 25->5|28->5|29->7|30->8|31->9|31->9|31->9|32->10|32->10|33->11|34->12|34->12|34->12|35->13|35->13|36->14|36->14|37->15|37->15|38->16|38->16|39->17|40->18|40->18|41->19
                  -- GENERATED --
              */
          