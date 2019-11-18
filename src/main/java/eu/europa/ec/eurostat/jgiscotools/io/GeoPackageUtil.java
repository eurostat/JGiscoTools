/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
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
	private final static Logger LOGGER = Logger.getLogger(GeoPackageUtil.class);

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
			/*GeoPackage gp = new GeoPackage(new File(file));
			FeatureEntry fe = gp.features().get(0);
			SimpleFeatureReader fr = gp.reader(fe, filter, new DefaultTransaction());

			ArrayList<Feature> fs = new ArrayList<Feature>();
			while(fr.hasNext()) {
				SimpleFeature sf = fr.next();
				Feature f = SimpleFeatureUtil.get(sf);
				fs.add(f);
			}
			fr.close();
			gp.close();
			return fs;*/

			HashMap<String, Object> map = new HashMap<>();
			map.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
			map.put(GeoPkgDataStoreFactory.DATABASE.key, file);
			DataStore store = DataStoreFinder.getDataStore(map);
			String[] names = store.getTypeNames();

			ArrayList<Feature> fs = new ArrayList<Feature>();
			for (String name : names) {
				LOGGER.debug(name);

				SimpleFeatureCollection features = store.getFeatureSource(name).getFeatures(filter);
				//SimpleFeatureCollection features = reader.getFeatures(name);
				SimpleFeatureIterator itr = features.features();

				while (itr.hasNext()) {
					SimpleFeature sf = itr.next();
					Feature f = SimpleFeatureUtil.get(sf);
					fs.add(f);
				}
			}

		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}



	//write

	public static void save(SimpleFeatureCollection sfc, String file){
		try {
			File fi = FileUtil.getFile(file, true, true);
			GeoPackage gp = new GeoPackage(fi);
			gp.init();
			gp.add(new FeatureEntry(), sfc);
			gp.close();
		} catch (IOException e) { e.printStackTrace(); }
	}

	public static <T extends Feature> void save(Collection<T> fs, String file, CoordinateReferenceSystem crs){
		SimpleFeatureCollection sfc = SimpleFeatureUtil.get(fs, crs);
		save(sfc, file);
	}

	public static <T extends Geometry> void saveGeoms(Collection<T> geoms, String outFile, CoordinateReferenceSystem crs) {
		save(SimpleFeatureUtil.getFeaturesFromGeometries(geoms), outFile, crs);
	}

}
