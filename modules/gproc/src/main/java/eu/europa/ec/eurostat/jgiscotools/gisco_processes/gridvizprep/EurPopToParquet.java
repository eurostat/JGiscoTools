/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.ParquetUtil;

/**
 * @author julien Gaffuri
 *
 */
public class EurPopToParquet {
	static Logger logger = LogManager.getLogger(EurPopToParquet.class.getName());


	//the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000 };
	private static String path = "/home/juju/Bureau/gisco/grid_pop/";

	public static void main(String[] args) {
		logger.info("Start");

		for(int r : resolutions) {
			logger.info("Resolution " + r + "m");		
			ParquetUtil.convertCSVToParquet(path + "pop_"+r+"m.csv", path, "pop_"+r+"m___.parquet", "GZIP");
		}

		logger.info("End");
	}

}
