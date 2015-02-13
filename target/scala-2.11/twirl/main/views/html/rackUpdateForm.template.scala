
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
* @param tubeForm form used to gather values for rack - must already have id set  *
**********************************************************************************/
object rackUpdateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[Form[Rack],String,Option[Component.HiddenFields],play.twirl.api.HtmlFormat.Appendable] {

  /**********************************************************************************
* Screen to update an existing component                                          *
* @param tubeForm form used to gather values for rack - must already have id set  *
**********************************************************************************/
  def apply/*5.2*/(rackInput: Form[Rack], id: String, hiddenFields: Option[Component.HiddenFields]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Rack
import models.Component

Seq[Any](format.raw/*5.83*/("""
"""),format.raw/*9.1*/("""
"""),format.raw/*10.67*/("""
"""),_display_(/*11.2*/mainComponent("Rack")/*11.23*/{_display_(Seq[Any](format.raw/*11.24*/("""
    """),_display_(/*12.6*/componentIntro("Rack", rackInput)),format.raw/*12.39*/("""
	"""),format.raw/*13.116*/("""
    """),_display_(/*14.6*/form(routes.RackController.updateRackFromForm(id), 'enctype -> "multipart/form-data", 'style -> "display:inline;")/*14.120*/ {_display_(Seq[Any](format.raw/*14.122*/("""
        """),_display_(/*15.10*/component(rackInput, true, Component.ComponentType.Rack, hiddenFields)),format.raw/*15.80*/("""
"""),format.raw/*16.80*/("""
"""),format.raw/*17.79*/("""
        """),_display_(/*18.10*/select(field = rackInput(Rack.layoutKey),
            options = options(ContainerDivisions.divisionTypes), '_label -> "Layout")),format.raw/*19.86*/("""
	    """),_display_(/*20.7*/inputFile(rackInput(Rack.rackScanKey), '_label -> "Scan File")),format.raw/*20.69*/("""
        """),_display_(/*21.10*/updateButtons()),format.raw/*21.25*/("""
    """)))}),format.raw/*22.6*/("""
	"""),_display_(/*23.3*/homeOption(Seq[(Call, String)]((routes.RackController.doBSPReport(
		rackInput(Component.formKey + "." + Component.idKey).value.getOrElse("0")), "BSP Report")))),format.raw/*24.94*/("""
""")))}),format.raw/*25.2*/("""
"""))}
  }

  def render(rackInput:Form[Rack],id:String,hiddenFields:Option[Component.HiddenFields]): play.twirl.api.HtmlFormat.Appendable = apply(rackInput,id,hiddenFields)

  def f:((Form[Rack],String,Option[Component.HiddenFields]) => play.twirl.api.HtmlFormat.Appendable) = (rackInput,id,hiddenFields) => apply(rackInput,id,hiddenFields)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/rackUpdateForm.scala.html
                  HASH: bfe81885a646d7b5c90a648f130c5761f60589d3
                  MATRIX: 1218->337|1445->418|1472->481|1501->548|1529->550|1559->571|1598->572|1630->578|1684->611|1715->727|1747->733|1871->847|1912->849|1949->859|2040->929|2069->1009|2098->1088|2135->1098|2283->1225|2316->1232|2399->1294|2436->1304|2472->1319|2508->1325|2537->1328|2718->1488|2750->1490
                  LINES: 25->5|30->5|31->9|32->10|33->11|33->11|33->11|34->12|34->12|35->13|36->14|36->14|36->14|37->15|37->15|38->16|39->17|40->18|41->19|42->20|42->20|43->21|43->21|44->22|45->23|46->24|47->25
                  -- GENERATED --
              */
          