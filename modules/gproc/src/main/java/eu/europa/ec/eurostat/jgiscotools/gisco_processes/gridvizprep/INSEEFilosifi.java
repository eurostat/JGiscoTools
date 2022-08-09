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
public class INSEEFilosifi {
	static Logger logger = LogManager.getLogger(INSEEFilosifi.class.getName());

	// 2015
	// IdINSPIRE,Id_carr1km,I_est_cr,Id_carr_n,Groupe,Depcom,I_pauv,Id_car2010,Ind,Men,Men_pauv,Men_1ind,Men_5ind,Men_prop,Men_fmp,Ind_snv,Men_surf,Men_coll,Men_mais,Log_av45,Log_45_70,Log_70_90,Log_ap90,Log_inc,Log_soc,Ind_0_3,Ind_4_5,Ind_6_10,Ind_11_17,Ind_18_24,Ind_25_39,Ind_40_54,Ind_55_64,Ind_65_79,Ind_80p,Ind_inc,I_est_1km
	// CRS3035RES200mN2893400E3763200,CRS3035RES1000mN2893000E3763000,"0",CRS3035RES1000mN2893000E3763000,"866132","75119","0",CRS3035RES200mN2893400E3763200,2818.5,990.0,280.0,340.0,164.0,21.0,159.0,44265483.1,57579.0,988.0,2.0,426.0,0.0,237.0,327.0,0.0,937.0,147.0,77.0,162.5,308.0,208.0,517.0,524.0,362.0,351.0,93.0,69.0,"0"

	// Ind - Nombre d’individus
	// Ind_0_3
	// Ind_4_5
	// Ind_6_10
	// Ind_11_17
	// Ind_18_24
	// Ind_25_39
	// Ind_40_54
	// Ind_55_64
	// Ind_65_79
	// Ind_80p
	// Ind_inc
	// Ind_snv - Somme des niveaux de vie winsorisés des individus
	// Men - Nombre de ménages
	// Men_pauv - Nombre de ménages pauvres
	// Men_1ind - Nombre de ménages d’un seul individu
	// Men_5ind - Nombre de ménages de 5 individus ou plus
	// Men_prop - Nombre de ménages propriétaires
	// Men_fmp - Nombre de ménages monoparentaux
	// Men_surf - Somme de la surface des logements* du carreau
	// Men_coll - Nombre de ménages en logement collectif
	// Men_mais - Nombre de ménages en maison
	// Log_av45 - Nombre de logements* construits avant 1945
	// Log_45_70 - Nombre de logements*construits entre 1945 et 1969
	// Log_70_90 - Nombre de logements* construits entre 1970 et 1989
	// Log_ap90 - Nombre de logements* construits depuis 1990
	// Log_inc - Nombre de logements* dont la date de construction est inconnue
	// Log_soc - Nombre de logements* sociaux
	// IdINSPIRE - id inspire
	// Id_carr_n - out
	// Id_carr1km - out
	// Id_car2010 - out
	// I_est_cr - Vaut 1 si le carreau est imputé par une valeur approchée, 0 sinon.
	// I_est_1km - out
	// Groupe
	// Depcom - code commune - out
	// I_pauv - Vaut 1 si le carreau a été traité pour respecter la confidentialité
	// sur le nombre de ménages pauvres (valeur ramenée à 80 % du nombre de
	// ménages).

	// 2017
	// Idcar_200m,I_est_200,Idcar_1km,I_est_1km,Idcar_nat,Groupe,Ind,Men_1ind,Men_5ind,Men_prop,Men_fmp,Ind_snv,Men_surf,Men_coll,Men_mais,Log_av45,Log_45_70,Log_70_90,Log_ap90,Log_inc,Log_soc,Ind_0_3,Ind_4_5,Ind_6_10,Ind_11_17,Ind_18_24,Ind_25_39,Ind_40_54,Ind_55_64,Ind_65_79,Ind_80p,Ind_inc,Men_pauv,Men,lcog_geo
	// CRS3035RES200mN2029800E4254200,1,CRS3035RES1000mN2029000E4254000,1,CRS3035RES8000mN2024000E4248000,14863,2,0.3,0,0.6,0,59349.4,145.1,0.1,0.8,0,0,0.4,0.5,0,0,0,0,0,0.3,0.2,0,0.4,0.3,0.7,0.1,0,0.2,0.9,2A041

	// Ind - Nombre d’individus
	// Ind_0_3
	// Ind_4_5
	// Ind_6_10
	// Ind_11_17
	// Ind_18_24
	// Ind_25_39
	// Ind_40_54
	// Ind_55_64
	// Ind_65_79
	// Ind_80p
	// Ind_inc
	// Ind_snv - Somme des niveaux de vie winsorisés des individus
	// Men - Nombre de ménages
	// Men_pauv - Nombre de ménages pauvres
	// Men_1ind - Nombre de ménages d’un seul individu
	// Men_5ind - Nombre de ménages de 5 individus ou plus
	// Men_prop - Nombre de ménages propriétaires
	// Men_fmp - Nombre de ménages monoparentaux
	// Men_surf - Somme de la surface des logements* du carreau
	// Men_coll - Nombre de ménages en logement collectif
	// Men_mais - Nombre de ménages en maison
	// Log_av45 - Nombre de logements* construits avant 1945
	// Log_45_70 - Nombre de logements*construits entre 1945 et 1969
	// Log_70_90 - Nombre de logements* construits entre 1970 et 1989
	// Log_ap90 - Nombre de logements* construits depuis 1990
	// Log_inc - Nombre de logements* dont la date de construction est inconnue
	// Log_soc - Nombre de logements* sociaux
	// Idcar_200m - id inspire
	// Idcar_1km - id inspire
	// Idcar_nat - out
	// I_est_200 - Vaut 1 si le carreau est imputé par une valeur approchée, 0
	// sinon.
	// I_est_1km - out
	// Groupe
	// lcog_geo

	// the target resolutions
	private static int[] resolutions = new int[] { 200, 400, 1000, 2000, 5000, 10000, 20000, 50000 };
	private static String basePath = "/home/juju/Bureau/gisco/cnt/fr/Filosofi_200m/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		// prepare2015();
		// prepare2017();
		//prepareInd();
		//prepareIndCh();
		// prepareSNV();
		//prepareMen();
		//prepareLog();
		aggregate();
		// tiling();
		logger.info("End");
	}

	private static void prepare2015() {

		logger.info("Load 2015");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "2015/Filosofi2015_carreaux_200m_metropole.csv");
		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("Remove colums");
		CSVUtil.removeColumn(data, "Id_carr1km", "Id_carr_n", "Groupe", "Depcom", "I_pauv", "Id_car2010", "I_est_1km");

		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "IdINSPIRE", "GRD_ID");
		CSVUtil.renameColumn(data, "I_est_cr", "imputed");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save 2015");
		CSVUtil.save(data, basePath + "out/2015_prepared.csv");
	}

	private static void prepare2017() {

		logger.info("Load 2017");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "2017/Filosofi2017_carreaux_200m_met.csv");
		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("Remove colums");
		CSVUtil.removeColumn(data, "Idcar_1km", "Idcar_nat", "I_est_1km", "Groupe", "lcog_geo");

		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "Idcar_200m", "GRD_ID");
		CSVUtil.renameColumn(data, "I_est_200", "imputed");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save 2017");
		CSVUtil.save(data, basePath + "out/2017_prepared.csv");
	}

	private static void prepareInd() {

		for (int year : new int[] { 2015, 2017 }) {
			logger.info("Ind " + year);
			ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "out/" + year + "_prepared.csv");

			CSVUtil.removeColumn(data, "Men_1ind", "Men_5ind", "Men_prop", "Men_fmp", "Men_surf", "Men_coll",
					"Men_mais", "Log_av45", "Log_45_70", "Log_70_90", "Log_ap90", "Log_inc", "Log_soc", "Men_pauv",
					"Men");
			// CSVUtil.removeColumn(data, "Ind", "Men_1ind", "Men_5ind", "Men_prop",
			// "Men_fmp", "Ind_snv", "Men_surf", "Men_coll", "Men_mais", "Log_av45",
			// "Log_45_70", "Log_70_90", "Log_ap90", "Log_inc", "Log_soc", "Ind_0_3",
			// "Ind_4_5", "Ind_6_10", "Ind_11_17", "Ind_18_24", "Ind_25_39", "Ind_40_54",
			// "Ind_55_64", "Ind_65_79", "Ind_80p", "Ind_inc", "Men_pauv", "Men", "GRD_ID",
			// "imputed");

			logger.info(data.get(0).keySet());

			logger.info("save ind " + year);
			CSVUtil.save(data, basePath + "out/" + year + "_ind.csv");
		}
	}

	private static void prepareMen() {

		for (int year : new int[] { 2015, 2017 }) {
			logger.info("Men " + year);
			ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "out/" + year + "_prepared.csv");

			CSVUtil.removeColumn(data, "Ind_snv", "Log_av45", "Log_45_70", "Log_70_90", "Log_ap90", "Log_inc",
					"Log_soc", "Ind_0_3", "Ind_4_5", "Ind_6_10", "Ind_11_17", "Ind_18_24", "Ind_25_39", "Ind_40_54",
					"Ind_55_64", "Ind_65_79", "Ind_80p", "Ind_inc");

			logger.info(data.get(0).keySet());

			logger.info("save men " + year);
			CSVUtil.save(data, basePath + "out/" + year + "_men.csv");
		}
	}

	private static void prepareLog() {

		for (int year : new int[] { 2015, 2017 }) {
			logger.info("Log " + year);
			ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "out/" + year + "_prepared.csv");

			CSVUtil.removeColumn(data, "Men_1ind", "Men_5ind", "Men_prop", "Men_fmp", "Ind_snv", "Men_surf", "Men_coll",
					"Men_mais", "Ind_0_3", "Ind_4_5", "Ind_6_10", "Ind_11_17", "Ind_18_24", "Ind_25_39", "Ind_40_54",
					"Ind_55_64", "Ind_65_79", "Ind_80p", "Ind_inc", "Men_pauv", "Men");

			logger.info(data.get(0).keySet());

			logger.info("save log " + year);
			CSVUtil.save(data, basePath + "out/" + year + "_log.csv");
		}
	}

	// derive resolutions
	private static void aggregate() {

		for (String ds : new String[] { "ind", "men", "log" }) {
			for (int year : new int[] { 2015, 2017 }) {
				logger.info("Load");
				ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "out/" + year + "_"+ds+".csv");
				logger.info(data.size());

				for (int res : resolutions) {
					logger.info("Aggregate " + res + "m");
					ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res,
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

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + "Filosofi2015_" + res + ".csv";

			logger.info("Load");
			ArrayList<Map<String, String>> cells = CSVUtil.load(f);
			logger.info(cells.size());

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "tiled/" + res + "m";
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "Filosofi 2015 resolution " + res + "m");

		}
	}

}
