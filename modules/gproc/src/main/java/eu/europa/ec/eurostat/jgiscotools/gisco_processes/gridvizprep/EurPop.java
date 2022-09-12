/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

/**
 * @author julien Gaffuri
 *
 */
public class EurPop {
	static Logger logger = LogManager.getLogger(EurPop.class.getName());


	//the target resolutions
	private static int[] resolutions = new int[] {1000, 2000, 3000, 5000, 7000, 10000, 15000, 25000, 40000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/cnt/hr/grid/";
	private static String[] files = new String[] {"Population_2011_Grid_1000m","Active_business_entities_2016_Grid_1000m","Tourism_2017_Grid_1000m"};


	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");


		
		logger.info("End");
	}

}
