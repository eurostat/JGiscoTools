package eu.europa.ec.eurostat.jgiscotools.gisco_processes.EBMvalidation;

import java.util.ArrayList;
import java.util.Collection;

import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geodiff.GeoDiff;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class GeodiffTests {

	public static void main(String[] args) {
		System.out.println("Start");

		String inFolder = "/home/juju/Bureau/gisco/geodata/EBM/";
		String outFolder = "/home/juju/Bureau/gisco/EBM_validation/geodiff/";

		for(String t : new String[] { "NUTS_1", "NUTS_2", "NUTS_3", "LAU", "EBM_P", "EBM_A" }) {
			System.out.println("GeoDiff of " + t);

			SimpleFeatureType sc1 = GeoData.getSchema(inFolder + "2022_"+t+".gpkg");
			//System.out.println(sc1);
			//System.out.println( sc1.getGeometryDescriptor() );

			SimpleFeatureType sc2 = GeoData.getSchema(inFolder + "2023_"+t+".gpkg");
			//System.out.println(sc2);
			//System.out.println( sc2.getGeometryDescriptor() );

			ArrayList<Feature> fs1 = GeoData.getFeatures(inFolder + "2022_"+t+".gpkg", "inspireId");
			System.out.println(fs1.size());

			ArrayList<Feature> fs2 = GeoData.getFeatures(inFolder + "2023_"+t+".gpkg", "inspireId");
			System.out.println(fs2.size());


			GeoDiff gd = new GeoDiff(fs1, fs2, 20);
			//SimpleFeatureTypeImpl BasicGeometry/EBM_P identified extends Feature(geom:geom,OBJECTID:OBJECTID,inspireId:inspireId,beginLifespanVersion:beginLifespanVersion,ICC:ICC,SHN:SHN)
			//SimpleFeatureTypeImpl BasicGeometry/EBM_P identified extends Feature(geom:geom,OBJECTID:OBJECTID,inspireId:inspireId,beginLifespanVersion:beginLifespanVersion,ICC:ICC,SHN:SHN)
			gd.setAttributesToIgnore("OBJECTID");


			//
			if(gd.getDifferences().size() > 0)
				GeoData.save(gd.getDifferences(), outFolder+t+"/geodiff.gpkg", sc1.getCoordinateReferenceSystem());
			if(gd.getHausdorffGeomDifferences().size() > 0)
				GeoData.save(gd.getHausdorffGeomDifferences(), outFolder+t+"/hausdorf.gpkg", sc1.getCoordinateReferenceSystem());
			if(gd.getIdentical().size() > 0)
				GeoData.save(gd.getIdentical(), outFolder+t+"/identical.gpkg", sc1.getCoordinateReferenceSystem());
			if(gd.getGeomDifferences().size() > 0)
				GeoData.save(gd.getGeomDifferences(), outFolder+t+"/geomDifferences.gpkg", sc1.getCoordinateReferenceSystem());

			Collection<Feature> si = GeoDiff.findIdStabilityIssues(gd.getDifferences(), 20);
			System.out.println("Stability issues: " + si.size());
			if(si.size() > 0)
				GeoData.save(si, outFolder+t+"/idStabIssues.gpkg", sc1.getCoordinateReferenceSystem());

		}

		System.out.println("End");
	}

}
