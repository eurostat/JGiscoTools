/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * - the percentage of the cell which is land area
 * 
 * @author julien Gaffuri
 *
 */
public class LandAreaProduction {
	static Logger logger = LogManager.getLogger(LandAreaProduction.class.getName());

	public static String basePath = "/home/juju/Bureau/gisco/grid_land_area/";
	public static String gridsPath = "/home/juju/Bureau/gisco/geodata/grids/";

	//the different resolutions, in KM
	public static int[] resKMs = new int[] {100,50,20,10,5,2,1};


	//use: -Xms8g -Xmx24g
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//set parameters
		String outpath = basePath + "output/";
		String inpath = basePath + "input/";
		//int bufferDistance = 1500;
		boolean parallel = true;

		logger.info("Load land area...");
		Collection<Geometry> landGeometries = FeatureUtil.getGeometriesSimple( GeoData.getFeatures(inpath+"land_areas.gpkg") );
		logger.info("Spatial index for land area...");
		SpatialIndex landGeometriesIndex = new STRtree();
		for(Geometry g : landGeometries) landGeometriesIndex.insert(g.getEnvelopeInternal(), g);
		landGeometries = null;

		logger.info("Load inland water area...");
		Collection<Geometry> inlandWaterGeometries = FeatureUtil.getGeometriesSimple( GeoData.getFeatures(inpath+"inland_water_areas.gpkg") );
		logger.info("Spatial index for inland water area...");
		SpatialIndex inlandWaterGeometriesIndex = new STRtree();
		for(Geometry g : inlandWaterGeometries) inlandWaterGeometriesIndex.insert(g.getEnvelopeInternal(), g);
		inlandWaterGeometries = null;

		//
		for(int resKM : resKMs) {
			logger.info(resKM + "km grid...");

			logger.info("Load grid cells...");
			Collection<Feature> cells = GeoData.getFeatures(gridsPath + "grid_" + resKMs + "km_surf.gpkg");
			logger.info(" " + cells.size());

			//clean cell attributes ?

			logger.info("Compute land proportion...");
			GridUtil.assignLandProportion(cells, "LAND_PC", landGeometriesIndex, inlandWaterGeometriesIndex, 2, parallel);
			//TODO make LAND_PC a nice decimal number (in gpkg)

		}

		logger.info("End");
	}

}
