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
public class RoadTomtom {
	static String basePath = "E:/workspace/basic_services_accessibility/";

	/*public static void main(String[] args) {
		Collection<Feature> fs = get(null);
		System.out.println(fs.size());
	}*/

	public static Collection<Feature> get(String costAttribute) {
		Filter fil = null;
		try {
			fil = CQL.toFilter("(NOT SPEEDCAT=0 AND NOT FEATTYP=4130 AND NOT FEATTYP=4165 AND NOT FRC=8)");
		} catch (CQLException e) { e.printStackTrace(); }
		Collection<Feature> fs = GeoData.getFeatures(basePath + "input_data/test_tomtom_FR_SE/roads.gpkg", null, fil);

		if(costAttribute != null)
			for(Feature f : fs) {
				double speedkmh = getSpeedKmH(f.getAttribute("KPH").toString());
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

	private static double getSpeedKmH(String kph) {

		if(!"".equals(kph)) {
			double ms = Double.parseDouble(kph);
			if(ms>0) return ms;
		}

		System.err.println("Could not find speed for OSM road section " + kph);
		return 0.0;
	}

}
