package eu.europa.ec.eurostat.jgiscotools.gisco_processes.adminValidation;

import java.util.ArrayList;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.TesselationQuality;

public class TopologyCheck {

	public static void main(String[] args) {
		System.out.println("Start");

		String inFolder = "/home/juju/Bureau/gisco/adm_validity_check/";
		String outFolder = "/home/juju/Bureau/gisco/adm_validity_check/";


		//SimpleFeatureType sc = GeoData.getSchema(inFolder + "2023_"+t+".gpkg");
		ArrayList<Feature> fs = GeoData.getFeatures(inFolder + "2023_"+t+".gpkg", "inspireId");
		System.out.println(fs.size());

		TesselationQuality.checkQuality(fs, 1, outFolder + "topocheck_"+t+".csv", true, false, 100000, 20000, true);

		System.out.println("End");
	}

}
