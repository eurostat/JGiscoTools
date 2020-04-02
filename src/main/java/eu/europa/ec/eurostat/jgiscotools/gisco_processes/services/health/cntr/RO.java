package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class RO {

	/**
	 * Format RO
	 */
	public static void formatRO() {

		try {
			/*
			 * String url = "http://infrastructura-sanatate.ms.ro/harta/extractSpitaleGPS";
			 * System.out.println(url); BufferedReader in = new BufferedReader(new
			 * InputStreamReader(new URL(url).openStream())); String line = in.readLine();
			 * System.out.println(line); //String[] parts = line.split(",");
			 */

			Scanner scanner = new Scanner(new File(HCUtil.path + "RO/extractSpitaleGPS.html"));
			String line = scanner.nextLine();
			scanner.close();

			JSONArray data = (JSONArray) new JSONParser().parse(line);
			Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String, String>>();
			for (Object h_ : data) {
				JSONObject h = (JSONObject) h_;
				// [numar_total_paturi, adresa_web, denumire, lng, numar_sectii, adresa,
				// spital_id, lat, numar_compartimente]
				// , beds_ address, name, next, number_sections, address, hospital_id, wide,
				// number_ compartments

				// new formatted hospital
				HashMap<String, String> hf = new HashMap<String, String>();
				hf.put("country", "RO");
				hf.put("id", h.get("spital_id").toString());
				hf.put("name", h.get("denumire").toString());
				hf.put("url", h.get("adresa_web").toString());
				hf.put("cap_beds", h.get("numar_total_paturi") == null ? "" : h.get("numar_total_paturi").toString());
				hf.put("data_year", "2020?"); // TODO check
				hf.put("lat", h.get("lat").toString());
				hf.put("lon", h.get("lng").toString());
				// adresa
				// numar_sectii - numar_compartimente -> capacity?

				hospitalsFormatted.add(hf);
			}
			// save
			CSVUtil.save(hospitalsFormatted, HCUtil.path + "RO/RO_geolocated.csv");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
