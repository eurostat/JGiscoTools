package eu.europa.ec.eurostat.jgiscotools.gisco_processes.EBMvalidation;

import java.util.ArrayList;

import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.TesselationQuality;

public class TopologyCheck {

	public static void main(String[] args) {
		System.out.println("Start");

		String inFolder = "/home/juju/Bureau/gisco/geodata/EBM/";
		String outFolder = "/home/juju/Bureau/gisco/EBM_validation/geodiff/";

		for(String t : new String[] { /*"EBM_A",*/ "NUTS_3",/* "LAU", "NUTS_2", "NUTS_1"*/ }) {
			System.out.println("Topology quality of " + t);

			SimpleFeatureType sc1 = GeoData.getSchema(inFolder + "2023_"+t+".gpkg");
			ArrayList<Feature> fs1 = GeoData.getFeatures(inFolder + "2023_"+t+".gpkg", "inspireId");
			System.out.println(fs1.size());

			TesselationQuality.checkQuality(null, 0, null, false, false, 0, 0, false);

		}
		
	}

}
