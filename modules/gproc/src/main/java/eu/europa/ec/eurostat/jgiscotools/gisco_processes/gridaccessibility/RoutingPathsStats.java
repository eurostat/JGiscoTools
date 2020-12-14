/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityRoutingPaths;

/**
 * @author clemoki
 *
 */
public class RoutingPathsStats {
	private static Logger logger = LogManager.getLogger(RoutingPathsStats.class.getName());

	/** @param args **/
	public static void main(String[] args) {
		logger.info("Start");

		String basePath = "E:/workspace/basic_services_accessibility/";
		String outPath = basePath + "routing_paths/";

		String cellIdAtt = "GRD_ID";

		logger.info("Load routing paths...");
		String serviceType = "education";
		ArrayList<Feature> paths = GeoData.getFeatures(outPath+"routes_FR_1km_"+serviceType+".gpkg");
		logger.info(paths.size() + " paths");

		logger.info("computation");
		StatsHypercube hc = AccessibilityRoutingPaths.computeStats(paths, cellIdAtt);

		logger.info("save");
		CSV.saveMultiValues(hc, outPath+"routing_paths_"+serviceType+"_stats.csv", "accInd");

		logger.info("End");
	}

}
