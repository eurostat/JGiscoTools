/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io.geo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;

/**
 * Some generic function to load data from mainstream data formats: gpkg, shp, geojson.
 * 
 * @author julien Gaffuri
 *
 */
public class GeoData {
	private final static Logger LOGGER = LogManager.getLogger(GeoData.class);

	private File file = null;
	private String typeName;
	private String idAtt;
	private Filter filter;
	private String format;

	/**
	 * Build a GeoData from a file.
	 * 
	 * @param filePath
	 */
	public GeoData(String filePath) { this(filePath, null, null, null); }

	/**
	 * Build a GeoData from a file.
	 * 
	 * @param filePath
	 * @param idAtt 
	 */
	public GeoData(String filePath, String idAtt) { this(filePath, null, idAtt, null); }

	/**
	 * Build a GeoData from a file.
	 * 
	 * @param filePath
	 * @param typeName
	 * @param idAtt 
	 * @param filter
	 */
	public GeoData(String filePath, String typeName, String idAtt, Filter filter) {
		this.file = new File(filePath);
		this.typeName = typeName;
		this.idAtt = idAtt;
		this.filter = filter;
		if(!this.file.exists()) {
			LOGGER.error("Data source: " + filePath + " not found.");
			return;
		}
		this.format = getExtension(filePath).get().toLowerCase();
		if(!HANDLERS.keySet().contains(this.format)) {
			LOGGER.error("Unsupported data format '" + this.format + "' for data source:" + filePath + " not found.");
			return;
		}
	}

	private ArrayList<Feature> features = null;

	/**
	 * @return The features
	 */
	public ArrayList<Feature> getFeatures() {
		if(features == null) {
			GeoDataFormatHandler dfh = HANDLERS.get(format);
			if(dfh != null)
				this.features = dfh.getFeatures(file, typeName, filter, idAtt);
			else
				LOGGER.error("Could not retrieve features from data source: " + this.file.getAbsolutePath());
		}
		return features;
	}

	private SimpleFeatureType schema = null;

	/**
	 * @return The schema
	 */
	public SimpleFeatureType getSchema() {
		if(schema == null) {
			GeoDataFormatHandler dfh = HANDLERS.get(format);
			if(dfh != null)
				this.schema = dfh.getSchema(file);
			else
				LOGGER.error("Could not retrieve schema from data source: " + this.file.getAbsolutePath());
		}
		return schema;
	}

	/**
	 * @return The coordinate reference system
	 */
	public CoordinateReferenceSystem getCRS() {
		return getSchema().getCoordinateReferenceSystem();
	}




	//TODO handle additional formats? WKT/WKB?
	private static final HashMap<String, GeoDataFormatHandler> HANDLERS;
	static {
		HANDLERS = new HashMap<String, GeoDataFormatHandler>();
		HANDLERS.put("gpkg", new GPKGHandler());
		HANDLERS.put("geojson", new GeoJSONHandler());
		HANDLERS.put("shp", new SHPHandler());
	}

	private abstract interface GeoDataFormatHandler {
		SimpleFeatureType getSchema(File file);
		ArrayList<Feature> getFeatures(File file, String typeName, Filter filter, String idAtt);
		ReferencedEnvelope getEnvelope(File file, String typeName);
		void save(SimpleFeatureCollection sfc, File file, CoordinateReferenceSystem crs, boolean createSpatialIndex);
		String getGeomColName();
	}

	private static class GPKGHandler implements GeoDataFormatHandler {
		@Override
		public ArrayList<Feature> getFeatures(File file, String typeName, Filter filter, String idAtt) {
			try {
				//build datastore
				HashMap<String, Object> params = new HashMap<>();
				params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
				params.put(GeoPkgDataStoreFactory.DATABASE.key, file);
				DataStore store = DataStoreFinder.getDataStore(params);

				//get typename
				if(typeName ==null) {
					String[] names = store.getTypeNames();
					typeName = names[0];
					if(names.length > 1)
						LOGGER.warn("Several types found in GPKG " + file.getAbsolutePath() + ". Only " + typeName + " will be considered.");
				}
				if(LOGGER.isDebugEnabled()) LOGGER.debug(typeName);

				SimpleFeatureSource fso = store.getFeatureSource(typeName);
				SimpleFeatureCollection sfc = filter==null? fso.getFeatures() : fso.getFeatures(filter);
				ArrayList<Feature> fs = SimpleFeatureUtil.get(sfc, idAtt);
				store.dispose();

				//remove 'geometry' attribute
				for(Feature f : fs) {
					//TODO really usefull ?
					/*Object o = */f.getAttributes().remove("geometry");
					//if(o == null) LOGGER.warn("Could not remove geometry attribute when loading data from " + this.filePath);
				}
				return fs;
			} catch (Exception e) { e.printStackTrace(); }
			return null;
		}

		@Override
		public SimpleFeatureType getSchema(File file) {
			try {
				HashMap<String, Object> params = new HashMap<>();
				params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
				params.put(GeoPkgDataStoreFactory.DATABASE.key, file);
				DataStore store = DataStoreFinder.getDataStore(params);
				String[] names = store.getTypeNames();
				//if(names.length >1 )
				//	LOGGER.warn("Several types found in GPKG " + file.getAbsolutePath() + ". Only " + names[0] + " will be considered.");
				String name = names[0];
				LOGGER.debug(name);
				SimpleFeatureType schema = store.getSchema(name);
				store.dispose();
				return schema;
			} catch (IOException e) { e.printStackTrace(); }
			return null;
		}

		@Override
		public ReferencedEnvelope getEnvelope(File file, String typeName) {
			try {
				//build datastore
				HashMap<String, Object> params = new HashMap<>();
				params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
				params.put(GeoPkgDataStoreFactory.DATABASE.key, file);
				DataStore store = DataStoreFinder.getDataStore(params);

				//get typename
				if(typeName ==null) {
					String[] names = store.getTypeNames();
					typeName = names[0];
					if(names.length > 1)
						LOGGER.warn("Several types found in GPKG " + file.getAbsolutePath() + ". Only " + typeName + " will be considered.");
				}
				if(LOGGER.isDebugEnabled()) LOGGER.debug(typeName);

				SimpleFeatureSource fso = store.getFeatureSource(typeName);
				ReferencedEnvelope env = fso.getBounds();
				store.dispose();
				return env;
			} catch (Exception e) { e.printStackTrace(); }
			return null;
		}


		@Override
		public void save(SimpleFeatureCollection sfc, File file, CoordinateReferenceSystem crs, boolean createSpatialIndex) {
			try {
				//create feature store
				HashMap<String, Serializable> params = new HashMap<String, Serializable>();
				params.put("url", file.toURI().toURL());
				params.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
				params.put(GeoPkgDataStoreFactory.DATABASE.key, file.getAbsolutePath());
				params.put("create spatial index", createSpatialIndex);
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

		@Override
		public String getGeomColName() { return "geometry"; }		
	};

	private static class GeoJSONHandler implements GeoDataFormatHandler {
		@Override
		public ArrayList<Feature> getFeatures(File file, String typeName, Filter filter, String idAtt) {
			try {
				InputStream input = new FileInputStream(file);
				SimpleFeatureCollection fc = (SimpleFeatureCollection) new FeatureJSON().readFeatureCollection(input);
				ArrayList<Feature> fs = null;
				if(filter == null)
					fs  = SimpleFeatureUtil.get(fc, idAtt);
				else {
					fs = new ArrayList<Feature>();
					for(Feature f : SimpleFeatureUtil.get(fc, idAtt))
						if(filter.evaluate(f)) fs.add(f);
				}
				/*/remove 'geometry' attribute
				for(Feature f : fs) {
					Object o = f.getAttributes().remove("geometry");
					if(o == null) LOGGER.warn("Could not remove geometry attribute when loading GeoJSON data.");
				}*/
				input.close();
				return fs;
			} catch (Exception e) { e.printStackTrace(); }
			return null;
		}

		@Override
		public SimpleFeatureType getSchema(File file) {
			try {
				InputStream input = new FileInputStream(file);
				SimpleFeatureType schema = new FeatureJSON().readFeatureCollectionSchema(input, true);
				input.close();
				return schema;
			} catch (Exception e) { e.printStackTrace(); }
			return null;
		}		

		@Override
		public ReferencedEnvelope getEnvelope(File file, String typeName) {
			LOGGER.error("getEnvelope method not implemented for GeoJSON format");
			return null;
		}


		@Override
		public void save(SimpleFeatureCollection sfc, File file, CoordinateReferenceSystem crs, boolean createSpatialIndex) {
			try {
				OutputStream output = new FileOutputStream(file);
				new FeatureJSON().writeFeatureCollection(sfc, output);
				output.close();
			} catch (Exception e) { e.printStackTrace(); }
		}

		@Override
		public String getGeomColName() { return "geometry"; }		
	};

	private static class SHPHandler implements GeoDataFormatHandler {
		@Override
		public ArrayList<Feature> getFeatures(File file, String typeName, Filter filter, String idAtt) {
			try {
				FileDataStore store = FileDataStoreFinder.getDataStore(file);
				SimpleFeatureCollection features = filter==null? store.getFeatureSource().getFeatures() : store.getFeatureSource().getFeatures(filter);
				store.dispose();
				return SimpleFeatureUtil.get(features, idAtt);
			} catch (Exception e) { e.printStackTrace(); }
			return null;
		}

		@Override
		public SimpleFeatureType getSchema(File file) {
			try {
				return FileDataStoreFinder.getDataStore(file).getSchema();
			} catch (Exception e) { e.printStackTrace(); }
			return null;
		}		

		@Override
		public ReferencedEnvelope getEnvelope(File file, String typeName) {
			try {
				FileDataStore store = FileDataStoreFinder.getDataStore(file);
				ReferencedEnvelope env = store.getFeatureSource().getBounds();
				store.dispose();
				return env;
			} catch (Exception e) { e.printStackTrace(); }
			return null;
		}

		@Override
		public void save(SimpleFeatureCollection sfc, File file, CoordinateReferenceSystem crs, boolean createSpatialIndex) {
			try {
				//create feature store
				HashMap<String, Serializable> params = new HashMap<String, Serializable>();
				params.put("url", file.toURI().toURL());
				params.put("create spatial index", createSpatialIndex);
				ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);

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
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public String getGeomColName() { return "the_geom"; }		
	};













	/**
	 * Get features
	 * 
	 * @param filePath
	 * @return
	 */
	public static ArrayList<Feature> getFeatures(String filePath)  {
		return new GeoData(filePath).getFeatures();
	}

	/**
	 * Get features
	 * 
	 * @param filePath
	 * @param idAtt 
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Feature> getFeatures(String filePath, String idAtt)  {
		return new GeoData(filePath, idAtt).getFeatures();
	}

	/**
	 * Get features
	 * 
	 * @param filePath
	 * @param typeName
	 * @param idAtt
	 * @return
	 */
	public static ArrayList<Feature> getFeatures(String filePath, String typeName, String idAtt)  {
		return new GeoData(filePath, typeName, idAtt, null).getFeatures();
	}

	/**
	 * Get features
	 * 
	 * @param filePath
	 * @param idAtt 
	 * @param filter 
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Feature> getFeatures(String filePath, String idAtt, Filter filter)  {
		return new GeoData(filePath, null, idAtt, filter).getFeatures();
	}

	/**
	 * Get features
	 * 
	 * @param filePath
	 * @param typeName
	 * @param idAtt 
	 * @param filter 
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Feature> getFeatures(String filePath, String typeName, String idAtt, Filter filter)  {
		return new GeoData(filePath, typeName, idAtt, filter).getFeatures();
	}

	/**
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureType getSchema(String filePath) {
		return new GeoData(filePath).getSchema();
	}

	/**
	 * @param filePath
	 * @return
	 */
	public static CoordinateReferenceSystem getCRS(String filePath) {
		return new GeoData(filePath).getCRS();
	}



	/**
	 * @param fs
	 * @param filePath
	 * @param crs
	 * @param createSpatialIndex
	 */
	public static <T extends Feature> void save(Collection<T> fs, String filePath, CoordinateReferenceSystem crs, boolean createSpatialIndex) {

		if(fs.size() == 0) {
			LOGGER.warn("Could not save file " + filePath + " - collection of features is empty");
			return;
		}

		//get format handler
		String format = getExtension(filePath).get().toLowerCase();
		GeoDataFormatHandler dfh = HANDLERS.get(format);
		if(dfh == null) {
			LOGGER.error("Unsuported output format: " + format);
			return;
		}

		//create GT feature collection
		SimpleFeatureType ft = SimpleFeatureUtil.getFeatureType(fs, dfh.getGeomColName(), crs);
		SimpleFeatureCollection sfc = SimpleFeatureUtil.get(fs, ft);

		//create output file
		File file = getFile(filePath, true, true);

		//save
		dfh.save(sfc, file, crs, createSpatialIndex);
	}

	/**
	 * @param fs
	 * @param filePath
	 * @param crs
	 */
	public static <T extends Feature> void save(Collection<T> fs, String filePath, CoordinateReferenceSystem crs) {
		save(fs, filePath, crs, true);
	}

	/**
	 * @param <T>
	 * @param geoms
	 * @param outFile
	 * @param crs
	 * @param createSpatialIndex
	 */
	public static <T extends Geometry> void saveGeoms(Collection<T> geoms, String outFile, CoordinateReferenceSystem crs, boolean createSpatialIndex) {
		save(SimpleFeatureUtil.getFeaturesFromGeometries(geoms), outFile, crs, createSpatialIndex);
	}




	private static void createFolders(String filePath){
		File parent = new File(filePath).getParentFile();
		if (!parent.exists() && !parent.mkdirs())
			throw new IllegalStateException("Couldn't create dir: " + parent);
	}

	private static File getFile(String filePath, boolean createFolders, boolean eraseOnExist){
		if(createFolders) createFolders(filePath);
		File file = new File(filePath);
		if(eraseOnExist && file.exists()) file.delete();
		return file;
	}

	private static Optional<String> getExtension(String filename) {
		return Optional.ofNullable(filename)
				.filter(f -> f.contains("."))
				.map(f -> f.substring(filename.lastIndexOf(".") + 1));
	}
}
