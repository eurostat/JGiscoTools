package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.Validation;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.io.XMLUtils;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

public class DE {

	public static void main(String[] args) throws Exception {
		String filePath = HCUtil.path + "DE/20200308_Verzeichnisabruf_aktuell.xml";
		Document doc = XMLUtils.parse(new FileInputStream(filePath));

		NodeList elts = doc.getDocumentElement().getChildNodes();
		//System.out.println(elts.getLength());

		//get hospital names
		HashMap<String,String> hospNames = new HashMap<>();
		for(int i=0; i<elts.getLength(); i++) {
			if(!elts.item(i).getNodeName().equals("Krankenhaus")) continue;
			Element elt = (Element) elts.item(i);
			String hospName = elt.getElementsByTagName("Bezeichnung").item(0).getTextContent();
			String version = elt.getElementsByTagName("Version").item(0).getTextContent();
			String iK = ((Element)elt.getElementsByTagName("HauptIK").item(0)).getElementsByTagName("IK").item(0).getTextContent();
			String hospId = iK + "_" + version;
			hospNames.put(hospId, hospName);
		}

		Collection<Map<String, String>> data = new ArrayList<Map<String, String>>();
		for(int i=0; i<elts.getLength(); i++) {
			if(!elts.item(i).getNodeName().equals("Standort")) continue;
			Element elt = (Element) elts.item(i);

			//identifier
			String id = "";
			String hospId = "";
			{
				String iK = ((Element)elt.getElementsByTagName("ReferenzKrankenhaus").item(0)).getElementsByTagName("IK").item(0).getTextContent();
				String version = ((Element)elt.getElementsByTagName("ReferenzKrankenhaus").item(0)).getElementsByTagName("Version").item(0).getTextContent();
				hospId = iK + "_" + version;
				String standortId = elt.getElementsByTagName("StandortId").item(0).getTextContent();
				id = hospId + "_" + standortId;
			}

			//name
			String siteName = elt.getElementsByTagName("Bezeichnung").item(0).getTextContent();
			String hospName = hospNames.get(hospId);
			if(hospName == null) {
				System.err.println("Could not find hospital name for " + hospId);
				hospName = siteName; siteName = "";
			}
			if(siteName.equals(hospName)) siteName = "";

			Element geoAdr = (Element) elt.getElementsByTagName("GeoAdresse").item(0);
			String lon = geoAdr.getElementsByTagName("Längengrad").item(0).getTextContent();
			String lat = geoAdr.getElementsByTagName("Breitengrad").item(0).getTextContent();
			String housenb = geoAdr.getElementsByTagName("Hausnummer").item(0).getTextContent();
			String street = geoAdr.getElementsByTagName("Straße").item(0).getTextContent();
			String postcode = geoAdr.getElementsByTagName("PLZ").item(0).getTextContent();
			String city = geoAdr.getElementsByTagName("Ort").item(0).getTextContent();

			HashMap<String, String> hf = new HashMap<String, String>();
			hf.put("id", id);
			hf.put("hospital_name", hospName);
			hf.put("site_name", siteName);
			hf.put("house_number", housenb);
			hf.put("street", street);
			hf.put("postcode", postcode);
			hf.put("city", city);
			hf.put("lon", lon);
			hf.put("lat", lat);
			hf.put("geo_qual", "1");
			//hf.put("facility_type", "standort");

			hf.put("cc", "DE");
			hf.put("country", "Germany");
			hf.put("ref_date", "08/03/2020");

			data.add(hf);
		}

		//TODO analyse id duplicates
		//

		// save
		System.out.println(data.size());
		CSVUtil.addColumns(data, HCUtil.cols, "");
		Validation.validate(data, "DE");
		CSVUtil.save(data, HCUtil.path + "DE/DE.csv");
		GeoData.save(CSVUtil.CSVToFeatures(data, "lon", "lat"), HCUtil.path + "DE/DE.gpkg", ProjectionUtil.getWGS_84_CRS());
	}

}
