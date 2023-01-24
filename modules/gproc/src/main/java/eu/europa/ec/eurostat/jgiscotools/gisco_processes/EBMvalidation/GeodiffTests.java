package eu.europa.ec.eurostat.jgiscotools.gisco_processes.EBMvalidation;

import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class GeodiffTests {

	public static void main(String[] args) {
		System.out.println("Start");

		SimpleFeatureType sc1 = GeoData.getSchema("/home/juju/Bureau/gisco/geodata/EBM/2022/StatisticalUnits/NUTS_3.gpkg");
		System.out.println(sc1);
		SimpleFeatureType sc2 = GeoData.getSchema("/home/juju/Bureau/gisco/geodata/EBM/2022/StatisticalUnits/NUTS_3.gpkg");
		System.out.println(sc2);


		//ArrayList<Feature> fs1 = GeoData.getFeatures("/home/juju/Bureau/gisco/geodata/EBM/2022/StatisticalUnits/NUTS_3.gpkg");
		//System.out.println(fs1.size());

		//ArrayList<Feature> fs2 = GeoData.getFeatures("/home/juju/Bureau/gisco/geodata/EBM/2023/StatisticalUnits/NUTS_3.gpkg");
		//System.out.println(fs2.size());


		System.out.println("End");
	}

}
