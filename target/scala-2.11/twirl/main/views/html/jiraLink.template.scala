
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
* Create link to jira issue associated with component          *
* @param componentForm form with item's fields                 *
***************************************************************/
object jiraLink extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template1[Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: Component
},play.twirl.api.HtmlFormat.Appendable] {

  /***************************************************************
* Create link to jira issue associated with component          *
* @param componentForm form with item's fields                 *
***************************************************************/
  def apply/*5.2*/(componentForm: Form[_ <: Component]):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import models.Component

Seq[Any](format.raw/*5.39*/("""
"""),_display_(/*7.2*/if(componentForm(Component.formKey + "." + Component.projectKey).value.isDefined)/*7.83*/{_display_(Seq[Any](format.raw/*7.84*/("""
	"""),format.raw/*8.2*/("""<a href ="http://ipa.broadinstitute.org:8090/browse/"""),_display_(/*8.55*/componentForm(Component.formKey + "." + Component.projectKey)/*8.116*/.value.get),format.raw/*8.126*/("""" target="_blank">Jira Issue</a>
""")))}),format.raw/*9.2*/("""
"""))}
  }

  def render(componentForm:Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: Component
}): play.twirl.api.HtmlFormat.Appendable = apply(componentForm)

  def f:((Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: Component
}) => play.twirl.api.HtmlFormat.Appendable) = (componentForm) => apply(componentForm)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Mon Feb 02 11:03:29 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/jiraLink.scala.html
                  HASH: 122c23415097b16e9b9b19e1a92967807ca2c576
                  MATRIX: 1083->261|1231->298|1258->325|1347->406|1385->407|1413->409|1492->462|1562->523|1593->533|1656->567
                  LINES: 27->5|30->5|31->7|31->7|31->7|32->8|32->8|32->8|32->8|33->9
                  -- GENERATED --
              */
          