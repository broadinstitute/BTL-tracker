
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
* Base contents of items                                       *
* @param itemForm form with item's fields                      *
* @param display true if displaying existing item (not create) *
* @param componentType type of component (e.g., plate)         *
* @param hiddenFields optional hidden fields to set in form    *
***************************************************************/
object component extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template4[Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: Component
},Boolean,Component.ComponentType.ComponentType,Option[Component.HiddenFields],play.twirl.api.HtmlFormat.Appendable] {

  /***************************************************************
* Base contents of items                                       *
* @param itemForm form with item's fields                      *
* @param display true if displaying existing item (not create) *
* @param componentType type of component (e.g., plate)         *
* @param hiddenFields optional hidden fields to set in form    *
***************************************************************/
  def apply/*8.2*/(componentForm: Form[_ <: Component], display: Boolean, componentType: Component.ComponentType.ComponentType,
		hiddenFields: Option[Component.HiddenFields] = None):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Component
import models.ComponentTag

Seq[Any](format.raw/*9.55*/("""
"""),format.raw/*13.1*/("""
"""),format.raw/*14.101*/("""
"""),_display_(/*15.2*/defining(if (display) List('readonly -> "readonly", '_showConstraints -> false) else List())/*15.94*/ { ro =>_display_(Seq[Any](format.raw/*15.102*/("""
    """),_display_(/*16.6*/inputText(componentForm(Component.formKey + "." + Component.idKey), List('_label -> "ID") ++ ro: _*)),format.raw/*16.106*/("""
    """),format.raw/*17.106*/("""
    """),format.raw/*18.5*/("""<input type="hidden" id=""""),_display_(/*18.31*/{Component.formKey + "_" + Component.typeKey}),format.raw/*18.76*/("""" name=""""),_display_(/*18.85*/{Component.formKey + "." + Component.typeKey}),format.raw/*18.130*/("""" value=""""),_display_(/*18.140*/componentType),format.raw/*18.153*/("""" />
    """),_display_(/*19.6*/textarea(componentForm(Component.formKey + "." + Component.descriptionKey), '_label -> "Description")),format.raw/*19.107*/("""
	"""),format.raw/*21.86*/("""
	"""),format.raw/*22.2*/("""<div id="""),_display_(/*22.11*/Component/*22.20*/.inputDiv),format.raw/*22.29*/(""">
		"""),_display_(/*23.4*/repeat(componentForm(Component.formKey + "." + Component.tagsKey), min = 0)/*23.79*/ { tag =>_display_(Seq[Any](format.raw/*23.88*/("""
			"""),format.raw/*24.4*/("""<div>
				"""),_display_(/*25.6*/inputText(tag(ComponentTag.tagKey), '_label -> "Tag")),format.raw/*25.59*/("""
				"""),_display_(/*26.6*/textarea(tag(ComponentTag.valueKey), '_label -> "Value")),format.raw/*26.62*/("""
				"""),format.raw/*27.5*/("""<a href="#" class=""""),_display_(/*27.25*/Component/*27.34*/.remTag),format.raw/*27.41*/("""">Remove Tag</a>
			</div>
		""")))}),format.raw/*29.4*/("""
	"""),format.raw/*30.2*/("""</div>
	<div>
		<p> </p>
		<a href="#" id=""""),_display_(/*33.20*/Component/*33.29*/.addTag),format.raw/*33.36*/("""">Add Tag</a>
		<p> </p>
	</div>
	"""),_display_(/*36.3*/inputText(componentForm(Component.formKey + "." + Component.projectKey), '_label -> "Project ID", 'style -> "display:inline;")),format.raw/*36.129*/("""
	"""),_display_(/*37.3*/jiraLink(componentForm)),format.raw/*37.26*/("""
	"""),_display_(/*38.3*/if(hiddenFields.isDefined)/*38.29*/ {_display_(Seq[Any](format.raw/*38.31*/("""
		"""),_display_(/*39.4*/if(hiddenFields.get.project.isDefined)/*39.42*/ {_display_(Seq[Any](format.raw/*39.44*/("""
			"""),format.raw/*40.4*/("""<input type="hidden" id=""""),_display_(/*40.30*/{Component.formKey + "." + Component.hiddenProjectKey}),format.raw/*40.84*/("""" name=""""),_display_(/*40.93*/{Component.formKey + "." + Component.hiddenProjectKey}),format.raw/*40.147*/("""" value=""""),_display_(/*40.157*/hiddenFields/*40.169*/.get.project.get),format.raw/*40.185*/("""" />
		""")))}),format.raw/*41.4*/("""
	""")))}),format.raw/*42.3*/("""
""")))}),format.raw/*43.2*/("""
"""))}
  }

  def render(componentForm:Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: Component
},display:Boolean,componentType:Component.ComponentType.ComponentType,hiddenFields:Option[Component.HiddenFields]): play.twirl.api.HtmlFormat.Appendable = apply(componentForm,display,componentType,hiddenFields)

  def f:((Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: Component
},Boolean,Component.ComponentType.ComponentType,Option[Component.HiddenFields]) => play.twirl.api.HtmlFormat.Appendable) = (componentForm,display,componentType,hiddenFields) => apply(componentForm,display,componentType,hiddenFields)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Thu Feb 12 20:36:25 EST 2015
                  SOURCE: /Users/nnovod/play/tracker/app/views/component.scala.html
                  HASH: 39d2ffc55ebc577fc533a3e9cea13ea65175f263
                  MATRIX: 1551->456|1869->620|1897->691|1927->792|1955->794|2056->886|2103->894|2135->900|2257->1000|2291->1106|2323->1111|2376->1137|2442->1182|2478->1191|2545->1236|2583->1246|2618->1259|2654->1269|2777->1370|2807->1559|2836->1561|2872->1570|2890->1579|2920->1588|2951->1593|3035->1668|3082->1677|3113->1681|3150->1692|3224->1745|3256->1751|3333->1807|3365->1812|3412->1832|3430->1841|3458->1848|3518->1878|3547->1880|3618->1924|3636->1933|3664->1940|3725->1975|3873->2101|3902->2104|3946->2127|3975->2130|4010->2156|4050->2158|4080->2162|4127->2200|4167->2202|4198->2206|4251->2232|4326->2286|4362->2295|4438->2349|4476->2359|4498->2371|4536->2387|4574->2395|4607->2398|4639->2400
                  LINES: 33->8|39->9|40->13|41->14|42->15|42->15|42->15|43->16|43->16|44->17|45->18|45->18|45->18|45->18|45->18|45->18|45->18|46->19|46->19|47->21|48->22|48->22|48->22|48->22|49->23|49->23|49->23|50->24|51->25|51->25|52->26|52->26|53->27|53->27|53->27|53->27|55->29|56->30|59->33|59->33|59->33|62->36|62->36|63->37|63->37|64->38|64->38|64->38|65->39|65->39|65->39|66->40|66->40|66->40|66->40|66->40|66->40|66->40|66->40|67->41|68->42|69->43
                  -- GENERATED --
              */
          