package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
public class HealthCareDataFormattingGeocoding {

	static String path = "E:\\dissemination\\shared-data\\MS_data\\Service - Health\\";

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		// formatAT();
		// formatCH();
		// formatLU();
		// ...

		
		geocode("AT/AT_formatted.csv", "AT/AT_geolocated.csv", false);
		// geocodeLU();
		// ...

		System.out.println("End");
	}

	/**
	 * Geocoding
	 * 
	 */
	private static void geocode(String inFile, String outFile, boolean usePostcode) {
		String filePath = path+inFile;
		ArrayList<Map<String,String>> hospitals = CSVUtil.load(filePath, CSVFormat.DEFAULT.withFirstRecordAsHeader());

		//int count = 0;
		int fails = 0;
		for(Map<String,String> hospital : hospitals) {
			//count++;
			String address = "";
			address += hospital.get("house_number");
			address += " ";
			address += hospital.get("street");
			address += " ";
			if(usePostcode) {
				address += hospital.get("postcode");
				address += " ";
			}
			address += hospital.get("city");
			address += " ";
			address += getCountryName(hospital.get("country"));
			System.out.println(address);

			Coordinate c = GISCOGeocoder.geocode(address);
			System.out.println(c);
			if(c.getX()==0 && c.getY()==0) fails++;

			//if(count > 10) break;
			hospital.put("lat", "" + c.y);
			hospital.put("lon", "" + c.x);
		}

		System.out.println("Failures: "+fails+"/"+hospitals.size());
		System.out.println("save");
		CSVUtil.save(hospitals,path+outFile);
	}

	/**
	 * Fotmat AT
	 */

	public static void formatAT() {

		String filePath = path+"AT/KA-Verzeichnis 2019-10-15.csv";
		ArrayList<Map<String,String>> hospitals = CSVUtil.load(filePath, CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(hospitals.size());


		Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String,String>>();
		for(Map<String, String> h : hospitals) {

			//new formatted hospital
			HashMap<String, String> hf = new HashMap<String, String>();

			//copy columns

			//country - AT
			hf.put("country", "AT");
			//id - KA-Nr
			hf.put("id", h.get("KA-Nr"));
			//name - Bezeichnung
			hf.put("name", h.get("Bezeichnung"));
			//list_specs - Intensivbereiche
			hf.put("list_specs", h.get("Intensivbereiche"));
			//url - Homepage
			hf.put("url", h.get("Homepage"));
			//tel - Telefon
			hf.put("tel", h.get("Telefon"));
			//cap_beds - Bettenanzahl
			hf.put("cap_beds", h.get("Bettenanzahl"));
			//year
			hf.put("data_year", h.get("2020"));

			//address
			//street	house_number	postcode	city   - Adresse
			//St. Veiter-Straße 46, 5621 St. Veit im Pongau
			String ad = h.get("Adresse");
			String[] parts = ad.split(",");
			if(parts.length != 2) System.err.println(ad);
			String rightPart = parts[1];
			String fc = rightPart.substring(0, 1);
			if(!fc.equals(" ")) System.err.println(fc);
			rightPart = rightPart.substring(1, rightPart.length());
			String postcode = rightPart.substring(0,4);
			hf.put("postcode", postcode);

			fc = rightPart.substring(4, 5);
			if(!fc.equals(" ")) System.err.println(fc);
			String city = rightPart.substring(5, rightPart.length());
			hf.put("city", city);

			String leftPart = parts[0];
			if(leftPart.equals("")) {
				hf.put("house_number", "");
				hf.put("street", "");
			} else {
				parts = leftPart.split(" ");
				String house_number = parts[parts.length-1];

				//assign the house_number, or "" if it equals "0"
				hf.put("house_number", house_number.equals("0")? "":house_number );

				leftPart = leftPart.replace(house_number, "");
				fc = leftPart.substring(leftPart.length()-1, leftPart.length());
				if(!fc.equals(" ")) System.err.println(fc);
				String street = leftPart.substring(0, leftPart.length()-1);
				hf.put("street", street);
			}

			//add to list
			hospitalsFormatted.add(hf);
		}

		//save
		CSVUtil.save(hospitalsFormatted, "E:\\dissemination\\shared-data\\MS_data\\Service - Health\\AT/AT_formatted.csv");
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





	//TODO
	public static String getCountryName(String countryCode2letters) {
		switch (countryCode2letters) {
		case "AT": return "Austria";
		case "LU": return "Luxembourg";
		case "FR": return "France";
		default:
			System.err.println("Non handled country code: " + countryCode2letters);
			return null;
		}
	}

}
