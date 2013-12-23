import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;

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
	//private String _defaultPage;
	private String _defaultRoot;

	/**
	 * Initializes all relevant members
	 * @param connectionSocket
	 * @param defaultPage
	 * @param defaultRoot
	 * @throws Exception
	 */
	public HTTPConnectionHandler(Socket connectionSocket, String defaultPage, String defaultRoot) throws Exception {
		
		this._connectionSocket = connectionSocket;
		//this._defaultPage = defaultPage;
		this._defaultRoot = defaultRoot;
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
			
				// CleanURL: WE DONT SUPPORT URLS WITH "../"
				String uriBeforeCleaned = req.path.replaceAll("../", "");
				req.path = uriBeforeCleaned;
				
				// hold the request path on our server
				String pagePath = _defaultRoot;
				
				// Logs the GET header
				System.out.println(req.getRequestTitle());
				
				if (req.method.equals(HTTPRequest.Method.GET) ||
						req.method.equals(HTTPRequest.Method.HEAD)) {
					
					// Checking whether chunked is needed
					isChunked = isChunkedNeeded(req, response, isChunked);
					handleGetRequest(req, response, isChunked, pagePath);
				}
				else if (req.method.equals(HTTPRequest.Method.POST)) {
					handlePostRequest(req, response);
				} 
				else if (req.method.equals(HTTPRequest.Method.OPTIONS)) {
					handleOptionsRequest(response);
				} 
				else if (req.method.equals(HTTPRequest.Method.TRACE)) {
					handleRequestTrace(req, response);
				} else {
					// HTTPRequest.Method.OTHER, Not supported
					response.generate501();
				}
				
				// Logs the HTTP Response line
				System.out.print(response.buildResponseFirstHeader());
				
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
			closeAllResources(response, outputStream, ex);
		} 
		catch (IOException ex) {	
			closeAllResources(response, outputStream, ex);
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param response
	 */
	private void handleRequestTrace(HTTPRequest req, HTTPResponse response) {
		byte[] responseBytes = response.generateTRACE_HTML(req.headers, req.requestTitle).getBytes();
		response.setHeader("content-type", "text/html");
		response.generate200OK(responseBytes);
	}

	/**
	 * 
	 * @param response
	 */
	private void handleOptionsRequest(HTTPResponse response) {
		String allowHeader = response.generateAllowHeader();
		response.setStatusCode(200, HTTPResponse.STATUS_200);
		response.setHeader("allow", allowHeader);
	}

	/**
	 * 
	 * @param req
	 * @param response
	 */
	private void handlePostRequest(HTTPRequest req, HTTPResponse response) {
		if (req.params.size() > 0) {
			// POST is fine and has parameters						
			byte[] responseBytes = response.generatePOST_HTML(req.params).getBytes();
			response.setHeader("content-type", "text/html");
			response.generate200OK(responseBytes);
		}
	}

	/**
	 * 
	 * @param req
	 * @param response
	 * @param isChunked
	 * @param pagePath
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void handleGetRequest(HTTPRequest req, HTTPResponse response, boolean isChunked, String pagePath) 
			throws IOException, FileNotFoundException {
		
		boolean isOurServerCookieExists = false;
		String currentLoggedUsername = null;
		byte[] contentBytes;
		
		ReminderDatabase reminderDatabase = ReminderDatabase.getInstance(_defaultRoot);
		
		// Searching for our cookie
		String cookieValue = req.getHeader("Cookie");
		if (cookieValue != null && cookieValue.contains("usermail")) {
			
			// We already did set-cookie sometime, so we know this user and we print is email
			String[] splittedCookie = cookieValue.split("=");
			if (splittedCookie.length > 1) {
				
				currentLoggedUsername = splittedCookie[1];				
				if (!currentLoggedUsername.equals("")) {
					// There is a valid cookie
					System.out.println("Welcome back! User: " + currentLoggedUsername);
					isOurServerCookieExists = true;
				}
			}
		}
		
		if (req.path.startsWith("/index.html")) {
			
			if (req.params.size() > 0) {
				// meaning we have params, we need to set cookie
				String submittedEmailAddress = req.params.get("email");
				
				// Set our server cookie in case it not exists
				if (!isOurServerCookieExists) {
					response.setCookie("usermail", submittedEmailAddress);
					response.generate301("index.html");
					response.setHeader("content-length", null);
				}
			} else {
				if (isOurServerCookieExists) {
					// cookie exists, we just redirect
					response.generate301("main.html");
					response.setHeader("content-length", null);
				} else {
					contentBytes = HTTPUtils.getFileBytes(pagePath + req.path);
					response.setHeader("content-type", "text/html");
					response.generate200OK(contentBytes);
				}	
			}
		} else if (req.path.equals("/main.html")) {
			if (isOurServerCookieExists) {
				
				// Cookie exists, we show main.html page
				contentBytes = HTTPUtils.getFileBytes(pagePath + req.path);
				response.setHeader("content-type", "text/html");
				response.generate200OK(contentBytes);
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
		} else if (req.path.equals("/logout.html")) {
			
			// we need to erase the cookie
			response.setCookie("usermail", "");
			
			// redirect back to index.html
			response.generate301("index.html");
			response.setHeader("content-length", null);
			
		} else if (req.path.endsWith(".jpg") || req.path.endsWith(".jpeg") ||
				req.path.endsWith(".bmp") || req.path.endsWith(".gif") ||
				req.path.endsWith(".png") || req.path.equals("/favicon.ico")) {
			
			contentBytes = HTTPUtils.getFileBytes(pagePath + req.path);
			response.generate200OK(contentBytes);
			response.setHeader("content-type", "image");
			
		} else if (req.path.equals("/reminders.html")) {
				
			if (isOurServerCookieExists) {
				// Cookie exists, we show reminders.html page
				
				ArrayList<ReminderInfo> reminders = reminderDatabase.getRemindersInfoDB().get(currentLoggedUsername);
				String reminderHTMLBody = Reminder.BuildReminderHTML(currentLoggedUsername, reminders);
				
				response.setHeader("content-type", "text/html");
				response.generate200OK(reminderHTMLBody.getBytes());
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
			
		} else if (req.path.equals("/reminder_editor.html")) {
			
			if (isOurServerCookieExists) {
				// Cookie exists, we show reminders.html page
		
				ArrayList<ReminderInfo> reminders = reminderDatabase.getRemindersInfoDB().get(currentLoggedUsername);
				String reminderEditorHTML = Reminder.BuildReminderEditorHTML(currentLoggedUsername, reminders);
				response.setHeader("content-type", "text/html");
				response.generate200OK(reminderEditorHTML.getBytes());
				
				
				//response.setHeader("content-type", "text/html");
				//response.generate200OK(reminderHTMLBody.getBytes());
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
			
		} else {
			
			// In all other pages we do:
			allOtherPages(response, isOurServerCookieExists);
		}
	}

	private void allOtherPages(HTTPResponse response,
			boolean isOurServerCookieExists) {
		if (isOurServerCookieExists) {
			// cookie exists, we just redirect
			response.generate301("main.html");
			response.setHeader("content-length", null);
		} else {
			
			// cookie does not exists we redirect to index.html
			response.generate301("index.html");
			response.setHeader("content-length", null);
		}
	}

	/**
	 * 
	 * @param req
	 * @param response
	 * @param isChunked
	 * @return
	 */
	private boolean isChunkedNeeded(HTTPRequest req, HTTPResponse response,
			boolean isChunked) {
		// Deals with transfer-encoding: chunked
		String chunkedHeader = req.headers.get("transfer-encoding");
		if (chunkedHeader != null && chunkedHeader.equals("chunked")) {
			isChunked = true;
			response.setHeader("Transfer-Encoding", "chunked");
		}
		return isChunked;
	}
	
	/**
	 * In case of an error, we log it and closes everything
	 * @param response
	 * @param outputStream
	 * @param ex
	 */
	void closeAllResources(HTTPResponse response, PrintStream outputStream, Exception ex) {
		
		// Logs the exception
		String exceptionString = "ERROR: some error occured! closing connection..." + ex.getMessage();
		System.err.println(exceptionString);
		
		// Generate 500 response and closes the stream
		response.generate500();
		outputStream.close();
		
		try {
			_connectionSocket.close();
		} catch (IOException ex1) {
			
			exceptionString = "ERROR: Could not close the connection proerply: " + ex1.getMessage();
			System.err.println(exceptionString);
		}
	}
}
