/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.grid.geomprod;

import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.algo.Decomposer;
import eu.europa.ec.eurostat.jgiscotools.algo.Partition.GeomType;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;

/**
 * @author julien Gaffuri
 *
 */
public class DecomposeLandWaterAreas {
	static Logger logger = Logger.getLogger(DecomposeLandWaterAreas.class.getName());

	public static void main(String[] args) throws Throwable {
		logger.info("Start");
		Decomposer.logger.setLevel(Level.ALL);

		String path = "E:/workspace/gridstat/data/CNTR_100k/";

		logger.info("Load data...");
		//Collection<Feature> fs = GeoPackageUtil.getFeatures(path+"CNTR_RG_100K_union_LAEA.gpkg"/*, CQL.toFilter("CNTR_ID='FR'")*/);
		Collection<Feature> fs = GeoPackageUtil.getFeatures(path+"LAKE_EURO_PL_100K.gpkg"/*, CQL.toFilter("CNTR_ID='FR'")*/);
		logger.info(fs.size());

		logger.info("Buffer 0...");
		for(Feature f : fs) f.setDefaultGeometry( f.getDefaultGeometry().buffer(0) );

		logger.info("Run decomposition...");
		Collection<Geometry> landGeometries = Decomposer.decompose(fs, 1000, 1000, GeomType.ONLY_AREAS, 0);
		logger.info(landGeometries.size());

		logger.info("Save...");
		//GeoPackageUtil.saveGeoms(landGeometries, path + "land_areas.gpkg", CRS.decode("EPSG:3035"));
		GeoPackageUtil.saveGeoms(landGeometries, path + "inland_water_areas.gpkg", CRS.decode("EPSG:3035"));

		logger.info("End");

	}

}
