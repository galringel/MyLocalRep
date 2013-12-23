import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * This is our main HTTP Message Object which reflects the basic fields and action of HTTP REQUEST AND RESPONSE.
 * It hold all the common variables of request and response like:
 * - GetBody(), SetBody()
 * - Get and Set Headers
 * - write() to stream at the end.
 * 
 * HTTPRequest and HTTPResponse will extends it for more functionality!
 * 
 * @author Omri Hering 026533067 & Gal Ringel 300922424
 * 
 */
public class HTTPMessage {

	/* Some relevant consts */
	public final static String HTTP_VERSION10 = "HTTP/1.0";
	public final static String HTTP_VERSION11 = "HTTP/1.1";
	final static String HDR_CONTENT_LENGTH = "Content-Length";
	final String CRLF = "\r\n";

	// HTTP version used in the request or response
	String version;

	// HTTP headers of the request or response
	Map<String, String> headers;

	// HTTP request POST body or Response body
	byte[] body;
	
	/**
	 * Ctor
	 */
	protected HTTPMessage() {
		this.headers = new HashMap<String, String>();
	}

	/**
	 * Return the HTTP message body.
	 * @return HTTP message body. if null, no body
	 */
	public byte[] getBody() {
		return this.body;
	}

	/**
	 * Set the HTTP message body and it's content-length header according to it.
	 * @param body to set
	 */
	public void setBody(byte[] body) {
		this.body = body;

		// If body null, it's a HTTP message with no body and content-length header
		if (body == null) {

			// Remove content-length header
			setHeader(HDR_CONTENT_LENGTH, null);
		} else {

			// Change content-length to the given body length
			setHeader(HDR_CONTENT_LENGTH, String.valueOf(body.length));
		}
	}

	/**
	 * Gets a header name and return it's value
	 * @param header - header name
	 * @return header value, or null if not exists
	 */
	public String getHeader(String header) {
		return this.headers.get(header.toLowerCase());
	}

	/**
	 * Gets header name and value and add it to headers map
	 * If the given header value is null, we remove the header
	 * @param header name
	 * @param value value
	 * @return
	 */
	public String setHeader(String header, String value) {
		
		// If null we remove the header from the map
		if (value == null)
			return this.headers.remove(header.toLowerCase());
		else
			return this.headers.put(header.toLowerCase(), value);
	}

	/**
	 * Returns the entire headers map
	 * @return Returns the entire headers map
	 */
	public Map<String, String> getHeaders() {
		return this.headers;
	}

	/**
	 * Reads all headers and parse them according to their structure:
	 * [Header_Name]: [HeaderValue]
	 * 
	 * @param in
	 */
	protected void readHeaders(HTTPStreamParsingUtil in) {

		try {
			
			// Reads the first line.
			String line = HTTPStreamParsingUtil.readLine(in);
			String[] keys;

			// Checks that the input line is not empty.
			while (line != null && line.length() > 0) {

				// Splits the line to two where ":" occurs.
				keys = line.split(": ");

				/* Checks that there are two strings in the array.
				 * If not, header is not valid, we skip it and read next header
				 */
				if (keys.length == 2) {
					// Valid header, store it
					setHeader(keys[0].toLowerCase(), keys[1]);
				}

				// Reads the next line
				line = HTTPStreamParsingUtil.readLine(in);
			}
			
		} catch (IOException ex) {
			String exceptionString = "ERROR: could not parse headers" + ex.getMessage();
			System.err.println(exceptionString);
		}
	}

	/**
	 * Read the body of an HTTP message. {@link #readHeaders(HTTPStreamParsingUtil)}
	 * must be called before calling this method. Note that this method doesn't
	 * check whether there <i>is</i> a body to be read: it attempts to read one
	 * in any case. If there was a <code>Content-Length</code> header, this
	 * method should read exactly that many bytes from the input stream. If not,
	 * it will assume the entire remaining input is the body, and read until the
	 * end of the stream.
	 * 
	 * @param in - the {@link HTTPStreamParsingUtil} to read from.
	 * @return true if completed successfully, false otherwise.
	 * @throws EOFException - if the stream ends before the body was read completely
	 * @throws IOException - if there was an I/O error.
	 */
	protected boolean readBody(HTTPStreamParsingUtil in) throws IOException {

		// Handles the exception.
		try {
			String len = getHeader(HDR_CONTENT_LENGTH);
			byte[] body = null;

			// Checks if there was a Content-Length header
			if (len != null) {

				// Creates new byte array for the body in the length needed.
				body = new byte[Integer.parseInt(len)];
			} else {

				// Read until the end of the stream.
				body = new byte[in.available()];
			}

			// Reads the hole input stream and inserts to the body array.
			HTTPStreamParsingUtil.readAllLines(in,body);

			// Sets the body
			setBody(body);
			return true;
			
		} catch (Exception ex) {
			String exceptionString = "ERROR: could not the body" + ex.getMessage();
			System.err.println(exceptionString);
			return false;
		}
	}

	/**
	 * Write the message to a {@link PrintStream}. If the message has a body but
	 * no content-length header, one will be added.
	 * 
	 * @param out
	 */
	public void write(PrintStream out, boolean isChunked) throws IOException {

		if (isChunked) {
			
			// If is chunked. We remove "Content-Length" header
			setHeader(HDR_CONTENT_LENGTH, null);
		} else if (getHeader(HDR_CONTENT_LENGTH) == null) {
			
			// Content-Length value was null, we update it to the right value
			int length = 0;
			if (this.body != null && this.body.length > 0) {
				length = this.body.length;
			}

			// Set the body content-length value
			setHeader(HDR_CONTENT_LENGTH, String.valueOf(length));
		}

		// Iterates all headers and write them to stream
		for (String header : this.headers.keySet()) {
			out.print(header + ":  " + this.headers.get(header) + this.CRLF);
		}

		// Add another line terminator for the body part
		out.print(this.CRLF);

		// if it has a body, write the body to the stream.
		if (this.body != null) {
			
			// If chucked write chucked and if not, write fully.
			if (isChunked) {				
				
				byte[] buffer = new byte[1024];
				ByteArrayInputStream bodyInputStream = new ByteArrayInputStream(this.body);
				int bytes = 0;

				// Copy requested file into the sockets output stream.
				while ((bytes = bodyInputStream.read(buffer)) != -1) {

					// calculate the chunked size as HEX and write it to stream
					String chunkSize = Integer.toHexString(bytes) + this.CRLF;
					out.write(chunkSize.getBytes(), 0, chunkSize.getBytes().length);
					
					// write the buffer chunked we just filled
					out.write(buffer, 0, bytes);								
					out.write(this.CRLF.getBytes(), 0, this.CRLF.getBytes().length);
				}
				
				// Now end chunked data
				String endOfChucnk = "0" + this.CRLF;
				byte[] endOfChunk = endOfChucnk.getBytes();
				out.write(endOfChunk, 0, endOfChunk.length);
				
			} else {
				out.write(getBody());
			}
		}

		// Flushes the output stream.
		out.flush();
	}
}