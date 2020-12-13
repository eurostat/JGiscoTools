/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class RoadERM {
	private static Logger logger = LogManager.getLogger(RoadERM.class.getName());
	static String basePath = "E:/workspace/basic_services_accessibility/";

	/**
	 * @param cnt
	 * @return
	 */
	public static Collection<Feature> get(String cnt) {
		Filter fil = null;
		try {
			//EXS Existence Category - RST Road Surface Type
			fil = CQL.toFilter("((EXS=28 OR EXS=0) AND (RST=1 OR RST=0))" + (cnt==null?"":" AND (ICC = '"+cnt+"')") );
		} catch (CQLException e) { e.printStackTrace(); }
		Collection<Feature> fs = GeoData.getFeatures(basePath+ "ERM_road/RoadL.gpkg", null, fil);
		return fs;
	}

	/**
	 * @return
	 */
	public static SpeedCalculator getSpeedCalculator() {
		return new SpeedCalculator() {
			@Override
			public double getSpeedKMPerHour(SimpleFeature sf) {
				//estimate speed of a transport section of ERM/EGM based on attributes
				//COR - Category of Road - 0 Unknown - 1 Motorway - 2 Road inside built-up area - 999 Other road (outside built-up area)
				//RTT - Route Intended Use - 0 Unknown - 16 National motorway - 14 Primary route - 15 Secondary route - 984 Local route
				String cor = sf.getAttribute("COR").toString();
				if(cor==null) { logger.warn("No COR attribute for feature "+sf.getID()); return 0; };
				String rtt = sf.getAttribute("RTT").toString();
				if(rtt==null) { logger.warn("No RTT attribute for feature "+sf.getID()); return 0; };

				//motorways
				if("1".equals(cor) || "16".equals(rtt)) return 110.0;
				//city roads
				if("2".equals(cor)) return 50.0;
				//fast roads
				if("14".equals(rtt) || "15".equals(rtt)) return 80.0;
				//local road
				if("984".equals(rtt)) return 80.0;
				return 50.0;
			}
		};
	}

}
