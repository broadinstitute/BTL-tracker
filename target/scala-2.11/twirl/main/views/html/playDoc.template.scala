
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

/***********************************
* Home page                        *
* @param message html body content *
***********************************/
object playDoc extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[String,play.twirl.api.HtmlFormat.Appendable] {

  /***********************************
* Home page                        *
* @param message html body content *
***********************************/
  def apply/*5.2*/(message: String):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {

Seq[Any](format.raw/*5.19*/("""

"""),format.raw/*7.56*/("""
"""),_display_(/*8.2*/main("Welcome to Tracker")/*8.28*/{_display_(Seq[Any](format.raw/*8.29*/("""
    """),format.raw/*9.5*/("""<link rel="shortcut icon" type="image/png" href=""""),_display_(/*9.55*/routes/*9.61*/.Assets.at("images/favicon.png")),format.raw/*9.93*/("""">
    <script src=""""),_display_(/*10.19*/routes/*10.25*/.Assets.at("javascripts/hello.js")),format.raw/*10.59*/("""" type="text/javascript"></script>
""")))}/*11.2*/{_display_(Seq[Any](format.raw/*11.3*/("""
    """),_display_(/*12.6*/play20/*12.12*/.welcome(message)),format.raw/*12.29*/("""
""")))}),format.raw/*13.2*/("""
"""))}
  }

  def render(message:String): play.twirl.api.HtmlFormat.Appendable = apply(message)

  def f:((String) => play.twirl.api.HtmlFormat.Appendable) = (message) => apply(message)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Jan 08 20:45:23 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/playDoc.scala.html
                  HASH: 1ff42f009b51e7cdf5418e74258061b4a236a9c5
                  MATRIX: 793->149|898->166|927->223|954->225|988->251|1026->252|1057->257|1133->307|1147->313|1199->345|1247->366|1262->372|1317->406|1371->442|1409->443|1441->449|1456->455|1494->472|1526->474
                  LINES: 25->5|28->5|30->7|31->8|31->8|31->8|32->9|32->9|32->9|32->9|33->10|33->10|33->10|34->11|34->11|35->12|35->12|35->12|36->13
                  -- GENERATED --
              */
          