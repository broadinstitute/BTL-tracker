
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

/****************************************************************
* Screen to create a new material                               *
* @param materialForm form to use to gather values for material *
****************************************************************/
object materialCreateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[Material],play.twirl.api.HtmlFormat.Appendable] {

  /****************************************************************
* Screen to create a new material                               *
* @param materialForm form to use to gather values for material *
****************************************************************/
  def apply/*5.2*/(materialForm: Form[Material]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Material

Seq[Any](format.raw/*5.32*/("""
"""),format.raw/*8.1*/("""
"""),format.raw/*9.69*/("""
"""),_display_(/*10.2*/mainComponent("Create Material")/*10.34*/{_display_(Seq[Any](format.raw/*10.35*/("""
    """),_display_(/*11.6*/componentIntro("Create Material", materialForm)),format.raw/*11.53*/("""
    """),format.raw/*12.123*/("""
    """),_display_(/*13.6*/form(routes.MaterialController.createMaterialFromForm(), 'style -> "display:inline;")/*13.91*/ {_display_(Seq[Any](format.raw/*13.93*/("""
        """),_display_(/*14.10*/component(materialForm, false, Component.ComponentType.Material)),format.raw/*14.74*/("""
        """),_display_(/*15.10*/select(field = materialForm(Material.materialTypeKey),
            options = options(Material.MaterialType.materialValues), '_label -> "Material type")),format.raw/*16.97*/("""
        """),_display_(/*17.10*/createButtons()),format.raw/*17.25*/("""
    """)))}),format.raw/*18.6*/("""
	"""),_display_(/*19.3*/homeOption(Seq.empty)),format.raw/*19.24*/("""
""")))}),format.raw/*20.2*/("""
"""))}
  }

  def render(materialForm:Form[Material]): play.twirl.api.HtmlFormat.Appendable = apply(materialForm)

  def f:((Form[Material]) => play.twirl.api.HtmlFormat.Appendable) = (materialForm) => apply(materialForm)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Feb 06 08:15:30 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/materialCreateForm.scala.html
                  HASH: 922483deff291d0b849caca001fe7ff1789de5ba
                  MATRIX: 1044->265|1200->295|1227->337|1255->406|1283->408|1324->440|1363->441|1395->447|1463->494|1497->617|1529->623|1623->708|1663->710|1700->720|1785->784|1822->794|1994->945|2031->955|2067->970|2103->976|2132->979|2174->1000|2206->1002
                  LINES: 25->5|29->5|30->8|31->9|32->10|32->10|32->10|33->11|33->11|34->12|35->13|35->13|35->13|36->14|36->14|37->15|38->16|39->17|39->17|40->18|41->19|41->19|42->20
                  -- GENERATED --
              */
          