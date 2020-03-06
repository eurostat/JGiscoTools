package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.io.File;
import java.io.FileNotFoundException;
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

public class EducationDataFormattingGeocoding {

	static String path = "E:\\dissemination\\shared-data\\MS_data\\Service - Education\\";

	// Consider classifying levels by
	// ISCE  https://en.wikipedia.org/wiki/International_Standard_Classification_of_Education

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

	//	formatMT();
	//	formatES();
	//  formatHU();
		formatBE();
		
//		geocodeMT();

		System.out.println("End");
	}

//	public static void formatMT() {
//
//		String filePath = path + "MT/Primary_Secondary schools by locality.csv";
//		ArrayList<Map<String, String>> schools = CSVUtil.load(filePath,
//				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
//		System.out.println(schools.size());
//
//		Collection<Map<String, String>> schoolsFormatted = new ArrayList<Map<String, String>>();
//		for (Map<String, String> s : schools) {
//
//			// new formatted school
//			HashMap<String, String> sf = new HashMap<String, String>();
//
//			// copy columns
//
//			sf.put("name", s.get("Name"));
//			sf.put("city", s.get("Locality"));
//			sf.put("country", "Malta");
//
//			// add to list
//			schoolsFormatted.add(sf);
//		}
//		// save
//		System.out.println("Start save");
//		CSVUtil.save(schoolsFormatted,
//				"E:\\dissemination\\shared-data\\MS_data\\Service - Education\\MT/MT_formatted.csv");
//		System.out.println("End save");
//	}
	
public static void formatES() {

		
		String filePath = path+"ES/Listado_non-university_en.csv";
		ArrayList<Map<String,String>> ES = CSVUtil.load(filePath, CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(ES.size());
		
		Collection<Map<String, String>> schoolsFormatted = new ArrayList<Map<String,String>>();
		for(Map<String, String> s1 : ES) {

			//new formatted school
			HashMap<String, String> sf = new HashMap<String, String>();
		
			//copy columns

			sf.put("name", s1.get("Name"));
			sf.put("city", s1.get("Locality"));
			sf.put("postcode", s1.get("C. Postal"));
			sf.put("country", "Spain");
			sf.put("cc", "ES");
			sf.put("school_type",s1.get("Type"));
			sf.put("house_number", "");
			sf.put("street", "");
				

			// address ES
			//Santos Mártires, s/n
			//San Segundo,  7

			// Split "Address" column into street and house_number
			String ad = s1.get("Address");
			// split in two where there's a comma
			String[] parts = ad.split(",");

			// on the right:
			String rightPart = parts[1];
			if (rightPart.equals("s/n")) {
				sf.put(rightPart, "");
			} else {
				// assign the house_number, or "" if it equals "0"
				sf.put("house_number", rightPart.equals("0") ? "" : rightPart);
			}
			System.out.println(rightPart);


//			String leftPart = parts[0];
//			sf.put("street", leftPart);
			

			
			//add to list
			schoolsFormatted.add(sf);
	}
		// save
//		System.out.println("Start save");
//		CSVUtil.save(schoolsFormatted, "E:\\dissemination\\shared-data\\MS_data\\Service - Education\\ES/ES_formatted.csv");
//		System.out.println("End save");
	}

	public static void formatHU() {

		String filePath = path + "HU/List of public education institutions in operation_2020_02_16_EN.csv";
		ArrayList<Map<String, String>> schools = CSVUtil.load(filePath,
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(schools.size());

		Collection<Map<String, String>> schoolsFormatted = new ArrayList<Map<String, String>>();
		for (Map<String, String> HU : schools) {

			// new formatted school
			HashMap<String, String> sf = new HashMap<String, String>();

			// copy columns

			sf.put("name", HU.get("Name"));
			// street
			// house_number
			sf.put("postcode", HU.get("Postcode"));
			sf.put("city", HU.get("City"));
			sf.put("cc", "HU");
			sf.put("country", "Hungary");
			sf.put("level", "");
			sf.put("level", "");
			sf.put("cap_students", "");
			sf.put("cap_teachers", "");
			sf.put("enrollment", "");
			sf.put("school_type", HU.get("Type"));
			sf.put("pub_priv", "public");
			sf.put("tel", HU.get("Phone"));
			sf.put("email", HU.get("E-mail"));
			sf.put("url", "");

			// address HU
			// Mezőszél utca 2.

			// Split "Address" column into street and house_number
			String ad = HU.get("Address");
			// split in two where there's a comma
			String[] parts = ad.split(",");

			// on the right:
			String rightPart = parts[1];
			if (rightPart.equals("s/n")) {
				sf.put(rightPart, "");
			} else {
				// assign the house_number, or "" if it equals "0"
				sf.put("house_number", rightPart.equals("0") ? "" : rightPart);
			}
			System.out.println(rightPart);

//					String leftPart = parts[0];
//					sf.put("street", leftPart);	
		
		//add to list
		schoolsFormatted.add(sf);
}
	//save
	System.out.println("Start save");
	CSVUtil.save(schoolsFormatted, "E:\\dissemination\\shared-data\\MS_data\\Service - Education\\MT/MT_formatted.csv");
	System.out.println("End save");
}

	public static void formatBE() throws Exception {

		try {
		/*
		 * String url = "http://infrastructura-sanatate.ms.ro/harta/extractSpitaleGPS";
		 * System.out.println(url); BufferedReader in = new BufferedReader(new
		 * InputStreamReader(new URL(url).openStream())); String line = in.readLine();
		 * System.out.println(line); //String[] parts = line.split(",");
		 */
		System.out.println("new scanner");
		Scanner scanner = new Scanner(new File(path + "BE/Flanders/universities.json"));
		String line = scanner.nextLine();
		scanner.close();
		System.out.println(line);
		System.out.println("scanner close");

		JSONObject data = (JSONObject) new JSONParser().parse(line);
		//print names of keys: meta, content
		System.out.println(data.keySet());
		JSONArray data_ = (JSONArray) data.get("content");
			Collection<Map<String, String>> unisFormatted = new ArrayList<Map<String,String>>();
			for(Object U : data_) {
				JSONObject u = (JSONObject)U;	
				//id, name, shortName, street, houseNumber, busNumber, city, postalCode, email, telephoneNumber, type, url,
				//admissionAndFurtherStudiesUrl, reviewStatus, startDateReview, endDateReview, associations


				//new formatted hospital
				HashMap<String, String> uf = new HashMap<String, String>();
				uf.put("country", "Belgium");
				uf.put("cc", "BE");
				uf.put("id", u.get("id").toString());
				uf.put("name", u.get("name").toString());
				uf.put("house_number", u.get("houseNumber").toString());
				uf.put("city", u.get("city").toString());
				uf.put("postcode", u.get("postalCode").toString());
			//	uf.put("email", u.get("email").toString());
				uf.put("tel", u.get("telephoneNumber").toString());
				uf.put("url", u.get("url").toString());
				uf.put("school_type", u.get("type").toString());
				uf.put("published_date", "09-19"); //TODO check
				uf.put("retrieval_date", "26-02-20"); //TODO check


				unisFormatted.add(uf);
			}
			//save
			CSVUtil.save(unisFormatted, path+"BE/BE_geolocated.csv");

		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	
	}
}
