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

		for(String t : new String[] { "EBM_A", /*"EBM_P",*/ "NUTS_3", "LAU", "NUTS_2", "NUTS_1", }) {
			System.out.println("GeoDiff of " + t);

			SimpleFeatureType sc1 = GeoData.getSchema(inFolder + "2022_"+t+".gpkg");
			//System.out.println(sc1);
			//System.out.println( sc1.getGeometryDescriptor() );

			//SimpleFeatureType sc2 = GeoData.getSchema(inFolder + "2023_"+t+".gpkg");
			//System.out.println(sc2);
			//System.out.println( sc2.getGeometryDescriptor() );

			ArrayList<Feature> fs1 = GeoData.getFeatures(inFolder + "2022_"+t+".gpkg", "inspireId");
			System.out.println("2022: " + fs1.size());

			ArrayList<Feature> fs2 = GeoData.getFeatures(inFolder + "2023_"+t+".gpkg", "inspireId");
			System.out.println("2023: " + fs2.size());


			GeoDiff gd = new GeoDiff(fs1, fs2, 20);
			//SimpleFeatureTypeImpl BasicGeometry/EBM_P identified extends Feature(geom:geom,OBJECTID:OBJECTID,inspireId:inspireId,beginLifespanVersion:beginLifespanVersion,ICC:ICC,SHN:SHN)
			//SimpleFeatureTypeImpl BasicGeometry/EBM_P identified extends Feature(geom:geom,OBJECTID:OBJECTID,inspireId:inspireId,beginLifespanVersion:beginLifespanVersion,ICC:ICC,SHN:SHN)
			gd.setAttributesToIgnore("OBJECTID", "Shape_Length", "Shape_Area");


			//compute and save geodiff

			System.out.println("Differences: " + gd.getDifferences().size());
			if(gd.getDifferences().size() > 0) {
				for(Feature f : gd.getDifferences()) f.setAttribute("beginLifespanVersion", f.getAttribute("beginLifespanVersion").toString());
				GeoData.save(gd.getDifferences(), outFolder+t+"/geodiff.gpkg", sc1.getCoordinateReferenceSystem());
			}

			System.out.println("Hausdorf Geom Differences: " + gd.getHausdorffGeomDifferences().size());
			if(gd.getHausdorffGeomDifferences().size() > 0)
				GeoData.save(gd.getHausdorffGeomDifferences(), outFolder+t+"/hausdorf.gpkg", sc1.getCoordinateReferenceSystem());

			/*System.out.println("Identical: " + gd.getIdentical().size());
			if(gd.getIdentical().size() > 0)
				GeoData.save(gd.getIdentical(), outFolder+t+"/identical.gpkg", sc1.getCoordinateReferenceSystem());
			 */

			System.out.println("Geom Differences: " + gd.getGeomDifferences().size());
			if(gd.getGeomDifferences().size() > 0)
				GeoData.save(gd.getGeomDifferences(), outFolder+t+"/geomDifferences.gpkg", sc1.getCoordinateReferenceSystem());

			Collection<Feature> si = GeoDiff.findIdStabilityIssues(gd.getDifferences(), 20);
			System.out.println("Stability issues: " + si.size());
			if(si.size() > 0) {
				for(Feature f : si) f.setAttribute("beginLifespanVersion", f.getAttribute("beginLifespanVersion").toString());
				GeoData.save(si, outFolder+t+"/idStabIssues.gpkg", sc1.getCoordinateReferenceSystem());
			}

		}

		System.out.println("End");
	}

}
