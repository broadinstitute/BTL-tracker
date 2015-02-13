
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

/**/
object add extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[Component.ComponentTypeClass],play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(addForm: Form[Component.ComponentTypeClass]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Component
import controllers.Application

Seq[Any](format.raw/*1.47*/("""
"""),format.raw/*5.1*/("""
"""),_display_(/*6.2*/main("Add Component")/*6.23*/ {_display_(Seq[Any](format.raw/*6.25*/("""
    """),format.raw/*7.5*/("""<link rel="shortcut icon" type="image/png" href=""""),_display_(/*7.55*/routes/*7.61*/.Assets.at("images/icon.png")),format.raw/*7.90*/("""">
""")))}/*8.2*/{_display_(Seq[Any](format.raw/*8.3*/("""
	"""),_display_(/*9.3*/componentIntro("Add Component", addForm)),format.raw/*9.43*/("""
	"""),_display_(/*10.3*/form(routes.Application.addFromForm, 'style -> "display:inline;")/*10.68*/ {_display_(Seq[Any](format.raw/*10.70*/("""
		"""),_display_(/*11.4*/select(field = addForm(Component.typeKey),
			options = options(Component.componentTypes), '_label -> "Component type")),format.raw/*12.77*/("""
		"""),format.raw/*13.3*/("""<input type="submit" value="Add" style="display:inline;">
	""")))}),format.raw/*14.3*/("""
	"""),_display_(/*15.3*/homeOption(Seq.empty)),format.raw/*15.24*/("""
""")))}),format.raw/*16.2*/("""
"""))}
  }

  def render(addForm:Form[Component.ComponentTypeClass]): play.twirl.api.HtmlFormat.Appendable = apply(addForm)

  def f:((Form[Component.ComponentTypeClass]) => play.twirl.api.HtmlFormat.Appendable) = (addForm) => apply(addForm)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Jan 30 21:45:22 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/add.scala.html
                  HASH: f7454539f78b53c69f5225de6ced7a304a04f1f9
                  MATRIX: 531->1|734->46|761->121|788->123|817->144|856->146|887->151|963->201|977->207|1026->236|1047->240|1084->241|1112->244|1172->284|1201->287|1275->352|1315->354|1345->358|1485->477|1515->480|1605->540|1634->543|1676->564|1708->566
                  LINES: 19->1|24->1|25->5|26->6|26->6|26->6|27->7|27->7|27->7|27->7|28->8|28->8|29->9|29->9|30->10|30->10|30->10|31->11|32->12|33->13|34->14|35->15|35->15|36->16
                  -- GENERATED --
              */
          