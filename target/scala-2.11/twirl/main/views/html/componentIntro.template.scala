
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
* Intro for components                                         *
* @param intro introductory label                              *
* @param form form with component's fields                     *
***************************************************************/
object componentIntro extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template2[String,Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: _root_.scala.Any
},play.twirl.api.HtmlFormat.Appendable] {

  /***************************************************************
* Intro for components                                         *
* @param intro introductory label                              *
* @param form form with component's fields                     *
***************************************************************/
  def apply/*6.2*/(intro: String, componentForm: Form[_]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {

Seq[Any](format.raw/*6.41*/("""
"""),format.raw/*7.1*/("""<h1>"""),_display_(/*7.6*/intro),format.raw/*7.11*/("""</h1>
"""),_display_(/*8.2*/componentForm/*8.15*/.globalErrors.map/*8.32*/ {error =>_display_(Seq[Any](format.raw/*8.42*/(""" """),format.raw/*8.43*/("""<span class="error">"""),_display_(/*8.64*/error/*8.69*/.message),format.raw/*8.77*/("""</span><br>""")))}),format.raw/*8.89*/("""
"""))}
  }

  def render(intro:String,componentForm:Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: _root_.scala.Any
}): play.twirl.api.HtmlFormat.Appendable = apply(intro,componentForm)

  def f:((String,Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: _root_.scala.Any
}) => play.twirl.api.HtmlFormat.Appendable) = (intro,componentForm) => apply(intro,componentForm)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Jan 29 20:33:14 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/componentIntro.scala.html
                  HASH: a11cdadfc57c747bafad079cc417df29f5b86888
                  MATRIX: 1233->326|1360->365|1387->366|1417->371|1442->376|1474->383|1495->396|1520->413|1567->423|1595->424|1642->445|1655->450|1683->458|1725->470
                  LINES: 29->6|32->6|33->7|33->7|33->7|34->8|34->8|34->8|34->8|34->8|34->8|34->8|34->8|34->8
                  -- GENERATED --
              */
          