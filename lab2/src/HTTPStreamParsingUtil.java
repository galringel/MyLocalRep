import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * This class get and InputStream from the client connection and:
 * - Parse each line
 * - Parse Fully untill the end
 * 
 * This class Extends FilterInputStream to support Stream Marking.
 * 
 * @author Omri Hering 026533067 & Gal Ringel 300922424
 *
 */
public class HTTPStreamParsingUtil extends FilterInputStream {
	
	/** For correct line parsing */
	static final char CR = '\r';
	static final char LF = '\n';
	
	/**
	 * Gets inputStream and wrap it in BufferInputStream for marking abilites
	 * @param in - input stream to wrap and activate markings
	 */
	public HTTPStreamParsingUtil(InputStream in) {
		super(in);
		
		// In order to support mark/reset by using BufferedInputStream
		if (!in.markSupported())
			this.in = new BufferedInputStream(in);
	}
	
	/**
	 * Reads a single line from the client inputStream
	 * @param inputStream inputStream to read from
	 * @return String of the line read
	 * @throws EOFException in case end-of-line is reached
	 * @throws IOException on IO error
	 */
	public static String readLine(InputStream inputStream) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		int currentByte = inputStream.read();
		boolean isCRFound = false;
		
		// making sure the first byte is CR or LF
		while (currentByte >= 0) 
		{
			switch (currentByte) 
			{
				case CR: {
					isCRFound = true;
					
					if (inputStream.markSupported()) {
						// we mark for later use in case its not a LF char. 
						inputStream.mark(1);
					}
					break;
				}
				case LF: {
					// We saw CRLF or just LF, so it's a new line for sure.
					return outputStream.toString();
				}
				default: {
					if (isCRFound) {
						if (inputStream.markSupported()) {
							// reposition to the last position we marked in the stream
							inputStream.reset();
						}
							
						return outputStream.toString();
					}
					
					// writes the bye to the stream
					outputStream.write(currentByte);
					break;
				}
			}
			
			// Read the next byte and continue
			currentByte = inputStream.read();
		}
		
		// We didn't find a CRLF, stream is not valid
		throw new EOFException("NO CRLF was found, Stream is not valid");
	}

	/**
	 * Reads all the data in the client InputStream into a big byte[]
	 *    
	 * @param inputStream the {@link InputStream} to read from
	 * @param inputStream inputStream to read from
	 * @throws EOFException in case end-of-line is reached
	 * @throws IOException on IO error
	 */
	public static void readAllLines(InputStream inputStream, byte[] buffer) throws IOException {
		
		int position = 0;
		while (position < buffer.length) {
			
			// fill the buffer
			int bytesRead = inputStream.read(buffer, position, buffer.length - position);
			
			// if bytesRead is empty, the stream was empty and it's not valid
			if (bytesRead < 0) {
				throw new EOFException("Stream was empty, not valid!");
			}
				
			// updates the position with the bytes read
			position += bytesRead;
		}
	}
}
