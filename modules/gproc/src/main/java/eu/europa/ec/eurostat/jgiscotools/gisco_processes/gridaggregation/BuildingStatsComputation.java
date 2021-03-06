package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaggregation;

import java.util.Collection;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.FeatureContributionCalculator;

public class BuildingStatsComputation {

	public static void main(String[] args) {
		System.out.println("Start");

		String outPutFolder = "";  //TODO

		System.out.println("Load cells...");
		Collection<Feature> cells = null; //TODO

		System.out.println("Load buildings...");
		Collection<Feature> fs = null; //TODO

		System.out.println("Define feature contribution calculator");
		FeatureContributionCalculator fcc = new FeatureContributionCalculator() {
			@Override
			public double getContribution(Feature f, Geometry inter) {
				if(inter == null || inter.isEmpty()) return 0;
				double area = inter.getArea();
				// TODO get height/nb of floors
				int nb = 1;
				return nb*area;
			}};

			//compute aggregation
			GridAggregator ga = new GridAggregator(cells, "GRD_ID", fs, fcc);
			ga.compute(true);

			//TODO round stat values?

			System.out.println("Save...");
			CSV.save(ga.getStats(), "bu_res_area", outPutFolder + "/building_residential_area.csv");

			System.out.println("End");
	}

}
