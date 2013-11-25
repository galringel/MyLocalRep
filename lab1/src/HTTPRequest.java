import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing a complete, parsed HTTP Request
 * 
 * @author 
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

	Method method;

	/**
	 * The path portion of the request URI.
	 */
	String path;

	/**
	 * The query portion of the request URI
	 */
	String query;

	/**
	 * The host for which this method was intended. null means the default host
	 * (no host header was provided).
	 */
	String host;
	
	/**
	 * Hold the first line of the request packet
	 */
	String requestTitle;

	/**
	 * Cookies supplied in the request. This field contains a map from cookie
	 * names to cookie values.
	 */
	Map<String, String> cookies;

	/**
	 * Parameters supplied in the request. This field contains a map from
	 * parameter names to values.
	 */
	Map<String, String> params;

	public HTTPRequest() {
		super(); // Not really needed
		method = Method.OTHER;
		cookies = new HashMap<String, String>();
		params = new HashMap<String, String>();
	}

	/**
	 * Return the request method.
	 * 
	 * @return
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * Return the request path.
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * Return the request title.
	 * 
	 * @return
	 */
	public String getRequestTitle() {
		return requestTitle;
	}

	/**
	 * Return the request query.
	 * 
	 * @return
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Return the request version.
	 * 
	 * @return
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Return the request host.
	 * 
	 * @return
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Return the value of a cookie. Cookie names are case insensitive.
	 * 
	 * @param cookie
	 *            the name of the cookie
	 * @return the value of the cookie, or null if the cookie wasn't supplied.
	 */
	public String getCookie(String cookie) {
		return cookies.get(cookie.toLowerCase());
	}

	/**
	 * Return the value of a parameter. Parameter names are case sensitive.
	 * 
	 * @param param
	 * @return the value of the parameter, or null if it wasn't supplied.
	 */
	public String getParam(String param) {
		return params.get(param);
	}

	/**
	 * Return the entire cookie map.
	 * 
	 * @return
	 */
	public Map<String, String> getCookies() {
		return cookies;
	}

	/**
	 * Parse the value of a cookie header. The results are added to the
	 * {@link #cookies} map (cookies that already appear in the map have their
	 * values replaced).
	 * 
	 * @param line
	 *            the line to parse (this is the value of the "Cookie:" header).
	 * @return true if parsing completed successfully.
	 */

	boolean parseCookieHeader(String line) {

		// Checks if the input is empty or null.
		if (line == null || line.length() == 0) {
			return false;
		}

		// Using regex pattern to parse the cookie.
		List<AVPair> pairs = AVPair.parseAvPairs(line);

		// Puts each cookie at the cookie map.
		for (AVPair pair : pairs) {
			cookies.put(pair.attr.toLowerCase(), pair.value);
		}
		return true;
	}

	/**
	 * Parse a query as a series of name=value pairs separated by ampersands.
	 * 
	 * @param query
	 */
	void parseParameters(String query) {

		// Puts every part of the input string in string array
		// Splits at "&" char.
		String[] parameters = query.split("&");

		// Each parameter at the array splits by the "=" char
		for (String param : parameters) {

			// Puts in another array.
			String[] keys = param.split("=");

			// Check that the there are two parts at the array.
			if (keys.length > 1) {

				// Handles the decode method exceptions
				try {

					// Puts in the parameters map in a decoded form (UTF-8).
					params.put(keys[0], URLDecoder.decode(keys[1], "UTF-8"));
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
	}

	/**
	 * Read and parse an HTTP request from an input stream.
	 * 
	 * @param in
	 *            the InputStream used as input.
	 * @return true if parsing completed correctly.
	 * @throws EOFException
	 *             if the stream ends before the request was read completely
	 * @throws IOException
	 *             if there was an I/O error.
	 */
	public boolean parse(HTTPInputStream in) throws IOException {
		boolean parseOk = true;

		// Handles the EOF exception
		try {

			String request = HTTPInputStream.readLine(in);
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
				if (requestMethod.equals("GET")) {
					method = Method.GET;
				} else if (requestMethod.equals("POST")) {
					method = Method.POST;
				} else if (requestMethod.equals("HEAD")) {
					method = Method.HEAD;
				} else if (requestMethod.equals("OPTIONS")) {
					method = Method.OPTIONS;
				} else if (requestMethod.equals("TRACE")) {
					method = Method.TRACE;
				}
				
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
				if (!this.version.equals(HTTP_VERSION11) || 
						!this.version.equals(HTTP_VERSION11)) {
					return false;
				}

				// Reads the headers from the input HTTPStream.
				this.readHeaders(in);

				// Checks if the cookie header exist.
				if (this.getHeader(HDR_COOKIE) != null) {

					// Parse the CookieHeader
					this.parseCookieHeader(this.getHeader(HDR_COOKIE));
				}

				// Checks if the host header exists.
				if (this.getHeader(HDR_HOST) != null) {

					// Update the Host.
					this.host = this.getHeader(HDR_HOST);
				}

				// Checks if the content header length is exist.
				if (this.getHeader(HDR_CONTENT_LENGTH) != null) {

					// Update the Body and parse query parameters
					this.readBody(in);
					String postQuery = new String(this.getBody());
					parseParameters(postQuery);
				}

			} else {
				parseOk = false;
			}
		} catch (EOFException e) {
		}
		return parseOk;
	}
}