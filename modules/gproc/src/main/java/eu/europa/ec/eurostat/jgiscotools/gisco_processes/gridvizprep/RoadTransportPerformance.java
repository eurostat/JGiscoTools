package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.CommandUtil;

public class RoadTransportPerformance {
	static Logger logger = LogManager.getLogger(RoadTransportPerformance.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_road_perf/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		resampling();
		//tiling();

		logger.info("End");
	}


	private static void resampling() {
		//Population in a neighbourhood of 120 km radius
		String inF = basePath + "road_transport_performance_grid_datasets/POPL_PROX_120KM.tif";

		for (int resT : resolutions) {
			logger.info("Tiling " + resT + "m");

			String outF = basePath + resT + ".tif";
			//https://gdal.org/programs/gdalwarp.html#gdalwarp
			String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+resT+" "+resT+" -r average";

			logger.info(cmd);
			CommandUtil.run(cmd);
		}
	}

	/*/ tile all resolutions
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
	 */


}
