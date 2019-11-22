/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;

/**
 * @author gaffuju
 *
 */
public class TestGDBRead {

	public static void main(String[] args) throws Exception {
		//See https://docs.geotools.org/stable/userguide/library/data/ogr.html
		//See https://gis.stackexchange.com/questions/243746/using-geotools-to-open-esri-file-geodatabase-filegdb

		System.out.println("start");


		//merge ERM roadL

		String egpath = "E:/dissemination/shared-data/";

		SimpleFeatureType ft = SHPUtil.getSchema(egpath+"ERM/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_14_15_16.shp");
		System.out.println(ft);
		DefaultFeatureCollection sfc = new DefaultFeatureCollection(null, ft);
		SimpleFeatureCollection f;

		System.out.println("load 1");
		f = SHPUtil.getSimpleFeatures(egpath+"ERM/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_14_15_16.shp", null);
		sfc.addAll(f); f = null;
		System.out.println(sfc.size());

		System.out.println("load 2");
		f = SHPUtil.getSimpleFeatures(egpath+"ERM/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_984.shp", null);
		sfc.addAll(f); f = null;
		System.out.println(sfc.size());

		System.out.println("load 3");
		f = SHPUtil.getSimpleFeatures(egpath+"ERM/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_0.shp", null);
		sfc.addAll(f); f = null;
		System.out.println(sfc.size());

		System.out.println("save");
		GeoPackageUtil.save(sfc, "E:/workspace/gridstat/data/RoadL.gpkg", true);

		System.out.println("end");
	}

}
