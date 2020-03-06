package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

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

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class HealthCareDataFormattingGeocoding {

	static String path = "E:\\dissemination\\shared-data\\MS_data\\Service - Health\\";

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		//

		// formatAT();
		// formatCH();
		// formatLU();
		// LocalParameters.loadProxySettings();
		// formatRO();
		// formatFI();
		formatIT();
		// ...

		/*
		 * /AT System.out.println("load"); ArrayList<Map<String,String>> hospitals =
		 * CSVUtil.load(path+"AT/AT_formatted.csv",
		 * CSVFormat.DEFAULT.withFirstRecordAsHeader()); geocodeGISCO(hospitals, false);
		 * LocalParameters.loadProxySettings(); //TODO fix that - GISCO geocoder does
		 * not work with proxy geocodeBing(hospitals, false);
		 * System.out.println("save");
		 * CSVUtil.save(hospitals,path+"AT/AT_geolocated.csv");
		 */

		System.out.println("End");
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

		String filePath = path + "CH/kzp17_daten.csv";
		ArrayList<Map<String, String>> hospitals = CSVUtil.load(filePath,
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(hospitals.size());

		Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String, String>>();
		for (Map<String, String> h : hospitals) {

			// new formatted hospital
			HashMap<String, String> hf = new HashMap<String, String>();

			// copy columns

			// country - CH
			hf.put("country", "CH");
			// Inst = name
			hf.put("name", h.get("Inst"));
			// list_specs - Intensivbereiche
			hf.put("list_specs", h.get("Akt"));
			// BettenStat = cap_beds (Betten)
			hf.put("cap_beds", h.get("BettenStat"));
			// PersA = cap_prac (Ärzte)
			hf.put("cap_prac", h.get("PersA"));
			// Typ - facility_type (Spitaltyp, gemäss BFS Spitaltypologie)
			hf.put("facility_type", h.get("Typ"));
			// Jahr = year
			hf.put("data_year", h.get("Jahr"));

			// address AT
			// street house_number postcode city - Adresse
			// St. Veiter-Straße 46, 5621 St. Veit im Pongau

			// address CH
			// Ort = postcode + city
			// 7000 Chur
			// Adr = streetname (street name and number)
			// Loestrasse 220

			// Split "Ort" column into postcode and city
			String ort = h.get("Ort");
			// split in two where there's a space
			String[] parts = ort.split(" ");
			// If there are more than 2 parts, print error
			if (parts.length != 2)
				System.err.println(ort);
			// on the right:
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

			String leftPart = h.get("Adr");
			// String leftPart = parts[0];
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
				"E:\\dissemination\\shared-data\\MS_data\\Service - Health\\CH/CH_formatted.csv");
	}

	public static void formatFI() {

		String filePath = path + "FI/topi-2020-01-06.csv";
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
		CSVUtil.save(hospitalsFormatted_FI,
				"E:\\dissemination\\shared-data\\MS_data\\Service - Health\\FI/FI_formatted.csv");

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
			hf.put("id", h.get("Codice Azienda") + "-" + h.get("Codice struttura") + "-" + h.get("Subcodice"));
			hf.put("hospital_name", h.get("Denominazione Struttura/Stabilimento"));
			hf.put("street", h.get("Indirizzo"));
			hf.put("city", h.get("Comune"));
			hf.put("cap_beds", h.get("Totale posti letto"));
			hf.put("facility_type", h.get("Descrizione tipo struttura"));
			hf.put("year", h.get("Anno"));

			// add to list
			hospitalsFormatted.add(hf);
		}

		for (Map<String, String> h : hospitalsFormatted) {
			//TODO shrink hospitals, summing number of beds
		}

		
		
		// save
		CSVUtil.save(hospitalsFormatted,
				"E:\\dissemination\\shared-data\\MS_data\\Service - Health\\IT/IT_formatted.csv");

	}
}
