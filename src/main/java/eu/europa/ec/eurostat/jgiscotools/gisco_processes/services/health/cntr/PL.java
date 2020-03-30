package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.europa.ec.eurostat.jgiscotools.geocoding.BingGeocoder;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.LocalParameters;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.ServicesGeocoding;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.io.XMLUtils;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

public class PL {

	public static void main(String[] args) {
		System.out.println("Start");

		try {
			String filePath = HCUtil.path + "PL/PublicznyRaportJednostekArt23A_2020-03-29.xml";
			Document doc = XMLUtils.parse(new FileInputStream(filePath));

			Collection<Map<String, String>> out = new ArrayList<Map<String, String>>();

			NodeList elts = doc.getDocumentElement().getElementsByTagName("jednostki").item(0).getChildNodes();
			HashSet<String> hnames = new HashSet<>();
			for(int i=0; i<elts.getLength(); i++) {
				Node n = elts.item(i);
				//jednostka - unit
				if(!n.getNodeName().equals("jednostka")) continue;
				Element elt = (Element)n;

				//nazwaPodmiotu - entity name
				//nazwaJednostki - unit name
				String hospital_name = elt.getElementsByTagName("nazwaPodmiotu").item(0).getTextContent();
				if(hnames.contains(hospital_name)) continue;
				hnames.add(hospital_name);

				//String site_name = elt.getElementsByTagName("nazwaJednostki").item(0).getTextContent();
				String id = elt.getElementsByTagName("numerWpisu").item(0).getTextContent();
				String email = elt.getElementsByTagName("email").item(0).getTextContent();
				String tel = elt.getElementsByTagName("telefon").item(0).getTextContent();

				Element address = (Element) elt.getElementsByTagName("adresPodmiotu").item(0);

				//gmina - community
				//miejscowosc - resort
				String gmina = address.getElementsByTagName("gmina").item(0).getTextContent();
				String miejscowosc = "";
				try {
					miejscowosc = address.getElementsByTagName("miejscowosc").item(0).getTextContent();
				} catch (Exception e1) {}
				String city = null;
				if(miejscowosc.length()==0 || gmina.equals(miejscowosc)) city = gmina;
				else city = gmina + " - " + miejscowosc;

				//kodPocztowy - postcode
				String postcode = "";
				postcode = address.getElementsByTagName("kodPocztowy").item(0).getTextContent();
				String street = "";
				try {
					street = address.getElementsByTagName("ulica").item(0).getTextContent();
				} catch (Exception e) {}
				//street += " ";
				//street += address.getElementsByTagName("rodzaj").item(0).getTextContent();

				//budynek - building
				String house_number = address.getElementsByTagName("budynek").item(0).getTextContent();

				//wojewodztwo - voivodeship
				//powiat - County

				Map<String, String> h = new HashMap<>();
				h.put("id", id);
				h.put("hospital_name", hospital_name);
				//h.put("site_name", site_name);
				h.put("street", street);
				h.put("house_number", house_number);
				h.put("postcode", postcode);
				h.put("city", city);
				h.put("cc", "PL");
				h.put("tel", tel);
				h.put("email", email);
				h.put("ref_date", "29/03/2020");

				out.add(h);
			}

			// save 1
			System.out.println(out.size());
			CSVUtil.addColumns(out, HCUtil.cols, "");
			CSVUtil.save(out, HCUtil.path + "PL/PL_formatted.csv");

			//geocode
			LocalParameters.loadProxySettings();
			ServicesGeocoding.set(BingGeocoder.get(), out, true, true);

			// save 2
			CSVUtil.addColumns(out, HCUtil.cols, "");
			CSVUtil.save(out, HCUtil.path + "PL/PL.csv");
			GeoData.save(CSVUtil.CSVToFeatures(out, "lon", "lat"), HCUtil.path + "PL/PL.gpkg", ProjectionUtil.getWGS_84_CRS());

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("End");
	}
}
