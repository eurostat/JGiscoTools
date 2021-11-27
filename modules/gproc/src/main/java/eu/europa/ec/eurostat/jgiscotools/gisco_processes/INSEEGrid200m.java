/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

/**
 * @author julien Gaffuri
 *
 */
public class INSEEGrid200m {
	static Logger logger = LogManager.getLogger(INSEEGrid200m.class.getName());


	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");

		String path = "/home/juju/Bureau/gisco/cnt/fr/fr_200m/";

		//prepare(path);

		//aggregate
		aggregate(path);

		//tiling
		//TODO


		logger.info("End");
	}


	static void aggregate(String path) {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(path + "Filosofi2015_prepared.csv");
		logger.info(data.size());

		logger.info("Aggregate");
		for(int res : )
		int res = 1000;
		ArrayList<Map<String, String>> out = INSEEGrid200m.gridAggregation(data, "x", "y", res);
		logger.info(out.size());

		logger.info("Save");
		CSVUtil.save(out, path + "Filosofi2015_"+res+".csv");
	}


	/**
	 * Remove attributes, set x and y.
	 * 
	 * @param path
	 */
	static void prepare(String path) {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(path + "Filosofi2015_carreaux_200m_metropole.csv");
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

		logger.info("Set x,y");
		for(Map<String, String> c : data) {
			String s = c.get("IdINSPIRE");
			//CRS3035RES200mN2940600E3844600
			s = s.split("mN")[1];
			c.put("x", Integer.parseInt(s.split("E")[0])+"" );
			c.put("y", Integer.parseInt(s.split("E")[1])+"" );
		}

		logger.info("Remove colums");
		CSVUtil.removeColumn(data, "IdINSPIRE");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save");
		CSVUtil.save(data, path + "Filosofi2015_prepared.csv");
	}













	//TODO move to grid module

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
	public static ArrayList<Map<String, String>> gridAggregation(ArrayList<Map<String, String>> cells, String xCol, String yCol, int res) {	

		//index input data by upper grid cell
		HashMap<String, Map<String, String>> index = new HashMap<>();
		for(Map<String, String> cell : cells) {

			//get coordinates of upper cell
			int[] pos = GridCell.getUpperCell(Integer.parseInt(cell.get(xCol)), Integer.parseInt(cell.get(yCol)), res);

			//get upper cell
			String key = pos[0]+"_"+pos[1];
			Map<String, String> cellAgg = index.get(key);

			if(cellAgg == null) {
				//create
				cellAgg = new HashMap<String, String> ();
				cellAgg.putAll(cell);
				index.put(key, cellAgg);
			}
			else {
				//add
				add(cellAgg, cell);
			}

			//override x,y of upper cell with correct values
			cellAgg.put(xCol, pos[0]+"");
			cellAgg.put(yCol, pos[1]+"");
		}

		//make output
		ArrayList<Map<String, String>> out = new ArrayList<Map<String,String>>();
		out.addAll(index.values());

		return out;
	}

	private static void add(Map<String, String> cell, Map<String, String> cellToAdd) {
		for(String k : cell.keySet()) {
			int v = 0;
			try {
				v = Integer.parseInt(cell.get(k));
			} catch (NumberFormatException e) {
				double v_ = Double.parseDouble(cell.get(k));
				double vToAdd = Double.parseDouble(cellToAdd.get(k));
				cell.put(k, (v+vToAdd)+"");
				continue;
			}
			int vToAdd = Integer.parseInt(cellToAdd.get(k));
			cell.put(k, (v+vToAdd)+"");
		}
	}

}
