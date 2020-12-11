/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.Collection;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.SpeedCalculator;

/**
 * @author clemoki
 *
 */
public class RoadBDTopo {
	static String basePath = "E:/workspace/basic_services_accessibility/";

	public static Collection<Feature> get() {
		Filter fil = null;
		try {
			fil = CQL.toFilter("(NOT NATURE='Sentier' AND NOT NATURE='Chemin' AND NOT NATURE='Piste Cyclable' AND NOT NATURE='Escalier')");
		} catch (CQLException e) { e.printStackTrace(); }
		Collection<Feature> fs = GeoData.getFeatures(basePath + "input_data/test_NMCA_FR_SE_road_tn/roads.gpkg", null, fil);
		return fs;
	}

	public static SpeedCalculator getSpeedCalculator() {
		return new SpeedCalculator() {
			@Override
			public double getSpeedKMPerHour(SimpleFeature sf) {
				return 70.0;
			}
		};
	}

}
