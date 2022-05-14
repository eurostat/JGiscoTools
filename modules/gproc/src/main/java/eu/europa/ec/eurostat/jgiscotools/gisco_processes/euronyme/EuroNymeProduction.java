/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.euronyme;

import java.util.ArrayList;

import eu.europa.ec.eurostat.jgiscotools.deprecated.GeoUtils;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author julien Gaffuri
 *
 */
public class EuroNymeProduction {

	//output structure:
	//name
	//lat/lon
	//font size
	//weight
	//zoom range


	public static void main(String[] args) {
		System.out.println("Start");

		//load input data
		String erm = "/home/juju/Bureau/gisco/geodata/euro-regional-map-gpkg/data/OpenEuroRegionalMap.gpkg";
		ArrayList<Feature> fs = GeoData.getFeatures(erm, "id");
		//BuiltupA BuiltupP EBM_NAM

		System.out.println(fs.size() + " features loaded");


		//structure

		//make output (large)



		System.out.println("End");
	}

	private static void simplify() {
		//TODO
	}



}
