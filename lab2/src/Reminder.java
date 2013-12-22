import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * @author pringles
 * 
 */
public class Reminder {
	
	/**
	 * Holds a single reminderInfo
	 * @author pringles
	 *
	 */
	public class ReminderInfo {
		
		public ReminderInfo() {
			//EMpty
		}
		
		public void set_reminderId(String _reminderId) {
			this._reminderId = _reminderId;
		}

		public void set_reminderTitle(String _reminderTitle) {
			this._reminderTitle = _reminderTitle;
		}

		public void set_reminderCreationDate(Date _reminderCreationDate) {
			this._reminderCreationDate = _reminderCreationDate;
		}

		public void set_reminderSendingDate(Date _reminderSendingDate) {
			this._reminderSendingDate = _reminderSendingDate;
		}

		public String get_reminderId() {
			return _reminderId;
		}

		public String get_reminderTitle() {
			return _reminderTitle;
		}

		public Date get_reminderCreationDate() {
			return _reminderCreationDate;
		}

		public Date get_reminderSendingDate() {
			return _reminderSendingDate;
		}

		private String _reminderId;
		private String _reminderTitle;
		private Date _reminderCreationDate;
		private Date _reminderSendingDate;
		
		public ReminderInfo(String reminderId, String reminderName, String reminderTitle, Date reminderCreationDate, Date reminderSendingDate) {
			_reminderId = reminderId;
			_reminderTitle = reminderTitle;
			_reminderCreationDate = reminderCreationDate;
			_reminderSendingDate = reminderSendingDate;
		}	
	}

	//public ArrayList<ReminderInfo> remindersInfo = new ArrayList<>()
	public Reminder(String rootFolder, String loggedUsername) {

		String reminderXmlPath = null;
		try {
			reminderXmlPath = HTTPUtils.LoadDatabaseXmlNameFromConfig("reminderFilePath");
			String xmlFilename = rootFolder + "/" + reminderXmlPath;
			LoadDatabaseXml(xmlFilename, loggedUsername);
		} catch (IOException ex) {
			System.err.println("ERROR: " + ex.getMessage());
		}
	}
	
	private void LoadDatabaseXml(String databaseXML, String loggedUsername) {
		try {
			
			File fXmlFile = new File(databaseXML);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			
			Properties xmlProperties = new Properties();
			xmlProperties.loadFromXML(new FileInputStream(databaseXML));
			
			System.out.println("DEBUG");
			
//			Element rootElement = doc.getDocumentElement();
//			Element reminderElement = HTTPUtils.getElementByAttributeValue(rootElement, loggedUsername);
//			NodeList nodes = reminderElement.getElementsByTagName("reminderInfo");
//			
//			// Iterates reminderInfo
//			for (int temp = 0; temp < nodes.getLength(); temp++) {
//				ReminderInfo reminderInfo = new ReminderInfo();
//				Node nNode = nodes.item(temp);
//				System.out.println("\nElement Name:" + nNode.getNodeName());
//				
//				NamedNodeMap nodeAttributes = nNode.getAttributes();
//				Attr attribute = (Attr)nodeAttributes.item(0);
//				System.out.println("\nAttribute Name: " + attribute.getName() + " Attribute Value: " + attribute.getValue());
//				reminderInfo.set_reminderId(attribute.getValue());
//				
//				if (nNode.hasChildNodes()) {
//					NodeList childNodes = nNode.getChildNodes();
//					
//					// Iterates reminderInfo child elements
//					for (int j=0; j< childNodes.getLength(); j++) {
//						
//						Node reminderChild = childNodes.item(j);
//						switch (reminderChild.getNodeName()) {
//							case "reminderTitle": {
//								reminderInfo.set_reminderTitle(reminderChild.getNodeValue());
//								break;
//								}
//							case "reminderCreationDate": {
//								reminderInfo.set_reminderCreationDate(new Date(reminderChild.getNodeValue()));
//								break;
//								}
//							case "reminderSubject": {
//								reminderInfo.set_reminderTitle(reminderChild.getNodeValue());
//								break;
//							}
//							case "reminderSendingDate": {
//								reminderInfo.set_reminderSendingDate(new Date(reminderChild.getNodeValue()));
//								break;
//							}
//						}
//					}
//				}
//			}
			System.out.println("DEBUG");	
		} catch (ParserConfigurationException ex) {
			System.err.println("ERROR: " + ex.getMessage());
		} catch (SAXException ex) {
			System.err.println("ERROR: " + ex.getMessage());
		} catch (IOException ex) {
			System.err.println("ERROR: " + ex.getMessage());
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public String BuildHTML () {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Reminders</h1>");	
		sb.append("<a href=\"reminder_editor.html\">Reminder Editor</a><br>");
		sb.append("<a href=\"main.html\">Main</a><br>");
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		sb.append("<th>ID</th>");
		sb.append("<th>Reminder Title</th>");
		sb.append("<th>The date and time of creation</th>");
		sb.append("<th>The date and time of reminding</th>");
		sb.append("<th>Edit</th>");
		sb.append("<th>Delete</th>");
		sb.append("</tr>");
		sb.append("<tr>");
		
		// Foreach XML values
//					<td>1</td>
//					<td>Reminder 1</td>
//					<td>20/12/2013 14:10:10</td>
//					<td>25/12/2013 14:10:10</td>
//					<td><a href="/edit?id=1">edit</a></td>
//					<td><a href="/delete?id=1">delete</a></td>
//				</tr>
//				<tr>
//					<td>2</td>
//					<td>Reminder 2</td>
//					<td>20/12/2013 14:10:10</td>
//					<td>25/12/2013 14:10:10</td>
//					<td><a href="/edit?id=2">edit</a></td>
//					<td><a href="/delete?id=2">delete</a></td>
//				</tr>
		sb.append("</table></center>");
		sb.append("</body>");
		sb.append("</html>");
		return sb.toString();
	}
}
