/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil.CRSType;

/**
 * @author julien Gaffuri
 *
 */
public class GeoPackageUtil {
	private final static Logger LOGGER = LogManager.getLogger(GeoPackageUtil.class);

	//See: https://docs.geotools.org/stable/userguide/library/data/geopackage.html
	//https://gis.stackexchange.com/questions/341981/reading-geopackage-file-with-geotools-error-on-missing-gpkg-contents-database


	//read

	public static SimpleFeatureType getSchema(String file){
		try {
			GeoPackage gp = new GeoPackage(new File(file));
			FeatureEntry fe = gp.features().get(0);
			SimpleFeatureReader fr = gp.reader(fe, null, new DefaultTransaction());
			SimpleFeatureType ft = fr.getFeatureType();
			fr.close();
			gp.close();
			return ft;
		} catch (IOException e) { e.printStackTrace(); }
		return null;
	}
	public static CoordinateReferenceSystem getCRS(String file){
		return getSchema(file).getCoordinateReferenceSystem();
	}
	public static CRSType getCRSType(String file) {
		return ProjectionUtil.getCRSType(getCRS(file));
	}


	public static ArrayList<Feature> getFeatures(String file) { return getFeatures(file, null); }
	public static ArrayList<Feature> getFeatures(String file, Filter filter){
		try {
			HashMap<String, Object> params = new HashMap<>();
			params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
			params.put(GeoPkgDataStoreFactory.DATABASE.key, file);
			DataStore store = DataStoreFinder.getDataStore(params);

			ArrayList<Feature> fs = new ArrayList<Feature>();
			String[] names = store.getTypeNames();
			for (String name : names) {
				LOGGER.debug(name);
				SimpleFeatureCollection features = filter==null? store.getFeatureSource(name).getFeatures() : store.getFeatureSource(name).getFeatures(filter);
				fs.addAll( SimpleFeatureUtil.get(features) );
			}
			store.dispose();
			return fs;

		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}



	//write

	public static void save(Collection<? extends Feature> fs, String outFile, CoordinateReferenceSystem crs, boolean withSpatialIndex) { save(fs,outFile,crs,null, withSpatialIndex); }
	public static void save(Collection<? extends Feature> fs, String outFile, CoordinateReferenceSystem crs, List<String> atts, boolean withSpatialIndex) { save(fs, outFile, SimpleFeatureUtil.getFeatureType(fs.iterator().next(), crs, atts), withSpatialIndex); }
	public static void save(Collection<? extends Feature> fs, String outFile, SimpleFeatureType ft, boolean withSpatialIndex) { save(SimpleFeatureUtil.get(fs, ft), outFile, withSpatialIndex); }
	public static void save(SimpleFeatureCollection sfc, String outFile, boolean withSpatialIndex){
		try {

			if(sfc.size() == 0){
				//file.createNewFile();
				LOGGER.warn("Could not save file "+outFile+" - collection of features is empty");
				return;
			}

			//create output file
			File fi = FileUtil.getFile(outFile, true, true);
			/*
			GeoPackage gp = new GeoPackage(fi);
			gp.init();
			FeatureEntry fe = new FeatureEntry();
			gp.add(fe, sfc);
			if(withSpatialIndex) gp.createSpatialIndex(fe);
			gp.close();
			 */


			//create feature store
			HashMap<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", fi.toURI().toURL());
			params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
			params.put(GeoPkgDataStoreFactory.DATABASE.key, outFile);
			params.put("create spatial index", Boolean.TRUE);
			DataStore ds = DataStoreFinder.getDataStore(params);

			ds.createSchema(sfc.getSchema());
			SimpleFeatureStore fst = (SimpleFeatureStore)ds.getFeatureSource(ds.getTypeNames()[0]);

			//creation transaction
			Transaction tr = new DefaultTransaction("create");
			fst.setTransaction(tr);
			try {
				fst.addFeatures(sfc);
				tr.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tr.rollback();
			} finally {
				tr.close();
				ds.dispose();
			}
		} catch (IOException e) { e.printStackTrace(); }
	}

	public static <T extends Geometry> void saveGeoms(Collection<T> geoms, String outFile, CoordinateReferenceSystem crs, boolean withSpatialIndex) {
		save(SimpleFeatureUtil.getFeaturesFromGeometries(geoms), outFile, crs, true);
	}

}
