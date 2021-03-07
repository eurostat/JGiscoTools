/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.Collection;

import org.opengis.filter.Filter;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.SpeedCalculator;

/**
 * @author julien Gaffuri
 *
 */
public class RoadBDTopo {
	static String basePath = "E:/workspace/basic_services_accessibility/";

	/**
	 * @param costAttribute
	 * @return
	 */
	public static Collection<Feature> get(String costAttribute) {

		Filter fil = null;
		//try {
		//	fil = CQL.toFilter("(NOT NATURE='Sentier' AND NOT NATURE='Chemin' AND NOT NATURE='Piste cyclable' AND NOT NATURE='Escalier')");
		//} catch (CQLException e) { e.printStackTrace(); }
		Collection<Feature> fs = GeoData.getFeatures(basePath + "input_data/test_NMCA_FR_SE_road_tn/roads.gpkg", null, fil);

		if(costAttribute != null)
			for(Feature f : fs) {
				double speedkmh = getSpeedKmH(f.getAttribute("NATURE").toString(), f.getAttribute("IMPORTANCE").toString());
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

	/**
	 * @return
	 */
	public static SpeedCalculator getSpeedCalculator() {
		return new SpeedCalculator() {
			@Override
			public double getSpeedKMPerHour(Feature sf) {
				Object nat_ = sf.getAttribute("NATURE");
				String nat = nat_==null?"":nat_.toString();
				Object imp_ = sf.getAttribute("IMPORTANCE");
				String imp = imp_==null?"":imp_.toString();
				return getSpeedKmH(nat, imp);
			}
		};
	}

	private static double getSpeedKmH(String nat, String imp) {
		if("Autoroute".equals(nat)) return 110.0;
		if("Quasi-autoroute".equals(nat)) return 100.0;
		if("1".equals(imp)) return 85.0;
		if("2".equals(imp)) return 75.0;
		if("3".equals(imp)) return 75.0;
		if("Bretelle".equals(nat)) return 65.0;
		if("4".equals(imp)) return 60.0;
		if("Route � 2 chauss�es".equals(nat)) return 60.0;
		if("Route empierr�e".equals(nat)) return 20.0;
		if("5".equals(imp)) return 35.0;
		if("6".equals(imp)) return 5.0;

		if("Route � 1 chauss�es".equals(nat)) return 50.0;
		if("Route � 1 chauss�e".equals(nat)) return 50.0;
		if("Piste cyclable".equals(nat)) return 5.0;

		if("Sentier".equals(nat)) return 5.0;
		if("Chemin".equals(nat)) return 15.0;
		if("Piste cyclable".equals(nat)) return 15.0;
		if("Escalier".equals(nat)) return 5.0;

		if("NC".equals(imp)) return 40.0;

		System.err.println("Could not find speed for BD TOPO road section " + nat + " " + imp);
		return 60.0;
	}

}
