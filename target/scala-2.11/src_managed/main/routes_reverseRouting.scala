// @SOURCE:/Users/nnovod/play/tracker/conf/routes
// @HASH:1b68b18887b27461948a2a45b29698c33fa19869
// @DATE:Thu Feb 12 20:36:24 EST 2015

import Routes.{prefix => _prefix, defaultPrefix => _defaultPrefix}
import play.core._
import play.core.Router._
import play.core.Router.HandlerInvokerFactory._
import play.core.j._

import play.api.mvc._
import _root_.controllers.Assets.Asset

import Router.queryString


// @LINE:92
// @LINE:89
// @LINE:86
// @LINE:84
// @LINE:82
// @LINE:80
// @LINE:77
// @LINE:75
// @LINE:73
// @LINE:71
// @LINE:68
// @LINE:66
// @LINE:64
// @LINE:62
// @LINE:59
// @LINE:57
// @LINE:55
// @LINE:53
// @LINE:50
// @LINE:48
// @LINE:46
// @LINE:44
// @LINE:41
// @LINE:39
// @LINE:37
// @LINE:35
// @LINE:32
// @LINE:30
// @LINE:28
// @LINE:26
// @LINE:23
// @LINE:21
// @LINE:19
// @LINE:16
// @LINE:14
// @LINE:11
// @LINE:9
// @LINE:6
package controllers {

// @LINE:92
class ReverseAssets {


// @LINE:92
def at(file:String): Call = {
   implicit val _rrc = new ReverseRouteContext(Map(("path", "/public")))
   Call("GET", _prefix + { _defaultPrefix } + "assets/" + implicitly[PathBindable[String]].unbind("file", file))
}
                        

}
                          

// @LINE:77
// @LINE:75
// @LINE:73
// @LINE:71
class ReverseSampleController {


// @LINE:75
def findSampleByID(id:String): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "sample/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:73
def createSampleFromForm(): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "sample/add")
}
                        

// @LINE:71
def addSample(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "sample/add")
}
                        

// @LINE:77
def updateSampleFromForm(id:String): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "sample/update/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

}
                          

// @LINE:59
// @LINE:57
// @LINE:55
// @LINE:53
class ReverseFreezerController {


// @LINE:55
def createFreezerFromForm(): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "freezer/add")
}
                        

// @LINE:57
def findFreezerByID(id:String): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "freezer/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:59
def updateFreezerFromForm(id:String): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "freezer/update/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:53
def addFreezer(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "freezer/add")
}
                        

}
                          

// @LINE:89
// @LINE:50
// @LINE:48
// @LINE:46
// @LINE:44
class ReverseRackController {


// @LINE:48
def findRackByID(id:String): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "rack/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:44
def addRack(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "rack/add")
}
                        

// @LINE:46
def createRackFromForm(): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "rack/add")
}
                        

// @LINE:50
def updateRackFromForm(id:String): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "rack/update/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:89
def doBSPReport(id:String): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "rack/BSPreport/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

}
                          

// @LINE:86
// @LINE:84
// @LINE:82
// @LINE:80
class ReverseMaterialController {


// @LINE:84
def findMaterialByID(id:String): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "material/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:82
def createMaterialFromForm(): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "material/add")
}
                        

// @LINE:80
def addMaterial(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "material/add")
}
                        

// @LINE:86
def updateMaterialFromForm(id:String): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "material/update/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

}
                          

// @LINE:41
// @LINE:39
// @LINE:37
// @LINE:35
class ReversePlateController {


// @LINE:39
def findPlateByID(id:String): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "plate/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:37
def createPlateFromForm(): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "plate/add")
}
                        

// @LINE:41
def updatePlateFromForm(id:String): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "plate/update/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:35
def addPlate(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "plate/add")
}
                        

}
                          

// @LINE:68
// @LINE:66
// @LINE:64
// @LINE:62
class ReverseWellController {


// @LINE:66
def findWellByID(id:String): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "well/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:62
def addWell(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "well/add")
}
                        

// @LINE:64
def createWellFromForm(): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "well/add")
}
                        

// @LINE:68
def updateWellFromForm(id:String): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "well/update/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

}
                          

// @LINE:23
// @LINE:21
// @LINE:19
// @LINE:16
// @LINE:14
// @LINE:11
// @LINE:9
// @LINE:6
class ReverseApplication {


// @LINE:23
def findWithID(id:String): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "find/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:19
def find(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "find")
}
                        

// @LINE:11
def playDoc(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "playDoc")
}
                        

// @LINE:16
def addFromForm(): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "add")
}
                        

// @LINE:14
def add(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "add")
}
                        

// @LINE:9
def test(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "test")
}
                        

// @LINE:6
def index(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix)
}
                        

// @LINE:21
def findFromForm(): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "find")
}
                        

}
                          

// @LINE:32
// @LINE:30
// @LINE:28
// @LINE:26
class ReverseTubeController {


// @LINE:32
def updateTubeFromForm(id:String): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "tube/update/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

// @LINE:26
def addTube(): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "tube/add")
}
                        

// @LINE:28
def createTubeFromForm(): Call = {
   import ReverseRouteContext.empty
   Call("POST", _prefix + { _defaultPrefix } + "tube/add")
}
                        

// @LINE:30
def findTubeByID(id:String): Call = {
   import ReverseRouteContext.empty
   Call("GET", _prefix + { _defaultPrefix } + "tube/" + implicitly[PathBindable[String]].unbind("id", dynamicString(id)))
}
                        

}
                          
}
                  


// @LINE:92
// @LINE:89
// @LINE:86
// @LINE:84
// @LINE:82
// @LINE:80
// @LINE:77
// @LINE:75
// @LINE:73
// @LINE:71
// @LINE:68
// @LINE:66
// @LINE:64
// @LINE:62
// @LINE:59
// @LINE:57
// @LINE:55
// @LINE:53
// @LINE:50
// @LINE:48
// @LINE:46
// @LINE:44
// @LINE:41
// @LINE:39
// @LINE:37
// @LINE:35
// @LINE:32
// @LINE:30
// @LINE:28
// @LINE:26
// @LINE:23
// @LINE:21
// @LINE:19
// @LINE:16
// @LINE:14
// @LINE:11
// @LINE:9
// @LINE:6
package controllers.javascript {
import ReverseRouteContext.empty

// @LINE:92
class ReverseAssets {


// @LINE:92
def at : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.Assets.at",
   """
      function(file) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "assets/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("file", file)})
      }
   """
)
                        

}
              

// @LINE:77
// @LINE:75
// @LINE:73
// @LINE:71
class ReverseSampleController {


// @LINE:75
def findSampleByID : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.SampleController.findSampleByID",
   """
      function(id) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "sample/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:73
def createSampleFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.SampleController.createSampleFromForm",
   """
      function() {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "sample/add"})
      }
   """
)
                        

// @LINE:71
def addSample : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.SampleController.addSample",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "sample/add"})
      }
   """
)
                        

// @LINE:77
def updateSampleFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.SampleController.updateSampleFromForm",
   """
      function(id) {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "sample/update/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

}
              

// @LINE:59
// @LINE:57
// @LINE:55
// @LINE:53
class ReverseFreezerController {


// @LINE:55
def createFreezerFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.FreezerController.createFreezerFromForm",
   """
      function() {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "freezer/add"})
      }
   """
)
                        

// @LINE:57
def findFreezerByID : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.FreezerController.findFreezerByID",
   """
      function(id) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "freezer/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:59
def updateFreezerFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.FreezerController.updateFreezerFromForm",
   """
      function(id) {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "freezer/update/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:53
def addFreezer : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.FreezerController.addFreezer",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "freezer/add"})
      }
   """
)
                        

}
              

// @LINE:89
// @LINE:50
// @LINE:48
// @LINE:46
// @LINE:44
class ReverseRackController {


// @LINE:48
def findRackByID : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.RackController.findRackByID",
   """
      function(id) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "rack/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:44
def addRack : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.RackController.addRack",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "rack/add"})
      }
   """
)
                        

// @LINE:46
def createRackFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.RackController.createRackFromForm",
   """
      function() {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "rack/add"})
      }
   """
)
                        

// @LINE:50
def updateRackFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.RackController.updateRackFromForm",
   """
      function(id) {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "rack/update/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:89
def doBSPReport : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.RackController.doBSPReport",
   """
      function(id) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "rack/BSPreport/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

}
              

// @LINE:86
// @LINE:84
// @LINE:82
// @LINE:80
class ReverseMaterialController {


// @LINE:84
def findMaterialByID : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.MaterialController.findMaterialByID",
   """
      function(id) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "material/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:82
def createMaterialFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.MaterialController.createMaterialFromForm",
   """
      function() {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "material/add"})
      }
   """
)
                        

// @LINE:80
def addMaterial : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.MaterialController.addMaterial",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "material/add"})
      }
   """
)
                        

// @LINE:86
def updateMaterialFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.MaterialController.updateMaterialFromForm",
   """
      function(id) {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "material/update/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

}
              

// @LINE:41
// @LINE:39
// @LINE:37
// @LINE:35
class ReversePlateController {


// @LINE:39
def findPlateByID : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.PlateController.findPlateByID",
   """
      function(id) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "plate/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:37
def createPlateFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.PlateController.createPlateFromForm",
   """
      function() {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "plate/add"})
      }
   """
)
                        

// @LINE:41
def updatePlateFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.PlateController.updatePlateFromForm",
   """
      function(id) {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "plate/update/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:35
def addPlate : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.PlateController.addPlate",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "plate/add"})
      }
   """
)
                        

}
              

// @LINE:68
// @LINE:66
// @LINE:64
// @LINE:62
class ReverseWellController {


// @LINE:66
def findWellByID : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.WellController.findWellByID",
   """
      function(id) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "well/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:62
def addWell : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.WellController.addWell",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "well/add"})
      }
   """
)
                        

// @LINE:64
def createWellFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.WellController.createWellFromForm",
   """
      function() {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "well/add"})
      }
   """
)
                        

// @LINE:68
def updateWellFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.WellController.updateWellFromForm",
   """
      function(id) {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "well/update/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

}
              

// @LINE:23
// @LINE:21
// @LINE:19
// @LINE:16
// @LINE:14
// @LINE:11
// @LINE:9
// @LINE:6
class ReverseApplication {


// @LINE:23
def findWithID : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.Application.findWithID",
   """
      function(id) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "find/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:19
def find : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.Application.find",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "find"})
      }
   """
)
                        

// @LINE:11
def playDoc : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.Application.playDoc",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "playDoc"})
      }
   """
)
                        

// @LINE:16
def addFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.Application.addFromForm",
   """
      function() {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "add"})
      }
   """
)
                        

// @LINE:14
def add : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.Application.add",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "add"})
      }
   """
)
                        

// @LINE:9
def test : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.Application.test",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "test"})
      }
   """
)
                        

// @LINE:6
def index : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.Application.index",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + """"})
      }
   """
)
                        

// @LINE:21
def findFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.Application.findFromForm",
   """
      function() {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "find"})
      }
   """
)
                        

}
              

// @LINE:32
// @LINE:30
// @LINE:28
// @LINE:26
class ReverseTubeController {


// @LINE:32
def updateTubeFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.TubeController.updateTubeFromForm",
   """
      function(id) {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "tube/update/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

// @LINE:26
def addTube : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.TubeController.addTube",
   """
      function() {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "tube/add"})
      }
   """
)
                        

// @LINE:28
def createTubeFromForm : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.TubeController.createTubeFromForm",
   """
      function() {
      return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "tube/add"})
      }
   """
)
                        

// @LINE:30
def findTubeByID : JavascriptReverseRoute = JavascriptReverseRoute(
   "controllers.TubeController.findTubeByID",
   """
      function(id) {
      return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "tube/" + (""" + implicitly[PathBindable[String]].javascriptUnbind + """)("id", encodeURIComponent(id))})
      }
   """
)
                        

}
              
}
        


// @LINE:92
// @LINE:89
// @LINE:86
// @LINE:84
// @LINE:82
// @LINE:80
// @LINE:77
// @LINE:75
// @LINE:73
// @LINE:71
// @LINE:68
// @LINE:66
// @LINE:64
// @LINE:62
// @LINE:59
// @LINE:57
// @LINE:55
// @LINE:53
// @LINE:50
// @LINE:48
// @LINE:46
// @LINE:44
// @LINE:41
// @LINE:39
// @LINE:37
// @LINE:35
// @LINE:32
// @LINE:30
// @LINE:28
// @LINE:26
// @LINE:23
// @LINE:21
// @LINE:19
// @LINE:16
// @LINE:14
// @LINE:11
// @LINE:9
// @LINE:6
package controllers.ref {


// @LINE:92
class ReverseAssets {


// @LINE:92
def at(path:String, file:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.Assets.at(path, file), HandlerDef(this.getClass.getClassLoader, "", "controllers.Assets", "at", Seq(classOf[String], classOf[String]), "GET", """ Map static resources from the /public folder to the /assets URL path""", _prefix + """assets/$file<.+>""")
)
                      

}
                          

// @LINE:77
// @LINE:75
// @LINE:73
// @LINE:71
class ReverseSampleController {


// @LINE:75
def findSampleByID(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.SampleController.findSampleByID(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.SampleController", "findSampleByID", Seq(classOf[String]), "GET", """ Display to edit existing sample settings""", _prefix + """sample/$id<[^/]+>""")
)
                      

// @LINE:73
def createSampleFromForm(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.SampleController.createSampleFromForm(), HandlerDef(this.getClass.getClassLoader, "", "controllers.SampleController", "createSampleFromForm", Seq(), "POST", """ Create sample from posted data""", _prefix + """sample/add""")
)
                      

// @LINE:71
def addSample(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.SampleController.addSample(), HandlerDef(this.getClass.getClassLoader, "", "controllers.SampleController", "addSample", Seq(), "GET", """ Get new sample settings""", _prefix + """sample/add""")
)
                      

// @LINE:77
def updateSampleFromForm(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.SampleController.updateSampleFromForm(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.SampleController", "updateSampleFromForm", Seq(classOf[String]), "POST", """ Update sample from posted data""", _prefix + """sample/update/$id<[^/]+>""")
)
                      

}
                          

// @LINE:59
// @LINE:57
// @LINE:55
// @LINE:53
class ReverseFreezerController {


// @LINE:55
def createFreezerFromForm(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.FreezerController.createFreezerFromForm(), HandlerDef(this.getClass.getClassLoader, "", "controllers.FreezerController", "createFreezerFromForm", Seq(), "POST", """ Create freezer from posted data""", _prefix + """freezer/add""")
)
                      

// @LINE:57
def findFreezerByID(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.FreezerController.findFreezerByID(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.FreezerController", "findFreezerByID", Seq(classOf[String]), "GET", """ Display to edit existing freezer settings""", _prefix + """freezer/$id<[^/]+>""")
)
                      

// @LINE:59
def updateFreezerFromForm(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.FreezerController.updateFreezerFromForm(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.FreezerController", "updateFreezerFromForm", Seq(classOf[String]), "POST", """ Update freezer from posted data""", _prefix + """freezer/update/$id<[^/]+>""")
)
                      

// @LINE:53
def addFreezer(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.FreezerController.addFreezer(), HandlerDef(this.getClass.getClassLoader, "", "controllers.FreezerController", "addFreezer", Seq(), "GET", """ Get new freezer settings""", _prefix + """freezer/add""")
)
                      

}
                          

// @LINE:89
// @LINE:50
// @LINE:48
// @LINE:46
// @LINE:44
class ReverseRackController {


// @LINE:48
def findRackByID(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.RackController.findRackByID(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "findRackByID", Seq(classOf[String]), "GET", """ Display to edit existing rack settings""", _prefix + """rack/$id<[^/]+>""")
)
                      

// @LINE:44
def addRack(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.RackController.addRack(), HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "addRack", Seq(), "GET", """ Get new rack settings""", _prefix + """rack/add""")
)
                      

// @LINE:46
def createRackFromForm(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.RackController.createRackFromForm(), HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "createRackFromForm", Seq(), "POST", """ Create rack from posted data""", _prefix + """rack/add""")
)
                      

// @LINE:50
def updateRackFromForm(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.RackController.updateRackFromForm(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "updateRackFromForm", Seq(classOf[String]), "POST", """ Update rack from posted data""", _prefix + """rack/update/$id<[^/]+>""")
)
                      

// @LINE:89
def doBSPReport(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.RackController.doBSPReport(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.RackController", "doBSPReport", Seq(classOf[String]), "GET", """ Do BSP report for specified rack""", _prefix + """rack/BSPreport/$id<[^/]+>""")
)
                      

}
                          

// @LINE:86
// @LINE:84
// @LINE:82
// @LINE:80
class ReverseMaterialController {


// @LINE:84
def findMaterialByID(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.MaterialController.findMaterialByID(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.MaterialController", "findMaterialByID", Seq(classOf[String]), "GET", """ Display to edit existing material settings""", _prefix + """material/$id<[^/]+>""")
)
                      

// @LINE:82
def createMaterialFromForm(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.MaterialController.createMaterialFromForm(), HandlerDef(this.getClass.getClassLoader, "", "controllers.MaterialController", "createMaterialFromForm", Seq(), "POST", """ Create material from posted data""", _prefix + """material/add""")
)
                      

// @LINE:80
def addMaterial(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.MaterialController.addMaterial(), HandlerDef(this.getClass.getClassLoader, "", "controllers.MaterialController", "addMaterial", Seq(), "GET", """ Get new material settings""", _prefix + """material/add""")
)
                      

// @LINE:86
def updateMaterialFromForm(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.MaterialController.updateMaterialFromForm(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.MaterialController", "updateMaterialFromForm", Seq(classOf[String]), "POST", """ Update material from posted data""", _prefix + """material/update/$id<[^/]+>""")
)
                      

}
                          

// @LINE:41
// @LINE:39
// @LINE:37
// @LINE:35
class ReversePlateController {


// @LINE:39
def findPlateByID(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.PlateController.findPlateByID(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.PlateController", "findPlateByID", Seq(classOf[String]), "GET", """ Display to edit existing plate settings""", _prefix + """plate/$id<[^/]+>""")
)
                      

// @LINE:37
def createPlateFromForm(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.PlateController.createPlateFromForm(), HandlerDef(this.getClass.getClassLoader, "", "controllers.PlateController", "createPlateFromForm", Seq(), "POST", """ Create plate from posted data""", _prefix + """plate/add""")
)
                      

// @LINE:41
def updatePlateFromForm(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.PlateController.updatePlateFromForm(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.PlateController", "updatePlateFromForm", Seq(classOf[String]), "POST", """ Update plate from posted data""", _prefix + """plate/update/$id<[^/]+>""")
)
                      

// @LINE:35
def addPlate(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.PlateController.addPlate(), HandlerDef(this.getClass.getClassLoader, "", "controllers.PlateController", "addPlate", Seq(), "GET", """ Get new plate settings""", _prefix + """plate/add""")
)
                      

}
                          

// @LINE:68
// @LINE:66
// @LINE:64
// @LINE:62
class ReverseWellController {


// @LINE:66
def findWellByID(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.WellController.findWellByID(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.WellController", "findWellByID", Seq(classOf[String]), "GET", """ Display to edit existing well settings""", _prefix + """well/$id<[^/]+>""")
)
                      

// @LINE:62
def addWell(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.WellController.addWell(), HandlerDef(this.getClass.getClassLoader, "", "controllers.WellController", "addWell", Seq(), "GET", """ Get new well settings""", _prefix + """well/add""")
)
                      

// @LINE:64
def createWellFromForm(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.WellController.createWellFromForm(), HandlerDef(this.getClass.getClassLoader, "", "controllers.WellController", "createWellFromForm", Seq(), "POST", """ Create well from posted data""", _prefix + """well/add""")
)
                      

// @LINE:68
def updateWellFromForm(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.WellController.updateWellFromForm(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.WellController", "updateWellFromForm", Seq(classOf[String]), "POST", """ Update well from posted data""", _prefix + """well/update/$id<[^/]+>""")
)
                      

}
                          

// @LINE:23
// @LINE:21
// @LINE:19
// @LINE:16
// @LINE:14
// @LINE:11
// @LINE:9
// @LINE:6
class ReverseApplication {


// @LINE:23
def findWithID(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.Application.findWithID(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "findWithID", Seq(classOf[String]), "GET", """ Find from get parameter""", _prefix + """find/$id<[^/]+>""")
)
                      

// @LINE:19
def find(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.Application.find(), HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "find", Seq(), "GET", """ Retrieve item""", _prefix + """find""")
)
                      

// @LINE:11
def playDoc(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.Application.playDoc(), HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "playDoc", Seq(), "GET", """To get play introduction""", _prefix + """playDoc""")
)
                      

// @LINE:16
def addFromForm(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.Application.addFromForm(), HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "addFromForm", Seq(), "POST", """ Post request to add a new item (posted data specified item type)""", _prefix + """add""")
)
                      

// @LINE:14
def add(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.Application.add(), HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "add", Seq(), "GET", """ Get settings to add a new item""", _prefix + """add""")
)
                      

// @LINE:9
def test(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.Application.test(), HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "test", Seq(), "GET", """ Test page""", _prefix + """test""")
)
                      

// @LINE:6
def index(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.Application.index(), HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "index", Seq(), "GET", """ Home page""", _prefix + """""")
)
                      

// @LINE:21
def findFromForm(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.Application.findFromForm(), HandlerDef(this.getClass.getClassLoader, "", "controllers.Application", "findFromForm", Seq(), "POST", """ Find from posted data""", _prefix + """find""")
)
                      

}
                          

// @LINE:32
// @LINE:30
// @LINE:28
// @LINE:26
class ReverseTubeController {


// @LINE:32
def updateTubeFromForm(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.TubeController.updateTubeFromForm(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.TubeController", "updateTubeFromForm", Seq(classOf[String]), "POST", """ Update tube from posted data""", _prefix + """tube/update/$id<[^/]+>""")
)
                      

// @LINE:26
def addTube(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.TubeController.addTube(), HandlerDef(this.getClass.getClassLoader, "", "controllers.TubeController", "addTube", Seq(), "GET", """ Get new tube settings""", _prefix + """tube/add""")
)
                      

// @LINE:28
def createTubeFromForm(): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.TubeController.createTubeFromForm(), HandlerDef(this.getClass.getClassLoader, "", "controllers.TubeController", "createTubeFromForm", Seq(), "POST", """ Create tube from posted data""", _prefix + """tube/add""")
)
                      

// @LINE:30
def findTubeByID(id:String): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
   controllers.TubeController.findTubeByID(id), HandlerDef(this.getClass.getClassLoader, "", "controllers.TubeController", "findTubeByID", Seq(classOf[String]), "GET", """ Display to edit existing tube settings""", _prefix + """tube/$id<[^/]+>""")
)
                      

}
                          
}
        
    