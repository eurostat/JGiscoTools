package eu.europa.ec.eurostat.jgiscotools.gisco_processes.EBMvalidation;

import java.util.ArrayList;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.TesselationQuality;

public class TopologyCheck {

	public static void main(String[] args) {
		System.out.println("Start");

		String inFolder = "/home/juju/Bureau/gisco/geodata/EBM/";
		String outFolder = "/home/juju/Bureau/gisco/EBM_validation/";

		for(String t : new String[] { "NUTS_3", "EBM_A", "LAU", "NUTS_2", "NUTS_1" }) {
			System.out.println("Topology quality of " + t);

			//SimpleFeatureType sc = GeoData.getSchema(inFolder + "2023_"+t+".gpkg");
			ArrayList<Feature> fs = GeoData.getFeatures(inFolder + "2023_"+t+".gpkg", "inspireId");
			System.out.println(fs.size());

			TesselationQuality.checkQuality(fs, 1, outFolder + "topocheck_"+t+".csv", true, false, 100000, 20000, true);
		}

		System.out.println("End");
	}

}
