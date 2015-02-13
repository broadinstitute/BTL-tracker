
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

/**********************************************************
* Screen to create a component                            *
* @param sampleForm form used to gather values for sample *
**********************************************************/
object sampleCreateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[Sample],play.twirl.api.HtmlFormat.Appendable] {

  /**********************************************************
* Screen to create a component                            *
* @param sampleForm form used to gather values for sample *
**********************************************************/
  def apply/*5.2*/(sampleInput: Form[Sample]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*5.29*/("""
"""),format.raw/*7.1*/("""
"""),format.raw/*8.69*/("""
"""),_display_(/*9.2*/mainComponent("Create Sample")/*9.32*/{_display_(Seq[Any](format.raw/*9.33*/("""
    """),_display_(/*10.6*/componentIntro("Create Sample", sampleInput)),format.raw/*10.50*/("""
    """),format.raw/*11.114*/("""
    """),_display_(/*12.6*/form(routes.SampleController.createSampleFromForm(), 'style -> "display:inline;")/*12.87*/ {_display_(Seq[Any](format.raw/*12.89*/("""
        """),_display_(/*13.10*/component(sampleInput, false, Component.ComponentType.Sample)),format.raw/*13.71*/("""
        """),_display_(/*14.10*/inputText(sampleInput(Location.locationKey), '_label -> "Location")),format.raw/*14.77*/("""
	    """),_display_(/*15.7*/inputText(sampleInput(Sample.externalIDKey), '_label -> "External ID")),format.raw/*15.77*/("""
	    """),_display_(/*16.7*/createButtons()),format.raw/*16.22*/("""
    """)))}),format.raw/*17.6*/("""
	"""),_display_(/*18.3*/homeOption(Seq.empty)),format.raw/*18.24*/("""
""")))}),format.raw/*19.2*/("""
"""))}
  }

  def render(sampleInput:Form[Sample]): play.twirl.api.HtmlFormat.Appendable = apply(sampleInput)

  def f:((Form[Sample]) => play.twirl.api.HtmlFormat.Appendable) = (sampleInput) => apply(sampleInput)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Jan 30 21:45:23 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/sampleCreateForm.scala.html
                  HASH: 6de0d3591f82f6833936dc5838d0aaa86d5e0c8b
                  MATRIX: 992->241|1122->268|1149->286|1177->355|1204->357|1242->387|1280->388|1312->394|1377->438|1411->552|1443->558|1533->639|1573->641|1610->651|1692->712|1729->722|1817->789|1850->796|1941->866|1974->873|2010->888|2046->894|2075->897|2117->918|2149->920
                  LINES: 25->5|28->5|29->7|30->8|31->9|31->9|31->9|32->10|32->10|33->11|34->12|34->12|34->12|35->13|35->13|36->14|36->14|37->15|37->15|38->16|38->16|39->17|40->18|40->18|41->19
                  -- GENERATED --
              */
          