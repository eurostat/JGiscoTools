/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Conversion functions from GeoTools SimpleFeatures from/to Feature
 * 
 * @author julien Gaffuri
 *
 */
public class SimpleFeatureUtil {
	private final static Logger LOGGER = LogManager.getLogger(SimpleFeatureUtil.class);

	/**
	 * Convert GeoTools SimpleFeature into feature
	 * 
	 * @param sf
	 * @param attId
	 * @param attNames
	 * @return
	 */
	private static Feature get(SimpleFeature sf, String attId, String[] attNames){
		Feature f = new Feature();

		//set id
		if(attId != null && sf.getAttribute(attId) != null && !"".equals(sf.getAttribute(attId)))
			f.setID(sf.getAttribute(attId).toString());
		else if(sf.getID() != null && !"".equals(sf.getID())) f.setID(sf.getID());

		//set geometry
		Property pg = sf.getProperty( sf.getFeatureType().getGeometryDescriptor().getName() );
		if(pg==null) pg = sf.getProperty("the_geom");
		if(pg==null) pg = sf.getProperty("geometry");
		if(pg==null) pg = sf.getProperty("geom");
		if(pg==null) LOGGER.warn("Could not find geometry attribute for simple feature " + sf.getFeatureType());
		f.setGeometry( (Geometry)pg.getValue() );

		//set attributes
		for(String attName : attNames) {
			Object attValue = sf.getProperty(attName).getValue();
			f.setAttribute(attName, attValue);
		}

		return f;
	}

	/**
	 * Convert GeoTools SimpleFeatures into features
	 * 
	 * @param sfs
	 * @param attId
	 * @return
	 */
	public static ArrayList<Feature> get(SimpleFeatureCollection sfs, String attId) {
		SimpleFeatureIterator it = sfs.features();
		SimpleFeatureType ft = sfs.getSchema();
		String[] attNames = getAttributeNames(ft);
		ArrayList<Feature> fs = new ArrayList<Feature>();
		while( it.hasNext()  )
			fs.add(get(it.next(), attId, attNames));
		it.close();
		return fs;
	}

	/**
	 * Convert features into GeoTools SimpleFeature
	 * 
	 * @param fs
	 * @param ft
	 * @return
	 */
	public static SimpleFeatureCollection get(Collection<? extends Feature> fs, SimpleFeatureType ft) {
		DefaultFeatureCollection sfc = new DefaultFeatureCollection(null, ft);
		SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(ft);
		String[] attNames = getAttributeNames(ft);
		for(Feature f : fs) {
			/*SimpleFeature sf = sfb.buildFeature(f.getID());
			sf.setDefaultGeometry(f.getDefaultGeometry());
			for(String attName : attNames)
				sf.setAttribute(attName, f.getAttribute(attName));*/
			Object[] data = new Object[attNames.length+1];
			data[0] = f.getGeometry();
			for(int i=0; i<attNames.length; i++)
				data[i+1] = f.getAttribute(attNames[i]);
			SimpleFeature sf = sfb.buildFeature(f.getID(), data);
			sfc.add(sf);
		}
		return sfc;
	}

	private static String[] getAttributeNames(SimpleFeatureType ft){
		ArrayList<String> atts = new ArrayList<String>();
		for(int i=0; i<ft.getAttributeCount(); i++){
			String att = ft.getDescriptor(i).getLocalName();
			String geomColName = ft.getGeometryDescriptor().getName().toString();
			if(geomColName != null && geomColName.equals(att)) continue;
			if("the_geom".equals(att)) continue;
			if("GEOM".equals(att)) continue;
			if("geom".equals(att)) continue;
			if("Geom".equals(att)) continue;
			atts.add(att);
		}
		return atts.toArray(new String[atts.size()]);
	}


	/**
	 * Get GeoTools FeatureType from features.
	 * NB: All features are assumed to have the same attributes names/types and geometry types.
	 * 
	 * @param <T>
	 * @param fs
	 * @param geomColName
	 * @param crs
	 * @return
	 */
	public static <T extends Feature> SimpleFeatureType getFeatureType(Collection<T> fs, String geomColName, CoordinateReferenceSystem crs) {
		SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
		sftb.setCRS(crs);
		sftb.setName("type");
		sftb.setNamespaceURI("http://geotools.org");
		sftb.setDefaultGeometry(geomColName);

		if(fs.size() == 0) {
			LOGGER.warn("Creating SimpleFeatureType from empty list of features.");
			sftb.add(geomColName, Point.class);
			return sftb.buildFeatureType();
		}

		HashMap<String,Class<?>> types = getAttributeGeomTypes(fs, geomColName);
		sftb.add(geomColName, types.get(geomColName));
		for(String att : types.keySet()) {
			if(geomColName.equals(att)) continue;
			sftb.add(att, types.get(att));
		}
		return sftb.buildFeatureType();
	}

	/**
	 * Get GeoTools FeatureType from features.
	 * 
	 * @param <T>
	 * @param f
	 * @param geomColName
	 * @param crs
	 * @return
	 */
	public static <T extends Feature> SimpleFeatureType getFeatureType(T f, String geomColName, CoordinateReferenceSystem crs) {
		ArrayList<T> fs = new ArrayList<T>();
		fs.add(f);
		return getFeatureType(fs, geomColName, crs);
	}

	/**
	 * Get attribute and geometry types for a list of features.
	 * 
	 * @param <T>
	 * @param fs
	 * @return
	 */
	public static <T extends Feature> HashMap<String, Class<?>> getAttributeGeomTypes(Collection<T> fs, String geomColName) {
		HashMap<String, Class<?>> out = new HashMap<>();
		if(fs.size()==0) return out;

		//get all attributes
		Set<String> atts = new HashSet<>();
		for(Feature f : fs)
			atts.addAll(f.getAttributes().keySet());

		//attribute types
		for(String att : atts) {
			Class<?> attClass = null;
			for(Feature f : fs) {
				Object o = f.getAttribute(att);
				if(o==null) continue;
				Class<? extends Object> kl = o.getClass();
				if(attClass==null) { attClass=kl; continue; }
				if(kl != attClass) {
					LOGGER.warn("Inconsistant attribute type for " + att + ". Store it as String type.");
					attClass = String.class;
					break;
				}
			}
			if(attClass == null) attClass = String.class;
			out.put(att, attClass);
		}
		//geometry type
		Class<?> gClass = null;
		for(Feature f : fs) {
			Geometry o = f.getGeometry();
			if(o==null || o.isEmpty()) continue;
			Class<? extends Object> kl = o.getClass();
			if(gClass==null) { gClass=kl; continue; }
			if(kl != gClass) {
				LOGGER.warn("Inconsistant geometry type. Store it as Point type.");
				gClass = Point.class;
				break;
			}
		}
		if(gClass == null) gClass = String.class;
		out.put(geomColName, gClass);
		return out;
	}

	/**
	 * @param <T>
	 * @param fs
	 * @return
	 */
	public static <T extends Feature> HashMap<String, Class<?>> getAttributeGeomTypes(Collection<T> fs) {
		return getAttributeGeomTypes(fs, "the_geom");
	}

	/**
	 * Create features from geometries
	 * 
	 * @param <T>
	 * @param geoms
	 * @return
	 */
	public static <T extends Geometry> Collection<Feature> getFeaturesFromGeometries(Collection<T> geoms) {
		ArrayList<Feature> fs = new ArrayList<Feature>();
		for(Geometry geom : geoms){
			Feature f = new Feature();
			f.setGeometry(geom);
			fs.add(f);
		}
		return fs;
	}

}
