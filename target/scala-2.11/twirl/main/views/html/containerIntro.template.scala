
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

/***************************************************************
* Intro for containers                                       *
* @param intro introductary label                              *
* @param form form with component;s fields                     *
***************************************************************/
object containerIntro extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template2[String,Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: _root_.scala.Any
},play.twirl.api.HtmlFormat.Appendable] {

  /***************************************************************
* Intro for containers                                       *
* @param intro introductary label                              *
* @param form form with component;s fields                     *
***************************************************************/
  def apply/*6.2*/(intro: String, containerForm: Form[_]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {

Seq[Any](format.raw/*6.41*/("""
"""),format.raw/*7.1*/("""<h1>"""),_display_(/*7.6*/intro),format.raw/*7.11*/("""</h1>
"""),_display_(/*8.2*/containerForm/*8.15*/.globalError.map/*8.31*/ { error =>_display_(Seq[Any](format.raw/*8.42*/(""" """),format.raw/*8.43*/("""<span class="error">"""),_display_(/*8.64*/error/*8.69*/.message),format.raw/*8.77*/("""</span> """)))}),format.raw/*8.86*/("""
"""))}
  }

  def render(intro:String,containerForm:Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: _root_.scala.Any
}): play.twirl.api.HtmlFormat.Appendable = apply(intro,containerForm)

  def f:((String,Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: _root_.scala.Any
}) => play.twirl.api.HtmlFormat.Appendable) = (intro,containerForm) => apply(intro,containerForm)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Wed Nov 26 20:47:25 EST 2014
                  SOURCE: /Users/nnovod/play/tracker/app/views/componentIntro.scala.html
                  HASH: 46a60e797e714a246b7d2fc3f41064b34ad45d3c
                  MATRIX: 1229->324|1356->363|1383->364|1413->369|1438->374|1470->381|1491->394|1515->410|1563->421|1591->422|1638->443|1651->448|1679->456|1718->465
                  LINES: 29->6|32->6|33->7|33->7|33->7|34->8|34->8|34->8|34->8|34->8|34->8|34->8|34->8|34->8
                  -- GENERATED --
              */
          