package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaggregation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.FeatureContributionCalculator;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class BuildingStatsComputation {
	private static Logger logger = LogManager.getLogger(BuildingStatsComputation.class.getName());

	//use: -Xms2G -Xmx12G
	/** @param args 
	 * @throws Exception **/
	public static void main(String[] args) {
		logger.info("Start");

		String basePath = "E:/workspace/building_stats/test/";

		logger.info("Load cells...");
		Filter fil = null;
		try {
			fil = CQL.toFilter("(NUTS_1_ID='FRL')");
		} catch (CQLException e) { e.printStackTrace(); }
		ArrayList<Feature> cells = GeoData.getFeatures("E:\\dissemination\\shared-data\\grid\\grid_1km_surf.gpkg", null, fil);
		logger.info(cells.size() + " cells");

		logger.info("Load buildings...");
		fil = null;
		try {
			fil = CQL.toFilter("(ETAT='En service' AND (USAGE1='RÃ©sidentiel' OR USAGE2='RÃ©sidentiel'))");
		} catch (CQLException e) { e.printStackTrace(); }
		Collection<Feature> fs = null;
		for(String dep : new String[] { "04", "05", "06", "84", "83", "13" }) {
			logger.info("   "+dep);
			if(fs == null) fs = GeoData.getFeatures(basePath + dep + "/buildings.gpkg", null, fil);
			else fs.addAll( GeoData.getFeatures(basePath + dep + "/buildings.gpkg", null, fil) );
			logger.info(fs.size() + " buildings");
		}

		logger.info("Remove duplicates");
		fs = removeDuplicates(fs, "ID");
		logger.info(fs.size() + " buildings");

		logger.info("Define feature contribution calculator");
		FeatureContributionCalculator fcc = new FeatureContributionCalculator() {
			@Override
			public double getContribution(Feature f, Geometry inter) {
				if(inter == null || inter.isEmpty()) return 0;

				//area
				double area = inter.getArea();
				String u1 = (String) f.getAttribute("USAGE1");
				String u2 = (String) f.getAttribute("USAGE2");
				if(u1==null && u2==null) {}
				else if("RÃ©sidentiel".equals(u1) && u2==null) {}
				else if("RÃ©sidentiel".equals(u1) && u2!=null) {area = area*0.7; }
				else if(!"RÃ©sidentiel".equals(u1) && u2==null) { area = area*0.3; }
				else if(!"RÃ©sidentiel".equals(u1) && "RÃ©sidentiel".equals(u2)) { area = area*0.3; }
				else logger.warn(" "+u1+" "+u2);

				Integer nb = (Integer) f.getAttribute("NB_ETAGES");
				if(nb == null) {
					//try to get it from height
					Double h = (Double) f.getAttribute("HAUTEUR");
					if(h==null) return area;
					return area * h/3.5;
				}
				return nb*area;
			}
		};

		//compute aggregation
		GridAggregator ga = new GridAggregator(cells, "GRD_ID", fs, fcc);
		ga.compute(true);

		logger.info("Round values...");
		for(Stat s : ga.getStats().stats)
			s.value = (int) Math.round(s.value);

		logger.info("Save...");
		CSV.save(ga.getStats(), "bu_res_area", basePath + "/building_residential_area.csv");

		logger.info("End");
	}


	/**
	 * Remove the duplicates, that is the features that have same attributes.
	 * TODO move to featureutil
	 * 
	 * @param fs
	 * @param idAtt
	 */
	public static ArrayList<Feature> removeDuplicates(Collection<Feature> fs, String idAtt) {

		ArrayList<Feature> out = new ArrayList<Feature>();
		HashSet<String> ids = new HashSet<String>();

		for(Feature f : fs) {
			String id = idAtt==null? f.getID() : f.getAttribute(idAtt).toString();
			if(ids.contains(id)) continue;
			ids.add(id);
			out.add(f);
		}

		return out;
	}

}
