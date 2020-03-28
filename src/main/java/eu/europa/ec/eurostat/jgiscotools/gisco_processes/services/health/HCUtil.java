/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * @author gaffuju
 *
 */
public class HCUtil {

	static String path = "E:/dissemination/shared-data/MS_data/Service - Health/";

	//country codes covered
	static String[] ccs = new String[] { "AT", "BE", "CH", "CY", "DE", "DK", "ES", "FI", "FR", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PT", "RO", "SE", "UK" };

	//CSV columns
	static String[] cols = new String[] {
			"id", "hospital_name", "site_name", "lat", "lon", "street", "house_number", "postcode", "city", "cc", "country", "emergency", "cap_beds", "cap_prac", "cap_rooms", "facility_type", "public_private", "list_specs", "tel", "email", "url", "ref_date", "pub_date", "geo_qual"
	};
	static List<String> cols_ = Arrays.asList(cols);

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

}
