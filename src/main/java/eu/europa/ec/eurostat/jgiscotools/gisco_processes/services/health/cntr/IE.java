package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.Validation;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

public class IE {

	static String cc = "IE";

	public static void main(String[] args) {
		System.out.println("Start");

		//load input data
		ArrayList<Map<String, String>> data = CSVUtil.load(HCUtil.path+cc + "/IE_raw.csv");
		System.out.println(data.size());

		CSVUtil.renameColumn(data, "web", "url");
		CSVUtil.renameColumn(data, "telephone", "tel");
		CSVUtil.removeColumn(data, "fax");
		CSVUtil.renameColumn(data, "beds", "cap_beds");
		CSVUtil.replaceValue(data, "cap_beds", "No. Beds: ", "");
		CSVUtil.removeColumn(data, "s_hai_ed");
		CSVUtil.removeColumn(data, "category");
		CSVUtil.renameColumn(data, "subcategory", "facility_type");

		CSVUtil.addColumn(data, "cc", cc);
		CSVUtil.addColumn(data, "site_name", "");
		CSVUtil.addColumn(data, "ref_date", "01/04/2020");
		CSVUtil.addColumn(data, "pub_date", "");
		CSVUtil.addColumn(data, "geo_qual", "1");
		CSVUtil.addColumn(data, "emergency", "");
		CSVUtil.addColumn(data, "public_private", "");

		//TODO try to further decompose the addresses OR use reverse geocoding
		CSVUtil.renameColumn(data, "address", "street");

		for(Map<String, String> h : data) {
			//name
			String name1 = h.get("name");
			String name2 = h.get("alternate_name");
			String name = name1.length()>name2.length() ? name1 : name2;
			h.put("hospital_name", name);
		}
		CSVUtil.removeColumn(data, "name");
		CSVUtil.removeColumn(data, "alternate_name");

		Validation.validate(data, cc);

		System.out.println("Save " + data.size());
		CSVUtil.save(data, HCUtil.path+cc + "/"+cc+".csv");
		GeoData.save(CSVUtil.CSVToFeatures(data, "lon", "lat"), HCUtil.path+cc + "/"+cc+".gpkg", ProjectionUtil.getWGS_84_CRS());

		System.out.println("End");
	}

}
