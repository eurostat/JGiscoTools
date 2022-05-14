/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.euronyme;

import eu.europa.ec.eurostat.jgiscotools.deprecated.GeoUtils;
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
		
		//load input data
		String erm = "/home/juju/Bureau/gisco/geodata/euro-regional-map-gpkg/data/OpenEuroRegionalMap.gpkg";
		GeoData.getFeatures(erm, "id");
		//BuiltupA BuiltupP EBM_NAM
		
		
		
		//structure
		
		//make output (large)
		
		
		
	}
	
	private static void simplify() {
		//TODO
	}

	
	
}
