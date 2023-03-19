/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.List;
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
public class INSEEFilosifiPopulation {
	static Logger logger = LogManager.getLogger(INSEEFilosifiPopulation.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 200, 400, 1000, 2000, 5000, 10000, 20000, 50000 };
	private static String basePath = "/home/juju/Bureau/gisco/cnt/fr/Filosofi_200m/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");

		prepare2015();
		prepare2017();
		join();
		aggregate();
		//tiling();

		logger.info("End");
	}


	private static void prepare2015() {

		// 2015
		// IdINSPIRE
		// Ind - Nombre d’individus

		logger.info("Load 2015");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "2015/Filosofi2015_carreaux_200m_metropole.csv");
		logger.info(data.size());
		//logger.info(data.get(0).keySet());

		logger.info("Remove colums");
		CSVUtil.removeColumn(data, "Id_carr1km", "Id_carr_n", "Groupe", "Depcom", "I_pauv", "Id_car2010", "Men", "Men_pauv", "Men_1ind", "Men_5ind", "Men_prop", "Men_fmp", "Ind_snv", "Men_surf", "Men_coll", "Men_mais", "Log_av45", "Log_45_70", "Log_70_90", "Log_ap90", "Log_inc", "Log_soc", "Ind_0_3", "Ind_4_5", "Ind_6_10", "Ind_11_17", "Ind_18_24", "Ind_25_39", "Ind_40_54", "Ind_55_64", "Ind_65_79", "Ind_80p", "Ind_inc", "I_est_1km");

		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "Ind", "Ind_2015");
		CSVUtil.renameColumn(data, "IdINSPIRE", "GRD_ID");
		CSVUtil.renameColumn(data, "I_est_cr", "imputed_2015");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		//logger.info("save 2015");
		CSVUtil.save(data, basePath + "out/2015_prepared.csv");
	}


	private static void prepare2017() {

		// 2017
		// Idcar_200m
		// Ind - Nombre d’individus

		logger.info("Load 2017");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "2017/Filosofi2017_carreaux_200m_met.csv");
		logger.info(data.size());
		//logger.info(data.get(0).keySet());

		logger.info("Remove colums");
		CSVUtil.removeColumn(data, "Idcar_1km", "I_est_1km", "Idcar_nat", "Groupe", "Men_1ind", "Men_5ind", "Men_prop",		"Men_fmp", "Ind_snv", "Men_surf", "Men_coll", "Men_mais", "Log_av45", "Log_45_70", "Log_70_90", "Log_ap90", "Log_inc", "Log_soc", "Ind_0_3", "Ind_4_5", "Ind_6_10",		"Ind_11_17", "Ind_18_24", "Ind_25_39", "Ind_40_54", "Ind_55_64", "Ind_65_79", "Ind_80p", "Ind_inc", "Men_pauv", "Men", "lcog_geo");

		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "Ind", "Ind_2017");
		CSVUtil.renameColumn(data, "Idcar_200m", "GRD_ID");
		CSVUtil.renameColumn(data, "I_est_200", "imputed_2017");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save 2017");
		CSVUtil.save(data, basePath + "out/2017_prepared.csv");
	}



	private static void join() {

		logger.info("Load 2015");
		ArrayList<Map<String, String>> data2015 = CSVUtil.load(basePath + "out/2015_prepared.csv");
		logger.info(data2015.size());

		logger.info("Load 2017");
		ArrayList<Map<String, String>> data2017 = CSVUtil.load(basePath + "out/2017_prepared.csv");
		logger.info(data2017.size());

		logger.info("join");
		List<Map<String, String>> data = CSVUtil.joinBothSides("GRD_ID", data2015, data2017, "0", false);
		logger.info(data.size());

		logger.info("save");
		CSVUtil.save(data, basePath + "out/joined.csv");

	}


	// derive resolutions
	private static void aggregate() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "out/joined.csv");
		logger.info(data.size());

		for (int res : resolutions) {
			logger.info("Aggregate " + res + "m");
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregationDeprecated(data, "GRD_ID", res,
					10000, null, null);
			logger.info(out.size());

			logger.info("Save");
			CSVUtil.save(out, basePath + "out/" +res+".csv");
		}

	}

	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			logger.info("Load");
			ArrayList<Map<String, String>> cells = CSVUtil.load(basePath + "out/" +res+".csv");
			logger.info(cells.size());

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 256);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "out/tiled/pop/" + res + "m";
			gst.save(outpath, GridTiler.Format.CSV, null, null, false);
			gst.saveTilingInfoJSON(outpath, GridTiler.Format.CSV, "Filosofi population resolution " + res + "m");

		}

	}

}
