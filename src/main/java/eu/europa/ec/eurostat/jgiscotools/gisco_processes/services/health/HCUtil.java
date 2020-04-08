/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

/**
 * @author julien Gaffuri
 *
 */
public class HCUtil {

	public static String path = "E:/dissemination/shared-data/MS_data/Service - Health/";

	//country codes covered
	static String[] ccs = new String[] { "AT", "BE", "BG","CH", "CY", "DE", "DK", "ES", "FI", "FR", /*"HU",*/ "IE", "IT", "LT", "LU", "LV", "MT", "NL", "NO", "PL", "PT", "RO", "SE", "SI"/*, "UK"*/ };

	//CSV columns
	public static String[] cols = new String[] {
			"id", "hospital_name", "site_name",
			"lat", "lon", "geo_qual",
			"street", "house_number", "postcode", "city", "cc", "country",
			"cap_beds", "cap_prac", "cap_rooms",
			"emergency", "facility_type", "public_private", "list_specs",
			"tel", "email", "url",
			"ref_date", "pub_date"
	};
	public static List<String> cols_ = Arrays.asList(cols);

	//date format
	static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");



	static void applyTypes(Collection<Feature> fs) {
		for(Feature f : fs) {
			for(String att : new String[] {"cap_beds", "cap_prac", "cap_rooms"}) {
				Object v = f.getAttribute(att);
				if(v==null) continue;
				if("".equals(v)) f.setAttribute(att, null);
				else f.setAttribute(att, Integer.parseInt(v.toString()));
			}
			for(String att : new String[] {"lat", "lon"}) {
				Object v = f.getAttribute(att);
				if(v==null) continue;
				if("".equals(v)) f.setAttribute(att, null);
				else f.setAttribute(att, Double.parseDouble(v.toString()));
			}
		}
	}




	/*
	static void geocodeGISCO(ArrayList<Map<String,String>> hospitals, boolean usePostcode, boolean printURLQuery) {
		//int count = 0;
		int fails = 0;
		for(Map<String,String> hospital : hospitals) {
			//count++;
			String address = "";
			if(hospital.get("house_number")!=null) address += hospital.get("house_number") + " ";
			address += hospital.get("street");
			address += " ";
			if(usePostcode) {
				address += hospital.get("postcode");
				address += " ";
			}
			address += hospital.get("city");
			address += " ";
			address += hospital.get("country");
			System.out.println(address);

			GeocodingResult gr = GISCOGeocoder.geocode(address, printURLQuery);
			Coordinate c = gr.position;
			System.out.println(c  + "  --- " + gr.matching + " --- " + gr.confidence);
			if(c.getX()==0 && c.getY()==0) fails++;

			//if(count > 10) break;
			hospital.put("latGISCO", "" + c.y);
			hospital.put("lonGISCO", "" + c.x);
			hospital.put("geo_qual", "" + gr.quality);
			hospital.put("geo_matchingGISCO", "" + gr.matching);
			hospital.put("geo_confidenceGISCO", "" + gr.confidence);
		}

		System.out.println("Failures: " + fails + "/" + hospitals.size());
	}
	 */

	public static void CSVToGPKG(String cc) {
		ArrayList<Map<String, String>> data = CSVUtil.load(HCUtil.path + cc+"/"+cc+".csv");
		GeoData.save(CSVUtil.CSVToFeatures(data, "lon", "lat"), HCUtil.path + cc+"/"+cc+".gpkg", ProjectionUtil.getWGS_84_CRS());
	}

}
