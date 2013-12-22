import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is the main class:
 * - Parse the config.ini file with all the relevant fields
 * - BONUS: Create a ThreadPool and BlockingQueue for the incoming connections
 * - Creates a ServerSocket
 * - Handles incoming connections
 * 
 * @author Omri Hering 026533067 & Gal Ringel 300922424
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
	public static String workingDir;
	public static boolean _ativateLogger;
	
	/** The queue of pending all the clients requests. */
	private static BlockingQueue<Runnable> blockingQueue;
	
	/** An executor handling all the clients requests. */
	private static Executor threadPool;

	
	/**
	 * 
	 * @param argv
	 */
	public static void main(String argv[]) {		
		
		try {
			// Parse config ini file
			parseConfigIni();
			System.out.println("LOGGER: config.ini was loaded successfully");
			
			// BONUS: If config.ini set the logger to be on, we create it
			ServerLogger.getInstance().getLogger().info("LOGGER: config.ini was loaded successfully");
			
			// Init the blocking queue and the thread pool
			blockingQueue = new LinkedBlockingQueue<Runnable>();
			threadPool = new ThreadPoolExecutor(_maxThreads, _maxThreads, Integer.MAX_VALUE, TimeUnit.MILLISECONDS, blockingQueue);

		} catch (NumberFormatException ex) {
			String exceptionString = "ERROR: Problem Initiazlie parameters. Exception details: " + ex.getMessage();
			System.err.println(exceptionString);
			ServerLogger.getInstance().getLogger().info(exceptionString);
			return;
		} catch (FileNotFoundException ex) {
			String exceptionString = "ERROR: config.ini file missing! Please place it in the \"src\\\" folder of the project. " +
					"\nException details: " + ex.getMessage();
			System.err.println(exceptionString);
			ServerLogger.getInstance().getLogger().info(exceptionString);
			return;
		} catch (IOException ex) {
			String exceptionString = "ERROR: Problem parsing config.ini. Exception details: " + ex.getMessage(); 
			System.err.println(exceptionString);
			ServerLogger.getInstance().getLogger().info(exceptionString);
			return;
		} catch (Exception ex) {
			String exceptionString = "ERROR: General Exception was cought. Exception details: " + ex.getMessage();
			System.err.println(exceptionString);
			ServerLogger.getInstance().getLogger().info(exceptionString);
			return;
		}
		try {
			_serverSocket = new ServerSocket(_serverPort, _maxThreads);	
			// Logs
			String msg = "Server is up and running on port: " + _serverPort;
			System.err.println(msg);
			ServerLogger.getInstance().getLogger().info(msg);

		} catch (IOException ex) {
			String exceptionString = "ERROR: Can't listen to Socket. Exception details: " + ex.getMessage();
			System.err.println(exceptionString);
			ServerLogger.getInstance().getLogger().info(exceptionString);
			return;
		} catch (Exception ex) {
			String exceptionString = "ERROR: Can't listen to Socket. Exception details: " + ex.getMessage();
			System.err.println(exceptionString);
			ServerLogger.getInstance().getLogger().info(exceptionString);
			return;
		}
		
		while (true) {

			try {
				
				// Accept incoming connection
				Socket connectionSocket = _serverSocket.accept();
				System.out.println("A new connection accepted succesfully!");
				ServerLogger.getInstance().getLogger().info("A new connection accepted succesfully!");
				
				// Create a new connectionHandler
				HTTPConnectionHandler currentClient = new HTTPConnectionHandler(connectionSocket, _defaultPage, _rootFolder);
				
				// Send to thread pool and execute.
				threadPool.execute(currentClient);
				
				/* FOR DEBUG:
				* If we passed _maxThreads, new connections will be added to this queue,
				* and will be printed to console in order to know how many pending connections there are */
				System.out.println("Current Queue Size: " + blockingQueue.size());
				ServerLogger.getInstance().getLogger().info("Current Queue Size: " + blockingQueue.size());

			} catch (Exception ex) {
				
				String exceptionString = "ERROR: Could not accept a new connection. waiting for a new one...\n" + 
						"Exception Details: " + ex.getMessage();
				System.err.println(exceptionString);
				ServerLogger.getInstance().getLogger().info(exceptionString);
			}
		}
	}

	/**
	 * 
	 * @throws Exception
	 */
	private static void parseConfigIni() throws Exception {

		_properties = new Properties();
		_properties.load(new FileInputStream("config.ini"));

		_maxThreads = Integer.parseInt(_properties.getProperty("maxThreads"));
		if (_maxThreads < 1 || _maxThreads > 10) {
			throw new Exception("maxThread must be between 1-10");
		}
		
		_serverPort = Integer.parseInt(_properties.getProperty("port"));
		_rootFolder = (_properties.getProperty("root"));
		_defaultPage = (_properties.getProperty("defaultPage"));
	}
}