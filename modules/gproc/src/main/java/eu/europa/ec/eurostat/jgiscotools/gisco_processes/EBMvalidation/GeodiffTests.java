package eu.europa.ec.eurostat.jgiscotools.gisco_processes.EBMvalidation;

import java.util.ArrayList;

import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geodiff.GeoDiff;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class GeodiffTests {

	public static void main(String[] args) {
		System.out.println("Start");

		String t = "EBM_P";

		SimpleFeatureType sc1 = GeoData.getSchema("/home/juju/Bureau/gisco/geodata/EBM/2022/"+t+".gpkg");
		System.out.println(sc1);
		//System.out.println( sc1.getGeometryDescriptor() );

		SimpleFeatureType sc2 = GeoData.getSchema("/home/juju/Bureau/gisco/geodata/EBM/2023/"+t+".gpkg");
		System.out.println(sc2);
		//System.out.println( sc2.getGeometryDescriptor() );


		ArrayList<Feature> fs1 = GeoData.getFeatures("/home/juju/Bureau/gisco/geodata/EBM/2022/"+t+".gpkg", "inspireId");
		System.out.println(fs1.size());

		ArrayList<Feature> fs2 = GeoData.getFeatures("/home/juju/Bureau/gisco/geodata/EBM/2023/"+t+".gpkg", "inspireId");
		System.out.println(fs2.size());


		GeoDiff gd = new GeoDiff(fs1, fs2, 20);
		//SimpleFeatureTypeImpl BasicGeometry/EBM_P identified extends Feature(geom:geom,OBJECTID:OBJECTID,inspireId:inspireId,beginLifespanVersion:beginLifespanVersion,ICC:ICC,SHN:SHN)
		//SimpleFeatureTypeImpl BasicGeometry/EBM_P identified extends Feature(geom:geom,OBJECTID:OBJECTID,inspireId:inspireId,beginLifespanVersion:beginLifespanVersion,ICC:ICC,SHN:SHN)
		gd.setAttributesToIgnore("OBJECTID");


		//
		if(gd.getDifferences().size() > 0)
			GeoData.save(gd.getDifferences(), "/home/juju/Bureau/gisco/EBM_validation/geodiff/"+t+"/geodiff.gpkg", sc1.getCoordinateReferenceSystem());
		if(gd.getHausdorffGeomDifferences().size() > 0)
			GeoData.save(gd.getHausdorffGeomDifferences(), "/home/juju/Bureau/gisco/EBM_validation/geodiff/"+t+"/hausdorf.gpkg", sc1.getCoordinateReferenceSystem());
		if(gd.getIdentical().size() > 0)
			GeoData.save(gd.getIdentical(), "/home/juju/Bureau/gisco/EBM_validation/geodiff/"+t+"/identical.gpkg", sc1.getCoordinateReferenceSystem());
		if(gd.getGeomDifferences().size() > 0)
			GeoData.save(gd.getGeomDifferences(), "/home/juju/Bureau/gisco/EBM_validation/geodiff/"+t+"/geomDifferences.gpkg", sc1.getCoordinateReferenceSystem());


		System.out.println("End");
	}

}
