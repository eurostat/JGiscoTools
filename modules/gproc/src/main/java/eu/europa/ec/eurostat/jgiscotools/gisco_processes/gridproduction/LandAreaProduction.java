/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
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
	public static int[] resKMs = new int[] {/*100,50,20,10,5,*/2,1};


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
			Collection<Feature> cells = GeoData.getFeatures(gridsPath + "grid_" + resKM + "km_surf.gpkg");
			logger.info(" " + cells.size());

			logger.info("Clean cell attributes...");
			for(Feature c : cells) {
				String id = c.getAttribute("GRD_ID").toString();
				c.getAttributes().clear();
				c.setAttribute("GRD_ID", id);
			}

			logger.info("Compute land proportion...");
			GridUtil.assignLandProportion(cells, "LAND_PC", landGeometriesIndex, inlandWaterGeometriesIndex, 2, parallel);

			logger.info("Clean cell geoms...");
			for(Feature c : cells) c.setGeometry(null);

			logger.info("Make tabular...");
			Collection<Map<String, String>> data = new ArrayList<>();
			for(Feature c : cells) {
				Map<String, String> a = new HashMap<>();
				a.put("GRD_ID", c.getAttribute("GRD_ID").toString());
				a.put("LAND_PC", c.getAttribute("LAND_PC").toString());
				data.add(a);
			}
			cells.clear();

			logger.info("Save as CSV " + data.size());
			CSVUtil.save(data, outpath + "land_pc_"+resKM+"km.csv");
			data.clear();
		}

		logger.info("End");
	}

}
