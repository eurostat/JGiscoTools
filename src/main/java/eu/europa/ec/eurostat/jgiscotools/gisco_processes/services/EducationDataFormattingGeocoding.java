package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class EducationDataFormattingGeocoding {

	static String path = "E:\\dissemination\\shared-data\\MS_data\\Service - Education\\";

	// Consider classifying levels by
	// ISCE  https://en.wikipedia.org/wiki/International_Standard_Classification_of_Education

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

	//	formatMT();
		formatES();
	//  formatHU();
		
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

}