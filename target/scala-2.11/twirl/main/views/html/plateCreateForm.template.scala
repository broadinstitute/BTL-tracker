
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
* Screen to create a new plate                            *
* @param plateForm form to use to gather values for plate *
**********************************************************/
object plateCreateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[Plate],play.twirl.api.HtmlFormat.Appendable] {

  /**********************************************************
* Screen to create a new plate                            *
* @param plateForm form to use to gather values for plate *
**********************************************************/
  def apply/*5.2*/(plateForm: Form[Plate]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Plate

Seq[Any](format.raw/*5.26*/("""
"""),format.raw/*8.1*/("""
"""),format.raw/*9.68*/("""
"""),_display_(/*10.2*/mainComponent("Create Plate")/*10.31*/{_display_(Seq[Any](format.raw/*10.32*/("""
  """),_display_(/*11.4*/componentIntro("Create Plate", plateForm)),format.raw/*11.45*/("""
  """),format.raw/*12.118*/("""
  """),_display_(/*13.4*/form(routes.PlateController.createPlateFromForm(), 'style -> "display:inline;")/*13.83*/ {_display_(Seq[Any](format.raw/*13.85*/("""
      """),_display_(/*14.8*/component(plateForm, false, Component.ComponentType.Plate)),format.raw/*14.66*/("""
"""),format.raw/*15.78*/("""
"""),format.raw/*16.77*/("""
	  """),_display_(/*17.5*/select(field = plateForm(Plate.layoutKey),
		  options = options(ContainerDivisions.divisionTypes), '_label -> "Layout")),format.raw/*18.78*/("""
	  """),_display_(/*19.5*/createButtons()),format.raw/*19.20*/("""
  """)))}),format.raw/*20.4*/("""
	"""),_display_(/*21.3*/homeOption(Seq.empty)),format.raw/*21.24*/("""
""")))}),format.raw/*22.2*/("""
"""))}
  }

  def render(plateForm:Form[Plate]): play.twirl.api.HtmlFormat.Appendable = apply(plateForm)

  def f:((Form[Plate]) => play.twirl.api.HtmlFormat.Appendable) = (plateForm) => apply(plateForm)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/plateCreateForm.scala.html
                  HASH: 269d880e99e6812f593f660fbf911a913f26330f
                  MATRIX: 990->241|1137->265|1164->304|1192->372|1220->374|1258->403|1297->404|1327->408|1389->449|1421->567|1451->571|1539->650|1579->652|1613->660|1692->718|1721->796|1750->873|1781->878|1922->998|1953->1003|1989->1018|2023->1022|2052->1025|2094->1046|2126->1048
                  LINES: 25->5|29->5|30->8|31->9|32->10|32->10|32->10|33->11|33->11|34->12|35->13|35->13|35->13|36->14|36->14|37->15|38->16|39->17|40->18|41->19|41->19|42->20|43->21|43->21|44->22
                  -- GENERATED --
              */
          