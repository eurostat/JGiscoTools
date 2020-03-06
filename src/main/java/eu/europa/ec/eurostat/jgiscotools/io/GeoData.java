/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private final static Logger LOGGER = LogManager.getLogger(GeoData.class);

	private String filePath;
	private String format;

	public GeoData(String filePath) {
		this.filePath = filePath;
		this.format = FilenameUtils.getExtension(filePath).toLowerCase();
		//TODO warnings
	}

	ArrayList<Feature> features = null;
	public ArrayList<Feature> getFeatures() {
		if(features == null) {
			switch(format) {
			case "shp":
				features = SHPUtil.getFeatures(filePath);
				break;
			case "geojson":
				features = GeoJSONUtil.getFeatures(filePath);
				break;
			case "gpkg":
				features = GeoPackageUtil.getFeatures(filePath);
				break;
			default:
				LOGGER.error("Could not retrieve features from data source: " + filePath);
			}
		}
		return features;
	}

	SimpleFeatureType schema = null;
	public SimpleFeatureType getSchema() {
		if(schema == null) {
			switch(format) {
			case "shp":
				schema = SHPUtil.getSchema(filePath);
				break;
			case "geojson":
				schema = GeoJSONUtil.getSchema(filePath);
				break;
			case "gpkg":
				schema = GeoPackageUtil.getSchema(filePath);
				break;
			default:
				LOGGER.error("Could not retrieve schema from data source: " + filePath);
			}
		}
		return schema;
	}

	public CoordinateReferenceSystem getCRS() {
		return getSchema().getCoordinateReferenceSystem();
	}

	public void save() {
		switch(format) {
		case "shp":
			SHPUtil.save(getFeatures(), filePath, getCRS());
			break;
		case "geojson":
			GeoJSONUtil.save(getFeatures(), filePath, getCRS());
			break;
		case "gpkg":
			GeoPackageUtil.save(getFeatures(), filePath, getCRS());
			break;
		default:
			LOGGER.error("Unsuported output format: " + format);
		}
	}






	/**
	 * Get features
	 * 
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Feature> getFeatures(String filePath) throws Exception {
		return new GeoData(filePath).getFeatures();
	}

	public static SimpleFeatureType getSchema(String filePath) throws Exception {
		return new GeoData(filePath).getSchema();
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
