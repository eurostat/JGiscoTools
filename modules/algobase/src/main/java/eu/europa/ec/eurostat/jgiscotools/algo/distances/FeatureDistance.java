package eu.europa.ec.eurostat.jgiscotools.algo.distances;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * Distance for simple features.
 * 
 * @author julien Gaffuri
 *
 */
public class FeatureDistance implements Distance<Feature> {

	public FeatureDistance(){}

	public double get(Feature f1, Feature f2) {
		Geometry g1 = f1.getGeometry();
		Geometry g2 = f2.getGeometry();
		return g1.distance(g2);
	}

}
