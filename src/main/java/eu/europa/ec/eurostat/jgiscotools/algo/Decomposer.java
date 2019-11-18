/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.algo.Partition.GeomType;
import eu.europa.ec.eurostat.jgiscotools.algo.Partition.PartitionedOperation;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;

/**
 * 
 * Decompose a set of geometries into a lot of smaller geometries, based on a quadtree decomposition.
 * 
 * @author julien Gaffuri
 *
 */
public class Decomposer {
	public static Logger logger = Logger.getLogger(Decomposer.class.getName());

	/**
	 * @param fs the input features
	 * @param maxCoordinatesNumber
	 * @param objMaxCoordinateNumber
	 * @param gt
	 * @param midRandom
	 * @return
	 */
	public static Collection<Geometry> decompose(Collection<Feature> fs, int maxCoordinatesNumber, int objMaxCoordinateNumber, GeomType gt, double midRandom) {
		final Collection<Geometry> out = new ArrayList<>();
		PartitionedOperation op = new PartitionedOperation() {
			@Override
			public void run(Partition p) {
				logger.debug(p.getCode());
				out.addAll(FeatureUtil.getGeometriesSimple(p.getFeatures()));
			}
		};
		Partition.runRecursively(fs, op, maxCoordinatesNumber, objMaxCoordinateNumber, true, gt, midRandom);
		return out;
	}

}
