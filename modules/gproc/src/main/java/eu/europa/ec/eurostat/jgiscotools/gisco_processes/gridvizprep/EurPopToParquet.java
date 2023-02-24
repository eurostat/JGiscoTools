/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.io.File;

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
	private static String path = "/home/juju/Bureau/a/";

	public static void main(String[] args) {
		logger.info("Start");

		for(int r : resolutions) {
			logger.info("Resolution " + r + "m");		

			String f = path + "pop_"+r+"m.csv";
			ParquetUtil.convertCSVToParquet(f, path, "population", "GZIP");

			new File(path + "population.parquet").renameTo(new File(path + "pop_"+r+"m.parquet"));
		}

		logger.info("End");
	}

}
