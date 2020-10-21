/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction;

import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.referencing.CRS;

import eu.europa.ec.eurostat.jgiscotools.algo.base.Decomposer;
import eu.europa.ec.eurostat.jgiscotools.algo.base.Partition.GeomType;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author julien Gaffuri
 *
 */
public class DecomposeLandWaterAreas {
	static Logger logger = LogManager.getLogger(DecomposeLandWaterAreas.class.getName());

	public static void main(String[] args) throws Throwable {
		logger.info("Start");
		Decomposer.logger.atLevel(Level.ALL);

		String path = "E:/workspace/gridstat/data/CNTR_100k/";

		logger.info("Load data...");
		//Collection<Feature> fs = GeoPackageUtil.getFeatures(path+"CNTR_RG_100K_union_LAEA.gpkg"/*, CQL.toFilter("CNTR_ID='FR'")*/);
		Collection<Feature> fs = GeoData.getFeatures(path+"LAKE_EURO_PL_100K_2019.gpkg"/*, CQL.toFilter("CNTR_ID='FR'")*/);
		logger.info(fs.size());

		logger.info("Buffer 0...");
		for(Feature f : fs) f.setGeometry( f.getGeometry().buffer(0) );

		logger.info("Run decomposition...");
		//Collection<Geometry> landGeometries = Decomposer.decomposeGeometry(fs, 1000, 500, GeomType.ONLY_AREAS, 0);
		//TODO it takes ages to save that !!! Fix that
		Collection<Feature> landFeatures = Decomposer.decomposeFeature(fs, false, 1000, 500, GeomType.ONLY_AREAS, 0);
		//logger.info(landGeometries.size());
		logger.info(landFeatures.size());

		logger.info("Save...");
		//GeoPackageUtil.saveGeoms(landGeometries, path + "land_areas.gpkg", CRS.decode("EPSG:3035"));
		//GeoPackageUtil.saveGeoms(landGeometries, path + "inland_water_areas.gpkg", CRS.decode("EPSG:3035"), true);
		GeoData.save(landFeatures, path + "inland_water_areas___.gpkg", CRS.decode("EPSG:3035"), true);

		logger.info("End");

	}

}
