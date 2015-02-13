
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
object tube extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template2[Form[Tube],Boolean,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(tubeForm: Form[Tube], display: Boolean):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Tube

Seq[Any](format.raw/*1.42*/("""
"""),format.raw/*4.1*/("""
"""),_display_(/*5.2*/defining(if (display) List('readonly -> "readonly") else List())/*5.66*/ { ro =>_display_(Seq[Any](format.raw/*5.74*/("""
    """),_display_(/*6.6*/inputText(tubeForm("id"), ro: _*)),format.raw/*6.39*/("""
    """),_display_(/*7.6*/textarea(tubeForm("description"))),format.raw/*7.39*/("""
""")))}),format.raw/*8.2*/("""
"""))}
  }

  def render(tubeForm:Form[Tube],display:Boolean): play.twirl.api.HtmlFormat.Appendable = apply(tubeForm,display)

  def f:((Form[Tube],Boolean) => play.twirl.api.HtmlFormat.Appendable) = (tubeForm,display) => apply(tubeForm,display)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Nov 14 22:57:20 EST 2014
                  SOURCE: /Users/nnovod/play/tracker/app/views/component.scala.html
                  HASH: d3dd184a979eb36169c20ff5ec5d6432ca0524c8
                  MATRIX: 516->1|678->41|705->79|732->81|804->145|849->153|880->159|933->192|964->198|1017->231|1048->233
                  LINES: 19->1|23->1|24->4|25->5|25->5|25->5|26->6|26->6|27->7|27->7|28->8
                  -- GENERATED --
              */
          