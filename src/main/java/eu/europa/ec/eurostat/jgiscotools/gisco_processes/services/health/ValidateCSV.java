/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

/**
 * Check the country CSV files are complant with the specs.
 * 
 * @author julien Gaffuri
 *
 */
public class ValidateCSV {

	static String path = "E:\\dissemination\\shared-data\\MS_data\\Service - Health\\";

	//country codes covered
	static String[] ccs = new String[] { "AT", "BE", "DE", "DK", "ES", "FI", "FR", "IE", "IT", "LU", "LV", "NL", "PT", "RO", "SE", "UK" };

	//CSV columns
	static String[] cols = new String[] {
			"id", "hospital_name", "site_name", "lat", "lon", "street", "house_number", "postcode", "city", "cc", "country", "emergency", "cap_beds", "cap_prac", "cap_rooms", "facility_type", "public_private", "list_specs", "tel", "email", "url", "ref_date", "pub_date"
	};
	static List<String> cols_ = Arrays.asList(cols);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start");

		//load
		for(String cc : ccs) {
			System.out.println("*** " + cc);

			//load data
			ArrayList<Map<String, String>> data = CSVUtil.load(path + cc+"/"+cc+".csv");
			System.out.println(data.size());

			//check presence of all columns
			Set<String> ch = checkNoUnexpectedColumn(data, cols_);
			if(ch.size()>0) System.err.println(ch);

			//TODO other tests
		}
		System.out.println("End");
	}

	private static Set<String> checkNoUnexpectedColumn(ArrayList<Map<String, String>> data, Collection<String> cols) {
		for(Map<String, String> h : data) {
			Set<String> cs = new HashSet<>(h.keySet());
			cs.removeAll(cols);
			if(cs.size() != 0)
				return cs;
		}
		return new HashSet<String>();
	}

}
