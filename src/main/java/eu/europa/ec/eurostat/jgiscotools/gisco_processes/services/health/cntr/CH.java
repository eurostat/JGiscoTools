package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class CH {

	/**
	 * Fotmat CH
	 */
	public static void formatCH() {

		ArrayList<Map<String, String>> hs = CSVUtil.load(HCUtil.path + "CH/CH_geolocated.csv");
		System.out.println(hs.size());

		Collection<Map<String, String>> out = new ArrayList<Map<String, String>>();
		for (Map<String, String> h : hs) {

			// new formatted hospital
			HashMap<String, String> hf = new HashMap<String, String>();

			hf.put("id", h.get("id"));
			hf.put("hospital_name", h.get("hospital_name"));
			hf.put("site_name", h.get("site_name"));
			hf.put("lat", h.get("lat"));
			hf.put("lon", h.get("lon"));
			hf.put("ref_date", h.get("ref_date"));
			hf.put("cc", "CH");

			String postcode_city = h.get("postcode_city");
			String postcode = postcode_city.substring(0, 4);
			String city = postcode_city.substring(5, postcode_city.length());
			hf.put("postcode", postcode);
			hf.put("city", city);

			//"street", "house_number"
			//no way to decompose that
			hf.put("street", h.get("address"));

			out.add(hf);
		}

		CSVUtil.addColumns(out, HCUtil.cols, "");

		// save
		CSVUtil.save(out, HCUtil.path + "CH/CH.csv");
	}

}
