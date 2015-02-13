package controllers

import models.Component.{HiddenFields,ComponentType}
import models.Sample
import play.api.data.Form
import play.api.mvc.Action

/**
 * @author Nathaniel Novod
 *         Date: 11/25/14
 *         Time: 10:21 AM
 */
object SampleController extends ComponentController[Sample] {
	// Play form for sample
	val form = Sample.form
	// Play formatter for Json verification/conversion
	implicit val formatter = Sample.formatter

	// Play html view to create sample
	def htmlForCreate = views.html.sampleCreateForm(_: Form[Sample])

	// Play html view to update sample
	def htmlForUpdate(id: String, hiddenFields: Option[HiddenFields]) =
		views.html.sampleUpdateForm(_: Form[Sample], id, hiddenFields)

	// Play redirect route to find a sample
	def routeFind = routes.SampleController.findSampleByID(_: String)

	// Component type
	val componentType = ComponentType.Sample

	/**
	 * Request to add a sample - we simply put up the form to get the parameters to create the sample.
	 * @return responds to request with html form to get sample input
	 */
	def addSample() = Action { add }

	/**
	 * Request to find a sample, identified by an ID.  We find the sample (or return an error if it can’t be found) and
	 * then go to the page to view/update the sample.
	 * @param id sample ID
	 * @return responds to request with html form to view/update sample
	 */
	def findSampleByID(id: String) = Action.async { request => find(id,request)}

	/**
	 * Go create a sample.  If all goes well the sample is created in the DB and the user is redirected to
	 * to view/update the newly created sample.
	 * @return responds to request with html form to view/update added sample
	 */
	def createSampleFromForm() = Action.async { request => create(request)}

	/**
	 * Go update a sample.  If all goes well the sample is updated in the DB and the user is redirected to
	 * to view/update the newly updated sample.
	 * @return responds to request with updated form to view/update sample
	 */
	def updateSampleFromForm(id: String) = Action.async { request => update(request, id)}
}
