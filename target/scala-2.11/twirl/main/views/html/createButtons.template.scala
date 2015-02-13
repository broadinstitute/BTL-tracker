
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
* Buttons for create
**/
object createButtons extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template0[play.twirl.api.HtmlFormat.Appendable] {

  /**
* Buttons for create
**/
  def apply/*4.2*/():play.twirl.api.HtmlFormat.Appendable = {
      _display_ {

Seq[Any](format.raw/*4.4*/("""
"""),format.raw/*5.1*/("""<input type="submit" value="Create" style="display:inline;">
"""))}
  }

  def render(): play.twirl.api.HtmlFormat.Appendable = apply()

  def f:(() => play.twirl.api.HtmlFormat.Appendable) = () => apply()

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Jan 30 20:17:34 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/createButtons.scala.html
                  HASH: bf1f70233ad3a149300dcfabb280effcdef95ffd
                  MATRIX: 554->30|643->32|670->33
                  LINES: 23->4|26->4|27->5
                  -- GENERATED --
              */
          