/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class GeocodingTest {

	public static void main(String[] args) {
		System.out.println("Start");

		//load hospital addresses
		ArrayList<Map<String, String>> data = CSVUtil.load("C:\\Users\\gaffuju\\workspace\\healthcare-services\\data\\csv/all.csv");
		
		//take few randomly
		
		//compute position with bing geocoder
		//compute position with gisco geocoder

		//
		
		System.out.println("End");
	}
	
}
