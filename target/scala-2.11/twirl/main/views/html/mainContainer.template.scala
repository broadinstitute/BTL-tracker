
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
object mainContainer extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template2[String,Html,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*1.2*/(title: String)(content: Html):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import models.Component
import models.ComponentTag

Seq[Any](format.raw/*1.32*/("""
"""),format.raw/*4.1*/("""
"""),_display_(/*5.2*/main(title)/*5.13*/ {_display_(Seq[Any](format.raw/*5.15*/("""
    """),format.raw/*6.5*/("""<link rel="shortcut icon" type="image/png" href=""""),_display_(/*6.55*/routes/*6.61*/.Assets.at("images/icon.png")),format.raw/*6.90*/("""">
    <script type="text/javascript">
        $(function() """),format.raw/*8.22*/("""{"""),format.raw/*8.23*/("""
            """),format.raw/*9.13*/("""var inpDiv = """"),_display_(/*9.28*/{Component.inputDiv}),format.raw/*9.48*/("""";
            var addDiv = $('#' + inpDiv);
            var i = $('#' + inpDiv + ' div').size();
            var containerTags = """"),_display_(/*12.35*/{Component.formKey + "." + Component.tagsKey}),format.raw/*12.80*/("""";
            $('#' + '"""),_display_(/*13.23*/Component/*13.32*/.addTag),format.raw/*13.39*/("""').click(function () """),format.raw/*13.60*/("""{"""),format.raw/*13.61*/("""
                """),format.raw/*14.17*/("""$('<div><dl id="tags_' + i + '_tag_field">' +
                  '<dt><label for="tags_' + i + '_tag">Tag</label></dt>' +
                  '<dd><input type="text" id="tags_' + i + '_tag" name="' + containerTags + '[' + i + ']."""),_display_(/*16.107*/ComponentTag/*16.119*/.tagKey),format.raw/*16.126*/("""" value=""/></dd>' +
                  '<dd class="info">Required</dd>' +
                  '</dl>' +
                  '<dl id="tags_' + i + '_value_field">' +
                  '<dt><label for="tags_' + i + '_value">Value</label></dt>' +
                  '<dd><textarea id="tags_' + i + '_value" name="' + containerTags + '[' + i + ']."""),_display_(/*21.100*/ComponentTag/*21.112*/.valueKey),format.raw/*21.121*/(""""></textarea></dd>' +
                  '</dl><a href="#" class=""""),_display_(/*22.45*/Component/*22.54*/.remTag),format.raw/*22.61*/("""">Remove Tag</a></div>').appendTo(addDiv);
                i++;
                return false;
             """),format.raw/*25.14*/("""}"""),format.raw/*25.15*/(""");
            $(document).on('click', '.' + '"""),_display_(/*26.45*/Component/*26.54*/.remTag),format.raw/*26.61*/("""', function () """),format.raw/*26.76*/("""{"""),format.raw/*26.77*/("""
                """),format.raw/*27.17*/("""$(this).parent('div').remove();
                return false;
            """),format.raw/*29.13*/("""}"""),format.raw/*29.14*/(""");
        """),format.raw/*30.9*/("""}"""),format.raw/*30.10*/(""")
    </script>
""")))}/*32.2*/(content)),format.raw/*32.11*/("""

"""))}
  }

  def render(title:String,content:Html): play.twirl.api.HtmlFormat.Appendable = apply(title)(content)

  def f:((String) => (Html) => play.twirl.api.HtmlFormat.Appendable) = (title) => (content) => apply(title)(content)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Wed Nov 26 20:47:25 EST 2014
                  SOURCE: /Users/nnovod/play/tracker/app/views/mainComponent.scala.html
                  HASH: dd59e8de278613ec7bb720929a60b7a1c619a346
                  MATRIX: 518->1|686->31|713->85|740->87|759->98|798->100|829->105|905->155|919->161|968->190|1055->250|1083->251|1123->264|1164->279|1204->299|1363->431|1429->476|1481->501|1499->510|1527->517|1576->538|1605->539|1650->556|1905->783|1927->795|1956->802|2323->1141|2345->1153|2376->1162|2469->1228|2487->1237|2515->1244|2650->1351|2679->1352|2753->1399|2771->1408|2799->1415|2842->1430|2871->1431|2916->1448|3018->1522|3047->1523|3085->1534|3114->1535|3149->1552|3179->1561
                  LINES: 19->1|23->1|24->4|25->5|25->5|25->5|26->6|26->6|26->6|26->6|28->8|28->8|29->9|29->9|29->9|32->12|32->12|33->13|33->13|33->13|33->13|33->13|34->14|36->16|36->16|36->16|41->21|41->21|41->21|42->22|42->22|42->22|45->25|45->25|46->26|46->26|46->26|46->26|46->26|47->27|49->29|49->29|50->30|50->30|52->32|52->32
                  -- GENERATED --
              */
          