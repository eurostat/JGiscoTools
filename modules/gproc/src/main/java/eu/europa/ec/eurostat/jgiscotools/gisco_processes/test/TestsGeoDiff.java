package eu.europa.ec.eurostat.jgiscotools.gisco_processes.test;

import java.util.Collection;

import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
import eu.europa.ec.eurostat.jgiscotools.geodiff.GeoDiff;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class TestsGeoDiff {


	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		System.out.println("load");

		Collection<Feature> fs1 = GeoData.getFeatures("E:\\dissemination\\shared-data\\EBM\\gpkg\\EBM_2020_LAEA\\LAU.gpkg", "LAU_CODE");
		System.out.println(fs1.size());
		Collection<Feature> fs2 = GeoData.getFeatures("E:\\dissemination\\shared-data\\EBM\\gpkg\\EBM_2021_LAEA\\LAU.gpkg", "LAU_CODE");
		System.out.println(fs2.size());

		System.out.println("compute diff");
		GeoDiff gd = new GeoDiff(fs1, fs2, 1);
		gd.setAttributesToIgnore("OBJECTID", "Shape_Length", "Shape_Area", "beginLifespanVersion", "inspireId");

		Collection<Feature> diff = gd.getDifferences();
		System.out.println(diff.size());

		for(Feature f : diff) {
			Geometry g = f.getGeometry();
			//if(g.getGeometryType() == "POINT")
			//	System.out.println(f.getGeometry());
			g = JTSGeomUtil.getPolygonal(g);
			f.setGeometry(g);
		}

		GeoData.save(diff, "C:\\Users\\gaffuju\\Desktop\\LAU\\lau_code.gpkg", CRS.decode("EPSG:3035"));

		System.out.println("End");
	}

}
