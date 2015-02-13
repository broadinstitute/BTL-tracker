// @SOURCE:/Users/nnovod/play/tracker/conf/routes
// @HASH:1b68b18887b27461948a2a45b29698c33fa19869
// @DATE:Thu Feb 12 20:36:24 EST 2015


import play.core._
import play.core.Router._
import play.core.Router.HandlerInvokerFactory._
import play.core.j._

import play.api.mvc._
import _root_.controllers.Assets.Asset

import Router.queryString

object Routes extends Router.Routes {

import ReverseRouteContext.empty

private var _prefix = "/"

def setPrefix(prefix: String) {
  _prefix = prefix
  List[(String,Routes)]().foreach {
    case (p, router) => router.setPrefix(prefix + (if(prefix.endsWith("/")) "" else "/") + p)
  }
}

def prefix = _prefix

lazy val defaultPrefix = { if(Routes.prefix.endsWith("/")) "" else "/" }


// @LINE:6
private[this] lazy val controllers_Application_index0_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix))))
private[this] lazy val controllers_Application_index0_invoker = createInvoker(
controllers.Application.index,
HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "index", Nil,"GET", """ Home page""", Routes.prefix + """"""))
        

// @LINE:9
private[this] lazy val controllers_Application_test1_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("test"))))
private[this] lazy val controllers_Application_test1_invoker = createInvoker(
controllers.Application.test,
HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "test", Nil,"GET", """ Test page""", Routes.prefix + """test"""))
        

// @LINE:11
private[this] lazy val controllers_Application_playDoc2_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("playDoc"))))
private[this] lazy val controllers_Application_playDoc2_invoker = createInvoker(
controllers.Application.playDoc,
HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "playDoc", Nil,"GET", """To get play introduction""", Routes.prefix + """playDoc"""))
        

// @LINE:14
private[this] lazy val controllers_Application_add3_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("add"))))
private[this] lazy val controllers_Application_add3_invoker = createInvoker(
controllers.Application.add,
HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "add", Nil,"GET", """ Get settings to add a new item""", Routes.prefix + """add"""))
        

// @LINE:16
private[this] lazy val controllers_Application_addFromForm4_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("add"))))
private[this] lazy val controllers_Application_addFromForm4_invoker = createInvoker(
controllers.Application.addFromForm,
HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "addFromForm", Nil,"POST", """ Post request to add a new item (posted data specified item type)""", Routes.prefix + """add"""))
        

// @LINE:19
private[this] lazy val controllers_Application_find5_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("find"))))
private[this] lazy val controllers_Application_find5_invoker = createInvoker(
controllers.Application.find,
HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "find", Nil,"GET", """ Retrieve item""", Routes.prefix + """find"""))
        

// @LINE:21
private[this] lazy val controllers_Application_findFromForm6_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("find"))))
private[this] lazy val controllers_Application_findFromForm6_invoker = createInvoker(
controllers.Application.findFromForm,
HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "findFromForm", Nil,"POST", """ Find from posted data""", Routes.prefix + """find"""))
        

// @LINE:23
private[this] lazy val controllers_Application_findWithID7_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("find/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_Application_findWithID7_invoker = createInvoker(
controllers.Application.findWithID(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "findWithID", Seq(classOf[String]),"GET", """ Find from get parameter""", Routes.prefix + """find/$id<[^/]+>"""))
        

// @LINE:26
private[this] lazy val controllers_TubeController_addTube8_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("tube/add"))))
private[this] lazy val controllers_TubeController_addTube8_invoker = createInvoker(
controllers.TubeController.addTube,
HandlerDef(this.getClass.getClassLoader, "", "controllers.TubeController", "addTube", Nil,"GET", """ Get new tube settings""", Routes.prefix + """tube/add"""))
        

// @LINE:28
private[this] lazy val controllers_TubeController_createTubeFromForm9_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("tube/add"))))
private[this] lazy val controllers_TubeController_createTubeFromForm9_invoker = createInvoker(
controllers.TubeController.createTubeFromForm,
HandlerDef(this.getClass.getClassLoader, "", "controllers.TubeController", "createTubeFromForm", Nil,"POST", """ Create tube from posted data""", Routes.prefix + """tube/add"""))
        

// @LINE:30
private[this] lazy val controllers_TubeController_findTubeByID10_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("tube/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_TubeController_findTubeByID10_invoker = createInvoker(
controllers.TubeController.findTubeByID(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.TubeController", "findTubeByID", Seq(classOf[String]),"GET", """ Display to edit existing tube settings""", Routes.prefix + """tube/$id<[^/]+>"""))
        

// @LINE:32
private[this] lazy val controllers_TubeController_updateTubeFromForm11_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("tube/update/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_TubeController_updateTubeFromForm11_invoker = createInvoker(
controllers.TubeController.updateTubeFromForm(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.TubeController", "updateTubeFromForm", Seq(classOf[String]),"POST", """ Update tube from posted data""", Routes.prefix + """tube/update/$id<[^/]+>"""))
        

// @LINE:35
private[this] lazy val controllers_PlateController_addPlate12_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("plate/add"))))
private[this] lazy val controllers_PlateController_addPlate12_invoker = createInvoker(
controllers.PlateController.addPlate,
HandlerDef(this.getClass.getClassLoader, "", "controllers.PlateController", "addPlate", Nil,"GET", """ Get new plate settings""", Routes.prefix + """plate/add"""))
        

// @LINE:37
private[this] lazy val controllers_PlateController_createPlateFromForm13_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("plate/add"))))
private[this] lazy val controllers_PlateController_createPlateFromForm13_invoker = createInvoker(
controllers.PlateController.createPlateFromForm,
HandlerDef(this.getClass.getClassLoader, "", "controllers.PlateController", "createPlateFromForm", Nil,"POST", """ Create plate from posted data""", Routes.prefix + """plate/add"""))
        

// @LINE:39
private[this] lazy val controllers_PlateController_findPlateByID14_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("plate/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_PlateController_findPlateByID14_invoker = createInvoker(
controllers.PlateController.findPlateByID(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.PlateController", "findPlateByID", Seq(classOf[String]),"GET", """ Display to edit existing plate settings""", Routes.prefix + """plate/$id<[^/]+>"""))
        

// @LINE:41
private[this] lazy val controllers_PlateController_updatePlateFromForm15_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("plate/update/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_PlateController_updatePlateFromForm15_invoker = createInvoker(
controllers.PlateController.updatePlateFromForm(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.PlateController", "updatePlateFromForm", Seq(classOf[String]),"POST", """ Update plate from posted data""", Routes.prefix + """plate/update/$id<[^/]+>"""))
        

// @LINE:44
private[this] lazy val controllers_RackController_addRack16_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("rack/add"))))
private[this] lazy val controllers_RackController_addRack16_invoker = createInvoker(
controllers.RackController.addRack,
HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "addRack", Nil,"GET", """ Get new rack settings""", Routes.prefix + """rack/add"""))
        

// @LINE:46
private[this] lazy val controllers_RackController_createRackFromForm17_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("rack/add"))))
private[this] lazy val controllers_RackController_createRackFromForm17_invoker = createInvoker(
controllers.RackController.createRackFromForm,
HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "createRackFromForm", Nil,"POST", """ Create rack from posted data""", Routes.prefix + """rack/add"""))
        

// @LINE:48
private[this] lazy val controllers_RackController_findRackByID18_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("rack/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_RackController_findRackByID18_invoker = createInvoker(
controllers.RackController.findRackByID(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "findRackByID", Seq(classOf[String]),"GET", """ Display to edit existing rack settings""", Routes.prefix + """rack/$id<[^/]+>"""))
        

// @LINE:50
private[this] lazy val controllers_RackController_updateRackFromForm19_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("rack/update/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_RackController_updateRackFromForm19_invoker = createInvoker(
controllers.RackController.updateRackFromForm(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "updateRackFromForm", Seq(classOf[String]),"POST", """ Update rack from posted data""", Routes.prefix + """rack/update/$id<[^/]+>"""))
        

// @LINE:53
private[this] lazy val controllers_FreezerController_addFreezer20_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("freezer/add"))))
private[this] lazy val controllers_FreezerController_addFreezer20_invoker = createInvoker(
controllers.FreezerController.addFreezer,
HandlerDef(this.getClass.getClassLoader, "", "controllers.FreezerController", "addFreezer", Nil,"GET", """ Get new freezer settings""", Routes.prefix + """freezer/add"""))
        

// @LINE:55
private[this] lazy val controllers_FreezerController_createFreezerFromForm21_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("freezer/add"))))
private[this] lazy val controllers_FreezerController_createFreezerFromForm21_invoker = createInvoker(
controllers.FreezerController.createFreezerFromForm,
HandlerDef(this.getClass.getClassLoader, "", "controllers.FreezerController", "createFreezerFromForm", Nil,"POST", """ Create freezer from posted data""", Routes.prefix + """freezer/add"""))
        

// @LINE:57
private[this] lazy val controllers_FreezerController_findFreezerByID22_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("freezer/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_FreezerController_findFreezerByID22_invoker = createInvoker(
controllers.FreezerController.findFreezerByID(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.FreezerController", "findFreezerByID", Seq(classOf[String]),"GET", """ Display to edit existing freezer settings""", Routes.prefix + """freezer/$id<[^/]+>"""))
        

// @LINE:59
private[this] lazy val controllers_FreezerController_updateFreezerFromForm23_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("freezer/update/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_FreezerController_updateFreezerFromForm23_invoker = createInvoker(
controllers.FreezerController.updateFreezerFromForm(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.FreezerController", "updateFreezerFromForm", Seq(classOf[String]),"POST", """ Update freezer from posted data""", Routes.prefix + """freezer/update/$id<[^/]+>"""))
        

// @LINE:62
private[this] lazy val controllers_WellController_addWell24_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("well/add"))))
private[this] lazy val controllers_WellController_addWell24_invoker = createInvoker(
controllers.WellController.addWell,
HandlerDef(this.getClass.getClassLoader, "", "controllers.WellController", "addWell", Nil,"GET", """ Get new well settings""", Routes.prefix + """well/add"""))
        

// @LINE:64
private[this] lazy val controllers_WellController_createWellFromForm25_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("well/add"))))
private[this] lazy val controllers_WellController_createWellFromForm25_invoker = createInvoker(
controllers.WellController.createWellFromForm,
HandlerDef(this.getClass.getClassLoader, "", "controllers.WellController", "createWellFromForm", Nil,"POST", """ Create well from posted data""", Routes.prefix + """well/add"""))
        

// @LINE:66
private[this] lazy val controllers_WellController_findWellByID26_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("well/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_WellController_findWellByID26_invoker = createInvoker(
controllers.WellController.findWellByID(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.WellController", "findWellByID", Seq(classOf[String]),"GET", """ Display to edit existing well settings""", Routes.prefix + """well/$id<[^/]+>"""))
        

// @LINE:68
private[this] lazy val controllers_WellController_updateWellFromForm27_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("well/update/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_WellController_updateWellFromForm27_invoker = createInvoker(
controllers.WellController.updateWellFromForm(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.WellController", "updateWellFromForm", Seq(classOf[String]),"POST", """ Update well from posted data""", Routes.prefix + """well/update/$id<[^/]+>"""))
        

// @LINE:71
private[this] lazy val controllers_SampleController_addSample28_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("sample/add"))))
private[this] lazy val controllers_SampleController_addSample28_invoker = createInvoker(
controllers.SampleController.addSample,
HandlerDef(this.getClass.getClassLoader, "", "controllers.SampleController", "addSample", Nil,"GET", """ Get new sample settings""", Routes.prefix + """sample/add"""))
        

// @LINE:73
private[this] lazy val controllers_SampleController_createSampleFromForm29_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("sample/add"))))
private[this] lazy val controllers_SampleController_createSampleFromForm29_invoker = createInvoker(
controllers.SampleController.createSampleFromForm,
HandlerDef(this.getClass.getClassLoader, "", "controllers.SampleController", "createSampleFromForm", Nil,"POST", """ Create sample from posted data""", Routes.prefix + """sample/add"""))
        

// @LINE:75
private[this] lazy val controllers_SampleController_findSampleByID30_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("sample/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_SampleController_findSampleByID30_invoker = createInvoker(
controllers.SampleController.findSampleByID(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.SampleController", "findSampleByID", Seq(classOf[String]),"GET", """ Display to edit existing sample settings""", Routes.prefix + """sample/$id<[^/]+>"""))
        

// @LINE:77
private[this] lazy val controllers_SampleController_updateSampleFromForm31_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("sample/update/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_SampleController_updateSampleFromForm31_invoker = createInvoker(
controllers.SampleController.updateSampleFromForm(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.SampleController", "updateSampleFromForm", Seq(classOf[String]),"POST", """ Update sample from posted data""", Routes.prefix + """sample/update/$id<[^/]+>"""))
        

// @LINE:80
private[this] lazy val controllers_MaterialController_addMaterial32_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("material/add"))))
private[this] lazy val controllers_MaterialController_addMaterial32_invoker = createInvoker(
controllers.MaterialController.addMaterial,
HandlerDef(this.getClass.getClassLoader, "", "controllers.MaterialController", "addMaterial", Nil,"GET", """ Get new material settings""", Routes.prefix + """material/add"""))
        

// @LINE:82
private[this] lazy val controllers_MaterialController_createMaterialFromForm33_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("material/add"))))
private[this] lazy val controllers_MaterialController_createMaterialFromForm33_invoker = createInvoker(
controllers.MaterialController.createMaterialFromForm,
HandlerDef(this.getClass.getClassLoader, "", "controllers.MaterialController", "createMaterialFromForm", Nil,"POST", """ Create material from posted data""", Routes.prefix + """material/add"""))
        

// @LINE:84
private[this] lazy val controllers_MaterialController_findMaterialByID34_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("material/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_MaterialController_findMaterialByID34_invoker = createInvoker(
controllers.MaterialController.findMaterialByID(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.MaterialController", "findMaterialByID", Seq(classOf[String]),"GET", """ Display to edit existing material settings""", Routes.prefix + """material/$id<[^/]+>"""))
        

// @LINE:86
private[this] lazy val controllers_MaterialController_updateMaterialFromForm35_route = Route("POST", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("material/update/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_MaterialController_updateMaterialFromForm35_invoker = createInvoker(
controllers.MaterialController.updateMaterialFromForm(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.MaterialController", "updateMaterialFromForm", Seq(classOf[String]),"POST", """ Update material from posted data""", Routes.prefix + """material/update/$id<[^/]+>"""))
        

// @LINE:89
private[this] lazy val controllers_RackController_doBSPReport36_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("rack/BSPreport/"),DynamicPart("id", """[^/]+""",true))))
private[this] lazy val controllers_RackController_doBSPReport36_invoker = createInvoker(
controllers.RackController.doBSPReport(fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "doBSPReport", Seq(classOf[String]),"GET", """ Do BSP report for specified rack""", Routes.prefix + """rack/BSPreport/$id<[^/]+>"""))
        

// @LINE:92
private[this] lazy val controllers_Assets_at37_route = Route("GET", PathPattern(List(StaticPart(Routes.prefix),StaticPart(Routes.defaultPrefix),StaticPart("assets/"),DynamicPart("file", """.+""",false))))
private[this] lazy val controllers_Assets_at37_invoker = createInvoker(
controllers.Assets.at(fakeValue[String], fakeValue[String]),
HandlerDef(this.getClass.getClassLoader, "", "controllers.Assets", "at", Seq(classOf[String], classOf[String]),"GET", """ Map static resources from the /public folder to the /assets URL path""", Routes.prefix + """assets/$file<.+>"""))
        
def documentation = List(("""GET""", prefix,"""controllers.Application.index"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """test""","""controllers.Application.test"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """playDoc""","""controllers.Application.playDoc"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """add""","""controllers.Application.add"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """add""","""controllers.Application.addFromForm"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """find""","""controllers.Application.find"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """find""","""controllers.Application.findFromForm"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """find/$id<[^/]+>""","""controllers.Application.findWithID(id:String)"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """tube/add""","""controllers.TubeController.addTube"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """tube/add""","""controllers.TubeController.createTubeFromForm"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """tube/$id<[^/]+>""","""controllers.TubeController.findTubeByID(id:String)"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """tube/update/$id<[^/]+>""","""controllers.TubeController.updateTubeFromForm(id:String)"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """plate/add""","""controllers.PlateController.addPlate"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """plate/add""","""controllers.PlateController.createPlateFromForm"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """plate/$id<[^/]+>""","""controllers.PlateController.findPlateByID(id:String)"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """plate/update/$id<[^/]+>""","""controllers.PlateController.updatePlateFromForm(id:String)"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """rack/add""","""controllers.RackController.addRack"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """rack/add""","""controllers.RackController.createRackFromForm"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """rack/$id<[^/]+>""","""controllers.RackController.findRackByID(id:String)"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """rack/update/$id<[^/]+>""","""controllers.RackController.updateRackFromForm(id:String)"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """freezer/add""","""controllers.FreezerController.addFreezer"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """freezer/add""","""controllers.FreezerController.createFreezerFromForm"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """freezer/$id<[^/]+>""","""controllers.FreezerController.findFreezerByID(id:String)"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """freezer/update/$id<[^/]+>""","""controllers.FreezerController.updateFreezerFromForm(id:String)"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """well/add""","""controllers.WellController.addWell"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """well/add""","""controllers.WellController.createWellFromForm"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """well/$id<[^/]+>""","""controllers.WellController.findWellByID(id:String)"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """well/update/$id<[^/]+>""","""controllers.WellController.updateWellFromForm(id:String)"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """sample/add""","""controllers.SampleController.addSample"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """sample/add""","""controllers.SampleController.createSampleFromForm"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """sample/$id<[^/]+>""","""controllers.SampleController.findSampleByID(id:String)"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """sample/update/$id<[^/]+>""","""controllers.SampleController.updateSampleFromForm(id:String)"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """material/add""","""controllers.MaterialController.addMaterial"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """material/add""","""controllers.MaterialController.createMaterialFromForm"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """material/$id<[^/]+>""","""controllers.MaterialController.findMaterialByID(id:String)"""),("""POST""", prefix + (if(prefix.endsWith("/")) "" else "/") + """material/update/$id<[^/]+>""","""controllers.MaterialController.updateMaterialFromForm(id:String)"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """rack/BSPreport/$id<[^/]+>""","""controllers.RackController.doBSPReport(id:String)"""),("""GET""", prefix + (if(prefix.endsWith("/")) "" else "/") + """assets/$file<.+>""","""controllers.Assets.at(path:String = "/public", file:String)""")).foldLeft(List.empty[(String,String,String)]) { (s,e) => e.asInstanceOf[Any] match {
  case r @ (_,_,_) => s :+ r.asInstanceOf[(String,String,String)]
  case l => s ++ l.asInstanceOf[List[(String,String,String)]]
}}
      

def routes:PartialFunction[RequestHeader,Handler] = {

// @LINE:6
case controllers_Application_index0_route(params) => {
   call { 
        controllers_Application_index0_invoker.call(controllers.Application.index)
   }
}
        

// @LINE:9
case controllers_Application_test1_route(params) => {
   call { 
        controllers_Application_test1_invoker.call(controllers.Application.test)
   }
}
        

// @LINE:11
case controllers_Application_playDoc2_route(params) => {
   call { 
        controllers_Application_playDoc2_invoker.call(controllers.Application.playDoc)
   }
}
        

// @LINE:14
case controllers_Application_add3_route(params) => {
   call { 
        controllers_Application_add3_invoker.call(controllers.Application.add)
   }
}
        

// @LINE:16
case controllers_Application_addFromForm4_route(params) => {
   call { 
        controllers_Application_addFromForm4_invoker.call(controllers.Application.addFromForm)
   }
}
        

// @LINE:19
case controllers_Application_find5_route(params) => {
   call { 
        controllers_Application_find5_invoker.call(controllers.Application.find)
   }
}
        

// @LINE:21
case controllers_Application_findFromForm6_route(params) => {
   call { 
        controllers_Application_findFromForm6_invoker.call(controllers.Application.findFromForm)
   }
}
        

// @LINE:23
case controllers_Application_findWithID7_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_Application_findWithID7_invoker.call(controllers.Application.findWithID(id))
   }
}
        

// @LINE:26
case controllers_TubeController_addTube8_route(params) => {
   call { 
        controllers_TubeController_addTube8_invoker.call(controllers.TubeController.addTube)
   }
}
        

// @LINE:28
case controllers_TubeController_createTubeFromForm9_route(params) => {
   call { 
        controllers_TubeController_createTubeFromForm9_invoker.call(controllers.TubeController.createTubeFromForm)
   }
}
        

// @LINE:30
case controllers_TubeController_findTubeByID10_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_TubeController_findTubeByID10_invoker.call(controllers.TubeController.findTubeByID(id))
   }
}
        

// @LINE:32
case controllers_TubeController_updateTubeFromForm11_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_TubeController_updateTubeFromForm11_invoker.call(controllers.TubeController.updateTubeFromForm(id))
   }
}
        

// @LINE:35
case controllers_PlateController_addPlate12_route(params) => {
   call { 
        controllers_PlateController_addPlate12_invoker.call(controllers.PlateController.addPlate)
   }
}
        

// @LINE:37
case controllers_PlateController_createPlateFromForm13_route(params) => {
   call { 
        controllers_PlateController_createPlateFromForm13_invoker.call(controllers.PlateController.createPlateFromForm)
   }
}
        

// @LINE:39
case controllers_PlateController_findPlateByID14_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_PlateController_findPlateByID14_invoker.call(controllers.PlateController.findPlateByID(id))
   }
}
        

// @LINE:41
case controllers_PlateController_updatePlateFromForm15_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_PlateController_updatePlateFromForm15_invoker.call(controllers.PlateController.updatePlateFromForm(id))
   }
}
        

// @LINE:44
case controllers_RackController_addRack16_route(params) => {
   call { 
        controllers_RackController_addRack16_invoker.call(controllers.RackController.addRack)
   }
}
        

// @LINE:46
case controllers_RackController_createRackFromForm17_route(params) => {
   call { 
        controllers_RackController_createRackFromForm17_invoker.call(controllers.RackController.createRackFromForm)
   }
}
        

// @LINE:48
case controllers_RackController_findRackByID18_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_RackController_findRackByID18_invoker.call(controllers.RackController.findRackByID(id))
   }
}
        

// @LINE:50
case controllers_RackController_updateRackFromForm19_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_RackController_updateRackFromForm19_invoker.call(controllers.RackController.updateRackFromForm(id))
   }
}
        

// @LINE:53
case controllers_FreezerController_addFreezer20_route(params) => {
   call { 
        controllers_FreezerController_addFreezer20_invoker.call(controllers.FreezerController.addFreezer)
   }
}
        

// @LINE:55
case controllers_FreezerController_createFreezerFromForm21_route(params) => {
   call { 
        controllers_FreezerController_createFreezerFromForm21_invoker.call(controllers.FreezerController.createFreezerFromForm)
   }
}
        

// @LINE:57
case controllers_FreezerController_findFreezerByID22_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_FreezerController_findFreezerByID22_invoker.call(controllers.FreezerController.findFreezerByID(id))
   }
}
        

// @LINE:59
case controllers_FreezerController_updateFreezerFromForm23_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_FreezerController_updateFreezerFromForm23_invoker.call(controllers.FreezerController.updateFreezerFromForm(id))
   }
}
        

// @LINE:62
case controllers_WellController_addWell24_route(params) => {
   call { 
        controllers_WellController_addWell24_invoker.call(controllers.WellController.addWell)
   }
}
        

// @LINE:64
case controllers_WellController_createWellFromForm25_route(params) => {
   call { 
        controllers_WellController_createWellFromForm25_invoker.call(controllers.WellController.createWellFromForm)
   }
}
        

// @LINE:66
case controllers_WellController_findWellByID26_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_WellController_findWellByID26_invoker.call(controllers.WellController.findWellByID(id))
   }
}
        

// @LINE:68
case controllers_WellController_updateWellFromForm27_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_WellController_updateWellFromForm27_invoker.call(controllers.WellController.updateWellFromForm(id))
   }
}
        

// @LINE:71
case controllers_SampleController_addSample28_route(params) => {
   call { 
        controllers_SampleController_addSample28_invoker.call(controllers.SampleController.addSample)
   }
}
        

// @LINE:73
case controllers_SampleController_createSampleFromForm29_route(params) => {
   call { 
        controllers_SampleController_createSampleFromForm29_invoker.call(controllers.SampleController.createSampleFromForm)
   }
}
        

// @LINE:75
case controllers_SampleController_findSampleByID30_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_SampleController_findSampleByID30_invoker.call(controllers.SampleController.findSampleByID(id))
   }
}
        

// @LINE:77
case controllers_SampleController_updateSampleFromForm31_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_SampleController_updateSampleFromForm31_invoker.call(controllers.SampleController.updateSampleFromForm(id))
   }
}
        

// @LINE:80
case controllers_MaterialController_addMaterial32_route(params) => {
   call { 
        controllers_MaterialController_addMaterial32_invoker.call(controllers.MaterialController.addMaterial)
   }
}
        

// @LINE:82
case controllers_MaterialController_createMaterialFromForm33_route(params) => {
   call { 
        controllers_MaterialController_createMaterialFromForm33_invoker.call(controllers.MaterialController.createMaterialFromForm)
   }
}
        

// @LINE:84
case controllers_MaterialController_findMaterialByID34_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_MaterialController_findMaterialByID34_invoker.call(controllers.MaterialController.findMaterialByID(id))
   }
}
        

// @LINE:86
case controllers_MaterialController_updateMaterialFromForm35_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_MaterialController_updateMaterialFromForm35_invoker.call(controllers.MaterialController.updateMaterialFromForm(id))
   }
}
        

// @LINE:89
case controllers_RackController_doBSPReport36_route(params) => {
   call(params.fromPath[String]("id", None)) { (id) =>
        controllers_RackController_doBSPReport36_invoker.call(controllers.RackController.doBSPReport(id))
   }
}
        

// @LINE:92
case controllers_Assets_at37_route(params) => {
   call(Param[String]("path", Right("/public")), params.fromPath[String]("file", None)) { (path, file) =>
        controllers_Assets_at37_invoker.call(controllers.Assets.at(path, file))
   }
}
        
}

}
     