import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.Properties;

/**
 * 
 * @author pringles
 *
 */
public final class WebServer {

	/* Global Variables */
	private static int _maxThreads;
	private static int _serverPort;
	private static ServerSocket _serverSocket;
	private static Properties _properties;
	public static String _rootFolder;
	public static String _defaultPage;

	
	public static void main(String argv[]) {

		try {

			//System.out.println(System.getProperty("user.dir"));
			
			// Parse config ini file
			parseConfigIni();

		} catch (NumberFormatException ex) {
			System.err.println("Problem Initiazlie parameters. Exception details: " + ex.getMessage());
			return;

		} catch (FileNotFoundException ex) {
			System.err.println("config.ini file missing. Exception details: " + ex.getMessage());
			return;

		} catch (IOException ex) {
			System.err.println("Problem parsing config.ini. Exception details: " + ex.getMessage());
			return;

		} catch (Exception ex) {
			System.err.println("Unknown Exception was cought. Exception details: " + ex.getMessage());
			return;
		}

		try {
			_serverSocket = new ServerSocket(_serverPort, _maxThreads);
			System.err.println("Server is up and running on port: " + _serverPort);

		} catch (IOException ex) {
			System.out.println("Can't listen to Socket. Exception details: " + ex.getMessage());
			return;
		}
		
		while (true) {

			try {
				handleConnections();

			} catch (Exception ex) {
				System.out.println("A Problem occured with the server. Exception Details: " + ex.getMessage());
			}
		}
	}

	private static void parseConfigIni() throws FileNotFoundException, 
	IOException, NumberFormatException {

		_properties = new Properties();
		_properties.load(new FileInputStream("/Users/pringles/Code/lab1/src/config.ini"));

		_maxThreads = Integer.parseInt(_properties.getProperty("maxThreads"));
		_serverPort = Integer.parseInt(_properties.getProperty("port"));
		_rootFolder = (_properties.getProperty("root"));
		_defaultPage = (_properties.getProperty("defaultPage"));
	}


	private static void handleConnections() throws Exception {
		
		// Listen for a TCP connection request.
		Socket connectionSocket = _serverSocket.accept();

		// For Debug:
		System.out.println("A new connection Accepted");
		
		HttpConnectionHandler httpRequestHandler = new HttpConnectionHandler(connectionSocket, _defaultPage, _rootFolder);
		Thread thread = new Thread(httpRequestHandler);
		thread.start();
	}
}