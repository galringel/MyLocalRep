import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Logger class for our server
 * This class is a singletone and created only once
 * @author 
 *
 */

/**
 * Logger class for our c00l and n33t server :) 
 * 
 * - We implemented this class to be Singleton, in order to be created once.
 *   because all the classes uses it and we don't want to have many instances in our code
 *   
 * - Every time the ServerSocket goes up, it creates a new log file with the current datetime.
 * - We log all the errors, info messages and the request and response first lines
 * 
 * - The log is saved automatically to the working directory of the .java files
 * 
 * @author @author Omri Hering 026533067 & Gal Ringel 300922424
 * 
 */
public class ServerLogger {

	private Logger logger;
	private FileHandler fh;	
	private static ServerLogger instance = null;
	
	protected ServerLogger() {
		Init("MyLogger");
	}
	
	/**
	 * 
	 * @return
	 */
	public static ServerLogger getInstance() {
		if (instance == null) {
			instance = new ServerLogger();
		}
		
		return instance;
	}
	
	/**
	 * 
	 * @param loggerName
	 */
	private void Init(String loggerName) {
		
		this.logger = Logger.getLogger(loggerName);	
		
		try {
			this.fh = new FileHandler(loggerName + "[" + generateLoggerDate() + "].log");
		} catch (SecurityException ex) {
			System.err.println("ERROR: Exception:" + ex.getMessage());
		} catch (IOException ex) {
			System.err.println("ERROR: Exception:" + ex.getMessage());
		}  
		
        this.logger.addHandler(this.fh);
        SimpleFormatter formatter = new SimpleFormatter();  
        this.fh.setFormatter(formatter); 
	}
	
	
	/**
	 * 
	 * @return
	 */
	public String generateLoggerDate() {
		
		Date date = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss"); 
		return simpleDateFormat.format(date);
	}
	
	/**
	 * 
	 * @return
	 */
	public Logger getLogger() {
		return this.logger;
	}
	
	/**
	 * 
	 */
	public void terminateLogger() {
		this.fh.close();
	}
}
