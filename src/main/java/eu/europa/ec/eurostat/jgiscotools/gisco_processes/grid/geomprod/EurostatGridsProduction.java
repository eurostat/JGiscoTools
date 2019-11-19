/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.grid.geomprod;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.grid.Grid;
import eu.europa.ec.eurostat.jgiscotools.grid.GridUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;

/**
 * Examples to produce European grids based on ETRS89-LAEA coordinate reference system (EPSG:3035)
 * for various resolutions. The cells are tagged by country and identified with their standard code.
 * 
 * @author julien Gaffuri
 *
 */
public class EurostatGridsProduction {
	static Logger logger = Logger.getLogger(EurostatGridsProduction.class.getName());

	//see also:
	//https://www.eea.europa.eu/data-and-maps/data/eea-reference-grids-2
	//https://www.efgs.info/data/
	//https://esdac.jrc.ec.europa.eu/content/european-reference-grids

	//the different resolutions, in KM
	public static int[] resKMs = new int[] {100,50,20,10,5,2,1};

	public static void main(String[] args) throws Exception {
		logger.info("Start");

		logger.setLevel(Level.ALL);
		Grid.logger.setLevel(Level.ALL);

		String outpath = "E:/workspace/gridstat/data/grid/";
		String path = "E:/workspace/gridstat/data/CNTR_100k/";
		int bufferDistance = 1500;

		logger.info("Get European countries (buffer) ...");
		ArrayList<Feature> cntsBuff = GeoPackageUtil.getFeatures(path+"CNTR_RG_100K_union_buff_"+bufferDistance+"_LAEA.gpkg");

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

		logger.info("Define output feature type...");
		SimpleFeatureType ftPolygon = SimpleFeatureUtil.getFeatureType("Polygon", 3035, "GRD_ID:String,CNTR_ID:String,LAND_PC:double,X_LLC:int,Y_LLC:int");
		SimpleFeatureType ftPoint = SimpleFeatureUtil.getFeatureType("Point", 3035, "GRD_ID:String,CNTR_ID:String,LAND_PC:double,X_LLC:int,Y_LLC:int");

		//build pan-European grids
		for(int resKM : resKMs) {
			logger.info("Make " + resKM + "km grid...");

			//build grid
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

			//TODO assign also nuts code? for each level?

			logger.info("Compute land proportion...");
			GridUtil.assignLandProportion(cells, "LAND_PC", landGeometriesIndex, inlandWaterGeometriesIndex, 2);

			logger.info("Save cells as GPKG...");
			GeoPackageUtil.save(cells, outpath+"grid_"+resKM+"km.gpkg", ftPolygon);

			if(resKM>3) {
				logger.info("Save cells as SHP...");
				SHPUtil.saveSHP(cells, outpath + "grid_"+resKM+"km_shp" + "/grid_"+resKM+"km.shp", ftPolygon);
			}

			logger.info("Set cell geometries as points...");
			GeometryFactory gf = cells.iterator().next().getDefaultGeometry().getFactory();
			for(Feature cell : cells)
				cell.setDefaultGeometry( gf.createPoint(new Coordinate((Integer)cell.getAttribute("X")+resKM*500, (Integer)cell.getAttribute("Y")+resKM*500)) );

			logger.info("Save cells (point) as GPKG...");
			GeoPackageUtil.save(cells, outpath+"grid_point_"+resKM+"km.gpkg", ftPoint);

			if(resKM>3) {
				logger.info("Save cells (point) as SHP...");
				SHPUtil.saveSHP(cells, outpath + "grid_"+resKM+"km_shp" + "/grid_point_"+resKM+"km.shp", ftPoint);
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

}
