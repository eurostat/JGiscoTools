package eu.europa.ec.eurostat.jgiscotools.gisco_processes.EBMvalidation;

import java.util.ArrayList;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class GeodiffTests {

	public static void main(String[] args) {

		
		ArrayList<Feature> fs2 = GeoData.getFeatures("/home/juju/Bureau/gisco/geodata/EBM/2023/StatisticalUnits/NUTS_3.gpkg");
		System.out.println(fs2.size());
		
	}

}
