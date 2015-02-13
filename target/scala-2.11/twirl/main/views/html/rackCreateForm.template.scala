
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
* Screen to create a new rack                             *
* @param rackForm form to use to gather values for rack   *
**********************************************************/
object rackCreateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[Rack],play.twirl.api.HtmlFormat.Appendable] {

  /**********************************************************
* Screen to create a new rack                             *
* @param rackForm form to use to gather values for rack   *
**********************************************************/
  def apply/*5.2*/(rackForm: Form[Rack]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Rack

Seq[Any](format.raw/*5.24*/("""
"""),format.raw/*8.1*/("""
"""),format.raw/*9.67*/("""
"""),_display_(/*10.2*/mainComponent("Create Rack")/*10.30*/{_display_(Seq[Any](format.raw/*10.31*/("""
    """),_display_(/*11.6*/componentIntro("Create Rack", rackForm)),format.raw/*11.45*/("""
    """),format.raw/*12.119*/("""
    """),_display_(/*13.6*/form(routes.RackController.createRackFromForm(), 'style -> "display:inline;")/*13.83*/ {_display_(Seq[Any](format.raw/*13.85*/("""
        """),_display_(/*14.10*/component(rackForm, false, Component.ComponentType.Rack)),format.raw/*14.66*/("""
"""),format.raw/*15.79*/("""
"""),format.raw/*16.78*/("""
        """),_display_(/*17.10*/select(field = rackForm(Rack.layoutKey),
            options = options(ContainerDivisions.divisionTypes), '_label -> "Layout")),format.raw/*18.86*/("""
        """),_display_(/*19.10*/createButtons()),format.raw/*19.25*/("""
    """)))}),format.raw/*20.6*/("""
	"""),_display_(/*21.3*/homeOption(Seq.empty)),format.raw/*21.24*/("""
""")))}),format.raw/*22.2*/("""
"""))}
  }

  def render(rackForm:Form[Rack]): play.twirl.api.HtmlFormat.Appendable = apply(rackForm)

  def f:((Form[Rack]) => play.twirl.api.HtmlFormat.Appendable) = (rackForm) => apply(rackForm)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Feb 06 08:15:30 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/rackCreateForm.scala.html
                  HASH: 443281de5b2a0ea8aa56cff76641b25795a450be
                  MATRIX: 988->241|1132->263|1159->301|1187->368|1215->370|1252->398|1291->399|1323->405|1383->444|1417->563|1449->569|1535->646|1575->648|1612->658|1689->714|1718->793|1747->871|1784->881|1931->1007|1968->1017|2004->1032|2040->1038|2069->1041|2111->1062|2143->1064
                  LINES: 25->5|29->5|30->8|31->9|32->10|32->10|32->10|33->11|33->11|34->12|35->13|35->13|35->13|36->14|36->14|37->15|38->16|39->17|40->18|41->19|41->19|42->20|43->21|43->21|44->22
                  -- GENERATED --
              */
          