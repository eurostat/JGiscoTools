package eu.europa.ec.eurostat.jgiscotools.gisco_processes.adminValidation;

import java.io.File;
import java.util.ArrayList;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.TesselationQuality;

public class TopologyCheck {

	//-Xms4g -Xmx12g
	public static void main(String[] args) {
		System.out.println("Start");

		String path = "/home/juju/Bureau/gisco/adm_validity_check/";

		//SimpleFeatureType sc = GeoData.getSchema(inFolder + "2023_"+t+".gpkg");
		ArrayList<Feature> fs = GeoData.getFeatures(path + "OSM_ALL.gpkg", "id");
		System.out.println(fs.size());

		String outFile = path + "topocheck_OSM_ALL.csv";
		File f = new File(outFile); if(f.exists()) f.delete();
		TesselationQuality.checkQuality(fs, 0.01, outFile, false, 100000, 20000, true);

		System.out.println("End");
	}

}
