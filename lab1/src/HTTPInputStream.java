import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A class that can read single lines and byte arrays (rather than char arrays like {@link BufferedReader} does).
 * This is a {@link FilterInputStream}, meaning that it can "wrap" any {@link InputStream} passed to the constructor.
 * @author talm
 *
 */
public class HTTPInputStream extends FilterInputStream {
	/**
	 * Constructor. Reading from the HTTPInputStream will eventually cause a read from the wrapped InputStream.
	 * If the passed InputStream does not support mark/reset, we will wrap it in a BufferedInputStream that does.
	 * 
	 * @param in the {@link InputStream} to wrap. 
	 * 
	 */
	public HTTPInputStream(InputStream in) {
		super(in);
		
		// Make sure we always support mark/reset by 
		// wrapping the input in a BufferedInputStream if
		// it doesn't support it already.
		if (!in.markSupported())
			this.in = new BufferedInputStream(in);
	}
	
	/**
	 * Read a single line from an InputStream.
	 * This method reads until it finds a \r\n (CR-LF) sequence, as required by the HTTP specification. 
	 * We use this rather than {@link BufferedReader#readLine()} because we need to be able
	 * to read a specific number of bytes for the body (and {@link BufferedReader} only handles chars).
	 * 
	 * If the InputStream supports mark/reset, the method will be more tolerant of non-standard line endings:
	 * lines that end with just a CR or just a LF will be parsed correctly. 
	 * It is highly recommended to use a <i>buffered</i> InputStream as the input to this method
	 * @param in the {@link InputStream} to read from.
	 * @return the line read, without the terminator.
	 * @throws EOFException if the stream ends before an end-of-line is reached.
	 * @throws IOException if there is an IO error.
	 */
	public static String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		int b = in.read();
		boolean foundCR = false;
		while (b >= 0) {
			switch (b) {
			case '\r':
				foundCR = true;
				if (in.markSupported())
					// we put a mark in the inputstream so that we can "put back" a following byte 
					// if it is not an LF character.
					in.mark(1);  
				break;
			case '\n':
				// We read a complete CR-LF sequence or just an LF; in either case we'll consider it a line.
				return out.toString();
			default:
				if (foundCR) {
					// We found a CR but not an LF. We'll allow it anyway.
					if (in.markSupported())
						// We read an extra character from the next line, so we reset if possible.
						in.reset();
					return out.toString();
				}
				out.write(b);
				break;
			}
			b = in.read();
		}
		// We didn't find a CR-LF and the InputStream ended!
		throw new EOFException();
	}

	/**
	 * See the general contract of {@link DataInput#readFully(byte[])}.
	 * The bytes are read from the {@link InputStream} in.   
	 * @param in the {@link InputStream} to read from
	 * @param buf the buffer to read into.
	 * @throws EOFException if the stream ends before the buffer was filled.
	 * @throws IOException if there is an I/O Error.
	 */
	public static void readFully(InputStream in, byte[] buf) throws IOException {
		int pos = 0;
		
		while (pos < buf.length) {
			int len = in.read(buf, pos, buf.length - pos);
			if (len < 0)
				throw new EOFException();
			pos += len;
		}
	}

}
