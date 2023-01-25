package eu.europa.ec.eurostat.jgiscotools.gisco_processes.EBMvalidation;

import java.util.ArrayList;

import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class TTTests2 {

	public static void main(String[] args) {
		System.out.println("Start");

		String inFolder = "/home/juju/Bureau/gisco/geodata/EBM/";
		String t = "NUTS_3";



		SimpleFeatureType sc1 = GeoData.getSchema(inFolder + "2022_"+t+".gpkg");
		//System.out.println(sc1);
		//System.out.println( sc1.getGeometryDescriptor() );

		ArrayList<Feature> fs1 = GeoData.getFeatures(inFolder + "2022_"+t+".gpkg", "inspireId");
		System.out.println("2022: " + fs1.size());

		Feature f = fs1.get(0);
		//System.out.println(f.getAttribute("beginLifespanVersion"));
		//System.out.println(f.getAttribute("beginLifespanVersion").getClass());
		//System.out.println(fs1.get(0).getAttributes());


		//System.out.println("Export");
		GeoData.save(fs1, "/home/juju/Bureau/testExport.gpkg", sc1.getCoordinateReferenceSystem());

		System.out.println("End");
	}

}
