
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
object find extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[Component.ComponentIDClass],play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(findForm: Form[Component.ComponentIDClass]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Component
import controllers.Application

Seq[Any](format.raw/*1.46*/("""
"""),format.raw/*5.1*/("""
"""),_display_(/*6.2*/main("Find item")/*6.19*/ {_display_(Seq[Any](format.raw/*6.21*/("""
    """),format.raw/*7.5*/("""<link rel="shortcut icon" type="image/png" href=""""),_display_(/*7.55*/routes/*7.61*/.Assets.at("images/icon.png")),format.raw/*7.90*/("""">
""")))}/*8.2*/{_display_(Seq[Any](format.raw/*8.3*/("""
	"""),_display_(/*9.3*/componentIntro("Find item", findForm)),format.raw/*9.40*/("""
	"""),_display_(/*10.3*/form(routes.Application.findFromForm(), 'style -> "display:inline;")/*10.71*/ {_display_(Seq[Any](format.raw/*10.73*/("""
		"""),_display_(/*11.4*/inputText(findForm(Component.idKey), '_label -> "ID")),format.raw/*11.57*/("""
		"""),format.raw/*12.3*/("""<input type="submit" value="Find" style="display:inline;">
	""")))}),format.raw/*13.3*/("""
	"""),_display_(/*14.3*/homeOption(Seq.empty)),format.raw/*14.24*/("""
""")))}),format.raw/*15.2*/("""
"""))}
  }

  def render(findForm:Form[Component.ComponentIDClass]): play.twirl.api.HtmlFormat.Appendable = apply(findForm)

  def f:((Form[Component.ComponentIDClass]) => play.twirl.api.HtmlFormat.Appendable) = (findForm) => apply(findForm)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Jan 30 21:45:23 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/find.scala.html
                  HASH: 59478e5bbee9b7df092c294b558f9cfdb9fe90ae
                  MATRIX: 530->1|732->45|759->120|786->122|811->139|850->141|881->146|957->196|971->202|1020->231|1041->235|1078->236|1106->239|1163->276|1192->279|1269->347|1309->349|1339->353|1413->406|1443->409|1534->470|1563->473|1605->494|1637->496
                  LINES: 19->1|24->1|25->5|26->6|26->6|26->6|27->7|27->7|27->7|27->7|28->8|28->8|29->9|29->9|30->10|30->10|30->10|31->11|31->11|32->12|33->13|34->14|34->14|35->15
                  -- GENERATED --
              */
          