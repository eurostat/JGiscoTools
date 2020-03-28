package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.util.ArrayList;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class FR {

	public static void main(String[] args) {

		//load data
		ArrayList<Map<String, String>> data = CSVUtil.load(HCUtil.path + "FR/FR_geolocated_v3_with_postcodes.csv");
		System.out.println(data.size());



		ValidateCSV.validate(data, "FR");
	}

}
