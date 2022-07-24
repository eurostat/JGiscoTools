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


	//the target resolutions
	private static int[] resolutions = new int[] {200, 400, 1000, 2000, 5000, 10000, 20000, 50000};
	private static String basePath = "/home/juju/Bureau/gisco/cnt/fr/Filosofi_2015_200m/";


	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		//prepare();
		//aggregate();
		tiling();
		logger.info("End");
	}



	//remove attributes
	private static void prepare() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "Filosofi2015_carreaux_200m_metropole.csv");
		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("Remove colums");
		CSVUtil.removeColumn(data,
				"Id_carr1km",
				"I_est_cr",
				"Id_carr_n",
				"Groupe",
				"Depcom",
				"I_pauv",
				"Id_car2010",
				"I_est_1km",

				"Men_1ind", //: Nombre de ménages d’un seul individu
				"Men_5ind", //: Nombre de ménages de 5 individus ou plus
				"Men_prop", //: Nombre de ménages propriétaires
				"Men_fmp", //: Nombre de ménages monoparentaux
				"Men_surf", //: Somme de la surface des logements du carreau
				"Men_coll", //: Nombre de ménages en logements collectifs
				"Men_mais", //: Nombre de ménages en maison
				"Log_av45", //: Nombre de logements construits avant 1945
				"Log_45_70", //: Nombre de logements construits entre 1945 et 1969
				"Log_70_90", //: Nombre de logements construits entre 1970 et 1989
				"Log_ap90", //: Nombre de logements construits depuis 1990
				"Log_inc", //: Nombre de logements dont la date de construction est inconnue
				"Log_soc" //: Nombre de logements sociaux
				);

		/*
		Ind : Nombre d’individus
		Men : Nombre de ménages
		Men_pauv : Nombre de ménages pauvres
		Ind_snv : Somme des niveaux de vie winsorisés des individus
		Ind_0_3 : Nombre d’individus de 0 à 3 ans
		Ind_4_5 : Nombre d’individus de 4 à 5 ans
		Ind_6_10 : Nombre d’individus de 6 à 10 ans
		Ind_11_17 : Nombre d’individus de 11 à 17 ans
		Ind_18_24 : Nombre d’individus de 18 à 24 ans
		Ind_25_39 : Nombre d’individus de 25 à 39 ans
		Ind_40_54 : Nombre d’individus de 40 à 54 ans
		Ind_55_64 : Nombre d’individus de 55 à 64 ans
		Ind_65_79 : Nombre d’individus de 65 à 79 ans
		Ind_80p : Nombre d’individus de 80 ans ou plus
		Ind_inc : Nombre d’individus dont l’âge est inconnu
		 */


		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "IdINSPIRE", "GRD_ID");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save");
		CSVUtil.save(data, basePath + "Filosofi2015_prepared.csv");
	}


	//derive resolutions
	private static void aggregate() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "Filosofi2015_prepared.csv");
		logger.info(data.size());

		for(int res : resolutions) {
			logger.info("Aggregate " + res + "m");
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, 10000, null, null);
			logger.info(out.size());

			logger.info("Save");
			CSVUtil.save(out, basePath + "Filosofi2015_"+res+".csv");
		}
	}


	//tile all resolutions
	private static void tiling() {

		for(int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + "Filosofi2015_"+res+".csv";

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
			gst.saveTilingInfoJSON(outpath, "Filosofi 2015 resolution " + res + "m");

		}
	}

}
