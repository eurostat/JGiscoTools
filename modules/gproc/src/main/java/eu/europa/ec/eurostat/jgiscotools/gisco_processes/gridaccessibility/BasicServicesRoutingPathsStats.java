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
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility.BasicServicesRoutingPaths.Case;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityRoutingPaths;

/**
 * @author clemoki
 *
 */
public class BasicServicesRoutingPathsStats {
	private static Logger logger = LogManager.getLogger(BasicServicesRoutingPathsStats.class.getName());

	/** @param args **/
	public static void main(String[] args) {
		logger.info("Start");

		String basePath = "E:/workspace/basic_services_accessibility/";
		String cnt = "FR";

		for(Case c : BasicServicesRoutingPaths.cases) {
			logger.info("Case: " + c.label);

			logger.info("Load routing paths...");
			ArrayList<Feature> paths = GeoData.getFeatures(basePath + "routing_paths/routes_"+(cnt==null?"":cnt+"_")+"1km"+"_"+c.label+".gpkg");

			logger.info(paths.size() + " paths");

		}
		logger.info("End");
	}

}
