package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridtiling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction.GridsProduction;

public class PopGridTiling {
	private static Logger logger = LogManager.getLogger(PopGridTiling.class.getName());

	public static void main(String[] args) {
		logger.info("Start");

		String basePath = "E:/workspace/gridstat/data/";

		for(int resKM : GridsProduction.resKMs) {
			for(int year : new int[] {2011, 2006}) {
				logger.info("*** year="+year+" resKM="+resKM);

				logger.info("Load data...");
				StatsHypercube sh = CSV.load(basePath+"pop_grid/pop_grid_"+year+"_"+resKM+"km.csv", "TOT_P");
				logger.info(sh.stats.size() + " values loaded");

				logger.info("Build tiles...");
				GriddedStatsTiler gst = new GriddedStatsTiler(sh, 256);
				gst.createTiles(true);
				logger.info(gst.getTiles().size() + " tiles created");

				logger.info("Save...");
				String outpath = basePath+"pop_grid_tiled/pop_grid_"+year+"_"+resKM+"km/";
				gst.saveCSV(outpath );
				gst.saveTilingInfoJSON(outpath, "Population in "+year);
			}
		}
		logger.info("End");
	}

}
