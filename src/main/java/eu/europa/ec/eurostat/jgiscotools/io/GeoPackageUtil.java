/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.datamodel.Feature;

/**
 * @author julien Gaffuri
 *
 */
public class GeoPackageUtil {

	public static SimpleFeatureType getSchema(String file){
		try {
			GeoPackage gp = new GeoPackage(new File(file));
			FeatureEntry fe = gp.features().get(0);
			SimpleFeatureReader fr = gp.reader(fe, null, new DefaultTransaction());
			return fr.getFeatureType();
		} catch (IOException e) { e.printStackTrace(); }
		return null;
	}

	public static SimpleFeatureReader getSimpleFeatureReader(String file){
		GeoPackage gp;
		try {
			gp = new GeoPackage(new File("C:/Users/gaffuju/Desktop/test.gpkg"));
			FeatureEntry fe = gp.features().get(0);
			return gp.reader(fe, null, new DefaultTransaction());
		} catch (IOException e) { e.printStackTrace(); }
		return null;
	}

	public static ArrayList<Feature> getFeatures(String file){
		try {
			ArrayList<Feature> fs = new ArrayList<Feature>();
			SimpleFeatureReader fr = getSimpleFeatureReader(file);
			while(fr.hasNext()) {
				SimpleFeature sf = fr.next();
				Feature f = SimpleFeatureUtil.get(sf);
				fs.add(f);
			}
			return fs;
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}



	//testing based on https://docs.geotools.org/stable/userguide/library/data/geopackage.html
	public static void main(String[] args) throws Exception {

		/*
		Map<String,String> params = new HashMap<>();
		params.put("dbtype", "geopkg");
		params.put("database", "C:/Users/gaffuju/Desktop/test.gpkg");

		JDBCDataStore ds = (JDBCDataStore)DataStoreFinder.getDataStore(params);
		System.out.println(ds.getSchema("test"));
		JDBCFeatureReader fr = (JDBCFeatureReader) ds.getFeatureReader(new Query("test"), new DefaultTransaction());
		while(fr.hasNext()) {
			SimpleFeature sf = fr.next();
			Feature f = SimpleFeatureUtil.get(sf);
			System.out.println(f.getAttribute("CNTR_ID") + " - " + f.getDefaultGeometry().getArea());
		}
		 */
/*
		GeoPackage gp = new GeoPackage(new File("C:/Users/gaffuju/Desktop/test.gpkg"));
		FeatureEntry fe = gp.features().get(0);
		SimpleFeatureReader fr = gp.reader(fe, null, new DefaultTransaction());
		SimpleFeatureType sc = fr.getFeatureType();
		System.out.println(sc);
		while(fr.hasNext()) {
			SimpleFeature sf = fr.next();
			Feature f = SimpleFeatureUtil.get(sf);
			System.out.println(f.getAttribute("CNTR_ID") + " - " + f.getDefaultGeometry().getArea());
		}
*/
		//BasicDataSource ds = (BasicDataSource)gp.getDataSource();
		//System.out.println(ds);

		System.out.println("End");
	}

}
