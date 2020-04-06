package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

public class BE {

	public static void formatBE() {
		//load csv
		ArrayList<Map<String, String>> raw = CSVUtil.load(HCUtil.path + "BE/BE_raw.csv");
		System.out.println(raw.size());
		HashMap<String, Map<String, String>> rawI = Util.index(raw, "NUMERO DE SITE");
		ArrayList<Map<String, String>> rawLonLat = CSVUtil.load(HCUtil.path + "BE/BE_raw_lonlat.csv");
		System.out.println(rawLonLat.size());

		ArrayList<Map<String, String>> out = new ArrayList<>();
		for(Map<String, String> r : rawLonLat) {
			Map<String, String> hf = new HashMap<>();

			String id = r.get("NUMERO DE SITE");
			hf.put("cc", "BE");
			hf.put("id", id);
			hf.put("hospital_name", r.get("HOPITAL"));
			hf.put("site_name", r.get("SITE"));
			hf.put("lon", r.get("Longitude").replace(",", "."));
			hf.put("lat", r.get("Latitude").replace(",", "."));

			Map<String, String> rd = rawI.get(id);
			if(rd == null) System.out.println("BE - no information for site " + id);
			hf.put("street", rd.get("ADRESSE")); //TODO decompose with house number?
			hf.put("postcode", rd.get("POST"));
			hf.put("city", rd.get("COMMUNE"));
			hf.put("tel", rd.get("TELEFON"));
			hf.put("url", rd.get("WEBSITE"));
			hf.put("facility_type", rd.get("TYPE HOPITAL"));
			hf.put("public_private", rd.get("STATUT"));
			hf.put("cap_beds", rd.get("TOTAL LITS"));
			if("X".equals(rd.get("PREMIERE PRISE EN CHARGE DES URGENCES")) || "X".equals(rd.get("SOINS URGENTS SPECIALISES")))
				hf.put("emergency", "1"); else hf.put("emergency", "0");
			hf.put("ref_date", "2020-02-01");

			out.add(hf);
		}

		System.out.println("Save "+out.size());
		CSVUtil.save(out, HCUtil.path + "BE/BE.csv");
	}

	
}
