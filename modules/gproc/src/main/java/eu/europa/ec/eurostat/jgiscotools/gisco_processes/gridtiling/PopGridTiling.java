package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridtiling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction.GridsProduction;

public class PopGridTiling {
	private static Logger logger = LogManager.getLogger(PopGridTiling.class.getName());

	public static void main(String[] args) {
		logger.info("Start");

		//TODO round when value is an integer

		String basePath = "/home/juju/Bureau/gisco/";

		for(int resKM : GridsProduction.resKMs) {

			logger.info("*** resKM="+resKM);

			StatsHypercube sh = null;
			for(int year : new int[] {2006, 2011, 2018}) {

				//load population figures
				StatsHypercube sh_ = CSV.load(basePath+"grid_pop/pop_grid_"+year+"_"+resKM+"km.csv", "TOT_P");
				logger.info(sh_.stats.size() + " values loaded for "  + year);

				//add time dimension
				sh_.dimLabels.add("time");
				for(Stat s : sh_.stats) s.dims.put("time", ""+year);

				if(sh == null) sh = sh_;
				else sh.stats.addAll(sh_.stats);
			}

			logger.info(sh.stats.size() + " values loaded");

			logger.info("Build tiles...");
			GriddedStatsTiler gst = new GriddedStatsTiler(128, sh, "time", "0");
			gst.createTiles(true);
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save...");
			String outpath = basePath+"grid_pop_tiled/"+resKM+"km";
			gst.saveCSV(outpath );
			gst.saveTilingInfoJSON(outpath, "Population " + resKM + "km");

		}
		logger.info("End");
	}

}
