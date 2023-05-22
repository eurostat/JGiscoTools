package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction.Aggregator;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler.Format;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class GridVizPrep {
	static Logger logger = LogManager.getLogger(GridVizPrep.class.getName());

	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000 };
	private static String basePath = "H:/ws/building_stats/";
	private static String basePath_ = basePath + "gridviz_tiles/";

	public static void main(String[] args) {
		logger.info("Start");

		aggregate();
		//tiling(Format.CSV, null, 128);

		logger.info("End");
	}




	private static void aggregate() {
		logger.info("*** Aggregate");

		logger.info("Load CSV");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "building_area.csv");
		logger.info(data.size());

		//define aggregations
		//res,indus,p_res,total_activity,p_agri,agri,total,p_act,comm_serv,p_comm_serv,typology_res_act,typology_act,p_indus
		Map<String, Aggregator> aggMap = new HashMap<String, Aggregator>();
		Collection<String> valuesToIgnore = new ArrayList<>(); valuesToIgnore.add("");
		aggMap.put("res", GridMultiResolutionProduction.getSumAggregator(10000, valuesToIgnore));
		aggMap.put("agri", GridMultiResolutionProduction.getSumAggregator(10000, valuesToIgnore));
		aggMap.put("indus", GridMultiResolutionProduction.getSumAggregator(10000, valuesToIgnore));
		aggMap.put("comm_serv", GridMultiResolutionProduction.getSumAggregator(10000, valuesToIgnore));

		for (int res : resolutions) {
			logger.info("Aggregate " + res + "m");

			//aggregate
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, aggMap );

			logger.info("Save " + out.size());
			CSVUtil.save(out, basePath_ + res + ".csv");
		}

	}




	// tile all resolutions
	private static void tiling(Format format, CompressionCodecName comp, int nbp) {
		logger.info("*** Tilling");

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath_ + res + ".csv";

			logger.info("Load");
			ArrayList<Map<String, String>> cells = CSVUtil.load(f);
			logger.info(cells.size());

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), nbp);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath_ + "tiled" + format + comp + nbp + "/" + res + "m";

			gst.save(outpath, format, "ddb", comp, true);
			gst.saveTilingInfoJSON(outpath, format, "Europe building statistics " + res + "m");

		}
	}

}
