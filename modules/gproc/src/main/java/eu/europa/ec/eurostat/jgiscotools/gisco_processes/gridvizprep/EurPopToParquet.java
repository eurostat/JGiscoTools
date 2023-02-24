/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author julien Gaffuri
 *
 */
public class EurPopToParquet {
	static Logger logger = LogManager.getLogger(EurPopToParquet.class.getName());


	//the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000 };
	private static String basePath = "/home/juju/Bureau/gisco/";
	private static String outPath = basePath + "grid_pop/";

	public static void main(String[] args) {
		logger.info("Start");

		for(int r : resolutions) {
			logger.info("Res: " + r);
			
			
			
		}

		logger.info("End");
	}

}
