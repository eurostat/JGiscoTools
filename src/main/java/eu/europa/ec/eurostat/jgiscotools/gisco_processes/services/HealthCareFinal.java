/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

/**
 * @author julien Gaffuri
 *
 */
public class HealthCareFinal {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start");

		String path = "/home/juju/Bureau/workspace/healthcare-services/";
		String timeStamp = new SimpleDateFormat("yyyy_MM_dd").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp);

		//the desired columns, ordered
		String[] cols = new String[] {
				"id", "name", "site_name", "lat", "lon", "street", "house_number", "postcode", "city", "cc", "country", "emergency", "cap_beds", "cap_prac", "cap_rooms", "facility_type", "public_private", "list_specs", "tel", "email", "url", "ref_date", "pub_date"
		};
		List<String> cols_ = Arrays.asList(cols);

		ArrayList<Map<String, String>> all = new ArrayList<Map<String, String>>();

		//load
		for(String cc : new String[] { "IT", "AT", "RO", "DE" }) {
			System.out.println("*** "+cc);

			//load data
			ArrayList<Map<String, String>> data = CSVUtil.load(path+"temp/"+cc+".csv");
			System.out.println(data.size());
			System.out.println(data.iterator().next().keySet());

			removeColumn(data, "latGISCO");
			removeColumn(data, "lonGISCO");
			changeColumnName(data, "latBing", "lat");
			changeColumnName(data, "lonBing", "lon");
			changeColumnName(data, "hospital_name", "name");
			changeColumnName(data, "type", "facility_type");
			changeColumnName(data, "year", "ref_date");
			changeColumnName(data, "data_year", "pub_date");

			Set<String> ch = checkNoUnexpectedColumn(data, cols_);
			if(ch.size()>0) System.err.println(ch);

			populateAllColumns(data, cols, null);

			for(Map<String, String> h : data)
				h.put("pub_date", timeStamp);

			System.out.println(data.iterator().next().keySet());

			//export as geojson and GPKG
			CSVUtil.save(data, path+"data/csv/"+cc+".csv", cols_);
			Collection<Feature> fs = CSVUtil.CSVToFeatures(data, "lon", "lat");
			GeoData.save(fs, path+"data/geojson/"+cc+".geojson", ProjectionUtil.getWGS_84_CRS());
			GeoData.save(fs, path+"data/gpkg/"+cc+".gpkg", ProjectionUtil.getWGS_84_CRS());

			//make big EU file
			all.addAll(data);
		}

		//export all
		System.out.println("*** All");
		System.out.println(all.size());
		CSVUtil.save(all, path+"data/csv/all.csv", cols_);
		Collection<Feature> fs = CSVUtil.CSVToFeatures(all, "lon", "lat");
		GeoData.save(fs, path+"data/geojson/all.geojson", ProjectionUtil.getWGS_84_CRS());
		GeoData.save(fs, path+"data/gpkg/all.gpkg", ProjectionUtil.getWGS_84_CRS());

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

	private static void populateAllColumns(ArrayList<Map<String, String>> data, String[] cols, String defaultValue) {
		for(String col : cols)
			for(Map<String, String> h : data) {
				if(h.get(col) != null) continue;
				h.put(col, defaultValue);
			}
	}

	private static void changeColumnName(ArrayList<Map<String, String>> data, String old, String new_) {
		for(Map<String, String> h : data) {
			if(h.get(old) != null) {
				h.put(new_, h.get(old));
				h.remove(old);
			}
		}
	}

	private static void removeColumn(ArrayList<Map<String, String>> data, String col) {
		for(Map<String, String> h : data) {
			if(h.get(col) != null)
				h.remove(col);
		}
	}

}
