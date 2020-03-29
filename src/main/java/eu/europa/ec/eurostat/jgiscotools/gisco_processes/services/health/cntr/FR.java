package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.LocalParameters;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

public class FR {

	public static void main(String[] args) {
		LocalParameters.loadProxySettings();

		//TODO extract that - geocoding improvement function

		//load data
		ArrayList<Map<String, String>> data = CSVUtil.load(HCUtil.path + "FR/FR_geolocated_v4.csv");
		System.out.println(data.size());

		CSVUtil.save(data, HCUtil.path + "FR/FR_geolocated_v5.csv");
		GeoData.save(CSVUtil.CSVToFeatures(data, "lon", "lat"), HCUtil.path + "FR/FR_geolocated_v5.gpkg", ProjectionUtil.getWGS_84_CRS());

		/*
		CSVUtil.setValue(data, "emergency", "");

		//CSVUtil.getUniqueValues(data, "public_private", true);
		//[, Etablissement public de santé]
		CSVUtil.replaceValue(data, "public_private", "Etablissement public de santé", "public");
		CSVUtil.setValue(data, "ref_date", "04/03/2020");
		CSVUtil.setValue(data, "pub_date", "28/03/2020");

		//CSVUtil.setValue(data, "geo_qual", "-1");
		for(Map<String, String> h : data) {
			String gc = h.get("geo_confidence");
			if("High".equals(gc))
				h.put("geo_qual", "1");
			else if("Medium".equals(gc))
				h.put("geo_qual", "2");
			else if("Low".equals(gc))
				h.put("geo_qual", "3");
			else System.out.println(gc);			
		}

		for(Map<String, String> h : data) {
			String pc = h.get("postcode").substring(0,3);
			String cc = "";
			switch (pc) {
			case "971": cc="GP"; break;
			case "972": cc="MQ"; break;
			case "973": cc="GF"; break;
			case "974": cc="RE"; break;
			case "975": cc="PM"; break;
			case "976": cc="YT"; break;
			default: cc="FR"; break;
			}
			h.put("cc", cc);
		}

		ValidateCSV.validate(data, "FR");

		CSVUtil.save(data, HCUtil.path + "FR/FR_geolocated_v4.csv");
		GeoData.save(CSVUtil.CSVToFeatures(data, "lon", "lat"), HCUtil.path + "FR/FR_geolocated_v4.gpkg", ProjectionUtil.getWGS_84_CRS());
		 */
		System.out.println("End");
	}

}
