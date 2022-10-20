package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.CommandUtil;
import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;

public class EurCLC {
	static Logger logger = LogManager.getLogger(EurCLC.class.getName());

	//*******************
	//resampling with GDAL
	//https://gdal.org/programs/gdalwarp.html#gdalwarp
	//https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-tr
	//https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-r
	//gdalwarp eudem_dem_3035_europe.tif 1000.tif -tr 1000 1000 -r average
	//*******************

	// the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100 };
	private static String basePath = "/home/juju/Bureau/gisco/clc/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		//resampling();
		tiling();

		logger.info("End");
	}



	private static void resampling() {
		String inF = basePath + "u2018_clc2018_v2020_20u1_raster100m/DATA/U2018_CLC2018_V2020_20u1.tif";

		for (int resT : resolutions) {
			logger.info("Tiling " + resT + "m");

			String outF = basePath + resT + ".tif";
			//https://gdal.org/programs/gdalwarp.html#gdalwarp
			String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+resT+" "+resT+" -r mode";

			logger.info(cmd);
			CommandUtil.run(cmd);
		}
	}

	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + res+".tif";

			logger.info("Load geoTiff");
			GridCoverage2D coverage = GeoTiffUtil.getGeoTIFFCoverage(f);

			logger.info("Load grid cells");
			ArrayList<Map<String, String>> cells = GeoTiffUtil.loadCells(coverage, new String[] {"clc"},
					(v)->{ return v[0]==0 || v[0]==128 || v[0]==44 || Double.isNaN(v[0]); }
					);
			logger.info(cells.size());

			//logger.info("Round");
			//for(Map<String, String> cell : cells)
			//	cell.put("elevation", "" + (int)Double.parseDouble(cell.get("elevation")));

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "out/" + res + "m";
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "Corine Land Cover 2018 " + res + "m");
		}

	}
}
