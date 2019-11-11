package eu.europa.ec.eurostat.jgiscotools.zzz;

import org.apache.log4j.Logger;

import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.gridstat.tiling.GridStatTiler;

public class PopGridTiling {
	private static Logger logger = Logger.getLogger(PopGridTiling.class.getName());

	public static void main(String[] args) {
		logger.info("Start");

		String basePath = "E:/gridstat/data/";

		logger.info("Load data...");
		StatsHypercube sh = CSV.load(basePath+"pop_grid/pop_grid_2006_1km.csv", "TOT_P");
		logger.info(sh.stats.size() + " values loaded");

		logger.info("Build tiles...");
		GridStatTiler gst = new GridStatTiler(sh);
		gst.createTiles();
		logger.info(gst.getTiles().size() + " tiles created");

		logger.info("Save tiles...");
		gst.save(basePath+"pop_grid/pop_grid_2006_1km_tiled/");

		logger.info("End");
	}

}
