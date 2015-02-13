
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
* Screen to create a new tube                                  *
* @param tubeForm form to use to gather values for tube        *
***************************************************************/
object tubeCreateForm extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[Tube],play.twirl.api.HtmlFormat.Appendable] {

  /***************************************************************
* Screen to create a new tube                                  *
* @param tubeForm form to use to gather values for tube        *
***************************************************************/
  def apply/*5.2*/(tubeForm: Form[Tube]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._

Seq[Any](format.raw/*5.24*/("""
"""),format.raw/*7.1*/("""
"""),format.raw/*8.67*/("""
"""),_display_(/*9.2*/mainComponent("Create Tube")/*9.30*/{_display_(Seq[Any](format.raw/*9.31*/("""
	"""),_display_(/*10.3*/componentIntro("Create Tube", tubeForm)),format.raw/*10.42*/("""
	"""),format.raw/*11.116*/("""
	"""),_display_(/*12.3*/form(routes.TubeController.createTubeFromForm(), 'style -> "display:inline;")/*12.80*/ {_display_(Seq[Any](format.raw/*12.82*/("""
		"""),_display_(/*13.4*/component(tubeForm, false, Component.ComponentType.Tube)),format.raw/*13.60*/("""
        """),_display_(/*14.10*/inputText(tubeForm(Location.locationKey), '_label -> "Location")),format.raw/*14.74*/("""
        """),_display_(/*15.10*/inputText(tubeForm(Container.contentKey), '_label -> "Content")),format.raw/*15.73*/("""
		"""),_display_(/*16.4*/createButtons()),format.raw/*16.19*/("""
	""")))}),format.raw/*17.3*/("""
	"""),_display_(/*18.3*/homeOption(Seq.empty)),format.raw/*18.24*/("""
""")))}))}
  }

  def render(tubeForm:Form[Tube]): play.twirl.api.HtmlFormat.Appendable = apply(tubeForm)

  def f:((Form[Tube]) => play.twirl.api.HtmlFormat.Appendable) = (tubeForm) => apply(tubeForm)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Fri Jan 30 21:45:23 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/tubeCreateForm.scala.html
                  HASH: fab59d0dd0c31cf466e1ac7a2fd0d6ba77f2c8be
                  MATRIX: 1028->261|1153->283|1180->301|1208->368|1235->370|1271->398|1309->399|1338->402|1398->441|1429->557|1458->560|1544->637|1584->639|1614->643|1691->699|1728->709|1813->773|1850->783|1934->846|1964->850|2000->865|2033->868|2062->871|2104->892
                  LINES: 25->5|28->5|29->7|30->8|31->9|31->9|31->9|32->10|32->10|33->11|34->12|34->12|34->12|35->13|35->13|36->14|36->14|37->15|37->15|38->16|38->16|39->17|40->18|40->18
                  -- GENERATED --
              */
          