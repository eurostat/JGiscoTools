/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

	static String path = "E:/dissemination/shared-data/MS_data/Service - Health/";

	//country codes covered
	static String[] ccs = new String[] { "AT", "BE", "CH", "DE", "DK", "ES", "FI", "FR", "IE", "IT", "LU", "LV", "NL", "PT", "RO", "SE", "UK" };

	//CSV columns
	static String[] cols = new String[] {
			"id", "hospital_name", "site_name", "lat", "lon", "street", "house_number", "postcode", "city", "cc", "country", "emergency", "cap_beds", "cap_prac", "cap_rooms", "facility_type", "public_private", "list_specs", "tel", "email", "url", "ref_date", "pub_date"
	};
	static List<String> cols_ = Arrays.asList(cols);

	//date format
	static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

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

			//check id is provided and unique
			boolean b = checkId(data, "id");
			if(!b) System.err.println("Problem with identifier for " + cc);

			//check emergency -yes/no
			b = checkValuesAmong(data, "emergency", "", "yes", "no");
			if(!b) System.err.println("Problem with emergency values for " + cc);

			//check public_private - public/private
			b = checkValuesAmong(data, "public_private", "", "public", "private");
			if(!b) System.err.println("Problem with public_private values for " + cc);

			//check date format DD/MM/YYYY
			b = checkDateFormat(data, "ref_date", dateFormat);
			if(!b) System.err.println("Problem with ref_date format for " + cc);
			checkDateFormat(data, "pub_date", dateFormat);
			if(!b) System.err.println("Problem with pub_date format for " + cc);

			//non null columns
			b = checkValuesNotNullOrEmpty(data, "hospital_name");
			if(!b) System.err.println("Missing values for hospital_name format for " + cc);
			b = checkValuesNotNullOrEmpty(data, "lat");
			if(!b) System.err.println("Missing values for lat format for " + cc);
			b = checkValuesNotNullOrEmpty(data, "lon");
			if(!b) System.err.println("Missing values for lon format for " + cc);
			b = checkValuesNotNullOrEmpty(data, "ref_date");
			if(!b) System.err.println("Missing values for ref_date format for " + cc);

			//check lon,lat extends
			checkGeoExtent(data, "lon", "lat");

			//TODO other tests ?
			//check list_specs
			//check empty columns
		}
		System.out.println("End");
	}

	private static void checkGeoExtent(ArrayList<Map<String, String>> data, String lonCol, String latCol) {
		for(Map<String, String> h : data) {
			String lon_ = h.get(lonCol);
			String lat_ = h.get(latCol);

			double lon = 0;
			try {
				lon = Double.parseDouble(lon_);
			} catch (NumberFormatException e) {
				System.err.println("Cannot decode longitude value " + lon_);
				continue;
			}
			double lat = 0;
			try {
				lat = Double.parseDouble(lat_);
			} catch (NumberFormatException e) {
				System.err.println("Cannot decode latitude value " + lat_);
				continue;
			}

			if(lat < -90.0 || lat > 90)
				System.err.println("Invalid latitude value: " + lat);
			if(lon < -180.0 || lon > 180)
				System.err.println("Invalid longitude value: " + lon);
		}
	}

	private static boolean checkId(ArrayList<Map<String, String>> data, String idCol) {
		//id values should be provided
		boolean b = checkValuesNotNullOrEmpty(data, idCol);
		if(!b) return false;
		//id values should be unique
		ArrayList<String> valL = getValues(data, "id");
		HashSet<String> valS = new HashSet<String>(valL);
		if(valL.size() != valS.size()) return false;
		return true;
	}

	private static ArrayList<String> getValues(ArrayList<Map<String, String>> data, String col) {
		ArrayList<String> out = new ArrayList<>();
		for(Map<String, String> h : data)
			out.add(h.get(col));
		return out;
	}


	private static boolean checkDateFormat(ArrayList<Map<String, String>> data, String col, SimpleDateFormat df) {
		for(Map<String, String> h : data) {
			String val = h.get(col);
			if(val == null || val.isEmpty())
				continue;
			try {
				df.parse(val);
			} catch (ParseException e) {
				//System.out.println("Could not parse date: " + val);
				return false;
			}
		}
		return true;
	}

	private static boolean checkValuesNotNullOrEmpty(ArrayList<Map<String, String>> data, String col) {
		for(Map<String, String> h : data) {
			String val = h.get(col);
			if(val == null || val.isEmpty())
				return false;
		}
		return true;
	}

	private static boolean checkValuesAmong(ArrayList<Map<String, String>> data, String col, String... values) {
		for(Map<String, String> h : data) {
			String val = h.get(col);
			boolean found = false;
			for(String v : values)
				if(val==null && v==null || v.equals(val)) {
					found=true; break;
				}
			if(!found) {
				//System.err.println("Unexpected value '" + val + "' for column '" + col + "'");
				return false;
			}
		}
		return true;
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
