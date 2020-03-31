/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start");

		//load
		for(String cc : HCUtil.ccs) {
			System.out.println("*** " + cc);

			//load data
			ArrayList<Map<String, String>> data = CSVUtil.load(HCUtil.path + cc+"/"+cc+".csv");
			System.out.println(data.size());

			validate(data, cc);
		}
		System.out.println("End");
	}

	public static void validate(Collection<Map<String, String>> data, String cc) {

		//check presence of all columns
		Set<String> ch = checkNoUnexpectedColumn(data, HCUtil.cols_);
		if(ch.size()>0) System.err.println(ch);

		boolean b;

		//id should be provided
		b = checkValuesNotNullOrEmpty(data, "id");
		if(!b) System.err.println("Identifier not provided for " + cc);

		//check id is provided and unique
		Set<String> dup = checkIdUnicity(data, "id");
		if(dup.size()>0) {
			System.err.println(dup.size() + " non unique identifiers for " + cc);
			for(String d : dup) System.out.print(d + ", ");
			System.out.println();
		}

		//check cc
		b = checkValuesAmong(data, "cc", cc);
		if(!b) System.err.println("Problem with cc values for " + cc);

		//check emergency -yes/no
		b = checkValuesAmong(data, "emergency", "", "yes", "no");
		if(!b) System.err.println("Problem with emergency values for " + cc);

		//check public_private - public/private
		b = checkValuesAmong(data, "public_private", "", "public", "private");
		if(!b) System.err.println("Problem with public_private values for " + cc);

		//check geo_qual -1,1,2,3
		b = checkValuesAmong(data, "geo_qual", "-1", "1", "2", "3");
		if(!b) System.err.println("Problem with geo_qual values for " + cc);

		//check date format DD/MM/YYYY
		b = checkDateFormat(data, "ref_date", HCUtil.dateFormat);
		if(!b) System.err.println("Problem with ref_date format for " + cc);
		checkDateFormat(data, "pub_date", HCUtil.dateFormat);
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

	private static void checkGeoExtent(Collection<Map<String, String>> data, String lonCol, String latCol) {
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

	private static Set<String> checkIdUnicity(Collection<Map<String, String>> data, String idCol) {
		ArrayList<String> ids = CSVUtil.getValues(data, idCol);

		Set<String> duplicates = new LinkedHashSet<>();
		Set<String> uniques = new HashSet<>();
		for(String id : ids)
			if(!uniques.add(id)) duplicates.add(id);

		return duplicates;
	}

	private static boolean checkDateFormat(Collection<Map<String, String>> data, String col, SimpleDateFormat df) {
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

	private static boolean checkValuesNotNullOrEmpty(Collection<Map<String, String>> data, String col) {
		for(Map<String, String> h : data) {
			String val = h.get(col);
			if(val == null || val.isEmpty())
				return false;
		}
		return true;
	}

	private static boolean checkValuesAmong(Collection<Map<String, String>> data, String col, String... values) {
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

	static Set<String> checkNoUnexpectedColumn(Collection<Map<String, String>> data, Collection<String> cols) {
		for(Map<String, String> h : data) {
			Set<String> cs = new HashSet<>(h.keySet());
			cs.removeAll(cols);
			if(cs.size() != 0)
				return cs;
		}
		return new HashSet<String>();
	}

}
