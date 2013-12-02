import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

/**
 * This class is in charge of creating a HTTP Response 
 * It support the following response codes:
 * - 200, 301, 400, 404, 500, 501
 * - BONUS: we added a 301 redirect response
 * - BONUS: we added a set-cookie of machineId to  computers that already visited our site
 * 
 * This class knows to generate a response from a GENERIC POST request (from any <form> type)
 * This class knows to generate a "ALLOW HEADER" in case of OPTIONS request
 * 
 * @author @author Omri Hering 026533067 & Gal Ringel 300922424
 * 
 */
public class HTTPResponse extends HTTPMessage {
	
	public static final String STATUS_200 = "OK";
	public static final String STATUS_301 = "Moved Permanently";
	public static final String STATUS_400 = "Bad Request";
	public static final String STATUS_404 = "Not Found";
	public static final String STATUS_500 = "Internal Server Error";
	public static final String STATUS_501 = "Not Implemented";
	
	/* For bonus */
	public static final String HEADER_LOCATION = "Location";
	public static final String HEADER_SETCOOKIE = "Set-Cookie";

	/* private members */
	int statusCode;
	String statusPhrase;

	/**
	 * ctor, get HTTP version and set it.
	 * @param version
	 */
	public HTTPResponse(String version) {
		super();
		this.version = version;
	}

	/**
	 * Gets the response status code
	 * @return
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Gets the response status phrase
	 * @return
	 */
	public String getStatusPhrase() {
		return this.statusPhrase;
	}
	

	/**
	 * Gets HTTP code and phrase and updates it
	 * @param code
	 * @param phrase
	 */
	public void setStatusCode(int httpCode, String httpPhrase) {
		this.statusCode = httpCode;
		this.statusPhrase = httpPhrase;
	}
	
	/**
	 * Generate a generic HTML response body with desired message
	 * @param msg
	 * @return
	 */
	String generateGeneric_HTML(String msg) {
		
		StringBuilder out = new StringBuilder();

		// Initialize the html page and his body.
		out.append("<html><body>\n");

		// The HTML page header.
		out.append("<h1>" + msg + "</h1>\n");

		// Close the body and the HTML page
		out.append("</body></html>");
		return out.toString();
	}
	
	/**
	 * Generate a generic HTML POST response body with a given parameters from the <form>
	 * @param parameters - got form the client
	 * @return
	 */
	String generatePOST_HTML(Map<String,String> parameters) {
		
		StringBuilder out = new StringBuilder();

		// Initialize the html page and his body.
		out.append("<html><body>\n");
		out.append("<style>body {\nbackground-image: url(./bg.jpg);\nbackground-size: 100%;\nbackground-origin: content;\nbackground-repeat: no-repeat; } </style>");
		// The HTML page header.
		out.append("<center><h1><font color=\"#FFFFFF\">POST parameters</font></h1>\n");
		
		out.append("<table>\n");
		out.append("<tr><td><b>\n");
		out.append("<font color=\"#FFFFFF\">Parameter Name&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		out.append("</font></b></td>\n");
		out.append("<td><b>\n");
		out.append("<font color=\"#FFFFFF\">Parameter Value&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		out.append("</font></b></td></tr>\n");
		for (Map.Entry<String, String> simpleEntry : parameters.entrySet()) {
			out.append("<tr>\n");
			out.append("<td>\n<font color=\"#FFFFFF\">");
			out.append(simpleEntry.getKey());
			out.append("</font></td>\n");
			out.append("<td>\n<font color=\"#FFFFFF\">");
			out.append(simpleEntry.getValue());
			out.append("</font></td>\n");
			out.append("</tr>\n");
		}

		// Close the body and the HTML page
		out.append("</center></table>");
		out.append("</body></html>");
		return out.toString();
	}
	
	/**
	 * Generate machineId to remember users that came back
	 * We will use it with "Set-Cookie"
	 * @return
	 */
	public String generateMachineId()
	{
	    return new BigInteger(130, new Random()).toString(32);
	}
	
	/**
	 * Generate a TRACE HTML response with all the original request metadata
	 * @param headers
	 * @param requestUrI
	 * @return
	 */
	 String generateTRACE_HTML(Map<String,String> headers, String requestUrI) {
	
		StringBuilder out = new StringBuilder();

		// Initialize the html page and his body.
		out.append("<html><body>\n");

		// The HTML page header.
		out.append("<h1>TRACE - Original Request</h1>\n");
		
		out.append(requestUrI + "<br>");
		
		// append request headers
		for (Map.Entry<String, String> simpleEntry : headers.entrySet()) {
			out.append(simpleEntry.getKey() + ": " + simpleEntry.getValue() + "<br>");
		}

		// Close the body and the HTML page
		out.append("</body></html>");
		return out.toString();
	}
	
	/**
	 * Build a "ALLOW" Header for the request type "OPTIONS"
	 * @return
	 */
	 String generateAllowHeader() {
		
		StringBuilder allowedHeader = new StringBuilder();
		allowedHeader.append(HTTPRequest.Method.GET + ",");
		allowedHeader.append(HTTPRequest.Method.POST + ",");
		allowedHeader.append(HTTPRequest.Method.HEAD + ",");
		allowedHeader.append(HTTPRequest.Method.TRACE + ",");
		allowedHeader.append(HTTPRequest.Method.OPTIONS);
		
		return allowedHeader.toString();
	}
	
	/**
	 * Generate a 501 response with all the relevant headers
	 */
	void generate501() {

		// Sets the header of the page and the body and directs to the page.
		setStatusCode(501, HTTPResponse.STATUS_501);
		setHeader("content-type", "text/html");
		setBody(generateGeneric_HTML(getStatusCode() + " " + STATUS_501).getBytes());
	}
	
	/**
	 * Generate a 301 response with all the relevant headers
	 */
	void generate301(String redirectURL) {
		
		setStatusCode(301, STATUS_301);
		setHeader(HEADER_LOCATION, redirectURL);
	}
	
	/**
	 * Generate a 200 OK response with with a given body and relevant headers
	 * @param body
	 */
	void generate200OK(byte[] body) {
		
		if (body == null) {
			generate404();
			return;
		}
		
		setStatusCode(200, STATUS_200);
		setBody(body);
	}
	
	/**
	 * Gets a Cookie Name and Cookie Value and add a "Set-Cookie" header with it.
	 * @param cookieName
	 * @param cookieValue
	 */
	void setCookie(String cookieName, String cookieValue) {
		
		String cookieHeaderValue = cookieName + "=" + cookieValue;
		setHeader("Set-Cookie", cookieHeaderValue);
	}
	
	/**
	 * Generate a 400 response with all the relevant headers
	 */
	void generate400() {

		// Sets the header of the page and the body and directs to the page.
		setStatusCode(400, HTTPResponse.STATUS_400);
		setHeader("content-type", "text/html");
		setBody(generateGeneric_HTML(getStatusCode() + " " + STATUS_400).getBytes());
	}
	

	/**
	 * Generate a 404 response with all the relevant headers
	 */
	void generate404() {

		// Sets the header of the page and the body and directs to the page.
		setStatusCode(404, HTTPResponse.STATUS_404);
		setBody(generateGeneric_HTML(getStatusCode() + " " + STATUS_404).getBytes());
		setHeader("content-type", "text/html");
	}
	
	/**
	 * Generate a 500 response with all the relevant headers
	 */
	void generate500() {

		// Sets the header of the page and the body and directs to the page.
		setStatusCode(404, HTTPResponse.STATUS_500);
		setHeader("content-type", "text/html");
		setBody(generateGeneric_HTML(getStatusCode() + " " + STATUS_500).getBytes());
	}
	
	/**
	 * Gets the HTTP Response first line
	 * @return
	 */
	String buildResponseFirstHeader() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(version + " ");
		sb.append(getStatusCode() + " ");
		sb.append(getStatusPhrase() + CRLF);
		
		return sb.toString();
	}

	@Override
	/**
	 * writes the response to the stream using super().write of HTTPMessage class
	 */
	public void write(PrintStream out, boolean isChunked) throws IOException {

		// Write the first line of the response by message status.		
		out.print(buildResponseFirstHeader());

		// Uses the HTTPMessage.write to write the headers and body.
		super.write(out, isChunked);
	}
}
