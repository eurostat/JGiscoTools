/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.grid.geomprod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.base.StatsIndex;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.grid.Grid;
import eu.europa.ec.eurostat.jgiscotools.grid.GridUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;

/**
 * Examples to produce European grids based on ETRS89-LAEA coordinate reference system (EPSG:3035)
 * for various resolutions.
 * The grid cell data which is computed is:
 * - its standard grid code
 * - the geostat population figures, for both 2006 and 2011
 * - the countries and nuts regions it intersects
 * - the percentage of the cell which is land area
 * - minimum distance to border and coast
 * 
 * @author julien Gaffuri
 *
 */
public class GridsProduction {
	static Logger logger = Logger.getLogger(GridsProduction.class.getName());

	//see also:
	//https://www.eea.europa.eu/data-and-maps/data/eea-reference-grids-2
	//https://www.efgs.info/data/
	//https://esdac.jrc.ec.europa.eu/content/european-reference-grids

	//the different resolutions, in KM
	public static int[] resKMs = new int[] {100,50,20,10,5,2,1};

	//use: -Xms2G -Xmx8G
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		logger.setLevel(Level.ALL);
		Grid.logger.setLevel(Level.ALL);

		String basePath = "E:/workspace/gridstat/data/";
		String outpath = basePath + "grid/";
		String path = basePath + "CNTR_100k/";
		int bufferDistance = 1500;

		logger.info("Get European countries (buffer) ...");
		ArrayList<Feature> cntsBuff = GeoPackageUtil.getFeatures(path+"CNTR_RG_100K_union_buff_"+bufferDistance+"_LAEA.gpkg");

		logger.info("Sort countries by id...");
		Comparator<Feature> cntComp = new Comparator<Feature>(){
			@Override
			public int compare(Feature f1, Feature f2) { return f1.getAttribute("CNTR_ID").toString().compareTo(f2.getAttribute("CNTR_ID").toString()); }
		};
		cntsBuff.sort(cntComp);

		logger.info("Get land area...");
		Collection<Geometry> landGeometries = FeatureUtil.getGeometriesSimple( GeoPackageUtil.getFeatures(path+"land_areas.gpkg") );

		logger.info("Index land area...");
		SpatialIndex landGeometriesIndex = new STRtree();
		for(Geometry g : landGeometries) landGeometriesIndex.insert(g.getEnvelopeInternal(), g);
		landGeometries = null;

		logger.info("Get inland water area...");
		Collection<Geometry> inlandWaterGeometries = FeatureUtil.getGeometriesSimple( GeoPackageUtil.getFeatures(path+"inland_water_areas.gpkg") );

		logger.info("Index inland water area...");
		SpatialIndex inlandWaterGeometriesIndex = new STRtree();
		for(Geometry g : inlandWaterGeometries) inlandWaterGeometriesIndex.insert(g.getEnvelopeInternal(), g);
		inlandWaterGeometries = null;

		logger.info("Load NUTS regions...");
		ArrayList<Feature> nuts0 = GeoPackageUtil.getFeatures(path+"NUTS_RG_100K_2016.gpkg", CQL.toFilter("STAT_LEVL_ = '0'"));
		ArrayList<Feature> nuts1 = GeoPackageUtil.getFeatures(path+"NUTS_RG_100K_2016.gpkg", CQL.toFilter("STAT_LEVL_ = '1'"));
		ArrayList<Feature> nuts2 = GeoPackageUtil.getFeatures(path+"NUTS_RG_100K_2016.gpkg", CQL.toFilter("STAT_LEVL_ = '2'"));
		ArrayList<Feature> nuts3 = GeoPackageUtil.getFeatures(path+"NUTS_RG_100K_2016.gpkg", CQL.toFilter("STAT_LEVL_ = '3'"));

		logger.info("Sort nuts regions by id...");
		Comparator<Feature> nutsComp = new Comparator<Feature>(){
			@Override
			public int compare(Feature f1, Feature f2) { return f1.getAttribute("NUTS_ID").toString().compareTo(f2.getAttribute("NUTS_ID").toString()); }
		};
		nuts0.sort(nutsComp);
		nuts1.sort(nutsComp);
		nuts2.sort(nutsComp);
		nuts3.sort(nutsComp);


		//TODO load file with shetland, islae of man, etc.

		logger.info("Load coastlines...");
		Collection<Geometry> coastLines = FeatureUtil.getGeometriesSimple( GeoPackageUtil.getFeatures(path+"NUTS_BN_100K_2016.gpkg", CQL.toFilter("COAS_FLAG = 'T'") ));

		logger.info("Index coastlines...");
		STRtree coastlineIndex = new STRtree();
		for(Geometry g : coastLines) coastlineIndex.insert(g.getEnvelopeInternal(), g);
		coastLines = null;

		logger.info("Load country boundaries...");
		Collection<Geometry> cntBn = FeatureUtil.getGeometriesSimple( GeoPackageUtil.getFeatures(path+"NUTS_BN_100K_2016.gpkg", CQL.toFilter("STAT_LEVL_='0' AND COAS_FLAG='F'") ));

		logger.info("Index country boundaries...");
		STRtree cntbnIndex = new STRtree();
		for(Geometry g : cntBn) cntbnIndex.insert(g.getEnvelopeInternal(), g);
		cntBn = null;



		
		logger.info("Define output feature type...");
		SimpleFeatureType ftPolygon = SimpleFeatureUtil.getFeatureType("Polygon", 3035, "GRD_ID:String,CNTR_ID:String,LAND_PC:double,X_LLC:int,Y_LLC:int,TOT_P_2006:int,TOT_P_2011:int,NUTS_0_ID:String,NUTS_1_ID:String,NUTS_2_ID:String,NUTS_3_ID:String,DIST_COAST:double,DIST_BORD:double");
		SimpleFeatureType ftPoint = SimpleFeatureUtil.getFeatureType("Point", 3035, "GRD_ID:String,CNTR_ID:String,LAND_PC:double,X_LLC:int,Y_LLC:int,TOT_P_2006:int,TOT_P_2011:int,NUTS_0_ID:String,NUTS_1_ID:String,NUTS_2_ID:String,NUTS_3_ID:String,DIST_COAST:double,DIST_BORD:double");


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
			GridUtil.assignRegionCode(cells, "CNTR_ID", cntsBuff, 0, "CNTR_ID");

			logger.info("Filtering " + cells.size() + " cells...");
			GridUtil.filterCellsWithoutRegion(cells, "CNTR_ID");
			logger.info(cells.size() + " cells left");


			logger.info("Assign NUTS codes...");
			GridUtil.assignRegionCode(cells, "NUTS_0_ID", nuts0, 0, "NUTS_ID");
			GridUtil.assignRegionCode(cells, "NUTS_1_ID", nuts1, 0, "NUTS_ID");
			GridUtil.assignRegionCode(cells, "NUTS_2_ID", nuts2, 0, "NUTS_ID");
			GridUtil.assignRegionCode(cells, "NUTS_3_ID", nuts3, 0, "NUTS_ID");

			logger.info("Compute land proportion...");
			GridUtil.assignLandProportion(cells, "LAND_PC", landGeometriesIndex, inlandWaterGeometriesIndex, 2);
			//TODO make LAND_PC a nice decimal number (in gpkg)

			logger.info("Compute distance to coast...");
			GridUtil.assignDistanceToLines(cells, "DIST_COAST", coastlineIndex, 2);
			//TODO deal with fully maritime cells. Should they have coast dist equal to 0 ?

			logger.info("Compute distance country boundaries...");
			GridUtil.assignDistanceToLines(cells, "DIST_BORD", cntbnIndex, 2);
			//TODO exclude vatican and san marino ?

			{
				logger.info("Load 2006 population data...");
				StatsIndex pop2006 = getPopulationData(basePath+"pop_grid/pop_grid_2006_"+resKM+"km.csv");

				logger.info("Load 2011 population data...");
				StatsIndex pop2011 = getPopulationData(basePath+"pop_grid/pop_grid_2011_"+resKM+"km.csv");

				logger.info("Assign population figures...");
				Stat pop;
				for(Feature cell : cells) {
					String id = cell.getAttribute("GRD_ID").toString();
					pop = pop2006.getSingleStat(id);
					cell.setAttribute("TOT_P_2006", pop==null? 0 : pop.value);
					pop = pop2011.getSingleStat(id);
					cell.setAttribute("TOT_P_2011", pop==null? 0 : pop.value);
				}
			}

			logger.info("Save cells as GPKG...");
			GeoPackageUtil.save(cells, outpath+"grid_"+resKM+"km.gpkg", ftPolygon, true);

			//do not save as shapefile for smaller resolution, because the file size limit is reached
			if(resKM>3) {
				logger.info("Save cells as SHP...");
				SHPUtil.saveSHP(cells, outpath + "grid_"+resKM+"km_shp" + "/grid_"+resKM+"km.shp", ftPolygon);
			}

			logger.info("Set cell geometries as points...");
			GeometryFactory gf = cells.iterator().next().getDefaultGeometry().getFactory();
			for(Feature cell : cells)
				cell.setDefaultGeometry( gf.createPoint(new Coordinate((Integer)cell.getAttribute("X_LLC")+resKM*500, (Integer)cell.getAttribute("Y_LLC")+resKM*500)) );

			logger.info("Save cells (point) as GPKG...");
			GeoPackageUtil.save(cells, outpath+"grid_"+resKM+"km_point.gpkg", ftPoint, true);

			if(resKM>3) {
				logger.info("Save cells (point) as SHP...");
				SHPUtil.saveSHP(cells, outpath + "grid_"+resKM+"km_point_shp" + "/grid_"+resKM+"km_point.shp", ftPoint);
			}

		}



		/*
		//build country 1km grids by country
		for(String countryCode : CountriesUtil.EuropeanCountryCodes) {

			logger.info("Make 1km grid for " + countryCode + "...");

			//get country geometry (buffer)
			Geometry countryGeom = SHPUtil.loadSHP(path+"CNTR_RG_100K_union_buff_"+bufferDistance+"_LAEA.shp", CQL.toFilter("CNTR_ID = '"+countryCode+"'"))
					.fs.iterator().next().getDefaultGeometry();

			//build cells
			StatGrid grid = new StatGrid()
					.setResolution(1000)
					.setEPSGCode("3035")
					.setGeometryToCover(countryGeom)
					;
			Collection<Feature> cells = grid.getCells();

			//set country code to cells
			for(Feature cell : cells) cell.setAttribute("CNTR_ID", countryCode);

			logger.info("Save " + cells.size() + " cells as SHP...");
			SHPUtil.saveSHP(cells, outpath+"grid_1km_shp/grid_1km_"+countryCode+".shp", crs);
			//logger.info("Save " + cells.size() + " cells as GPKG...");
			//GeoPackageUtil.save(cells, outpath+"1km/grid_1km_"+countryCode+".gpkg", crs);
		}
		 */

		logger.info("End");
	}



	private static StatsIndex getPopulationData(String file) {
		StatsHypercube sh = CSV.load(file, "TOT_P");
		return new StatsIndex(sh, "GRD_ID");
	}

}
