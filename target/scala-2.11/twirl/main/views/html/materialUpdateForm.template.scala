
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

/*****************************************************************************************
* Screen to update an existing component                                                 *
* @param materialForm form used to gather values for material - must already have id set *
*****************************************************************************************/
object materialUpdateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[Form[Material],String,Option[Component.HiddenFields],play.twirl.api.HtmlFormat.Appendable] {

  /*****************************************************************************************
* Screen to update an existing component                                                 *
* @param materialForm form used to gather values for material - must already have id set *
*****************************************************************************************/
  def apply/*5.2*/(materialInput: Form[Material], id: String, hiddenFields: Option[Component.HiddenFields]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Material

Seq[Any](format.raw/*5.91*/("""
"""),format.raw/*8.1*/("""
"""),format.raw/*9.71*/("""
"""),_display_(/*10.2*/mainComponent("Material")/*10.27*/{_display_(Seq[Any](format.raw/*10.28*/("""
    """),_display_(/*11.6*/componentIntro("Material", materialInput)),format.raw/*11.47*/("""
    """),format.raw/*12.114*/("""
    """),_display_(/*13.6*/form(routes.MaterialController.updateMaterialFromForm(id), 'style -> "display:inline;")/*13.93*/ {_display_(Seq[Any](format.raw/*13.95*/("""
        """),_display_(/*14.10*/component(materialInput, true, Component.ComponentType.Material, hiddenFields)),format.raw/*14.88*/("""
        """),format.raw/*15.42*/("""
        """),_display_(/*16.10*/select(field = materialInput(Material.materialTypeKey),
            options = options(Material.MaterialType.materialValues),
            args = '_label -> "Material type")),format.raw/*18.47*/("""
	    """),_display_(/*19.7*/updateButtons()),format.raw/*19.22*/("""
    """)))}),format.raw/*20.6*/("""
	"""),_display_(/*21.3*/homeOption(Seq.empty)),format.raw/*21.24*/("""
""")))}),format.raw/*22.2*/("""
"""))}
  }

  def render(materialInput:Form[Material],id:String,hiddenFields:Option[Component.HiddenFields]): play.twirl.api.HtmlFormat.Appendable = apply(materialInput,id,hiddenFields)

  def f:((Form[Material],String,Option[Component.HiddenFields]) => play.twirl.api.HtmlFormat.Appendable) = (materialInput,id,hiddenFields) => apply(materialInput,id,hiddenFields)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/materialUpdateForm.scala.html
                  HASH: 139c5515ab810328764482dbcd5c2dc27b0646b5
                  MATRIX: 1282->365|1497->454|1524->496|1552->567|1580->569|1614->594|1653->595|1685->601|1747->642|1781->756|1813->762|1909->849|1949->851|1986->861|2085->939|2122->981|2159->991|2351->1162|2384->1169|2420->1184|2456->1190|2485->1193|2527->1214|2559->1216
                  LINES: 25->5|29->5|30->8|31->9|32->10|32->10|32->10|33->11|33->11|34->12|35->13|35->13|35->13|36->14|36->14|37->15|38->16|40->18|41->19|41->19|42->20|43->21|43->21|44->22
                  -- GENERATED --
              */
          