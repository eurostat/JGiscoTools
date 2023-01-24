package eu.europa.ec.eurostat.jgiscotools.gisco_processes.EBMvalidation;

import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class GeodiffTests {

	public static void main(String[] args) {
		System.out.println("Start");

		String t = "EBM_A";

		SimpleFeatureType sc1 = GeoData.getSchema("/home/juju/Bureau/gisco/geodata/EBM/2022/BasicGeometry/"+t+".gpkg");
		System.out.println(sc1);
		System.out.println( sc1.getGeometryDescriptor() );

		SimpleFeatureType sc2 = GeoData.getSchema("/home/juju/Bureau/gisco/geodata/EBM/2023/BasicGeometry/"+t+".gpkg");
		System.out.println(sc2);
		System.out.println( sc2.getGeometryDescriptor() );


		//ArrayList<Feature> fs1 = GeoData.getFeatures("/home/juju/Bureau/gisco/geodata/EBM/2022/BasicGeometry/"+t+".gpkg");
		//System.out.println(fs1.size());

		//ArrayList<Feature> fs2 = GeoData.getFeatures("/home/juju/Bureau/gisco/geodata/EBM/2023/BasicGeometry/"+t+".gpkg");
		//System.out.println(fs2.size());

		System.out.println("End");
	}

}
