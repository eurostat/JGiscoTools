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
 * Production process of Eurostat-GISCO grids:
 * https://ec.europa.eu/eurostat/web/gisco/geodata/reference-data/grids
 * based on ETRS89-LAEA coordinate reference system (EPSG:3035)  * for various resolutions.
 * The grid cell data which is computed is:
 * - its standard grid code
 * - the geostat population figures
 * - the countries and nuts regions it intersects
 * - the percentage of the cell which is land area
 * - minimum distance to border and coast
 * ...
 * 
 * @author julien Gaffuri
 *
 */
public class GridsProduction {
	static Logger logger = LogManager.getLogger(GridsProduction.class.getName());

	//TODO use better info source for coast line / land area

	public static String basePath = "E:/workspace/statistical_grids/";

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
		String inpath = basePath + "input_data/";
		int bufferDistance = 1500;
		boolean parallel = true;

		logger.info("Get European countries (buffer) ...");
		ArrayList<Feature> cntsBuff = GeoData.getFeatures(inpath+"CNTR_RG_100K_union_buff_"+bufferDistance+"_LAEA.gpkg");

		logger.info("Sort countries by id...");
		Comparator<Feature> cntComp = new Comparator<Feature>(){
			@Override
			public int compare(Feature f1, Feature f2) { return f1.getAttribute("CNTR_ID").toString().compareTo(f2.getAttribute("CNTR_ID").toString()); }
		};
		cntsBuff.sort(cntComp);

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

		logger.info("Load coastlines...");
		Collection<Geometry> coastLines = FeatureUtil.getGeometriesSimple( GeoData.getFeatures(inpath+"CNTR_BN_100K_2016_LAEA_decomposed.gpkg", null, CQL.toFilter("COAS_FLAG = 'T'") ));
		logger.info("Spatial index of coastlines...");
		STRtree coastlineIndex = new STRtree();
		for(Geometry g : coastLines) coastlineIndex.insert(g.getEnvelopeInternal(), g);
		coastLines = null;

		logger.info("Load country boundaries...");
		Collection<Geometry> cntBn = FeatureUtil.getGeometriesSimple( GeoData.getFeatures(inpath+"CNTR_BN_100K_2016_LAEA_decomposed.gpkg", null, CQL.toFilter("COAS_FLAG='F'") ));
		logger.info("Spatial index of country boundaries...");
		STRtree cntbnIndex = new STRtree();
		for(Geometry g : cntBn) cntbnIndex.insert(g.getEnvelopeInternal(), g);
		cntBn = null;

		logger.info("Load NUTS regions...");
		ArrayList<Feature>[] nuts2016 = new ArrayList[4], nuts2021 = new ArrayList[4];
		for(int level = 0; level <=3; level++) {
			nuts2016[level] = GeoData.getFeatures(inpath+"GISCO.NUTS_RG_100K_2016_LAEA.gpkg", null, CQL.toFilter("STAT_LEVL_CODE = '"+level+"'"));
			nuts2021[level] = GeoData.getFeatures(inpath+"GISCO.NUTS_RG_100K_2021_LAEA.gpkg", null, CQL.toFilter("STAT_LEVL_CODE = '"+level+"'"));
		}

		logger.info("Sort nuts regions by id...");
		Comparator<Feature> nutsComp = new Comparator<Feature>(){
			@Override
			public int compare(Feature f1, Feature f2) { return f1.getAttribute("NUTS_ID").toString().compareTo(f2.getAttribute("NUTS_ID").toString()); }
		};
		for(int level = 0; level <=3; level++) {
			nuts2016[level].sort(nutsComp);
			nuts2021[level].sort(nutsComp);
		}


		//build pan-European grids
		for(int resKM : resKMs) {
			logger.info("Make " + resKM + "km grid...");

			//build grid cells
			Grid grid = new Grid()
					.setResolution(resKM*1000)
					.setEPSGCode("3035")
					.addGeometryToCover( FeatureUtil.getGeometries(cntsBuff) )
					;
			Collection<Feature> cells = grid.getCells();

			logger.info("Assign country codes...");
			GridUtil.assignRegionCode(cells, "CNTR_ID", cntsBuff, 0, "CNTR_ID", parallel);

			logger.info("Filtering " + cells.size() + " cells not assigned to a country...");
			GridUtil.filterCellsWithoutRegion(cells, "CNTR_ID");
			logger.info(cells.size() + " cells left");

			logger.info("Assign NUTS codes...");
			for(int level = 0; level <=3; level++) {
				logger.info("  level " + level);
				GridUtil.assignRegionCode(cells, "NUTS2016_"+level, nuts2016[level], 0, "NUTS_ID", parallel);
				GridUtil.assignRegionCode(cells, "NUTS2021_"+level, nuts2021[level], 0, "NUTS_ID", parallel);
			}

			logger.info("Compute land proportion...");
			GridUtil.assignLandProportion(cells, "LAND_PC", landGeometriesIndex, inlandWaterGeometriesIndex, 2, parallel);
			//TODO make LAND_PC a nice decimal number (in gpkg)

			logger.info("Compute distance to coast...");
			GridUtil.assignDistanceToLines(cells, "DIST_COAST", coastlineIndex, 2, parallel);
			//TODO deal with fully maritime cells. Should they have coast dist equal to 0 ?

			logger.info("Compute distance country boundaries...");
			GridUtil.assignDistanceToLines(cells, "DIST_BORD", cntbnIndex, 2, parallel);
			//NB: Vatican and san marino are excluded

			{
				logger.info("Load 2006 population data...");
				StatsIndex pop2006 = getPopulationData(basePath+"pop_grid/pop_grid_2006_"+resKM+"km.csv");

				logger.info("Load 2011 population data...");
				StatsIndex pop2011 = getPopulationData(basePath+"pop_grid/pop_grid_2011_"+resKM+"km.csv");

				logger.info("Load 2018 population data...");
				StatsIndex pop2018 = getPopulationData(basePath+"pop_grid/pop_grid_2018_"+resKM+"km.csv");

				logger.info("Assign population figures...");
				Stream<Feature> st = cells.stream(); if(parallel) st = st.parallel();
				st.forEach(cell -> {
					Stat pop;
					String id = cell.getAttribute("GRD_ID").toString();
					pop = pop2006.getSingleStat(id);
					cell.setAttribute("TOT_P_2006", pop==null? 0 : pop.value);
					pop = pop2011.getSingleStat(id);
					cell.setAttribute("TOT_P_2011", pop==null? 0 : pop.value);
					pop = pop2018.getSingleStat(id);
					cell.setAttribute("TOT_P_2018", pop==null? 0 : pop.value);
				});
			}

			logger.info("Save cells as GPKG...");
			GeoData.save(cells, outpath+"grid_"+resKM+"km_surf.gpkg", CRSUtil.getETRS89_LAEA_CRS(), true);

			//do not save as shapefile for smaller resolution, because the file size limit is reached
			/*if(resKM>3) {
				logger.info("Save cells as SHP...");
				GeoData.save(cells, outpath + "grid_"+resKM+"km_surf_shp" + "/grid_"+resKM+"km.shp", CRSUtil.getETRS89_LAEA_CRS());
			}*/

			logger.info("Set cell geometries as points...");
			GeometryFactory gf = cells.iterator().next().getGeometry().getFactory();
			for(Feature cell : cells)
				cell.setGeometry( gf.createPoint(new Coordinate((Integer)cell.getAttribute("X_LLC")+resKM*500, (Integer)cell.getAttribute("Y_LLC")+resKM*500)) );

			logger.info("Save cells (point) as GPKG...");
			GeoData.save(cells, outpath+"grid_"+resKM+"km_point.gpkg", CRSUtil.getETRS89_LAEA_CRS(), true);

			/*if(resKM>3) {
				logger.info("Save cells (point) as SHP...");
				GeoData.save(cells, outpath + "grid_"+resKM+"km_point_shp" + "/grid_"+resKM+"km_point.shp", CRSUtil.getETRS89_LAEA_CRS());
			}*/
		}

		logger.info("End");
	}


	private static StatsIndex getPopulationData(String file) {
		StatsHypercube sh = CSV.load(file, "TOT_P");
		return new StatsIndex(sh, "GRD_ID");
	}

}
