package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.CommandUtil;
import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class EurRoadTransportPerformance {
	static Logger logger = LogManager.getLogger(EurRoadTransportPerformance.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_road_perf/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		//resampling();
		joiningTiling();

		logger.info("End");
	}



	private static void resampling() {

		//population within a 90-minute drive:
		//Population in a neighbourhood of 120 km radius
		//Transport performance by car:
		for(String in : new String[] {"ROAD_ACC_1H30", "POPL_PROX_120KM", "ROAD_PERF_1H30"}) {

			String inF = basePath + "road_transport_performance_grid_datasets/"+in+".tif";

			for (int res : resolutions) {
				logger.info("Resamplig " + res + "m");

				String outF = basePath +in+"_"+ res + ".tif";
				//https://gdal.org/programs/gdalwarp.html#gdalwarp
				String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -tap -r average -co TILED=YES";

				//TILED=YES
				//use -co to tile output
				//https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-co
				//https://gdal.org/drivers/raster/gtiff.html#creation-options

				logger.info(cmd);
				CommandUtil.run(cmd);
			}
		}

	}



	// tile all resolutions
	private static void joiningTiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String in;

			in = "ROAD_ACC_1H30";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsRA = GeoTiffUtil.loadCells(
					basePath +in+"_"+ res + ".tif",
					new String[] {"ra"},
					(v)->{ return v[0]==-1; },
					false
					);
			logger.info(cellsRA.size());

			in = "POPL_PROX_120KM";
			logger.info("Load grid cells " + in);
			List<Map<String, String>> cellsPP = GeoTiffUtil.loadCells(
					basePath +in+"_"+ res + ".tif",
					new String[] {"pp"},
					(v)->{ return v[0]==-1; },
					false
					);
			logger.info(cellsPP.size());

			in = "ROAD_PERF_1H30";
			logger.info("Load grid cells " + in);
			List<Map<String, String>> cellsRP = GeoTiffUtil.loadCells(
					basePath +in+"_"+ res + ".tif",
					new String[] {"rp"},
					(v)->{ return v[0]==-1; },
					false
					);
			logger.info(cellsRP.size());

			ArrayList<Map<String, String>> pop = CSVUtil.load("/home/juju/Bureau/gisco/grid_pop/pop_"+res+"m.csv");
			logger.info("pop: " + pop.size());
			CSVUtil.removeColumn(pop, "2006", "2011");
			CSVUtil.renameColumn(pop, "2018", "TOT_P");
			logger.info(pop.get(0).keySet());

			logger.info("Join 1");
			List<Map<String, String>> cells = CSVUtil.joinBothSides("GRD_ID", cellsRA, cellsPP, "", false);
			logger.info(cells.size());

			logger.info("Join 2");
			cells = CSVUtil.joinBothSides("GRD_ID", cells, cellsRP, "", false);
			logger.info(cells.size());

			logger.info("Join pop");
			cells = CSVUtil.joinBothSides("GRD_ID", cells, pop, "", false);
			logger.info(cells.size());

			logger.info(cells.get(0).keySet());

			//TODO remove those with TOT_P = 0
			//cells.stream().filter(arg0)

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "tiled/" + res + "m";
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "CSV", "Road transport performance " + res + "m");

		}
	}

}
