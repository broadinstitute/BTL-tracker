
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

/**
* Buttons for update
**/
object updateButtons extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template0[play.twirl.api.HtmlFormat.Appendable] {

  /**
* Buttons for update
**/
  def apply/*4.2*/():play.twirl.api.HtmlFormat.Appendable = {
      _display_ {

Seq[Any](format.raw/*4.4*/("""
"""),format.raw/*5.1*/("""<input type="submit" value="Update" style="display:inline;">
"""))}
  }

  def render(): play.twirl.api.HtmlFormat.Appendable = apply()

  def f:(() => play.twirl.api.HtmlFormat.Appendable) = () => apply()

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Jan 30 19:48:47 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/updateButtons.scala.html
                  HASH: 7d586f173244f654ec61eadbfd0d8462fe158a79
                  MATRIX: 554->30|643->32|670->33
                  LINES: 23->4|26->4|27->5
                  -- GENERATED --
              */
          