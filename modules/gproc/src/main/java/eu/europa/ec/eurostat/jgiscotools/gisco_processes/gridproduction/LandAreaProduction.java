/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.base.StatsIndex;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.grid.Grid;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.CRSUtil;
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

	//the different resolutions, in KM
	public static int[] resKMs = new int[] {100,50,20,10,5,2,1};


	//use: -Xms8g -Xmx24g
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//set loggers
		logger.atLevel(Level.ALL);
		Grid.logger.atLevel(Level.ALL);

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

			logger.info("Compute land proportion...");
			//GridUtil.assignLandProportion(cells, "LAND_PC", landGeometriesIndex, inlandWaterGeometriesIndex, 2, parallel);
			//TODO make LAND_PC a nice decimal number (in gpkg)

		}

		logger.info("End");
	}

}
