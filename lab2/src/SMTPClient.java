import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

/**
 * 
 * @author pringles
 *
 */
public class SMTPClient {

	private static Properties _properties;
	private static String _smtpHostname;
	private static int _smtpPort;
	private static String _smtpUsername;
	private static String _smtpPassword;
	private static String _localhostName;
	private static boolean _smtpIsAuthLogin;
	private final static String CRLF = "\r\n";
	private Socket _smtpSocket = null;
	private static BufferedReader _smtpInputStream;
	private static DataOutputStream _smtpOutputStream;
	
	// smtp variables
	private static String _lastSmtpResponse;
	private static String _lastSmtpResponseCode;
	
	/**
	 * 
	 */
	public SMTPClient () {
		try {
			parseConfigIni();
		} catch (Exception ex) {
			System.out.println("ERROR: parsing configuration failed! Exception: " + ex);
		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private static void parseConfigIni() throws Exception {
		_properties = new Properties();
		_properties.load(new FileInputStream("config.ini"));
		_smtpHostname = _properties.getProperty("SMTPName");
		_smtpPort = Integer.parseInt(_properties.getProperty("SMTPPort"));
		_smtpUsername = _properties.getProperty("SMTPUsername");
		_smtpPassword = _properties.getProperty("SMTPPassword");
		_smtpIsAuthLogin = Boolean.parseBoolean(_properties.getProperty("SMTPIsAuthLogin"));
		_localhostName = _properties.getProperty("ServerName");
	}
	
	/**
	 * 
	 */
	private void initSmtpStreams() {
		try {
			_smtpOutputStream = new DataOutputStream(_smtpSocket.getOutputStream());
			_smtpInputStream = new BufferedReader(new InputStreamReader(_smtpSocket.getInputStream()));
			
		} catch (IOException e) {
			System.err.println("Problem with server sockets initilization");
		}
    }

	/**
	 * 
	 */
	public boolean SendMail() {
		
		// connect to smtp server
		if (!connect()) {
			System.out.println("ERROR: Could not connect to remote smtp server!");
			return false;
		}

		if (_smtpSocket != null && _smtpOutputStream != null && _smtpInputStream != null) {
			try {
				
				// Reads the welcome message from the server
				getSmtpResponse();
				if (!_lastSmtpResponseCode.equals("220")) {
					System.out.println("ERROR: Didn't get a welcome message, could not connect to server!");
					return false;
				}
				
				if (_smtpIsAuthLogin) {
					// AUTH LOGIN IS NEEDED
					EHLO();
					
				} else {
					HELO();
				}

				// From here it's common for HELO or EHLO:
				MAIL_FROM();
				RCTP_TO();
				DATA();
				
				if (_lastSmtpResponse.startsWith("250 Queued")); {
					// "250 Queued (27.440 seconds)"
					System.out.println("Mail was sent sucessfully!");
				}
				
				QUIT();
				closeAllResources();
				
				// Mail was sent Successfully!
				return true;
				
			} catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
				closeAllResources();
				return false;
				
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
				closeAllResources();
				return false;
				
			} catch (Exception e) {
				System.err.println("IOException:  " + e);
				closeAllResources();
				return false;
			}
		}
		
		// Could not connect to server
		return false;
	}

	private boolean connect() {
		try {
			
			// Connecto to smtp server
			_smtpSocket = new Socket(_smtpHostname, _smtpPort);
			
			// We sleep for 2 sec to give the client a chance to connect (for slow Internet)
			Thread.sleep(2000);
			
			// Init input and output streams
			initSmtpStreams();
			return true;
		} catch (UnknownHostException ex) {
			System.err.println("ERROR: Unknown hostname");
			return false;
		} catch (IOException ex) {
			System.err.println("ERROR: Could not get I/O for the connection to the given hostname!");
			return false;
		} catch (InterruptedException e) {
			System.err.println("ERROR: Could not run Thread.sleep()!");
			return false;
		}
	}

	/**
	 * 
	 * @throws Exception
	 * @throws IOException
	 */
	private void HELO() throws Exception, IOException {
		sendSmtpCommand("HELO " + _localhostName);
		getSmtpResponse();
	}

	/**
	 * 
	 * @throws Exception
	 * @throws IOException
	 */
	private void QUIT() throws Exception, IOException {
		// Ends the connection
		sendSmtpCommand("QUIT");
		getSmtpResponse();
	}

	/**
	 * 
	 * @throws Exception
	 * @throws IOException
	 */
	private void RCTP_TO() throws Exception, IOException {
		String rcptTo = "ringel.gal@post.idc.ac.il";
		sendSmtpCommand("RCPT TO: " + rcptTo);
		getSmtpResponse();
	}

	/**
	 * 
	 * @throws Exception
	 * @throws IOException
	 */
	private void MAIL_FROM() throws Exception, IOException {
		String mailFrom = "ringel.gal@post.idc.ac.il";
		sendSmtpCommand("MAIL FROM: " + mailFrom);
		getSmtpResponse();
	}

	/**
	 * 
	 * @throws Exception
	 * @throws IOException
	 */
	private void DATA() throws Exception, IOException {
		sendSmtpCommand("DATA");
		getSmtpResponse();
		
		// Mail data
		sendSmtpCommand("Subject: testing");
		sendSmtpCommand("From: Mr.Tasker");
		sendSmtpCommand("To: ringel.gal@post.idc.ac.il");
		
		// Mail body
		sendSmtpCommand("tasker body");
		
		// By the RFC: <CR><LF>.<CR><LF>
		sendSmtpCommand("");
		sendSmtpCommand(".");
		sendSmtpCommand("");
		getSmtpResponse();
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 * @throws IOException
	 */
	private boolean EHLO() throws Exception, IOException {
		sendSmtpCommand("EHLO " + "OmriHering GalRingel");
		
		getSmtpResponse();
		while (!_lastSmtpResponse.equals("250 AUTH LOGIN")) {
			getSmtpResponse();
		}
		
		sendSmtpCommand("AUTH LOGIN");
		getSmtpResponse();
		
		String smtpUsernameAsBase64 = DatatypeConverter.printBase64Binary(_smtpUsername.getBytes());
		sendSmtpCommand(smtpUsernameAsBase64);
		getSmtpResponse();
		
		String smtpPasswordAsBase64 = DatatypeConverter.printBase64Binary(_smtpPassword.getBytes());
		sendSmtpCommand(smtpPasswordAsBase64);
		getSmtpResponse();
		
		if (_lastSmtpResponseCode.equals("535")) {
			// Authentication was failed:
			// "535 Authentication failed. Too many invalid logon attempts."
			// "535 Authentication failed."
			System.err.println("ERROR: Autentication was faild. closing everyting");
			closeAllResources();
			return false;
		} else if (_lastSmtpResponseCode.equals("235")) {
				// "235 authenticated."
				return true;
		} else {
			// Unknown response
			return false;
		}
	}
	
	/**
	 * clean up everything
	 */
	private void closeAllResources() {
		try {
			_smtpOutputStream.close();
			_smtpInputStream.close();
			_smtpSocket.close();
		} catch (IOException ex) {
			System.err.println("ERROR: Could not close streams or socket! Exception: " + ex);
		}
		   
	}
	
	/**
	 * Send a command and return the response
	 * @param command
	 * @return
	 * @throws Exception 
	 */
	private static void sendSmtpCommand(String command) throws Exception {
		
		try {
			_smtpOutputStream.writeBytes(command + CRLF);
			_smtpOutputStream.flush();
			
			// We sleep for 100 mili to make sure the command will arrive to server
			// before we continue
			Thread.sleep(100);
			
		} catch (IOException ex) {
			throw new Exception("ERROR: Could not read from input stream! Exception: " + ex);
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception 
	 */
	private static void getSmtpResponse() throws IOException {
		
		try {
			_lastSmtpResponse = _smtpInputStream.readLine();
			if (_lastSmtpResponse == null) {
				throw new IOException();
			}
			String[] responseLine = _lastSmtpResponse.split(" ");
			
			if (responseLine.length > 0) {
				_lastSmtpResponseCode = responseLine[0];
			}
			
			System.out.println(_lastSmtpResponse);
		} catch (IOException ex) {
			throw new IOException("ERROR: Could not read from input stream! Exception: " + ex);
		}
	}

	// FOR DEBUGING:
//	public static void main(String[] args) {
//
//		SMTPClient smtpC = new SMTPClient();
//		boolean success = smtpC.SendMail();
//		
//	}           
}
