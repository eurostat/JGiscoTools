/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridtiling.GriddedStatsTiler;
import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

/**
 * @author julien Gaffuri
 *
 */
public class INSEEGrid200m {
	static Logger logger = LogManager.getLogger(INSEEGrid200m.class.getName());


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



	//remove attributes, set x and y.
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
				"I_est_1km");

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
			ArrayList<Map<String, String>> out = INSEEGrid200m.gridAggregation(data, "GRD_ID", res);
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

			logger.info("Load header");
			ArrayList<String> header = CSVUtil.getHeader(f);
			header.remove("GRD_ID");
			logger.info(header);
			String[] hs = header.toArray(new String[header.size()]);

			logger.info("Load");
			StatsHypercube sh = CSV.loadMultiValues(f, "indic", hs );
			logger.info(sh.stats.size());

			logger.info("Build tiles");
			GriddedStatsTiler gst = new GriddedStatsTiler(128, sh, "indic", "0");
			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath+"tiled/"+res+"m";
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "Filosofi 2015 resolution " + res + "m");

		}
	}












	//TODO generic: move that to grid module

	/**
	 * Aggregate cell data (from CSV file usually) into a target resolution.
	 * Sum of attributes. Attributes are numerical values.
	 * 
	 * @param cells
	 * @param xCol
	 * @param yCol
	 * @param res
	 * @return
	 */
	public static ArrayList<Map<String, String>> gridAggregation(ArrayList<Map<String, String>> cells, String gridIdCol, int res) {	

		//index input data by upper grid cell
		HashMap<String, Map<String, String>> index = new HashMap<>();
		for(Map<String, String> cell : cells) {

			//get upper cell
			GridCell up = new GridCell(cell.get(gridIdCol)).getUpperCell(res);
			String id = up.getId();

			//get upper cell
			Map<String, String> cellAgg = index.get(id);

			if(cellAgg == null) {
				//create
				cellAgg = new HashMap<String, String> ();
				cellAgg.putAll(cell);
				index.put(id, cellAgg);
			}
			else {
				//add
				add(cellAgg, cell, gridIdCol);
			}

			//set grid id
			cellAgg.put(gridIdCol, id);

		}

		//make output
		ArrayList<Map<String, String>> out = new ArrayList<Map<String,String>>();
		out.addAll(index.values());

		return out;
	}

	private static void add(Map<String, String> cell, Map<String, String> cellToAdd, String gridIdCol) {
		for(String k : cell.keySet()) {
			if(k.equals(gridIdCol)) continue;
			double v = Double.parseDouble(cell.get(k));
			double vToAdd = Double.parseDouble(cellToAdd.get(k));
			v += vToAdd;
			//get value as int if it is an integer
			cell.put(k, (v % 1) == 0 ? Integer.toString((int)v) : Double.toString(v) );
		}
	}

}
