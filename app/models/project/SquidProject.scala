package models.project

/**
 * Based on minimalist SOAP client from Bob Jamison. (http://wiki.summercode.com/a_trivial_soap_client_in_scala)
 * Get Squid project name given a GSSR ID using Squid's SOAP interface
 * Created by nnovod on 5/4/15.
 */
import scala.xml.{Elem,XML}
import scala.io.Source

object SquidProject {
	// Soap request parameters
	private val sampleLSID = "sampleLSID"
	private val projectName = "projectName"
	private val sampleBarcode = "sampleBarcode"
	private val sampleProjectStatus = "sampleProjectStatus"

	/**
	 * Wrap a soap request in a soap envelope returning the entire XML soap request
	 * @param xml Soap request
	 * @param xmlnsUrn optional urn to put in envelope header
	 * @return complete XML soap request
	 */
	private def wrap(xml: Elem, xmlnsUrn: Option[String]) : String = {
		val buf = new StringBuilder
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
		buf.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"")
		if (xmlnsUrn.isDefined) buf.append(" xmlns:urn=\"" + xmlnsUrn.get + "\"")
		buf.append(">\n")
		buf.append("<soapenv:Header/>\n")
		buf.append("<soapenv:Body>\n")
		buf.append(xml.toString())
		buf.append("\n</soapenv:Body>\n")
		buf.append("</soapenv:Envelope>\n")
		buf.toString()
	}

	/**
	 * Get http connection to host.
	 * @param host request server
	 * @return connection to server
	 */
	private def getConnection(host: String) = {
		try {
			val url = new java.net.URL(host)
			url.openConnection.asInstanceOf[java.net.HttpURLConnection]
		} catch {
			case e: Exception =>
				throw new Exception(s"Error initializing connection to Squid: ${e.getLocalizedMessage}")
		}
	}

	/**
	 * Squid host for sample soap requests
	 */
	private val squidHost = "http://squid-ui.broadinstitute.org:8000/squid/services/SampleDetailsService"

	/**
	 * Send out a soap request and get the response
	 * @param req request (to be put SOAP envelope)
	 * @param urn optional urn to put in envelope header
	 * @return optional response from request (should be something there if no errors and the response isn't blank)
	 */
	private def sendMessage(req: Elem, urn: Option[String]) : Option[Elem] = {
		val conn = getConnection(squidHost)
		try {
			val outs = wrap(req, urn).getBytes
			conn.setRequestMethod("POST")
			conn.setDoOutput(true)
			conn.setRequestProperty("Content-Length", outs.length.toString)
			conn.setRequestProperty("Content-Type", "text/xml")
			conn.getOutputStream.write(outs)
			conn.getOutputStream.close()
			val ret = Some(XML.load(conn.getInputStream))
			conn.getInputStream.close()
			ret
		} catch {
			case e: Exception =>
				val exc = try {
					val err = "\n" + Source.fromInputStream(conn.getErrorStream).mkString
					conn.getErrorStream.close()
					new Exception(s"Error posting request to Squid: ${e.getLocalizedMessage}$err")
				} catch {
					case e1: Exception =>
						new Exception(s"Error posting request to Squid: ${e.getLocalizedMessage}")
				}
				throw exc
		}
	}

	/**
	 * Send out sample request
	 * @param req request (to be put in SOAP envelope)
	 * @return optional response from request (should be something there if no errors and the response isn't blank)
	 */
	private def sendSampleRequest(req: Elem) =
		sendMessage(req, Some("urn:SpfSampleDetails"))

	/**
	 * Find sample information based on a gssr barcode.  We set the gssr barcode parameter and send off the request.
	 * @param gssrBarcode gssr barcode to base query on
	 * @return optional response from request (should be something there if no errors and the response isn't blank)
	 */
	private def findSampleElemByBarcode(gssrBarcode: String) = {
		val req =
			<urn:findSampleDetailsByBarcode>
				<barcode>{gssrBarcode}</barcode>
			</urn:findSampleDetailsByBarcode>
		sendSampleRequest(req)
	}

	/**
	 * Find project information based on a lsid.  We set the lsid parameter and send off the request.
	 * @param sampleLSIDs lsid to base query on
	 * @return optional response from request (should be something there if no errors and the response isn't blank)
	 */
	private def findProjectsElemByLSIDs(sampleLSIDs: List[String]) = {
		val req =
			<urn:findSampleProjectStatusByLSID>
				<samplelsid>
					{for (lsid <- sampleLSIDs) yield <gssrSampleLSID>{lsid}</gssrSampleLSID>}
				</samplelsid>
			</urn:findSampleProjectStatusByLSID>
		sendSampleRequest(req)
	}

	/**
	 * Find projects for a list of LSIDs.  It's much more efficient to retrieve multiple projects for multiple LSIDs
	 * as opposed to retrieving one project at a time for each LSID so this method gives you the opportunity to do
	 * that.
	 * @param sampleLSIDs LSIDs to find projects for
	 * @return map containg gssrID->project
	 */
	def findProjectsByLSIDs(sampleLSIDs: List[String]) = {
		findProjectsElemByLSIDs(sampleLSIDs) match {
			case Some(projects) =>
				val ret = (projects \\ sampleProjectStatus).flatMap((p) => {
					val bc = p \\ sampleBarcode
					if (bc.isEmpty) List.empty else {
						val proj = p \\ projectName
						if (proj.isEmpty) List.empty else List(bc.text -> proj.text)
					}
				})
				ret.toMap
			case _ => Map.empty[String, String]
		}
	}

	/**
	 * Find LSID associated with a GSSR barcode
	 * @param gssrBarcode gssr barcode to base query on
	 * @return optional LSID (should be something there if no errors and the response isn't blank)
	 */
	def findSampleByBarcode(gssrBarcode: String) = {
		// Find LSID using the gssr barcode
		// If nothing is returned from either query we will not drop into the yield and return None
		for {sampleData <- findSampleElemByBarcode(gssrBarcode)
			 lsID = (sampleData \\ sampleLSID).text
			 if !lsID.isEmpty
		} yield {
			lsID
		}
	}

	/**
	 * Find project name associated with a LSID
	 * @param lsID LSID to base query on
	 * @return optional project name (should be something there if no errors and the response isn't blank)
	 */
	def findProjNameByLSID(lsID: String) = {
		// Find project name using the LSID
		// If nothing is returned from either query we will not drop into the yield and return None
		for {projectData <- findProjectsElemByLSIDs(List(lsID))
			 proj = (projectData \\ projectName).text
			 if !proj.isEmpty
		} yield {
			proj
		}
	}

	/**
	 * Find project name associated with a GSSR barcode
	 * @param gssrBarcode gssr barcode to base query on
	 * @return optional project name (should be something there if no errors and the response isn't blank)
	 */
	def findProjNameByBarcode(gssrBarcode: String) = {
		// First find LSID using the gssr barcode then find project name using the LSID
		// If nothing is returned from either query we will not drop into the yield and return None
		for {sampleData <- findSampleElemByBarcode(gssrBarcode)
			 lsID = (sampleData \\ sampleLSID).text
			 if !lsID.isEmpty
			 projectData <- findProjectsElemByLSIDs(List(lsID))
			 proj = (projectData \\ projectName).text
			 if !proj.isEmpty
		} yield {
			proj
		}
	}
}
