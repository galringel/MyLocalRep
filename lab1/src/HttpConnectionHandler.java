import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

/**
 * This class handle a single connection:
 * It is also implements runnable because each connection is running in a different thread
 * Class action:
 * - Gets the inputStream from the connection and parse it
 * - Parse the inputStream into an HTTPRequest
 * - Decide what to do by the HTTP REQUEST type (GET,POST,HEAD,TRACE,OPTIONS) as requested
 * - Generate a proper HTTP RESPONSE to the client and write it to the stream
 * - On each bad request it generate 404
 * - BONUS: if you surf to /redirect it generates 301 to the client to "GET /"
 * - BONUS: On each response we generate a unique MachineId and Set-Cookie it.
 * 			On each request we search for this cookie and prints out if we found it.
 * 			That way we can manage users if we would like.
 * @author Omri Hering 026533067 & Gal Ringel 300922424
 *
 */
public class HTTPConnectionHandler implements Runnable {
	
	/* Client connection Socket */
	private Socket _connectionSocket;
	
	/* Default root and default page settings from config.ini */
	private String _defaultPage;
	private String _defaultRoot;

	/**
	 * Inits all relevant members
	 * @param connectionSocket
	 * @param defaultPage
	 * @param defaultRoot
	 * @throws Exception
	 */
	public HTTPConnectionHandler(Socket connectionSocket, String defaultPage, String defaultRoot) throws Exception {
		
		this._connectionSocket = connectionSocket;
		this._defaultPage = defaultPage;
		this._defaultRoot = defaultRoot;
	}
	
	/**
	 * Get a string to file path on server, and load it into byte[] for further use
	 * @param path - filename path on hard drive
	 * @return byte[] with the asked file
	 * @throws IOException, FileNotFoundException - if he doesn't find 
	 */
	private byte[] getFileBytes(String path) throws IOException, FileNotFoundException {
		
		File pageFile = new File(path);
		byte[] pageContent = null;
		
		// If file exists, read it into byte[]
		if (pageFile.exists()) {
			
		    FileInputStream fis = new FileInputStream(pageFile);
		    pageContent = new byte[(int)pageFile.length()];
		    fis.read(pageContent);
		    fis.close();
		}
		
		return pageContent;
	}

	@Override
	/**
	 * Each connectionHandler thread main function
	 */
	public void run() {
		
		HTTPRequest req = new HTTPRequest();
		HTTPStreamParsingUtil httpInputStream = null;
		PrintStream outputStream = null;
		
		// we always response as HTTP/1.1
		HTTPResponse response = new HTTPResponse(HTTPMessage.HTTP_VERSION11);
		
		try {
			
			// Some useful booleans
			boolean isOurServerCookieExists = false;
			boolean isChunked = false;
			
			// Gets the inputStream of the client connection
			httpInputStream = new HTTPStreamParsingUtil(_connectionSocket.getInputStream());
			
			// Parse the current request connection
			boolean isSuccedd = req.parseRequest(httpInputStream);
			if (!isSuccedd) {
				
				// Request REGEX was failed meaning request was invalid
				// We return "400 Bad Request" (server failed to parse/understand the request)"
				response.generate400();
				
			} else {
			
				// Check if our server cookie exists
				String cookieValue = req.getHeader("Cookie");
				if (cookieValue != null && cookieValue.contains("Machine-Id")) {
					
					// We already did set-cookie sometime, so we know this user and we print is machineId
					System.out.println("Welcome back! cookieValue: " + cookieValue);
					ServerLogger.getInstance().getLogger().info("Welcome back! cookieValue: " + cookieValue);
					isOurServerCookieExists = true;
				}
				
				// CleanURL: WE DONT SUPPORT URLS WITH "../"
				String uriBeforeCleaned = req.path.replaceAll("../", "");
				req.path = uriBeforeCleaned;
				
				// hold the request path on our server
				String pagePath = _defaultRoot + req.path;
				
				byte[] contentBytes = null;
				
				// Logs the GET header
				System.out.println(req.getRequestTitle());
				ServerLogger.getInstance().getLogger().info(req.getRequestTitle());
				
				if (req.method.equals(HTTPRequest.Method.GET) ||
						req.method.equals(HTTPRequest.Method.HEAD)) 
				{
					// Deals with transfer-encoding: chunked
					String chunkedHeader = req.headers.get("transfer-encoding");
					if (chunkedHeader != null && chunkedHeader.equals("chunked")) {
						isChunked = true;
						response.setHeader("Transfer-Encoding", "chunked");
					}
					
					// Set our server cookie in case it not exists
					if (!isOurServerCookieExists) {
						String machineId = response.generateMachineId();
						response.setCookie("Machine-Id", machineId);
					}
					
					// default GET /
					if (req.path.equals("/")) {
						contentBytes = getFileBytes(pagePath + _defaultPage);
						response.setHeader("content-type", "text/html");
						response.generate200OK(contentBytes);
					}
					else if ((req.path.endsWith(".htm") || 
							req.path.endsWith(".html")) && req.params.size() == 0) {				
						contentBytes = getFileBytes(pagePath);
						response.setHeader("content-type", "text/html");
						response.generate200OK(contentBytes);
					} 
					else if (req.path.endsWith(".jpg") || req.path.endsWith(".jpeg") ||
							req.path.endsWith(".bmp") || req.path.endsWith(".gif") ||
							req.path.endsWith(".png") || req.path.equals("/favicon.ico")) {
						
						contentBytes = getFileBytes(pagePath);
						response.generate200OK(contentBytes);
						response.setHeader("content-type", "image");
						
					} else if (req.params.size() > 0) {
						 
						//POST request in method=GET format	case					
						byte[] responseBytes = response.generatePOST_HTML(req.params).getBytes();
						response.generate200OK(responseBytes);
						
					} else if (req.path.equals("/redirect")) {
						// BONUS: redirection
						response.generate301("/");
						response.setHeader("content-length", null);
					} else {
						
						// All other cases
						contentBytes = getFileBytes(pagePath);
						response.setHeader("content-type", "application/octet-stream");
						response.generate200OK(contentBytes);
						
					}
				} 
				else if (req.method.equals(HTTPRequest.Method.POST)) {
					if (req.params.size() > 0) {
						// POST is fine and has parameters						
						byte[] responseBytes = response.generatePOST_HTML(req.params).getBytes();
						response.setHeader("content-type", "text/html");
						response.generate200OK(responseBytes);
					}
				} 
				else if (req.method.equals(HTTPRequest.Method.OPTIONS)) {
					String allowHeader = response.generateAllowHeader();
					response.setStatusCode(200, HTTPResponse.STATUS_200);
					response.setHeader("allow", allowHeader);
				} 
				else if (req.method.equals(HTTPRequest.Method.TRACE)) {
					byte[] responseBytes = response.generateTRACE_HTML(req.headers, req.requestTitle).getBytes();
					response.setHeader("content-type", "text/html");
					response.generate200OK(responseBytes);
				} else {
					// HTTPRequest.Method.OTHER, Not supported
					response.generate501();
				}
				
				// Logs the HTTP Response line
				System.out.print(response.buildResponseFirstHeader());
				ServerLogger.getInstance().getLogger().info(response.buildResponseFirstHeader());
				
				// If it was HEAD request, we delete response body.
				if (req.method.equals(HTTPRequest.Method.HEAD)) {
					response.setBody(null);
				}
			}
			
			// Writes the response to the client connection output stream socket
			outputStream = new PrintStream(_connectionSocket.getOutputStream());
			response.write(outputStream, isChunked);			
			outputStream.close();
		}
		catch (FileNotFoundException ex) {	
			CloseAllResources(response, outputStream, ex);
		} 
		catch (IOException ex) {	
			CloseAllResources(response, outputStream, ex);
		}
	}
	
	/**
	 * In case of an error, we log it and closes everything
	 * @param response
	 * @param outputStream
	 * @param ex
	 */
	void CloseAllResources(HTTPResponse response, PrintStream outputStream, Exception ex) {
		
		// Logs the exception
		String exceptionString = "ERROR: some error occured! closing connection..." + ex.getMessage();
		System.err.println(exceptionString);
		ServerLogger.getInstance().getLogger().info(exceptionString);
		
		// Generate 500 response and closes the stream
		response.generate500();
		outputStream.close();
		
		try {
			_connectionSocket.close();
		} catch (IOException ex1) {
			
			exceptionString = "ERROR: Could not close the connection proerply: " + ex1.getMessage();
			System.err.println(exceptionString);
			ServerLogger.getInstance().getLogger().info(exceptionString);
		}
	}
}
