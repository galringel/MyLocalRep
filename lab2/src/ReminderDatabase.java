import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

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
 * @author 
 *
 */
public class ReminderDatabase {

	// Singleton to database xml
	private static ReminderDatabase instance = null;

	public static ReminderDatabase getInstance(String rootFolder) {
		
	    if (instance == null) {
			synchronized (ReminderDatabase.class) {  
	            if (instance == null) {  
	            	instance = new ReminderDatabase(rootFolder);
	            }
	        }
	    }
	    
	    return instance;
	}
	
		// Hold the reminders.xml serialized
		private HashMap<String, ArrayList<ReminderInfo>> _remindersInfoDB = new HashMap<String, ArrayList<ReminderInfo>>();
		
		/**
		 * 
		 * @return
		 */
		public HashMap<String, ArrayList<ReminderInfo>> getRemindersInfoDB() {
			return _remindersInfoDB;
		}
	
	private ReminderDatabase(String rootFolder) {
		
		String reminderXmlPath = null;
		try {
			reminderXmlPath = HTTPUtils.LoadDatabaseXmlNameFromConfig("reminderFilePath");
			String xmlFilename = rootFolder + "/" + reminderXmlPath;
			
			if (serializeDatabaseXml(xmlFilename)) {
				System.out.println("Reminder database xml was parsed and loaded intp memory!");
			} else {
				System.err.println("ERROR: Could not load reminder.xml");
			}
			
			
		} catch (IOException ex) {
			System.err.println("ERROR: " + ex.getMessage());
		}
	}
	/**
	 * Gets and xml path to a database, and serialize it into a proper class
	 * @param databaseXML
	 */
	private boolean serializeDatabaseXml(String databaseXML) {
		try {
			
			File fXmlFile = new File(databaseXML);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			
			Element rootElement = doc.getDocumentElement();
			NodeList reminderNodes = rootElement.getElementsByTagName("reminder");
			
			// Iterates reminder elements
			for (int i = 0; i < reminderNodes.getLength(); i++) {
				
				// Single reminder node
				Node reminderNode = reminderNodes.item(i);
				//System.out.println("Element Name:" + reminderNode.getNodeName());
				
				// Gets the email address found in "id" element
				NamedNodeMap reminderAttributes = reminderNode.getAttributes();
				Attr attribute = (Attr)reminderAttributes.item(0);
				//System.out.println("Attribute Name: " + attribute.getName() + " Attribute Value: " + attribute.getValue());
				String reminderEmail = attribute.getValue();
				
				// Continue iterating the xml
				if (reminderNode.hasChildNodes()) {
					
					// Get all reminder childs (reminderInfo)
					Element reminderElement = (Element)reminderNode;
					NodeList reminderInfoNodes = reminderElement.getElementsByTagName("reminderInfo");
					ArrayList<ReminderInfo> reminderInfoList = new ArrayList<>();
					
					// Iterates all reminderInfo elements
					for (int j = 0; j < reminderInfoNodes.getLength(); j++) {
						
						// Gets a single reminderInfo
						Node reminderInfoNode = reminderInfoNodes.item(j);
						// Creates a reminderInfo object
						//System.out.println("Element Name:" + reminderInfoNode.getNodeName());
						
						// Gets reminderInfo attribute "id"
						NamedNodeMap nodeAttributes = reminderInfoNode.getAttributes();
						attribute = (Attr)nodeAttributes.item(0);
						//System.out.println("Attribute Name: " + attribute.getName() + ", Attribute Value: " + attribute.getValue());
						
						if (reminderInfoNode.hasChildNodes()) {
							
							NodeList reminderChildNodes = reminderInfoNode.getChildNodes();
							ReminderInfo reminderInfo = new ReminderInfo();
							
							// Iterates reminderInfo child elements
							for (int k = 0; k < reminderChildNodes.getLength(); k++) {
								reminderInfo.setReminderId(attribute.getValue());
								Node reminderChild = reminderChildNodes.item(k);
								
								// We skip #text element (unknown shit of DOM parsing)
								if (!(reminderChild.getNodeType() == Node.TEXT_NODE)) {
									
									String reminderChildName = reminderChild.getNodeName();
									String reminderChildValue = reminderChild.getTextContent();
									//System.out.println("Element Name: " + reminderChildName +", Element Value: " + reminderChildValue);
									
									switch (reminderChildName) {
										case "reminderTitle": {
											reminderInfo.setReminderTitle(reminderChildValue);
											break;
											}
										case "reminderCreationDate": {
											SimpleDateFormat sdf1 = new SimpleDateFormat();
										    sdf1.applyPattern("dd/MM/yyyy HH:mm:ss");
										    Date date = sdf1.parse(reminderChildValue);
											reminderInfo.setReminderCreationDate(date);
											break;
											}
										case "reminderBody": {
											reminderInfo.setReminderBody(reminderChildValue);
											break;
										}
										case "reminderSendingDate": {
											SimpleDateFormat sdf1 = new SimpleDateFormat();
										    sdf1.applyPattern("dd/MM/yyyy HH:mm:ss");
										    Date date = sdf1.parse(reminderChildValue);
											reminderInfo.setReminderSendingDate(date);
											break;
										}
									}
								}	
							}
							
							// Add a new reminderInfo to to the list
							reminderInfoList.add(reminderInfo);
						}
					}
					
					// add the result to the dictionary
					this._remindersInfoDB.put(reminderEmail, reminderInfoList);
				}
			}
			
			return true;
		} catch (ParserConfigurationException ex) {
			System.err.println("ERROR: " + ex.getMessage());
			return false;
		} catch (SAXException ex) {
			System.err.println("ERROR: " + ex.getMessage());
			return false;
		} catch (IOException ex) {
			System.err.println("ERROR: " + ex.getMessage());
			return false;
		} catch (ParseException ex) {
			System.err.println("ERROR: " + ex.getMessage());
			return false;
		}
	}
}
