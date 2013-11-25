import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * 
 * @author 
 *
 */
public class HTTPResponse extends HTTPMessage {
	public static final String STATUS_200 = "OK";
	public static final String STATUS_303 = "See Other";
	public static final String STATUS_400 = "Bad Request";
	public static final String STATUS_404 = "Not Found";
	public static final String STATUS_500 = "Internal Server Error";
	public static final String STATUS_501 = "Not Implemented";
	public static final String HDR_LOCATION = "Location";
	public static final String HDR_SETCOOKIE = "Set-Cookie";

	int statusCode;
	String statusPhrase;

	public HTTPResponse(String version) {
		super();
		this.version = version;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusPhrase() {
		return statusPhrase;
	}
	

	public void setStatusCode(int code, String phrase) {
		statusCode = code;
		statusPhrase = phrase;
	}
	
	/**
	 * 
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
	 * 
	 * @param parameters
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
	 * 
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
	 * 
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
	 * 
	 * @param res
	 */
	void generate501() {

		// Sets the header of the page and the body and directs to the page.
		setStatusCode(501, HTTPResponse.STATUS_501);
		setHeader("content-type", "text/html");
		setBody(generateGeneric_HTML(getStatusCode() + " " + STATUS_501).getBytes());
	}
	
	/**
	 * 
	 * @param body
	 */
	void generate200OK(byte[] body) {
		
		if (body == null) {
			generate404();
			return;
		}
		
		setStatusCode(200, STATUS_200);
		setHeader("content-type", "text/html");
		setBody(body);
	}
	
	/**
	 * 
	 * @param res
	 */
	void generate400() {

		// Sets the header of the page and the body and directs to the page.
		setStatusCode(400, HTTPResponse.STATUS_400);
		setHeader("content-type", "text/html");
		setBody(generateGeneric_HTML(getStatusCode() + " " + STATUS_400).getBytes());
	}
	

	/**
	 * Generate a 404 not found response.
	 * 
	 * @param res
	 * @param req
	 */
	void generate404() {

		// Sets the header of the page and the body and directs to the page.
		setStatusCode(404, HTTPResponse.STATUS_404);
		setBody(generateGeneric_HTML(getStatusCode() + " " + STATUS_404).getBytes());
		setHeader("content-type", "text/html");
	}
	
	/**
	 * Generate a 500 server error
	 * 
	 * @param res
	 * @param req
	 */
	void generate500() {

		// Sets the header of the page and the body and directs to the page.
		setStatusCode(404, HTTPResponse.STATUS_500);
		setHeader("content-type", "text/html");
		setBody(generateGeneric_HTML(getStatusCode() + " " + STATUS_500).getBytes());
	}
	
	/**
	 * 
	 * @return
	 */
	String buildResponseFirstHeader() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(version + " ");
		sb.append(getStatusCode() + " ");
		sb.append(getStatusPhrase() + "\r\n");
		
		return sb.toString();
	}

	@Override
	public void write(PrintStream out, boolean isChunked) throws IOException {

		// Write the first line of the response by message status.		
		out.print(buildResponseFirstHeader());

		// Uses the HTTPMessage.write to write the headers and body.
		super.write(out, isChunked);
	}
}
