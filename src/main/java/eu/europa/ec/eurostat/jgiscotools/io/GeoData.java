/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;
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
		String inpuat = FilenameUtils.getExtension(filePath).toLowerCase();
		switch(inpuat) {
		case "shp":
			return SHPUtil.getFeatures(filePath);
		case "geojson":
			return GeoJSONUtil.load(filePath);
		case "gpkg":
			return GeoPackageUtil.getFeatures(filePath);
		default:
			throw new Exception("Could not retrieve features from data source: "+filePath);
		}
	}

	public static CoordinateReferenceSystem getCRS(String filePath) throws Exception {
		String format = FilenameUtils.getExtension(filePath).toLowerCase();
		switch(format) {
		case "shp":
			return SHPUtil.getCRS(filePath);
		case "geojson":
			//TODO
			return null;
		case "gpkg":
			return GeoPackageUtil.getCRS(filePath);
		default:
			throw new Exception("Could not retrieve CRS from data source: "+filePath);
		}
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
			GeoPackageUtil.save(fs, filePath, crs, true);
			break;
		default:
			throw new Exception("Unsuported output format: " + format);
		}
	}



}
