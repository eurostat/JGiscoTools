package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * @author julien Gaffuri
 *
 */
public class XMLUtils {

	/**
	 * Get a XML document from a URL.
	 * 
	 * @param urlString
	 * @return
	 */
	public static Document parseXMLfromURL(String urlString){
		try{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new URL(urlString).openConnection().getInputStream());
			new URL(urlString).openConnection().getInputStream().close();
			return doc;
			//InputStream in = new URL(urlString).openConnection().getInputStream();
			//return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		}
		catch(Exception e){
			e.printStackTrace();
		}       
		return null;
	}

	/**
	 * Return the namespace of a XML document.
	 * 
	 * @param file
	 * @return
	 */
	public static String getFileNameSpace(File file) {
		String ns = null;
		try {
			FileInputStream ips = new FileInputStream(file);
			Document XMLDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( ips );
			ns = XMLDoc.getDocumentElement().getAttribute("xmlns");
			try { ips.close(); } catch (IOException e) { e.printStackTrace(); }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ns;
	}


	/**
	 * @param stream
	 * @return
	 */
	public static Document parse(InputStream stream) {
		Document XMLDoc = null;
		DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
		fact.setValidating(false);
		fact.setNamespaceAware(false);
		try {
			XMLDoc = fact.newDocumentBuilder().parse(stream);
		} catch (Exception e) { e.printStackTrace(); }
		return XMLDoc;
	}

}
