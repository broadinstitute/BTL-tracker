
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

/**********************************************************
* Screen to display the results of a match of two plates. *
* One is a rack that was scanned to see what tubes are    *
* present.  The other is a one or more racks input by BSP *
* for the same Jira ticket.  We check if the tubes found  *
* in the scanned rack are in the same BSP rack/wells.     *
* @param Jira issue                                       *
* @param plate comparison of scanned rack and BSP racks   *
* @param rows # of rows in rack                           *
* @param cols # of columns in rack                        *
**********************************************************/
object rackScanForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template6[String,String,String,org.broadinstitute.LIMStales.sampleRacks.BarcodedContentList[org.broadinstitute.LIMStales.sampleRacks.BSPTube]#MatchByPos[org.broadinstitute.LIMStales.sampleRacks.BSPTube],Int,Int,play.twirl.api.HtmlFormat.Appendable] {

  /**********************************************************
* Screen to display the results of a match of two plates. *
* One is a rack that was scanned to see what tubes are    *
* present.  The other is a one or more racks input by BSP *
* for the same Jira ticket.  We check if the tubes found  *
* in the scanned rack are in the same BSP rack/wells.     *
* @param Jira issue                                       *
* @param plate comparison of scanned rack and BSP racks   *
* @param rows # of rows in rack                           *
* @param cols # of columns in rack                        *
**********************************************************/
  def apply/*12.2*/(title: String)(issue: String, rack: String,
        plate: org.broadinstitute.LIMStales.sampleRacks.
        BarcodedContentList[org.broadinstitute.LIMStales.sampleRacks.BSPTube]#
                MatchByPos[org.broadinstitute.LIMStales.sampleRacks.BSPTube], rows: Int, cols: Int):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import org.broadinstitute.LIMStales.sampleRacks._
import helper._

Seq[Any](format.raw/*15.100*/("""

"""),format.raw/*19.1*/("""
"""),_display_(/*20.2*/main(title)/*20.13*/ {_display_(Seq[Any](format.raw/*20.15*/("""
	"""),format.raw/*21.2*/("""<style>
	table """),format.raw/*22.8*/("""{"""),format.raw/*22.9*/("""
	    """),format.raw/*23.6*/("""border: 2px solid black;
	    border-collapse: collapse;
	"""),format.raw/*25.2*/("""}"""),format.raw/*25.3*/("""
	"""),format.raw/*26.2*/("""th """),format.raw/*26.5*/("""{"""),format.raw/*26.6*/("""
	    """),format.raw/*27.6*/("""border: 2px solid black;
	    text-align: center;
	    padding: 2px;
	"""),format.raw/*30.2*/("""}"""),format.raw/*30.3*/("""
	"""),format.raw/*31.2*/("""td """),format.raw/*31.5*/("""{"""),format.raw/*31.6*/("""
	    """),format.raw/*32.6*/("""border: 1px solid black;
	    padding: 2px;
	    text-align: center;
	"""),format.raw/*35.2*/("""}"""),format.raw/*35.3*/("""
	"""),format.raw/*36.2*/("""td.warning """),format.raw/*36.13*/("""{"""),format.raw/*36.14*/(""" """),format.raw/*36.15*/("""background-color: yellow """),format.raw/*36.40*/("""}"""),format.raw/*36.41*/("""
	"""),format.raw/*37.2*/("""td.error """),format.raw/*37.11*/("""{"""),format.raw/*37.12*/(""" """),format.raw/*37.13*/("""background-color: orange """),format.raw/*37.38*/("""}"""),format.raw/*37.39*/("""
	"""),format.raw/*38.2*/("""td.fatal """),format.raw/*38.11*/("""{"""),format.raw/*38.12*/(""" """),format.raw/*38.13*/("""background-color: red """),format.raw/*38.35*/("""}"""),format.raw/*38.36*/("""
    """),format.raw/*39.5*/("""td.ok """),format.raw/*39.11*/("""{"""),format.raw/*39.12*/(""" """),format.raw/*39.13*/("""background-color: green """),format.raw/*39.37*/("""}"""),format.raw/*39.38*/("""
    """),format.raw/*40.5*/("""tr """),format.raw/*40.8*/("""{"""),format.raw/*40.9*/("""
        """),format.raw/*41.9*/("""height: 50px;
    """),format.raw/*42.5*/("""}"""),format.raw/*42.6*/("""
	"""),format.raw/*43.2*/("""</style>
""")))}/*44.2*/{_display_(Seq[Any](format.raw/*44.3*/("""
    """),format.raw/*45.5*/("""<h2>Rack """),_display_(/*45.15*/rack),format.raw/*45.19*/(""" """),format.raw/*45.20*/("""matched to BSP input for issue <a href ="http://ipa.broadinstitute.org:8090/browse/"""),_display_(/*45.104*/issue),format.raw/*45.109*/("""">"""),_display_(/*45.112*/issue),format.raw/*45.117*/("""</a></h2>
    <table style="width:100">
        <tr>
            <th> </th>
            """),_display_(/*49.14*/for(c <- 1 to cols) yield /*49.33*/ {_display_(Seq[Any](format.raw/*49.35*/("""<th>"""),_display_(/*49.40*/(f"$c%02d")),format.raw/*49.51*/("""</th>""")))}),format.raw/*49.57*/("""
        """),format.raw/*50.9*/("""</tr>
        """),_display_(/*51.10*/for(r <- 'A'.toInt to ('A'+(rows-1))) yield /*51.47*/ {_display_(Seq[Any](format.raw/*51.49*/("""
            """),format.raw/*52.13*/("""<tr>
            <th>"""),_display_(/*53.18*/r/*53.19*/.toChar.toString),format.raw/*53.35*/("""</th>
            """),_display_(/*54.14*/for(c <- 1 to cols; cell=r.toChar.toString+(f"$c%02d")) yield /*54.69*/ {_display_(Seq[Any](format.raw/*54.71*/("""
                """),_display_(/*55.18*/plate/*55.23*/.get(cell)/*55.33*/ match/*55.39*/ {/*56.21*/case Some((MatchFound.Match, Some(tube))) =>/*56.65*/ {_display_(Seq[Any](format.raw/*56.67*/("""<td class="ok">"""),_display_(/*56.83*/tube/*56.87*/.sampleID),format.raw/*56.96*/("""</td>""")))}/*57.21*/case Some((MatchFound.NotWell, Some(tube))) =>/*57.67*/ {_display_(Seq[Any](format.raw/*57.69*/("""<td class="warning">"""),_display_(/*57.90*/tube/*57.94*/.sampleID),format.raw/*57.103*/("""</td>""")))}/*58.21*/case Some((MatchFound.NotRack, Some(tube))) =>/*58.67*/ {_display_(Seq[Any](format.raw/*58.69*/("""<td class="error">"""),_display_(/*58.88*/tube/*58.92*/.sampleID),format.raw/*58.101*/("""</td>""")))}/*59.21*/case Some((MatchFound.NotFound, None)) =>/*59.62*/ {_display_(Seq[Any](format.raw/*59.64*/("""<td class="fatal"> </td>""")))}/*60.21*/case _ =>/*60.30*/ {_display_(Seq[Any](format.raw/*60.32*/("""<td> </td>""")))}}),format.raw/*61.18*/("""
            """)))}),format.raw/*62.14*/("""
            """),format.raw/*63.13*/("""</tr>
        """)))}),format.raw/*64.10*/("""
    """),format.raw/*65.5*/("""</table>
	<br>
	<table>
		<tr style="height:0px;">
			<th>Legend for matching of rack scan and BSP input</th>
		</tr>
		<tr style="height:0px;">
			<td class="ok" style="text-align:left;">Rack and well location match</td>
		</tr>
		<tr style="height:0px;">
			<td class="warning" style="text-align:left;">Match of rack only (well locations differ)</td>
		</tr>
		<tr style="height:0px;">
			<td class="error" style="text-align:left;">Rack and well location both do not match</td>
		</tr>
		<tr style="height:0px;">
			<td class="fatal" style="text-align:left;">Tube not found in BSP samples</td>
		</tr>
	</table>
	<br>
	"""),_display_(/*85.3*/homeOption(Seq[(Call, String)]((routes.RackController.findRackByID(rack), "Return to rack")))),format.raw/*85.96*/("""
""")))}),format.raw/*86.2*/("""
"""))}
  }

  def render(title:String,issue:String,rack:String,plate:org.broadinstitute.LIMStales.sampleRacks.BarcodedContentList[org.broadinstitute.LIMStales.sampleRacks.BSPTube]#MatchByPos[org.broadinstitute.LIMStales.sampleRacks.BSPTube],rows:Int,cols:Int): play.twirl.api.HtmlFormat.Appendable = apply(title)(issue,rack,plate,rows,cols)

  def f:((String) => (String,String,org.broadinstitute.LIMStales.sampleRacks.BarcodedContentList[org.broadinstitute.LIMStales.sampleRacks.BSPTube]#MatchByPos[org.broadinstitute.LIMStales.sampleRacks.BSPTube],Int,Int) => play.twirl.api.HtmlFormat.Appendable) = (title) => (issue,rack,plate,rows,cols) => apply(title)(issue,rack,plate,rows,cols)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Sat Feb 07 21:18:54 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/rackScanForm.scala.html
                  HASH: d642513b8bfbe5330183cc783c924ff4925a77c7
                  MATRIX: 2017->661|2452->941|2481->1011|2509->1013|2529->1024|2569->1026|2598->1028|2640->1043|2668->1044|2701->1050|2786->1108|2814->1109|2843->1111|2873->1114|2901->1115|2934->1121|3031->1191|3059->1192|3088->1194|3118->1197|3146->1198|3179->1204|3276->1274|3304->1275|3333->1277|3372->1288|3401->1289|3430->1290|3483->1315|3512->1316|3541->1318|3578->1327|3607->1328|3636->1329|3689->1354|3718->1355|3747->1357|3784->1366|3813->1367|3842->1368|3892->1390|3921->1391|3953->1396|3987->1402|4016->1403|4045->1404|4097->1428|4126->1429|4158->1434|4188->1437|4216->1438|4252->1447|4297->1465|4325->1466|4354->1468|4382->1478|4420->1479|4452->1484|4489->1494|4514->1498|4543->1499|4655->1583|4682->1588|4713->1591|4740->1596|4856->1685|4891->1704|4931->1706|4963->1711|4995->1722|5032->1728|5068->1737|5110->1752|5163->1789|5203->1791|5244->1804|5293->1826|5303->1827|5340->1843|5386->1862|5457->1917|5497->1919|5542->1937|5556->1942|5575->1952|5590->1958|5601->1981|5654->2025|5694->2027|5737->2043|5750->2047|5780->2056|5805->2083|5860->2129|5900->2131|5948->2152|5961->2156|5992->2165|6017->2192|6072->2238|6112->2240|6158->2259|6171->2263|6202->2272|6227->2299|6277->2340|6317->2342|6361->2388|6379->2397|6419->2399|6462->2428|6507->2442|6548->2455|6594->2470|6626->2475|7274->3097|7388->3190|7420->3192
                  LINES: 39->12|46->15|48->19|49->20|49->20|49->20|50->21|51->22|51->22|52->23|54->25|54->25|55->26|55->26|55->26|56->27|59->30|59->30|60->31|60->31|60->31|61->32|64->35|64->35|65->36|65->36|65->36|65->36|65->36|65->36|66->37|66->37|66->37|66->37|66->37|66->37|67->38|67->38|67->38|67->38|67->38|67->38|68->39|68->39|68->39|68->39|68->39|68->39|69->40|69->40|69->40|70->41|71->42|71->42|72->43|73->44|73->44|74->45|74->45|74->45|74->45|74->45|74->45|74->45|74->45|78->49|78->49|78->49|78->49|78->49|78->49|79->50|80->51|80->51|80->51|81->52|82->53|82->53|82->53|83->54|83->54|83->54|84->55|84->55|84->55|84->55|84->56|84->56|84->56|84->56|84->56|84->56|84->57|84->57|84->57|84->57|84->57|84->57|84->58|84->58|84->58|84->58|84->58|84->58|84->59|84->59|84->59|84->60|84->60|84->60|84->61|85->62|86->63|87->64|88->65|108->85|108->85|109->86
                  -- GENERATED --
              */
          