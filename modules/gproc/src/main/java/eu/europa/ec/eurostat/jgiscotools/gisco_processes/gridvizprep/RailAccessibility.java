package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RailAccessibility {
	static Logger logger = LogManager.getLogger(RailAccessibility.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_rail_perf/rail-2022-grid-data/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		prepare();
		//aggregate();
		//tiling();
		logger.info("End");
	}

	private static void prepare() {
		
		
		
	}
	
}
