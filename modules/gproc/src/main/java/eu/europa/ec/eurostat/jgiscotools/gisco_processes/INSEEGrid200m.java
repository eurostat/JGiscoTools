/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction.DataPreparation;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction.GridsProduction;
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
		System.out.println("Start");

		String path = "/home/juju/Bureau/gisco/cnt/fr/fr_200m/";

		//prepare(path);

		//aggregate
		aggregate(path);

		//tiling


		System.out.println("End");
	}


	static void aggregate(String path) {

		System.out.println("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(path + "Filosofi2015_prepared.csv");
		System.out.println(data.size());


	}


	/**
	 * Remove attributes, set x and y.
	 * 
	 * @param path
	 */
	static void prepare(String path) {

		System.out.println("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(path + "Filosofi2015_carreaux_200m_metropole.csv");
		System.out.println(data.size());
		System.out.println(data.get(0).keySet());

		System.out.println("Remove colums");
		CSVUtil.removeColumn(data,
				"Id_carr1km",
				"I_est_cr",
				"Id_carr_n",
				"Groupe",
				"Depcom",
				"I_pauv",
				"Id_car2010",
				"I_est_1km");

		System.out.println("Set x,y");
		for(Map<String, String> c : data) {
			String s = c.get("IdINSPIRE");
			//CRS3035RES200mN2940600E3844600
			s = s.split("mN")[1];
			c.put("x", Integer.parseInt(s.split("E")[0])+"" );
			c.put("y", Integer.parseInt(s.split("E")[1])+"" );
		}

		System.out.println("Remove colums");
		CSVUtil.removeColumn(data, "IdINSPIRE");

		System.out.println(data.size());
		System.out.println(data.get(0).keySet());

		System.out.println("save");
		CSVUtil.save(data, path + "Filosofi2015_prepared.csv");
	}




	/**
	 * Aggregate cell data (from CSV file usually) into a target resolution.
	 * Sum of attributes.
	 * 
	 * @param cells
	 * @param xCol
	 * @param yCol
	 * @param res
	 * @return
	 */
	public ArrayList<Map<String, String>> gridAggregation(ArrayList<Map<String, String>> cells, String xCol, String yCol, int res) {	

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



}
