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

	// the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500 /*, 200 /*, 100*/ };
	private static String basePath = "/home/juju/Bureau/gisco/geodata/elevation/EU_DEM_mosaic_1000K/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");
		resampling();
		tiling();
		logger.info("End");
	}

	public static void resampling() {
		for (int res : resolutions) {
			logger.info("Resampling to " + res + "m");
			EurForest.resample(basePath +"EU_DEM_mosaic_1000K/eudem_dem_3035_europe.tif", basePath + res+".tif", res, "average");
		}
	}

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
			gst.save(outpath, GridTiler.Format.CSV, null, null, false);
			gst.saveTilingInfoJSON(outpath, GridTiler.Format.CSV, "EU DEM Europe elevation " + res + "m");
		}
	}

}
