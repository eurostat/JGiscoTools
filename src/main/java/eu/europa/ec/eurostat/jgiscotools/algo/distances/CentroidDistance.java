/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.distances;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.datamodel.Feature;

/**
 * @author julien Gaffuri
 *
 */
public class CentroidDistance implements Distance<Feature> {

	public double get(Feature f1, Feature f2) {
		Geometry g1 = f1.getDefaultGeometry();
		Geometry g2 = f2.getDefaultGeometry();
		return g1.getCentroid().distance(g2.getCentroid());
	}

}
