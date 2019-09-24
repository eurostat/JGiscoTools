/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.opencarto.util.JTSGeomUtil;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author julien Gaffuri
 *
 */
public class Main {

	public static void main(String[] args) throws Exception {

		//example
		//https://krankenhausatlas.statistikportal.de/

		System.out.println("Start");

		//create xkm grid
		String outPath = "C:/Users/gaffuju/Desktop";

		double res = 5000;
		int epsg = 3035;







		final SimpleFeatureType TYPE = DataUtilities.createType("Grid","the_geom:Polygon:srid="+epsg+",cellId:String,");
		System.out.println("TYPE:" + TYPE);


		ArrayList<SimpleFeature> features = new ArrayList<>();
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

		for(double x=0; x<100000; x+=res)
			for(double y=0; y<100000; y+=res) {

				Polygon poly = JTSGeomUtil.createPolygon( x,y, x+res,y, x+res,y+res, x,y+res, x,y );
				String id = "CRS"+Integer.toString((int)epsg)+"RES"+Integer.toString((int)res)+x+y;

				featureBuilder.add(poly);
				featureBuilder.add(id);
				SimpleFeature feature = featureBuilder.buildFeature(id);
				features.add(feature);
			}

		File newFile = new File(outPath+"/out/grid.shp");
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(TYPE);


		Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
		SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
		System.out.println("SHAPE:" + SHAPE_TYPE);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();
			} finally {
				transaction.close();
			}
			System.exit(0); // success!
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}





		/*
		Collection<Feature> fs = new ArrayList<Feature>();
		for(double x=0; x<10000000; x+=res)
			for(double y=0; y<10000000; y+=res) {
				Feature f = new Feature();
				f.setDefaultGeometry( JTSGeomUtil.createPolygon( x,y, x+res,y, x+res,y+res, x,y+res, x,y ) );
				f.setID( "CRS"+Integer.toString((int)epsg)+"RES"+Integer.toString((int)res)+x+y );
				f.setAttribute("cellId", f.getID());
				fs.add(f);
			}
		System.out.println("Save " + fs.size() + " cells");
		SHPUtil.saveSHP(fs, outPath+"/out/grid.shp", ProjectionUtil.getCRS(epsg));
		 */

		System.out.println("End");
	}

}
