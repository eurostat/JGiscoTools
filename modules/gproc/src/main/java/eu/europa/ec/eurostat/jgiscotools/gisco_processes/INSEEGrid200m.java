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
		
		System.out.println("Format");
		ArrayList<Map<String, String>> data = CSVUtil.load("/home/juju/Bureau/gisco/cnt/fr/fr_200m/Filosofi2015_carreaux_200m_metropole.csv");
		System.out.println(data.size());
		System.out.println(data.get(0).keySet());

		CSVUtil.removeColumn(data,
				"IdINSPIRE",
				"Id_carr1km",
				"I_est_cr",
				"Id_carr_n",
				"Groupe",
				"Depcom",
				"I_pauv",
				"Id_car2010");


		//get geom
		//"IdINSPIRE"

		CSVUtil.removeColumn(data"IdINSPIRE")

		System.out.println("End");
	}

}
