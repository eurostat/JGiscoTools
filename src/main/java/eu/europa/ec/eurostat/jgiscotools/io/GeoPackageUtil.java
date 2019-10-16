/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.datamodel.Feature;
import eu.europa.ec.eurostat.jgiscotools.util.FileUtil;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil.CRSType;

/**
 * @author julien Gaffuri
 *
 */
public class GeoPackageUtil {
	//See: https://docs.geotools.org/stable/userguide/library/data/geopackage.html


	//read

	public static SimpleFeatureType getSchema(String file){
		try {
			GeoPackage gp = new GeoPackage(new File(file));
			FeatureEntry fe = gp.features().get(0);
			SimpleFeatureReader fr = gp.reader(fe, null, new DefaultTransaction());
			return fr.getFeatureType();
		} catch (IOException e) { e.printStackTrace(); }
		return null;
	}
	public static CoordinateReferenceSystem getCRS(String file){
		return getSchema(file).getCoordinateReferenceSystem();
	}
	public static CRSType getCRSType(String file) {
		return ProjectionUtil.getCRSType(getCRS(file));
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



	//write

	public static void save(SimpleFeatureCollection sfc, String file){
		try {
			File fi = FileUtil.getFile(file, true, true);
			GeoPackage gp = new GeoPackage(fi);
			gp.init();
			gp.add(new FeatureEntry(), sfc);
		} catch (IOException e) { e.printStackTrace(); }
	}

	public static <T extends Feature> void save(Collection<T> fs, CoordinateReferenceSystem crs, String file){
		SimpleFeatureCollection sfc = SimpleFeatureUtil.get(fs, crs);
		save(sfc, file);
	}

}
