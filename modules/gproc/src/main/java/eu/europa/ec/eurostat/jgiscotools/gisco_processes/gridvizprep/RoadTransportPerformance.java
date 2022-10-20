package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.coverage.grid.GridCoverage2D;

import eu.europa.ec.eurostat.jgiscotools.CommandUtil;
import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil.SkipFunction;

public class RoadTransportPerformance {
	static Logger logger = LogManager.getLogger(RoadTransportPerformance.class.getName());

	// the target resolutions
	//private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_road_perf/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		//resampling();
		tiling();

		logger.info("End");
	}




	private static void resampling() {

		//population within a 90-minute drive:
		//Population in a neighbourhood of 120 km radius
		//Transport performance by car:
		for(String in : new String[] {"ROAD_ACC_1H30", "POPL_PROX_120KM", "ROAD_PERF_1H30"}) {

			String inF = basePath + "road_transport_performance_grid_datasets/"+in+".tif";

			for (int res : resolutions) {
				logger.info("Tiling " + res + "m");

				String outF = basePath +in+"_"+ res + ".tif";
				//https://gdal.org/programs/gdalwarp.html#gdalwarp
				String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -r average";

				logger.info(cmd);
				CommandUtil.run(cmd);
			}
		}

	}



	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");
			for(String in : new String[] {"ROAD_ACC_1H30"/*, "POPL_PROX_120KM", "ROAD_PERF_1H30"*/}) {

				logger.info("Load grid cells");
				ArrayList<Map<String, String>> cells = GeoTiffUtil.loadCells(
						basePath +in+"_"+ res + ".tif",
						new String[] {"v"},
						(v)->{ return v[0]==-1; }
						);
				logger.info(cells.size());

				logger.info(cells.get(0));

				/*
				logger.info("Build tiles");
				GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

				gst.createTiles();
				logger.info(gst.getTiles().size() + " tiles created");

				logger.info("Save");
				String outpath = basePath + "out/" + res + "m";
				gst.saveCSV(outpath);
				gst.saveTilingInfoJSON(outpath, "Road transport performance " + res + "m");
				 */
			}
		}

	}


}
