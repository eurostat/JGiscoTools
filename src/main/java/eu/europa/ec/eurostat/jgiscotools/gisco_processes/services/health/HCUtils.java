/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * @author gaffuju
 *
 */
public class HCUtils {

	static String path = "E:/dissemination/shared-data/MS_data/Service - Health/";

	//country codes covered
	static String[] ccs = new String[] { "AT", "BE", "CH", "CY", "DE", "DK", "ES", "FI", "FR", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PT", "RO", "SE", "UK" };

	//CSV columns
	static String[] cols = new String[] {
			"id", "hospital_name", "site_name", "lat", "lon", "street", "house_number", "postcode", "city", "cc", "country", "emergency", "cap_beds", "cap_prac", "cap_rooms", "facility_type", "public_private", "list_specs", "tel", "email", "url", "ref_date", "pub_date", "geo_qual"
	};
	static List<String> cols_ = Arrays.asList(cols);

	//date format
	static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");








	static void replace(ArrayList<Map<String, String>> data, String col, String iniVal, String finVal) {
		for(Map<String, String> h :data) {
			String v = h.get(col);
			if(iniVal == null && v == null || iniVal != null && iniVal.equals(v))
				h.put(col, finVal);
		}
	}

	static void applyTypes(Collection<Feature> fs) {
		for(Feature f : fs) {
			for(String att : new String[] {"cap_beds", "cap_prac", "cap_rooms"}) {
				Object v = f.getAttribute(att);
				if(v==null) continue;
				if("".equals(v)) f.setAttribute(att, null);
				else f.setAttribute(att, Integer.parseInt(v.toString()));
			}
			for(String att : new String[] {"lat", "lon"}) {
				Object v = f.getAttribute(att);
				if(v==null) continue;
				if("".equals(v)) f.setAttribute(att, null);
				else f.setAttribute(att, Double.parseDouble(v.toString()));
			}
		}
	}


	static ArrayList<String> getValues(ArrayList<Map<String, String>> data, String col) {
		ArrayList<String> out = new ArrayList<>();
		for(Map<String, String> h : data)
			out.add(h.get(col));
		return out;
	}

	static void replaceValue(ArrayList<Map<String, String>> data, String ini, String fin) {
		for(Map<String, String> h : data)
			for(Entry<String,String> e : h.entrySet())
				if(e.getValue() != null && ini.equals(e.getValue()))
					e.setValue(fin);
	}

	static void changeColumnName(ArrayList<Map<String, String>> data, String old, String new_) {
		for(Map<String, String> h : data) {
			if(h.get(old) != null) {
				h.put(new_, h.get(old));
				h.remove(old);
			}
		}
	}

	static void removeColumn(ArrayList<Map<String, String>> data, String col) {
		for(Map<String, String> h : data) {
			if(h.get(col) != null)
				h.remove(col);
		}
	}


	static void populateAllColumns(Collection<Map<String, String>> data, String[] cols, String defaultValue) {
		for(String col : cols)
			for(Map<String, String> h : data) {
				if(h.get(col) == null || "".equals(h.get(col))) {
					h.put(col, defaultValue);
				}
			}
	}

}
