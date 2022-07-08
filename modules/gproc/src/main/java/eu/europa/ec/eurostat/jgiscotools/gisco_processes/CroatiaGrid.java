/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

/**
 * @author julien Gaffuri
 *
 */
public class CroatiaGrid {
	static Logger logger = LogManager.getLogger(CroatiaGrid.class.getName());


	//the target resolutions
	private static int[] resolutions = new int[] {1000, 2000, 3000, 4000, 5000, 7000, 10000};
	private static String basePath = "/home/juju/Bureau/gisco/cnt/hr/grid/";
	private static String[] files = new String[] {"Population_2011_Grid_1000m.csv","Active_business_entities_2016_Grid_1000m.csv","Tourism_2017_Grid_1000m.csv"};


	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		aggregate();
		logger.info("End");
	}


	//derive resolutions
	private static void aggregate() {

		for(String file : files) {
			logger.info("Load " + file);
			ArrayList<Map<String, String>> data = CSVUtil.load(basePath + file);
			logger.info(data.size());

			for(int res : resolutions) {
				logger.info("Aggregate " + res + "m");
				ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, 10000);
				logger.info(out.size());

				logger.info("Save");
				CSVUtil.save(out, basePath + file +"_"+res+".csv");
			}
		}
	}

}
