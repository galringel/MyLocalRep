import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A common base class for {@link HTTPRequest} and {@link HTTPResponse}.
 * 
 * @author talm
 * 
 */
public class HTTPMessage {

	public final static String HTTP_VERSION10 = "HTTP/1.0";
	public final static String HTTP_VERSION11 = "HTTP/1.1";

	final static String HDR_CONTENT_LENGTH = "Content-Length";

	/**
	 * The HTTP version used in the message.
	 */
	String version;

	/**
	 * Map from from headers to values. The keys are all lower case. Each key is
	 * mapped to the value of the last occurrence of that header.
	 */
	Map<String, String> headers;

	/**
	 * The message body. This is null if there is no body.
	 */
	byte[] body;

	/**
	 * Return the message body.
	 * 
	 * @return the message body, or null if the message did not have one.
	 */
	public byte[] getBody() {
		return body;
	}

	/**
	 * Set the body and change the content-length header if necessary.
	 * 
	 * @param body
	 *            the new body.
	 */
	public void setBody(byte[] body) {
		this.body = body;

		// Checks if the body valid
		if (body == null) {

			// Remove content-length header
			setHeader(HDR_CONTENT_LENGTH, null);
		} else {

			// Change content-length to new value
			setHeader(HDR_CONTENT_LENGTH, String.valueOf(body.length));
		}
	}

	/**
	 * Get a message header. The header name is case-insensitive.
	 * 
	 * @param header
	 *            the header name
	 * @return the header, or null if this message did not have the header.
	 */
	public String getHeader(String header) {
		return headers.get(header.toLowerCase());
	}

	/**
	 * 
	 * @param header
	 * @param value
	 * @return
	 */
	public String setHeader(String header, String value) {
		if (value == null)
			return headers.remove(header.toLowerCase());
		else
			return headers.put(header.toLowerCase(), value);
	}

	/**
	 * Get all the headers.
	 * 
	 * @return a map from header names to values.
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Return the number of headers.
	 * 
	 * @return the number of headers.
	 */
	public int getNumHeaders() {
		return headers.size();
	}

	protected HTTPMessage() {
		headers = new HashMap<String, String>();
	}

	/**
	 * The method HTTPMessage.readHeaders.This method reads headers,parses them
	 * and stores the values in the headers map. Each header line of the form:
	 * Header-Name: Header Value should be split into the key (Header-Name) and
	 * the value (Header Value).Only the last occurring value for each key is
	 * stored in the map. Note that the header names are case-insensitive, so
	 * convert them all to lower case before storing in the map.
	 */
	/**
	 * Read headers into the {@link #headers} map. Each line of the form:
	 * 
	 * <pre>
	 * Header-Name: Header Value
	 * </pre>
	 * 
	 * is split into the key (Header-Name) and the value (Header Value). Only
	 * the last occurring value for each key is stored in the map.
	 * 
	 * @param in
	 * @return true if completed successfully, false otherwise.
	 * @throws IOException
	 */
	protected boolean readHeaders(HTTPInputStream in) throws IOException {

		// Reads the first line.
		String line = HTTPInputStream.readLine(in);
		String[] keys;

		// Checks that the input line is not empty.
		while (line != null && line.length() > 0) {

			// Splits the line to two where ":" occurs.
			keys = line.split(": ");

			// Checks that there are two strings in the array.
			if (keys.length != 2) {
				return false;
			}

			// Inserts the key and value into the headers map.
			setHeader(keys[0].toLowerCase(), keys[1]);

			// Reads the next line
			line = HTTPInputStream.readLine(in);
		}
		return true;
	}

	/**
	 * Read the body of an HTTP message. {@link #readHeaders(HTTPInputStream)}
	 * must be called before calling this method. Note that this method doesn't
	 * check whether there <i>is</i> a body to be read: it attempts to read one
	 * in any case. If there was a <code>Content-Length</code> header, this
	 * method should read exactly that many bytes from the input stream. If not,
	 * it will assume the entire remaining input is the body, and read until the
	 * end of the stream.
	 * 
	 * @param in
	 *            the {@link HTTPInputStream} to read from.
	 * @return true if completed successfully, false otherwise.
	 * @throws EOFException
	 *             if the stream ends before the body was read completely
	 * @throws IOException
	 *             if there was an I/O error.
	 */
	protected boolean readBody(HTTPInputStream in) throws IOException {

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
			HTTPInputStream.readFully(in,body);

			// Sets the body
			setBody(body);
			return true;
		} catch (Exception e) {
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

		// Checks if the Content-Length header exists.
		if (getHeader(HDR_CONTENT_LENGTH) == null) {
			int length = 0;

			// Check the length of the body.
			if (body != null && body.length > 0) {
				length = body.length;
			}

			// Add a Content-Length header
			setHeader(HDR_CONTENT_LENGTH, String.valueOf(length));
		}
		
		if (isChunked) {
			
			// If is chunked. We remove "Content-Length" header
			setHeader(HDR_CONTENT_LENGTH, null);
		}

		// Writes out the headers.
		for (String header : headers.keySet()) {
			out.print(header + ":  " + headers.get(header) + "\r\n");
		}

		// Terminated by an empty line
		out.print("\r\n");

		// f it has a body, write the body to the PrintStream.
		if (body != null) {
			
			if (isChunked) {				
				byte[] buffer = new byte[1024];
				ByteArrayInputStream bodyInputStream = new ByteArrayInputStream(body);
				int bytes = 0;
				String CRLF = new String("\r\n");

				// Copy requested file into the sockets output stream.
				while ((bytes = bodyInputStream.read(buffer)) != -1) {

					// calculate the chunked size as HEX and write it to stream
					String chunkSize = Integer.toHexString(bytes) + "\r\n";
					out.write(chunkSize.getBytes(), 0, chunkSize.getBytes().length);
					
					// write the buffer chunked we just filled
					out.write(buffer, 0, bytes);								
					out.write(CRLF.getBytes(), 0, CRLF.getBytes().length);
				}
				
				// Now end chunked data
				
				String endOfChucnk = "0" + CRLF;
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