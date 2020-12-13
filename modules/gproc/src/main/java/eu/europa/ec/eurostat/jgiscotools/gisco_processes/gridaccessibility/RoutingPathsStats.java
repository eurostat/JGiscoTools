/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author clemoki
 *
 */
public class RoutingPathsStats {
	private static Logger logger = LogManager.getLogger(RoutingPathsStats.class.getName());

	/** @param args **/
	public static void main(String[] args) {
		logger.info("Start");

		//1
		//load paths
		//while there are paths
		//pop first
		//get grid cell id
		//pop all others with same grid cell id
		//compute stats on grid cell id
		//next

		//2
		//for each grid cell
		//get cell id
		//get all paths from the grid cell
		//get

		//Compute indicator 1 - Shortest transport time to the nearest service
		//Compute indicator 2- Average transport time to the X nearest services
		//Compute indicator 3 - Service capacity within X, Y, Z minutes


		logger.info("End");
	}

}
