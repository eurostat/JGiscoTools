package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class DK {

	public static void formatDK() {
		try {
			ArrayList<Map<String, String>> raw = CSVUtil.load(HCUtil.path + "DK/DK_geolocated.csv");
			System.out.println(raw.size());

			ArrayList<Map<String, String>> out = new ArrayList<>();
			for(Map<String, String> r : raw) {
				Map<String, String> hf = new HashMap<>();

				hf.put("hospital_name", r.get("hospital_name"));
				hf.put("lat", r.get("lat"));
				hf.put("lon", r.get("lon"));
				hf.put("cc", "DK");
				hf.put("country", "Denmark");

				//street,house_number,postcode,city
				String add = r.get("Adresse");
				String[] parts = add.split(",");
				if(parts.length == 1) {
					hf.put("city", add);
				} else {
					String postcodeCity = parts[parts.length - 1].trim();
					String postcode = postcodeCity.substring(0, 4);
					String city = postcodeCity.substring(5, postcodeCity.length());
					hf.put("postcode", postcode);
					hf.put("city", city);

					String st = parts[0].trim();
					hf.put("street", st);
				}

				out.add(hf);
			}

			System.out.println("Save " + out.size());
			CSVUtil.save(out, HCUtil.path + "DK/DK.csv");		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
