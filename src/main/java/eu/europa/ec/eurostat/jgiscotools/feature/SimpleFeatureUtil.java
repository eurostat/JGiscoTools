/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.feature;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Conversion functions from GT SimpleFeatures from/to Feature
 * 
 * @author julien Gaffuri
 *
 */
public class SimpleFeatureUtil {
	private final static Logger LOGGER = LogManager.getLogger(SimpleFeatureUtil.class);


	//SimpleFeature to feature
	public static Feature get(SimpleFeature sf, String attId, String[] attNames){
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
		for(String attName : attNames) f.setAttribute(attName, sf.getProperty(attName).getValue());

		return f;
	}

	public static ArrayList<Feature> get(SimpleFeatureCollection sfs, String attId) {
		SimpleFeatureIterator it = sfs.features();
		SimpleFeatureType sh = sfs.getSchema();
		String[] attNames = getAttributeNames(sh);
		ArrayList<Feature> fs = new ArrayList<Feature>();
		while( it.hasNext()  )
			fs.add(get(it.next(), attId, attNames));
		it.close();
		return fs;
	}

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

	public static String[] getAttributeNames(SimpleFeatureType ft){
		ArrayList<String> atts = new ArrayList<String>();
		for(int i=0; i<ft.getAttributeCount(); i++){
			String att = ft.getDescriptor(i).getLocalName();
			if("the_geom".equals(att)) continue;
			if("GEOM".equals(att)) continue;
			if("geom".equals(att)) continue;
			if("Geom".equals(att)) continue;
			atts.add(att);
		}
		return atts.toArray(new String[atts.size()]);
	}


	/**
	 * @param <T>
	 * @param fs
	 * @param crs
	 * @return
	 */
	public static <T extends Feature> SimpleFeatureType getFeatureType(Collection<T> fs, CoordinateReferenceSystem crs) {
		SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
		Feature f = fs.iterator().next(); //TODO
		sftb.setCRS(crs);
		sftb.setName( "type" );
		sftb.setNamespaceURI("http://geotools.org");
		sftb.add("the_geom", f.getGeometry().getClass());
		sftb.setDefaultGeometry("the_geom");
		for(String att : f.getAttributes().keySet()) {
			sftb.add(att, f.getAttribute(att).getClass());
		}
		SimpleFeatureType sc = sftb.buildFeatureType();
		return sc;
	}


	/**
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
