import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class HTTPUtils {

	/**
	 * Get a string to file path on server, and load it into byte[] for further use
	 * @param path - filename path on hard drive
	 * @return byte[] with the asked file
	 * @throws IOException, FileNotFoundException - if he doesn't find 
	 */
	public static byte[] getFileBytes(String path) throws IOException, FileNotFoundException {
		
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

	/**
	 * 
	 * @param attribute
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws Exception
	 */
	public static String LoadDatabaseXmlNameFromConfig(String value) throws FileNotFoundException, IOException{

		Properties _properties = new Properties();
		_properties.load(new FileInputStream("config.ini"));
		return (_properties.getProperty(value));
	}
	
	/**
	 * 
	 * @param rootElement
	 * @param attributeValue
	 * @return
	 */
	public static Element getElementByAttributeValue(Node rootElement, String attributeValue) {

	    if (rootElement != null && rootElement.hasChildNodes()) {
	        NodeList nodeList = rootElement.getChildNodes();

	        for (int i = 0; i < nodeList.getLength(); i++) {
	            Node subNode = nodeList.item(i);

	            if (subNode.hasAttributes()) {
	                NamedNodeMap nnm = subNode.getAttributes();

	                for (int j = 0; j < nnm.getLength(); j++) {
	                    Node attrNode = nnm.item(j);

	                    if (attrNode.getNodeType() == Node.ATTRIBUTE_NODE) {
	                        Attr attribute = (Attr) attrNode;

	                        if (attributeValue.equals(attribute.getValue())) {
	                            return (Element)subNode;
	                        } else {
	                            return getElementByAttributeValue(subNode, attributeValue);
	                        }
	                    }
	                }               
	            }
	        }
	    }
	    
	    return null;
	}
}
