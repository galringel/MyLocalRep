import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is in charge of parsing a HTTP request connection
 * It parses the following:
 * - Request method
 * - Request uri
 * - Request query parameters
 * - Request headers
 * - Request Body (if it is a post)
 * - Request Cookies
 * 
 * It uses a regex to validate the first REQUEST line,
 * and then keep parsing all the headers and store them in the appropriate variables
 * 
 * @author @author Omri Hering 026533067 & Gal Ringel 300922424
 * 
 */
public class HTTPRequest extends HTTPMessage {
	final static Pattern requestLine = Pattern.compile("(\\S+)\\s+([^\\s?]+)(\\?(\\S+))?\\s+(HTTP/[0-9.]+)");
	final static String HDR_COOKIE = "Cookie";
	final static String HDR_HOST = "Host";

	public enum Method {
		GET, 
		POST,
		HEAD,
		OPTIONS, 
		TRACE,
		OTHER
	};

	// Request method parsed (see enum)
	Method method;

	// Request URI
	String path;

	// Request URI parameters
	String query;

	// Request host header
	String host;
	
	// Full request first line header ([method] [path][query] [http version])
	String requestTitle;

	// Request parameters stored in Map for better performance later
	Map<String, String> params;

	/**
	 * Ctor
	 */
	public HTTPRequest() {
		
		// Init default parameters
		this.method = Method.OTHER;
		this.params = new HashMap<String, String>();
	}

	/**
	 * Return the request method parsed.
	 * 
	 * @return
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return the request URI parsed.
	 * 
	 * @return
	 */
	public String getPath() {
		return this.path;
	}
	
	/**
	 * Return the full request title first line
	 * 
	 * @return
	 */
	public String getRequestTitle() {
		return this.requestTitle;
	}

	/**
	 * Return the request query string (hold all the request parameters).
	 * 
	 * @return
	 */
	public String getQuery() {
		return this.query;
	}

	/**
	 * Return the request HTTP version.
	 * 
	 * @return
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Return the request host header.
	 * 
	 * @return
	 */
	public String getHost() {
		return this.host;
	}


	/**
	 * Gets the request parameter name and return it's value
	 * 
	 * @param param - request parameter name
	 * @return parameter value, or null if the parameter isn't exists.
	 */
	public String getParam(String param) {
		return this.params.get(param);
	}

	/**
	 * Gets a full URI query which hold the the request parameters
	 * Parse it into a map of key/value.
	 * key = parameter name
	 * value = parameter value
	 * 
	 * @param query
	 */
	void parseParameters(String query) {

		// parameters are supplied with '&' between them so we split by it 
		String[] parameters = query.split("&");

		// iterate each parameter found
		for (String param : parameters) {

			// each parameter is given as "name=value", so we split by '='
			String[] keys = param.split("=");

			// Validates the the current parameter is valid (name=value)
			// If not, we skip it.
			if (keys.length > 1) {

				try {
					// Decode parameter value as UTF-8 and add to the query parameters map
					this.params.put(keys[0], URLDecoder.decode(keys[1], "UTF-8"));
				} catch (UnsupportedEncodingException ex) {
					
					String exceptionString = "ERROR: could not parse parameter value as utf-8" + ex.getMessage();
					System.err.println(exceptionString);
				}
			}
		}
	}

	/**
	 * Gets an inputStream of the request and parse it into our Request Object
	 * 
	 * @param in - the InputStream used as input.
	 * @return true if parsing completed correctly.
	 * @throws EOFException - if the stream ends before the request was read completely
	 * @throws IOException - if there was an I/O error.
	 */
	public boolean parseRequest (HTTPStreamParsingUtil in) {
		
		// Handles the EOF exception
		try {

			String request = HTTPStreamParsingUtil.readLine(in);
			if (request.isEmpty()) {
				// Not a valid request
				return false;
			}

			// Finds the matches in line by the pattern request line.
			Matcher matcher = requestLine.matcher(request);

			// Check is matches have been made.
			if (matcher.find()) {

				this.requestTitle = request;
				
				String requestMethod = matcher.group(1);
				
				// Find outs which request type is it
				FindRequestType(requestMethod);
				
				// Checks whether the path or the query is not null.
				if (matcher.group(2) != null && matcher.group(4) != null) {

					// Update the Path.
					this.path = matcher.group(2);

					// Update the Query.
					this.query = matcher.group(4);

					// Parse parameters from query.
					parseParameters(query);
				} else {

					// Update the Query.
					if (matcher.group(2) == null) {
						return false;
					}
					
					this.path = matcher.group(2);
				}

				// Update the Version.
				this.version = matcher.group(5);
				if (!this.version.equals(HTTP_VERSION11) && !this.version.equals(HTTP_VERSION10)) {
					return false;
				}

				// Reads the headers from the input HTTPStream.
				this.readHeaders(in);

				// Checks if the host header exists.
				if (this.getHeader(HDR_HOST) != null) {

					// Update the Host.
					this.host = this.getHeader(HDR_HOST);
				}

				// Checks if the content header length is exist.
				if (this.getHeader(HDR_CONTENT_LENGTH) != null) {

					// Try to parse request body
					if (!this.readBody(in)) {
						return false;
					}
					
					// Update the Body and parse query parameters
					String postQuery = new String(this.getBody());
					parseParameters(postQuery);
				}
				
				return true;

			}
			
			// Request regex did not match first line
			return false;
			
		}
		catch (EOFException ex) {
			// It's ok (Get with no body for example...)
			return true;
		}
		catch (IOException ex) {
			
			String exceptionString = "ERROR: Request is bad. closing connection!" + ex.getMessage();
			System.err.println(exceptionString);
			return false;
		}
	}

	/**
	 * Gets the request method which was matched from the request header
	 * And checks which type is it
	 * @param requestMethod
	 */
	private void FindRequestType(String requestMethod) {
		switch (requestMethod) {
			case "GET": {
				method = Method.GET;
				break;
			}
			case "POST": {
				method = Method.POST;
				break;
			}
			case "HEAD": {
				method = Method.HEAD;
				break;
			}
			case "OPTIONS": {
				method = Method.OPTIONS;
				break;
			}
			case "TRACE": {
				method = Method.TRACE;
				break;
			}
		}
	}
}