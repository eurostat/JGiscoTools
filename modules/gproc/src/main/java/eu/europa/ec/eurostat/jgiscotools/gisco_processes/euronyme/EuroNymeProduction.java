/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.euronyme;

import java.util.ArrayList;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author julien Gaffuri
 *
 */
public class EuroNymeProduction {

	public static void main(String[] args) {
		System.out.println("Start");

		//load input data
		String erm = "/home/juju/Bureau/gisco/geodata/euro-regional-map-gpkg/data/OpenEuroRegionalMap.gpkg";
		ArrayList<Feature> buP = GeoData.getFeatures(erm, "BuiltupP", "id");
		System.out.println(buP.size() + " features loaded");
		ArrayList<Feature> buA = GeoData.getFeatures(erm, "BuiltupA", "id");
		System.out.println(buA.size() + " features loaded");
		
		ArrayList<Feature> areas = new ArrayList<Feature>();
		for(Feature fa : buA) {
			String id = fa.getAttribute("PopulatedPlaceId").toString();
			System.out.println(id);
			

		}
		
		
		//BuiltupP
		//NAMN1 PPL/PP1-PP2 population

		
		//ArrayList<Feature> name = GeoData.getFeatures(erm, "EBM_NAM", "id");
		//System.out.println(name.size() + " features loaded");
		//index EBM
		//id: SHN attribute
		//NAMN
		//PPL - population
		//ARA - area
				

		//set names
		

		//structure

		//output structure:
		//name
		//lat/lon (3 decimals)
		//font size
		//font weight
		//zoom range

		//make output (large)



		System.out.println("End");
	}

	private static void simplify() {
		//TODO
	}



}
