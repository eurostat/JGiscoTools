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

	public static void main(String[] args) {
		System.out.println("Start");
		
		System.out.println("Format");
		ArrayList<Map<String, String>> data = CSVUtil.load("/home/juju/Bureau/gisco/cnt/fr/fr_200m/");
		System.out.println(data.size());
		System.out.println(data.get(0).keySet());
		System.out.println(data.get(0).keySet().size());


		System.out.println("End");
	}
	
	
}
