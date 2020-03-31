package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.geocoding.BingGeocoder;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.LocalParameters;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.ServicesGeocoding;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.ValidateCSV;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

public class SI {

	public static void main(String[] args) {
		System.out.println("Start");

		//load data
		ArrayList<Map<String, String>> data = CSVUtil.load(HCUtil.path + "SI/SI_geolocated.csv");
		System.out.println(data.size());

		CSVUtil.addColumn(data, "cc", "SI");
		CSVUtil.addColumn(data, "geo_qual", "-1"); //ask kimberly
		CSVUtil.addColumn(data, "ref_date", "06/03/2020");
		CSVUtil.addColumns(data, HCUtil.cols, "");

		for(Map<String, String> h : data) {
			String s = h.get("postcode_city");
			String postcode = s.substring(0, 4);
			String city = s.substring(5, s.length());
			h.put("postcode", postcode);
			h.put("city", city);

			s = h.get("street_number");
			String[] parts = s.split(" ");
			String hn = parts[parts.length-1];
			s = s.replace(hn, "").trim();
			h.put("street", s);
		}
		CSVUtil.removeColumn(data, "postcode_city");
		CSVUtil.removeColumn(data, "street_number");

		// save 1
		System.out.println(data.size());
		CSVUtil.save(data, HCUtil.path + "SI/SI.csv");

		LocalParameters.loadProxySettings();
		ServicesGeocoding.set(BingGeocoder.get(), data, "lon", "lat", true, true);

		ValidateCSV.validate(data, "SI");

		GeoData.save(CSVUtil.CSVToFeatures(data, "lon", "lat"), HCUtil.path + "SI/SI.gpkg", ProjectionUtil.getWGS_84_CRS());

		System.out.println("End");
	}

}
