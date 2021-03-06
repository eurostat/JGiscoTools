package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaggregation;

import java.util.Collection;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.FeatureContributionCalculator;

public class BuildingStatsComputation {

	public static void main(String[] args) {
		System.out.println("Start");

		//TODO load input data
		Collection<Feature> cells = null;
		Collection<Feature> fs = null;

		//define feature contribution
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

			//save
			//StatsHypercube sh = ga.getStats();

			System.out.println("End");
	}

}
