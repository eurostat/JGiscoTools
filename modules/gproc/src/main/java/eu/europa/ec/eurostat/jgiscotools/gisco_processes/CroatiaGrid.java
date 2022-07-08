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
public class CroatiaGrid {
	static Logger logger = LogManager.getLogger(CroatiaGrid.class.getName());


	//the target resolutions
	private static int[] resolutions = new int[] {1000, 2000, 3000, 4000, 5000, 7000, 10000};
	private static String basePath = "/home/juju/Bureau/gisco/cnt/hr/grid/";
	private static String[] files = new String[] {"Population_2011_Grid_1000m","Active_business_entities_2016_Grid_1000m","Tourism_2017_Grid_1000m"};


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
			ArrayList<Map<String, String>> data = CSVUtil.load(basePath + file + ".csv");
			logger.info(data.size());

			//restructure
			CSVUtil.renameColumn(data, "IDENTIFIER", "GRD_ID");
			for(Map<String, String> d : data) {
				String id = d.get("GRD_ID");
				//1kmN2186E5020
				//CRS3035RES200mN2893400E3763200
				id = id.replace("1kmN", "");
				String[] s = id.split("E");
				id = "CRS3035RES1000mN"+ (1000*Integer.parseInt(s[0])) +"E" + (1000*Integer.parseInt(s[1]));
				d.put("GRD_ID",id);				
			}

			//replace D and Z
			CSVUtil.replaceValue(data, "D", "0");
			CSVUtil.replaceValue(data, "Z", "0");

			for(int res : resolutions) {
				logger.info("Aggregate " + res + "m");
				ArrayList<Map<String, String>> cells = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, 10000);
				logger.info(cells.size());

				//logger.info("Save");
				//CSVUtil.save(cells, basePath + (file.replace("_1000m", "")) +"_agg_"+res+".csv");

				logger.info("Build tiles");
				GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0,0), 128);

				gst.createTiles();
				logger.info(gst.getTiles().size() + " tiles created");

				logger.info("Save");
				String tag = (file.replace("_Grid_1000m", ""));
				String outpath = basePath+ tag + "/" + res + "m";
				gst.saveCSV(outpath);
				gst.saveTilingInfoJSON(outpath, tag+" resolution " + res + "m");
			}

		}
	}

}
