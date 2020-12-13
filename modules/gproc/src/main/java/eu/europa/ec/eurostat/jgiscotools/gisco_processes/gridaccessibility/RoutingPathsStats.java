/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

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
		String outPath = basePath + "accessibility_output/";

		String cellIdAtt = "GRD_ID";

		logger.info("Load routing paths...");
		ArrayList<Feature> paths = GeoData.getFeatures(outPath+"routes_FR_1km_schools.gpkg");
		logger.info(paths.size() + " paths");

		//while there are paths
		while(paths.size() >0) {
			//get cell id of the first path
			String cellId = paths.get(0).getAttribute(cellIdAtt).toString();
			logger.info(cellId);

			//get all paths of the cell
			ArrayList<Feature> paths_ = new ArrayList<Feature>();
			for(Feature path : paths)
				if(path.getAttribute(cellIdAtt).toString().equals(cellId))
					paths_.add(path);

			//remove
			paths.removeAll(paths_);

			System.out.println(paths_.size());
			System.out.println(paths.size());
			//compute stats on grid cell id
			//TODO
			//Compute indicator 1 - Shortest transport time to the nearest service
			//Compute indicator 2- Average transport time to the X nearest services
			//Compute indicator 3 - Service capacity within X, Y, Z minutes
		}

		//save stats
		//TODO

		logger.info("End");
	}

}
