package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaggregation;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;

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
		ArrayList<Feature> cells = GeoData.getFeatures(basePath + "grid_1km_surf_FRL0.gpkg",null);
		logger.info(cells.size() + " cells");

		logger.info("Load buildings...");
		//TODO filter - ETAT = En service - USAGE1 - USAGE2 - Résidentiel
		Collection<Feature> fs = GeoData.getFeatures(basePath + "04/buildings.gpkg",null);
		logger.info(fs.size() + " buildings");

		logger.info("Define feature contribution calculator");
		FeatureContributionCalculator fcc = new FeatureContributionCalculator() {
			@Override
			public double getContribution(Feature f, Geometry inter) {
				if(inter == null || inter.isEmpty()) return 0;
				double area = inter.getArea();
				String nbS = f.getAttribute("NB_ETAGES").toString();
				int nb = 1;
				nb = Integer.parseInt(nbS);
				// TODO use also USAGE1 + USAGE2 Résidentiel
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

}
