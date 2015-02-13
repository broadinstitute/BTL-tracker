
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
***************************************************************/
object container extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template3[Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: Component
},Boolean,Component.ComponentType.ComponentType,play.twirl.api.HtmlFormat.Appendable] {

  /***************************************************************
* Base contents of items                                       *
* @param itemForm form with item's fields                      *
* @param display true if displaying existing item (not create) *
***************************************************************/
  def apply/*6.2*/(containerForm: Form[_ <: Component], display: Boolean, containerType: Component.ComponentType.ComponentType):play.twirl.api.HtmlFormat.Appendable = {
      _display_ {import helper._
import models.Component
import models.ComponentTag

Seq[Any](format.raw/*6.111*/("""
"""),format.raw/*10.1*/("""
"""),format.raw/*11.101*/("""
"""),_display_(/*12.2*/defining(if (display) List('readonly -> "readonly", '_showConstraints -> false) else List())/*12.94*/ { ro =>_display_(Seq[Any](format.raw/*12.102*/("""
    """),_display_(/*13.6*/inputText(containerForm(Component.formKey + "." + Component.idKey), List('_label -> "ID") ++ ro: _*)),format.raw/*13.106*/("""
    """),format.raw/*14.106*/("""
    """),format.raw/*15.5*/("""<input type="hidden" id="container_container" name="""),_display_(/*15.57*/{Component.formKey + "." + Component.typeKey}),format.raw/*15.102*/(""" """),format.raw/*15.103*/("""value=""""),_display_(/*15.111*/containerType),format.raw/*15.124*/("""" />
    """),_display_(/*16.6*/textarea(containerForm(Component.formKey + "." + Component.descriptionKey), '_label -> "Description")),format.raw/*16.107*/("""
    """),format.raw/*18.92*/("""
    """),format.raw/*19.5*/("""<div id="""),_display_(/*19.14*/Component/*19.23*/.inputDiv),format.raw/*19.32*/(""">
        """),_display_(/*20.10*/repeat(containerForm(Component.formKey + "." + Component.tagsKey), min = 0)/*20.85*/ { tag =>_display_(Seq[Any](format.raw/*20.94*/("""
            """),format.raw/*21.13*/("""<div>
                """),_display_(/*22.18*/inputText(tag(ComponentTag.tagKey), '_label -> "Tag")),format.raw/*22.71*/("""
                """),_display_(/*23.18*/textarea(tag(ComponentTag.valueKey), '_label -> "Value")),format.raw/*23.74*/("""
                """),format.raw/*24.17*/("""<a href="#" class=""""),_display_(/*24.37*/Component/*24.46*/.remTag),format.raw/*24.53*/("""">Remove Tag</a>
            </div>
        """)))}),format.raw/*26.10*/("""
    """),format.raw/*27.5*/("""</div>
    <div>
        <p> </p>
        <a href="#" id=""""),_display_(/*30.26*/Component/*30.35*/.addTag),format.raw/*30.42*/("""">Add Tag</a>
        <p> </p>
    </div>
""")))}),format.raw/*33.2*/("""
"""))}
  }

  def render(containerForm:Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: Component
},display:Boolean,containerType:Component.ComponentType.ComponentType): play.twirl.api.HtmlFormat.Appendable = apply(containerForm,display,containerType)

  def f:((Form[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: Component
},Boolean,Component.ComponentType.ComponentType) => play.twirl.api.HtmlFormat.Appendable) = (containerForm,display,containerType) => apply(containerForm,display,containerType)

  def ref: this.type = this

}
              /*
                  -- GENERATED --
                  DATE: Wed Nov 26 20:47:25 EST 2014
                  SOURCE: /Users/nnovod/play/tracker/app/views/component.scala.html
                  HASH: 7858ac5aeb9337065df3224c9993d45c205ef761
                  MATRIX: 1260->326|1524->435|1552->506|1582->607|1610->609|1711->701|1758->709|1790->715|1912->815|1946->921|1978->926|2057->978|2124->1023|2154->1024|2190->1032|2225->1045|2261->1055|2384->1156|2417->1354|2449->1359|2485->1368|2503->1377|2533->1386|2571->1397|2655->1472|2702->1481|2743->1494|2793->1517|2867->1570|2912->1588|2989->1644|3034->1661|3081->1681|3099->1690|3127->1697|3203->1742|3235->1747|3321->1806|3339->1815|3367->1822|3440->1865
                  LINES: 29->6|34->6|35->10|36->11|37->12|37->12|37->12|38->13|38->13|39->14|40->15|40->15|40->15|40->15|40->15|40->15|41->16|41->16|42->18|43->19|43->19|43->19|43->19|44->20|44->20|44->20|45->21|46->22|46->22|47->23|47->23|48->24|48->24|48->24|48->24|50->26|51->27|54->30|54->30|54->30|57->33
                  -- GENERATED --
              */
          