package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

		//prepare2021Pop();
		join();
		aggregate();
		tiling(Format.CSV, null, 128);

		logger.info("End");
	}

	private static void prepare2021Pop() {
		logger.info("*** Prepare 2021 pop");

		logger.info("Load pop stats");
		ArrayList<Map<String, String>> dataPop = CSVUtil.load("E:/dissemination/shared-data/grid/grid_1km.csv");
		logger.info(dataPop.size());

		logger.info("Clean");
		//logger.info(dataPop.get(0).keySet());
		CSVUtil.removeColumn(dataPop, "DIST_BORD", "TOT_P_2018", "TOT_P_2006", "TOT_P_2011", "Y_LLC", "CNTR_ID", "NUTS2016_3", "NUTS2016_2", "NUTS2016_1", "NUTS2016_0", "LAND_PC", "X_LLC", "NUTS2021_3", "NUTS2021_2", "DIST_COAST", "NUTS2021_1", "NUTS2021_0");
		logger.info(dataPop.get(0).keySet());

		logger.info("save pop 2021 data CSV");
		CSVUtil.save(dataPop, basePath_ + "pop2021.csv");
	}


	private static void join() {
		logger.info("*** Prepare and join");

		logger.info("Load pop stats");
		ArrayList<Map<String, String>> dataPop = CSVUtil.load(basePath_ + "pop2021.csv");
		logger.info(dataPop.size());

		logger.info("Load bu stats");
		ArrayList<Map<String, String>> dataBu = CSVUtil.load(basePath + "building_area.csv");
		logger.info(dataBu.size());

		logger.info("join");
		List<Map<String, String>> data_ = CSVUtil.joinBothSides("GRD_ID", dataBu, dataPop, "0", false);
		logger.info(data_.size());

		logger.info("save joined data CSV");
		CSVUtil.save(data_, basePath_ + "joined.csv");
	}




	private static void aggregate() {
		logger.info("*** Aggregate");

		logger.info("Load CSV");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath_ + "joined.csv");
		logger.info(data.size());

		//define aggregations
		//res,indus,p_res,total_activity,p_agri,agri,total,p_act,comm_serv,p_comm_serv,typology_res_act,typology_act,p_indus
		Map<String, Aggregator> aggMap = new HashMap<String, Aggregator>();
		Collection<String> ignore = new ArrayList<>(); ignore.add("");
		aggMap.put("res", GridMultiResolutionProduction.getSumAggregator(10000, ignore));
		aggMap.put("agri", GridMultiResolutionProduction.getSumAggregator(10000, ignore));
		aggMap.put("indus", GridMultiResolutionProduction.getSumAggregator(10000, ignore));
		aggMap.put("comm_serv", GridMultiResolutionProduction.getSumAggregator(10000, ignore));
		aggMap.put("TOT_P_2021", GridMultiResolutionProduction.getSumAggregator(10000, null));

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
