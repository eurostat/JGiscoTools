/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

/**
 * @author julien Gaffuri
 *
 */
public class INSEEGrid200m {

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
	
}
