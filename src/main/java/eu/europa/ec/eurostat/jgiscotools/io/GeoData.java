/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * Some generic function to load data from a variety of data sources
 * 
 * @author julien Gaffuri
 *
 */
public class GeoData {

	/**
	 * Get features
	 * 
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Feature> getFeatures(String filePath) throws Exception {
		String input = FilenameUtils.getExtension(filePath).toLowerCase();
		switch(input) {
		case "shp":
			return SHPUtil.getFeatures(filePath);
		case "geojson":
			return GeoJSONUtil.getFeatures(filePath);
		case "gpkg":
			return GeoPackageUtil.getFeatures(filePath);
		default:
			throw new Exception("Could not retrieve features from data source: "+filePath);
		}
	}

	public static SimpleFeatureType getSchema(String filePath) throws Exception {
		String format = FilenameUtils.getExtension(filePath).toLowerCase();
		switch(format) {
		case "shp":
			return SHPUtil.getSchema(filePath);
		case "geojson":
			return GeoJSONUtil.getSchema(filePath);
		case "gpkg":
			return GeoPackageUtil.getSchema(filePath);
		default:
			throw new Exception("Could not retrieve schema from data source: "+filePath);
		}
	}

	public static CoordinateReferenceSystem getCRS(String filePath) throws Exception {
		return getSchema(filePath).getCoordinateReferenceSystem();
	}

	public static void save(Collection<Feature> fs, String filePath, CoordinateReferenceSystem crs) throws Exception {
		if(fs.size() == 0) return;
		String format = FilenameUtils.getExtension(filePath).toLowerCase();
		switch(format) {
		case "shp":
			SHPUtil.save(fs, filePath, crs);
			break;
		case "geojson":
			GeoJSONUtil.save(fs, filePath, crs);
			break;
		case "gpkg":
			GeoPackageUtil.save(fs, filePath, crs);
			break;
		default:
			throw new Exception("Unsuported output format: " + format);
		}
	}



}
