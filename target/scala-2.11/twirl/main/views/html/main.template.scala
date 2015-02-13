
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
object main extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[String,Html,Html,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(title: String)(head: Html)(content: Html):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {

Seq[Any](format.raw/*1.44*/("""

"""),format.raw/*3.1*/("""<!DOCTYPE html>

<html>
    <head>
        <title>"""),_display_(/*7.17*/title),format.raw/*7.22*/("""</title>
        <link rel="stylesheet" media="screen" href=""""),_display_(/*8.54*/routes/*8.60*/.Assets.at("stylesheets/main.css")),format.raw/*8.94*/("""">
        <script src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
        """),_display_(/*10.10*/head),format.raw/*10.14*/("""
    """),format.raw/*11.5*/("""</head>
    <body>
        """),_display_(/*13.10*/content),format.raw/*13.17*/("""
    """),format.raw/*14.5*/("""</body>
</html>
"""))}
  }

  def render(title:String,head:Html,content:Html): play.twirl.api.HtmlFormat.Appendable = apply(title)(head)(content)

  def f:((String) => (Html) => (Html) => play.twirl.api.HtmlFormat.Appendable) = (title) => (head) => (content) => apply(title)(head)(content)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Nov 21 21:59:36 EST 2014
                  SOURCE: /Users/nnovod/play/tracker/app/views/main.scala.html
                  HASH: 8de0202da695ab10644eeb164f9ad12c6076158c
                  MATRIX: 514->1|644->43|672->45|749->96|774->101|862->163|876->169|930->203|1066->312|1091->316|1123->321|1178->349|1206->356|1238->361
                  LINES: 19->1|22->1|24->3|28->7|28->7|29->8|29->8|29->8|31->10|31->10|32->11|34->13|34->13|35->14
                  -- GENERATED --
              */
          