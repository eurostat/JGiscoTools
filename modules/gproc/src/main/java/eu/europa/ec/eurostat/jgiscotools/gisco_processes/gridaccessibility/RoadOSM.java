/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.Collection;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;


/**
 * @author gaffuju
 *
 */
public class RoadOSM {
	static String basePath = "E:/workspace/basic_services_accessibility/";

	public static void main(String[] args) {
		Collection<Feature> fs = get(null);
		System.out.println(fs.size());
	}

	public static Collection<Feature> get(String costAttribute) {
		Filter fil = null;
		try {
			fil = CQL.toFilter("(NOT fclass='bridleway' AND NOT fclass='track_grade1' AND NOT fclass='track_grade2' AND NOT fclass='track_grade3' AND NOT fclass='track_grade4' AND NOT fclass='track_grade5')");
		} catch (CQLException e) { e.printStackTrace(); }
		Collection<Feature> fs = GeoData.getFeatures(basePath + "input_data/test_osm_road_FR_SE/roads_integrate2.shp", null, fil);

		if(costAttribute != null)
			for(Feature f : fs) {
				double speedkmh = getSpeedKmH(f.getAttribute("maxspeed").toString(), f.getAttribute("fclass").toString());
				if(speedkmh == 0) {
					System.err.println("Pb: speed=0. " + f);
					speedkmh = 5;
				}
				double speedmmin = speedkmh *1000.0/60.0;
				double duration = f.getGeometry().getLength() / speedmmin;
				f.setAttribute(costAttribute, duration);
			}

		return fs;
	}

	private static double getSpeedKmH(String maxspeed, String fclass) {

		//TODO check that
		//TODO run integrate ?
		
		if(!"".equals(maxspeed)) {
			double ms = Double.parseDouble(maxspeed);
			if(ms>0) return ms;
		}

		if("motorway".equals(fclass)) return 100.0;
		if("trunk".equals(fclass)) return 75.0;
		if("primary".equals(fclass)) return 85.0;
		if("secondary".equals(fclass)) return 75.0;
		if("tertiary".equals(fclass)) return 65.0;
		if("unclassified".equals(fclass)) return 35.0;

		if("motorway_link".equals(fclass)) return 70.0;
		if("trunk_link".equals(fclass)) return 60.0;
		if("primary_link".equals(fclass)) return 35.0;
		if("secondary_link".equals(fclass)) return 35.0;
		if("tertiary_link".equals(fclass)) return 20.0;
		if("service".equals(fclass)) return 30.0;

		if("residential".equals(fclass)) return 35.0;
		if("living_street".equals(fclass)) return 20.0;
		if("pedestrian".equals(fclass)) return 5.0;
		if("track".equals(fclass)) return 15.0;
		if("cycleway".equals(fclass)) return 10.0;
		if("footway".equals(fclass)) return 5.0;
		if("path".equals(fclass)) return 5.0;
		if("steps".equals(fclass)) return 5.0;

		if("unknown".equals(fclass)) return 20.0;

		System.err.println("Could not find speed for OSM road section " + maxspeed + " " + fclass);
		return 50.0;
	}

}
