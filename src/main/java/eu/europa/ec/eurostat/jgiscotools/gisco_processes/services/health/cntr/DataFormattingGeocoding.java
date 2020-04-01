package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.csv.CSVFormat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.LocalParameters;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

public class DataFormattingGeocoding {

	public static String path = HCUtil.path;

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		//

		// formatAT();
		//formatCH();
		// formatLU();
		// LocalParameters.loadProxySettings();
		// formatRO();
		//formatFI();
		//formatIT();
		//formatDE();
		//formatBE();
		//formatFR();
		//formatDK();
		//formatLT();


		/*
		 * /AT System.out.println("load"); ArrayList<Map<String,String>> hospitals =
		 * CSVUtil.load(path+"AT/AT_formatted.csv",
		 * CSVFormat.DEFAULT.withFirstRecordAsHeader()); geocodeGISCO(hospitals, false);
		 * LocalParameters.loadProxySettings(); //TODO fix that - GISCO geocoder does
		 * not work with proxy geocodeBing(hospitals, false);
		 * System.out.println("save");
		 * CSVUtil.save(hospitals,path+"AT/AT_geolocated.csv");
		 */

		//geocoding
		LocalParameters.loadProxySettings();
		/*System.out.println("load");
		ArrayList<Map<String,String>> hospitals = CSVUtil.load(path + "LT/LT_formatted.csv");
		HCUtil.geocodeBing(hospitals, true);
		System.out.println("save");
		CSVUtil.save(hospitals, path + "LT/LT_geolocated.csv");
		//save as gpkg
		GeoData.save(CSVUtil.CSVToFeatures(hospitals, "lon", "lat"), path + "LT/LT_geolocated.gpkg", ProjectionUtil.getWGS_84_CRS());
*/
		System.out.println("End");
	}

	public static void formatLT() {
		try {
			ArrayList<Map<String, String>> hs = CSVUtil.load(path + "LT/LT_.csv");
			System.out.println(hs.size());

			for(Map<String, String> h : hs) {


				h.remove("Code of legal entity");

				//street	house_number	postcode
				String add = h.get("address");
				//System.out.println(add);
				String[] parts = add.split(";");
				if(parts.length == 2) {
					String[] parts_ = parts[0].split(" ");
					String hn = parts_[parts_.length-1].trim();
					h.put("house_number", hn.toLowerCase());
					//System.out.println(h.get("house_number"));
					String street = parts[0].replace(hn, "").trim();
					h.put("street", street);
					//System.out.println(h.get("street"));

					String rest = parts[1].trim();
					int i = rest.indexOf("LT-");
					if(i>=0) {
						//get postcode
						String postcode = "LT-" + rest.substring(i+3, i+8);
						//System.out.println(postcode);
						h.put("postcode", postcode);
						rest = rest.replace(postcode, "").replace("  ", "").trim();
					}
					//System.out.println(rest + " - " + h.get("city"));
					h.put("city", rest);
				} else if(parts.length == 3) {
					//System.out.println(add);
					String[] parts_ = parts[0].split(" ");
					String hn = parts_[parts_.length-1].trim();
					hn = hn.toLowerCase().replace("km.", "");
					h.put("house_number", hn);
					//System.out.println(h.get("house_number"));

					String street = parts[0].replace(parts_[parts_.length-1], "").trim();
					h.put("street", street);
					//System.out.println(h.get("street"));

					String rest = parts[1].trim() + ";" + parts[2].trim();
					int i = rest.indexOf("LT-");
					if(i>=0) {
						//get postcode
						String postcode = "LT-" + rest.substring(i+3, i+8);
						//System.out.println(postcode);
						h.put("postcode", postcode);
						rest = rest.replace(postcode, "").replace("  ", "").trim();
					}
					//System.out.println(" --- " + street + " --- " +h.get("city"));
					//System.out.println(rest);
				} else {
					System.out.println(add);
				}

				//h.remove("address");

				//sub_priv_pub - public_private
				String spp = h.get("sub_priv_pub");
				h.put("public_private", "8".equals(spp)?"private":"public");
				h.remove("sub_priv_pub");

				h.put("cc", "LT");
				h.put("country", "");
				h.put("emergency", "");
				h.put("list_specs", "");
				h.put("cap_prac", "");
				h.put("cap_rooms", "");
				h.put("tel", "");
				h.put("email", "");
				h.put("url", "");
				h.put("pub_date", "");
				h.put("ref_date", "31/12/2018");
			}

			System.out.println("Save " + hs.size());
			CSVUtil.save(hs, path + "LT/LT_formatted.csv");
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public static void formatDK() {
		try {
			String basePath = "/home/juju/Bureau/DK/";
			ArrayList<Map<String, String>> raw = CSVUtil.load(basePath + "DK_geolocated.csv");
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
			CSVUtil.save(out, basePath + "DK.csv");		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void formatBE() {
		String basePath = "/home/juju/Bureau/BE/";
		//load csv
		ArrayList<Map<String, String>> raw = CSVUtil.load(basePath + "BE_raw.csv");
		System.out.println(raw.size());
		HashMap<String, Map<String, String>> rawI = Util.index(raw, "NUMERO DE SITE");
		ArrayList<Map<String, String>> rawLonLat = CSVUtil.load(basePath + "BE_raw_lonlat.csv");
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
		CSVUtil.save(out, basePath + "BE.csv");
	}


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

			Scanner scanner = new Scanner(new File(path + "RO/extractSpitaleGPS.html"));
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
			CSVUtil.save(hospitalsFormatted, path + "RO/RO_geolocated.csv");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Format AT
	 */
	public static void formatAT() {

		String filePath = path + "AT/KA-Verzeichnis 2019-10-15.csv";
		ArrayList<Map<String, String>> hospitals = CSVUtil.load(filePath,
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(hospitals.size());

		Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String, String>>();
		for (Map<String, String> h : hospitals) {

			// new formatted hospital
			HashMap<String, String> hf = new HashMap<String, String>();

			// copy columns

			// country - AT
			hf.put("country", "AT");
			// id - KA-Nr
			hf.put("id", h.get("KA-Nr"));
			// name - Bezeichnung
			hf.put("name", h.get("Bezeichnung"));
			// list_specs - Intensivbereiche
			hf.put("list_specs", h.get("Intensivbereiche"));
			// url - Homepage
			hf.put("url", h.get("Homepage"));
			// tel - Telefon
			hf.put("tel", h.get("Telefon"));
			// cap_beds - Bettenanzahl
			hf.put("cap_beds", h.get("Bettenanzahl"));
			// year
			hf.put("data_year", "2020");

			// address
			// street house_number postcode city - Adresse
			// St. Veiter-Straße 46, 5621 St. Veit im Pongau
			String ad = h.get("Adresse");
			String[] parts = ad.split(",");
			if (parts.length != 2)
				System.err.println(ad);
			String rightPart = parts[1];
			String fc = rightPart.substring(0, 1);
			if (!fc.equals(" "))
				System.err.println(fc);
			rightPart = rightPart.substring(1, rightPart.length());
			String postcode = rightPart.substring(0, 4);
			hf.put("postcode", postcode);

			fc = rightPart.substring(4, 5);
			if (!fc.equals(" "))
				System.err.println(fc);
			String city = rightPart.substring(5, rightPart.length());
			hf.put("city", city);

			String leftPart = parts[0];
			if (leftPart.equals("")) {
				hf.put("house_number", "");
				hf.put("street", "");
			} else {
				parts = leftPart.split(" ");
				String house_number = parts[parts.length - 1];

				// assign the house_number, or "" if it equals "0"
				hf.put("house_number", house_number.equals("0") ? "" : house_number);

				leftPart = leftPart.replace(house_number, "");
				fc = leftPart.substring(leftPart.length() - 1, leftPart.length());
				if (!fc.equals(" "))
					System.err.println(fc);
				String street = leftPart.substring(0, leftPart.length() - 1);
				hf.put("street", street);
			}

			// add to list
			hospitalsFormatted.add(hf);
		}

		// save
		CSVUtil.save(hospitalsFormatted,
				"E:\\dissemination\\shared-data\\MS_data\\Service - Health\\AT/AT_formatted.csv");
	}

	/**
	 * Fotmat CH
	 */

	public static void formatCH() {

		ArrayList<Map<String, String>> hs = CSVUtil.load(path + "CH/CH_geolocated.csv");
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
		CSVUtil.save(out, path + "CH/CH.csv");
	}

	public static void formatFI() {

		String filePath = "/home/juju/Bureau/FI_geolocated.csv";
		ArrayList<Map<String, String>> hospitals_FI = CSVUtil.load(filePath,
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(hospitals_FI.size());

		Collection<Map<String, String>> hospitalsFormatted_FI = new ArrayList<Map<String, String>>();
		for (Map<String, String> h : hospitals_FI) {

			// new formatted hospital
			HashMap<String, String> h_FI = new HashMap<String, String>();

			// copy columns

			// country code

			h_FI.put("hospital_name", h.get("organization name"));
			h_FI.put("site_name", h.get("name"));
			h_FI.put("cc", "FI");
			h_FI.put("country", "Finland");
			h_FI.put("postcode", h.get("postcode"));
			h_FI.put("municipality", h.get("municipality"));
			h_FI.put("facility_type", h.get("services"));
			h_FI.put("list_specs", h.get("services"));

			// address
			// PL 13

			// add to list
			hospitalsFormatted_FI.add(h_FI);

		}

		// save
		CSVUtil.save(hospitalsFormatted_FI, "/home/juju/Bureau/FI.csv");
	}

	public static void formatIT() {

		String filePath = path + "IT/C_17_dataset_96_0_upFile.csv";
		ArrayList<Map<String, String>> hospitals = CSVUtil.load(filePath,
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(hospitals.size());

		Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String, String>>();
		for (Map<String, String> h : hospitals) {

			if( !"2018".equals(h.get("Anno")) ) continue;

			// new formatted hospital
			HashMap<String, String> hf = new HashMap<String, String>();
			hf.put("cc", "IT");
			hf.put("country", "Italy");
			hf.put("id", h.get("Codice Azienda").trim() + "-" + h.get("Codice struttura").trim() + "-" + h.get("Subcodice").trim());
			hf.put("hospital_name", h.get("Denominazione Struttura/Stabilimento").trim());
			hf.put("street", h.get("Indirizzo").trim());
			hf.put("city", h.get("Comune").trim());
			hf.put("cap_beds", h.get("Totale posti letto").trim());
			hf.put("facility_type", h.get("Descrizione tipo struttura").trim());
			hf.put("year", h.get("Anno").trim());

			// add to list
			hospitalsFormatted.add(hf);
		}
		System.out.println(hospitalsFormatted.size());

		//compact the hospitals
		Map<String, Map<String, String>> hospitalsCompacted = new HashMap<>();
		for (Map<String, String> h : hospitalsFormatted) {
			//get hospital compacted
			Map<String, String> hC = hospitalsCompacted.get(h.get("id"));
			if(hC==null) {
				hospitalsCompacted.put(h.get("id"), h);
			} else {
				hC.put("cap_beds", ""+(Integer.parseInt(hC.get("cap_beds")) + Integer.parseInt(h.get("cap_beds"))));
			}
		}
		hospitalsFormatted = hospitalsCompacted.values();
		System.out.println(hospitalsFormatted.size());


		// save
		CSVUtil.save(hospitalsFormatted, path + "IT/IT_formatted.csv");
	}

}
