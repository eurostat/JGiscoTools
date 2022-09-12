/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

/**
 * @author julien Gaffuri
 *
 */
public class EurPop {
	static Logger logger = LogManager.getLogger(EurPop.class.getName());


	//the target resolutions
	private static String basePath = "/home/juju/Bureau/gisco/grid_pop/";


	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");

		int year = 2018;

		for(int resKm : new int[] {100, 50, 20, 10, 5}) {

			logger.info("Load");
			ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "pop_grid_" + year + "_"+ resKm +"km.csv");
			logger.info(data.size());
			logger.info(data.get(0).keySet());

			for(Map<String, String> cell : data) {

				String id = cell.get("GRD_ID");
				GridCell gc = new GridCell(id);
				cell.put("x", gc.getLowerLeftCornerPositionX()+"");
				cell.put("y", gc.getLowerLeftCornerPositionY()+"");
				cell.remove("GRD_ID");

				cell.put("population", cell.get("TOT_P"));
				cell.remove("TOT_P");
			}

			logger.info("save");
			CSVUtil.save(data, basePath + "xy/" + "pop_grid_xy_" + year + "_"+ resKm +"km.csv");

		}

		logger.info("End");
	}

}
