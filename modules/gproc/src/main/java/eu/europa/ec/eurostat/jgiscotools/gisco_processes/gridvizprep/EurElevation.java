package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;

public class EurElevation {
	static Logger logger = LogManager.getLogger(EurElevation.class.getName());

	//https://docs.geotools.org/stable/userguide/library/coverage/geotiff.html
	//https://docs.qgis.org/testing/en/docs/user_manual/working_with_raster/raster_analysis.html#raster-calculator
	//https://docs.qgis.org/3.22/en/docs/gentle_gis_introduction/index.html

	//https://qgis.org/pyqgis/3.16/analysis/QgsAlignRaster.html
	//or https://qgis.org/pyqgis/3.16/analysis/QgsRasterCalculator.html

	//*******************
	//resampling with GDAL

	//gdal
	//https://gdal.org/programs/gdalwarp.html#gdalwarp
	//https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-tr
	//https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-r
	//gdalwarp eudem_dem_3035_europe.tif 1000.tif -tr 1000 1000 -r average
	//*******************


	// the target resolutions
	//private static int[] resolutions = new int[] { 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static int[] resolutions = new int[] { /*100000, 50000, 20000, 10000, 5000, 2000, 1000, 500,*/ 200 /*, 100*/ };
	private static String basePath = "/home/juju/Bureau/gisco/elevation/EU_DEM_mosaic_1000K/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");
		tiling();
		logger.info("End");
	}

	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + res+".tif";

			logger.info("Load geoTiff");
			GridCoverage2D coverage = GeoTiffUtil.getGeoTIFFCoverage(f);

			logger.info("Load grid cells");
			ArrayList<Map<String, String>> cells = GeoTiffUtil.loadCells(coverage, new String[] {"elevation"}, (v)->{ return v[0]==0 || Double.isNaN(v[0]); }, true );
			logger.info(cells.size());

			//logger.info("Round");
			//for(Map<String, String> cell : cells)
			//	cell.put("elevation", "" + (int)Double.parseDouble(cell.get("elevation")));

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "out/tiled/" + res + "m";
			gst.save(outpath, GridTiler.Format.CSV, null, false);
			gst.saveTilingInfoJSON(outpath, GridTiler.Format.CSV, "EU DEM Europe elevation " + res + "m");
		}
	}

}
