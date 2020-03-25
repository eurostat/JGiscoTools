/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

/**
 * Copy country CSV files to github repository.
 * Combine them in the all.csv file.
 * 
 * @author julien Gaffuri
 *
 */
public class HealthCarePublish {

	static String originPath = "E:\\dissemination\\shared-data\\MS_data\\Service - Health\\";
	static String destinationPath = "C:\\Users\\gaffuju\\workspace\\healthcare-services\\";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start");

		//the desired columns, ordered
		String[] cols = new String[] {
				"id", "hospital_name", "site_name", "lat", "lon", "street", "house_number", "postcode", "city", "cc", "country", "emergency", "cap_beds", "cap_prac", "cap_rooms", "facility_type", "public_private", "list_specs", "tel", "email", "url", "ref_date", "pub_date"
		};
		List<String> cols_ = Arrays.asList(cols);

		ArrayList<Map<String, String>> all = new ArrayList<Map<String, String>>();
		//load
		for(String cc : new String[] { "AT", "BE", "DE", "DK", "ES", "FI", "FR", "IE", "IT", "LU", "LV", "NL", "PT", "RO", "SE", "UK" }) {
			System.out.println("*** " + cc);

			//load data
			ArrayList<Map<String, String>> data = CSVUtil.load(originPath+cc+".csv");
			System.out.println(data.size());

			//store for big EU file
			all.addAll(data);

			//export as geojson and GPKG
			CSVUtil.save(data, destinationPath+"data/csv/"+cc+".csv", cols_);
			Collection<Feature> fs = CSVUtil.CSVToFeatures(data, "lon", "lat");
			applyTypes(fs);
			GeoData.save(fs, destinationPath+"data/geojson/"+cc+".geojson", ProjectionUtil.getWGS_84_CRS());
			GeoData.save(fs, destinationPath+"data/gpkg/"+cc+".gpkg", ProjectionUtil.getWGS_84_CRS());
		}

		//export all
		System.out.println("*** All");
		System.out.println(all.size());
		CSVUtil.save(all, destinationPath+"data/csv/all.csv", cols_);
		Collection<Feature> fs = CSVUtil.CSVToFeatures(all, "lon", "lat");
		applyTypes(fs);
		GeoData.save(fs, destinationPath+"data/geojson/all.geojson", ProjectionUtil.getWGS_84_CRS());
		GeoData.save(fs, destinationPath+"data/gpkg/all.gpkg", ProjectionUtil.getWGS_84_CRS());

		System.out.println("End");
	}

	private static void applyTypes(Collection<Feature> fs) {
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

}
