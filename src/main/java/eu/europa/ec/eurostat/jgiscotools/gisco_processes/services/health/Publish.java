/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.deprecated.NUTSUtils;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

/**
 * Copy country CSV files to github repository.
 * Combine them in the all.csv file.
 * Convert as GeoJSON and GPKG format.
 * 
 * @author julien Gaffuri
 *
 */
public class Publish {

	static String destinationPath = "C:/Users/gaffuju/workspace/healthcare-services/";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start");

		//publication date
		String timeStamp = ValidateCSV.dateFormat.format(Calendar.getInstance().getTime());
		System.out.println(timeStamp);

		//make outpur folders
		new File(destinationPath + "data/csv/").mkdirs();
		new File(destinationPath + "data/geojson/").mkdirs();
		new File(destinationPath + "data/gpkg/").mkdirs();

		ArrayList<Map<String, String>> all = new ArrayList<Map<String, String>>();
		for(String cc : ValidateCSV.ccs) {
			System.out.println("*** " + cc);

			//load data
			ArrayList<Map<String, String>> data = CSVUtil.load(ValidateCSV.path + cc+"/"+cc+".csv");
			System.out.println(data.size());

			//cc, country
			String cntr = NUTSUtils.getName(cc);
			if(cntr == null) System.err.println("cc: " + cc);
			cntr.replace("Germany (until 1990 former territory of the FRG)", "Germany");
			for(Map<String, String> h :data) {
				h.put("cc", cc);
				h.put("country", cntr);
			}

			//apply publication date
			for(Map<String, String> h : data)
				h.put("pub_date", timeStamp);

			//store for big EU file
			all.addAll(data);

			//export as geojson and GPKG
			CSVUtil.save(data, destinationPath+"data/csv/"+cc+".csv", ValidateCSV.cols_);
			Collection<Feature> fs = CSVUtil.CSVToFeatures(data, "lon", "lat");
			applyTypes(fs);
			GeoData.save(fs, destinationPath+"data/geojson/"+cc+".geojson", ProjectionUtil.getWGS_84_CRS());
			GeoData.save(fs, destinationPath+"data/gpkg/"+cc+".gpkg", ProjectionUtil.getWGS_84_CRS());
		}

		//append cc to id
		for(Map<String, String> h : all) {
			String cc = h.get("cc");
			String id = h.get("id");
			if(id == null || "".equals(id)) {
				System.err.println("No identifier for items in " + cc);
				break;
			}
			String cc_ = id.length()>=2? id.substring(0, 2) : "";
			if(cc_.equals(cc)) continue;
			h.put("id", cc + "_" + id);
		}

		//export all
		System.out.println("*** All");
		System.out.println(all.size());
		CSVUtil.save(all, destinationPath+"data/csv/all.csv", ValidateCSV.cols_);
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
