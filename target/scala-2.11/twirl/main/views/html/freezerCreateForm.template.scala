
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

/************************************************
* Screen to create a component                  *
* @param freezerForm form used to gather values *
************************************************/
object freezerCreateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[Freezer],play.twirl.api.HtmlFormat.Appendable] {

  /************************************************
* Screen to create a component                  *
* @param freezerForm form used to gather values *
************************************************/
  def apply/*5.2*/(freezerInput: Form[Freezer]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*5.31*/("""
"""),format.raw/*7.1*/("""
"""),format.raw/*8.70*/("""
"""),_display_(/*9.2*/mainComponent("Create Freezer")/*9.33*/{_display_(Seq[Any](format.raw/*9.34*/("""
    """),_display_(/*10.6*/componentIntro("Create Freezer", freezerInput)),format.raw/*10.52*/("""
    """),format.raw/*11.114*/("""
    """),_display_(/*12.6*/form(routes.FreezerController.createFreezerFromForm(), 'style -> "display:inline;")/*12.89*/ {_display_(Seq[Any](format.raw/*12.91*/("""
        """),_display_(/*13.10*/component(freezerInput, false, Component.ComponentType.Freezer)),format.raw/*13.73*/("""
	    """),_display_(/*14.7*/inputText(freezerInput(Freezer.addressKey), '_label -> "Address")),format.raw/*14.72*/("""
	    """),_display_(/*15.7*/inputText(freezerInput(Freezer.temperatureKey), '_label -> "Temperature (Celsius)")),format.raw/*15.90*/("""
	    """),_display_(/*16.7*/createButtons()),format.raw/*16.22*/("""
    """)))}),format.raw/*17.6*/("""
	"""),_display_(/*18.3*/homeOption(Seq.empty)),format.raw/*18.24*/("""
""")))}),format.raw/*19.2*/("""
"""))}
  }

  def render(freezerInput:Form[Freezer]): play.twirl.api.HtmlFormat.Appendable = apply(freezerInput)

  def f:((Form[Freezer]) => play.twirl.api.HtmlFormat.Appendable) = (freezerInput) => apply(freezerInput)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Jan 30 21:45:23 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/freezerCreateForm.scala.html
                  HASH: 6ec1e699bd2bed8c5db4d2ab1993e7bc3bb7f121
                  MATRIX: 914->201|1046->230|1073->248|1101->318|1128->320|1167->351|1205->352|1237->358|1304->404|1338->518|1370->524|1462->607|1502->609|1539->619|1623->682|1656->689|1742->754|1775->761|1879->844|1912->851|1948->866|1984->872|2013->875|2055->896|2087->898
                  LINES: 25->5|28->5|29->7|30->8|31->9|31->9|31->9|32->10|32->10|33->11|34->12|34->12|34->12|35->13|35->13|36->14|36->14|37->15|37->15|38->16|38->16|39->17|40->18|40->18|41->19
                  -- GENERATED --
              */
          