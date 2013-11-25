import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

/**
 * 
 * @author 
 *
 */
public class HttpConnectionHandler implements Runnable {
	
	private Socket _connectionSocket;
	private String _defaultPage;
	private String _defaultRoot;

	/**
	 * 
	 * @param connectionSocket
	 * @param defaultPage
	 * @param defaultRoot
	 * @throws Exception
	 */
	public HttpConnectionHandler(Socket connectionSocket, String defaultPage, String defaultRoot) throws Exception {
		
		this._connectionSocket = connectionSocket;
		this._defaultPage = defaultPage;
		this._defaultRoot = defaultRoot;
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 * @throws IOException, FileNotFoundException 
	 */
	private byte[] getFileBytes(String path) throws IOException, FileNotFoundException {
		
		File pageFile = new File(path);
		byte[] pageContent = null;
		if (pageFile.exists()) {
			
		    FileInputStream fis = new FileInputStream(pageFile);
		    pageContent = new byte[(int)pageFile.length()];
		    fis.read(pageContent);
		    fis.close();
		}
		
		return pageContent;
	}

	@Override
	public void run() {
		
		HTTPRequest req = new HTTPRequest();
		HTTPInputStream httpInputStream = null;
		PrintStream outputStream = null;
		HTTPResponse response = new HTTPResponse(HTTPMessage.HTTP_VERSION11);
		
		try {
			
			boolean isChunked = false;
			httpInputStream = new HTTPInputStream(_connectionSocket.getInputStream());
			
			// Parse the current request
			boolean isSuccedd = req.parse(httpInputStream);
			if (!isSuccedd) {
				
				// Request REGEX was failed meaning request was invalid
				// We return "400 Bad Request" (server failed to parse/understand the request)"
				response.generate400();
				
			} else {
			
				// hold the request path on our server
				String pagePath = _defaultRoot + req.path;		
				byte[] contentBytes = null;
				
				// Print GET header
				System.out.println(req.getRequestTitle());
				
				if (req.method.equals(HTTPRequest.Method.GET) ||
						req.method.equals(HTTPRequest.Method.HEAD)) 
				{
					// Deals with transfer-encoding: chunked
					String chunkedHeader = req.headers.get("transfer-encoding");
					if (chunkedHeader != null && chunkedHeader.equals("chunked")) {
						isChunked = true;
						response.setHeader("Transfer-Encoding", "chunked");
					}
					
					// request Router
					if (req.path.equals("/")) {
						contentBytes = getFileBytes(pagePath + _defaultPage);
						response.generate200OK(contentBytes);
					}
					else if ((req.path.endsWith(".htm") || 
							req.path.endsWith(".html")) && req.params.size() == 0) {				
						contentBytes = getFileBytes(pagePath);
						response.generate200OK(contentBytes);
					} 
					else if (req.path.endsWith(".jpg") || req.path.endsWith(".jpeg") ||
							req.path.endsWith(".bmp") || req.path.endsWith(".gif") ||
							req.path.endsWith(".png") || req.path.equals("/favicon.ico")) {
						
						contentBytes = getFileBytes(pagePath);
						response.generate200OK(contentBytes);
						response.setHeader("content-type", "image");
						
					} else if (req.params.size() > 0) {
						 
						//POST request in method=GET format						
						byte[] responseBytes = response.generatePOST_HTML(req.params).getBytes();
						response.generate200OK(responseBytes);
						
					} else {
						
						contentBytes = getFileBytes(pagePath);
						response.generate200OK(contentBytes);
						response.setHeader("content-type", "application/octet-stream");
					}
				} 
				else if (req.method.equals(HTTPRequest.Method.POST)) {
					if (req.params.size() > 0) {
						// POST is fine has parameters						
						byte[] responseBytes = response.generatePOST_HTML(req.params).getBytes();
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
					response.generate200OK(responseBytes);
				} else {
					// HTTPRequest.Method.OTHER, Not supported
					response.generate501();
				}
				
				System.out.println(response.buildResponseFirstHeader());
				
				// If it was HEAD request, we delete response body.
				if (req.method.equals(HTTPRequest.Method.HEAD)) {
					response.setBody(null);
				}
			}
			
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
	 * 
	 * @param response
	 * @param outputStream
	 * @param ex
	 */
	void CloseAllResources(HTTPResponse response, PrintStream outputStream, Exception ex) {
		System.err.println(ex.getMessage());			
		response.generate500();
		outputStream.close();
		try {
			_connectionSocket.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
}
