package controllers

import models.Component.{HiddenFields,ComponentType}
import models.Material
import play.api.data.Form
import play.api.mvc.Action

/**
 * Material is content that is put into a container.
 * Created by nnovod on 11/28/14.
 */
object MaterialController extends ComponentController[Material] {
	// Play form for material
	val form = Material.form
	// Play formatter for Json verification/conversion
	implicit val formatter = Material.formatter

	// Play html view to create material
	def htmlForCreate = views.html.materialCreateForm(_: Form[Material])

	// Play html view to update material
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]) =
		views.html.materialUpdateForm(_: Form[Material], id, hiddenFields)

	// Play redirect route to find a material
	def routeFind = routes.MaterialController.findMaterialByID(_: String)

	// Component type
	val componentType = ComponentType.Material

	/**
	 * Request to add a material - we simply put up the form to get the parameters to create the material.
	 * @return responds to request with html form to get material input
	 */
	def addMaterial() = Action { add }

	/**
	 * Request to find a material, identified by an ID.  We find the material (or return an error if it can’t be found) and
	 * then go to the page to view/update the material.
	 * @param id material ID
	 * @return responds to request with html form to view/update material
	 */
	def findMaterialByID(id: String) = Action.async { request => find(id,request)}

	/**
	 * Go create a material.  If all goes well the material is created in the DB and the user is redirected to
	 * to view/update the newly created material.
	 * @return responds to request with html form to view/update added material
	 */
	def createMaterialFromForm() = Action.async { request => create(request)}

	/**
	 * Go update a material.  If all goes well the material is updated in the DB and the user is redirected to
	 * to view/update the newly updated material.
	 * @return responds to request with updated form to view/update material
	 */
	def updateMaterialFromForm(id: String) = Action.async { request => update(request, id)}

}
