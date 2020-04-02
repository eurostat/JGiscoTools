package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class AT {

	/**
	 * Format AT
	 */
	public static void formatAT() {

		String filePath = HCUtil.path + "AT/KA-Verzeichnis 2019-10-15.csv";
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


}
