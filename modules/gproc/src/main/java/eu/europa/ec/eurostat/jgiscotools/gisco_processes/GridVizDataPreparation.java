/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

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
public class GridVizDataPreparation {
	static Logger logger = LogManager.getLogger(GridVizDataPreparation.class.getName());


	//the target resolutions
	private static int[] resolutions = new int[] {1000, 2000, 5000, 10000, 20000, 50000};
	private static String basePath = "/home/juju/Bureau/gisco/cnt/se/pop_grid/";


	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		aggregate();
		tiling();
		logger.info("End");
	}


	//derive resolutions
	private static void aggregate() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "se.csv");
		logger.info(data.size());

		for(int res : resolutions) {
			logger.info("Aggregate " + res + "m");
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, 10000);
			logger.info(out.size());

			logger.info("Save");
			CSVUtil.save(out, basePath + "se_"+res+".csv");
		}
	}


	//tile all resolutions
	private static void tiling() {

		for(int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + "se_"+res+".csv";

			logger.info("Load");
			ArrayList<Map<String, String>> cells = CSVUtil.load(f);
			logger.info(cells.size());

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0,0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath+"tiled/"+res+"m";
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "Statistics Sweden population by gender and age - resolution " + res + "m");

		}
	}

}
