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
public class INSEEFilosifiPopulation {
	static Logger logger = LogManager.getLogger(INSEEFilosifiPopulation.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 200, 400, 1000, 2000, 5000, 10000, 20000, 50000 };
	private static String basePath = "/home/juju/Bureau/gisco/cnt/fr/Filosofi_200m/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");

		prepare();

		//aggregate();
		//tiling();

		logger.info("End");
	}

	private static void prepare() {

		// 2015
		// IdINSPIRE
		// Ind - Nombre d’individus

		logger.info("Load 2015");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "2015/Filosofi2015_carreaux_200m_metropole.csv");
		//logger.info(data.size());
		//logger.info(data.get(0).keySet());

		logger.info("Remove colums");
		CSVUtil.removeColumn(data, "Id_carr1km", "Id_carr_n", "Groupe", "Depcom", "I_pauv", "Id_car2010", "I_est_1km");

		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "IdINSPIRE", "GRD_ID");
		CSVUtil.renameColumn(data, "I_est_cr", "imputed");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		//logger.info("save 2015");
		//CSVUtil.save(data, basePath + "out/2015_prepared.csv");


/*
		// 2017
		// Idcar_200m
		// Ind - Nombre d’individus

		logger.info("Load 2017");
		data = CSVUtil.load(basePath + "2017/Filosofi2017_carreaux_200m_met.csv");
		//logger.info(data.size());
		//logger.info(data.get(0).keySet());

		logger.info("Remove colums");
		CSVUtil.removeColumn(data, "Idcar_1km", "Idcar_nat", "I_est_1km", "Groupe", "lcog_geo");

		/*
		CSVUtil.removeColumn(data, "Men_1ind", "Men_5ind", "Men_prop", "Men_fmp", "Men_surf", "Men_coll",
				"Men_mais", "Log_av45", "Log_45_70", "Log_70_90", "Log_ap90", "Log_inc", "Log_soc", "Men_pauv",
				"Men");

		CSVUtil.removeColumn(data, "Ind_snv", "Log_av45", "Log_45_70", "Log_70_90", "Log_ap90", "Log_inc",
				"Log_soc", "Ind_0_3", "Ind_4_5", "Ind_6_10", "Ind_11_17", "Ind_18_24", "Ind_25_39", "Ind_40_54",
				"Ind_55_64", "Ind_65_79", "Ind_80p", "Ind_inc");

			CSVUtil.removeColumn(data, "Men_1ind", "Men_5ind", "Men_prop", "Men_fmp", "Ind_snv", "Men_surf", "Men_coll",
					"Men_mais", "Ind_0_3", "Ind_4_5", "Ind_6_10", "Ind_11_17", "Ind_18_24", "Ind_25_39", "Ind_40_54",
					"Ind_55_64", "Ind_65_79", "Ind_80p", "Ind_inc", "Men_pauv", "Men");

		 */

		/*
		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "Idcar_200m", "GRD_ID");
		CSVUtil.renameColumn(data, "I_est_200", "imputed");

		logger.info(data.size());
		logger.info(data.get(0).keySet());
*/

//logger.info("save 2017");
		//CSVUtil.save(data, basePath + "out/2017_prepared.csv");
	}



	// derive resolutions
	private static void aggregate() {

		for (String ds : new String[] { "ind", "men", "log" }) {
			for (int year : new int[] { 2015, 2017 }) {
				logger.info("Load "+ds+" "+year);
				ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "out/" + year + "_"+ds+".csv");
				logger.info(data.size());

				for (int res : resolutions) {
					logger.info("Aggregate " + res + "m");
					ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregationDeprecated(data, "GRD_ID", res,
							10000, null, null);
					logger.info(out.size());

					logger.info("Save");
					CSVUtil.save(out, basePath + "out/" + year + "_"+ds+"_"+res+".csv");
				}
			}
		}

	}

	// tile all resolutions
	private static void tiling() {

		/*
		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			logger.info("Load");
			ArrayList<Map<String, String>> cells = CSVUtil.load(basePath + "out/" + year + "_"+ds+"_"+res+".csv");
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
		*/
	}

}
