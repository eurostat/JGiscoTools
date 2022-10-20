package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoadTransportPerformance {
	static Logger logger = LogManager.getLogger(RoadTransportPerformance.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_road_perf/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");
		logger.info("End");
	}


}
