/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.base;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.algo.base.Partition.GeomType;
import eu.europa.ec.eurostat.jgiscotools.algo.base.Partition.PartitionedOperation;
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
	public static Logger logger = LogManager.getLogger(Decomposer.class.getName());

	/**
	 * @param fs the input features
	 * @param maxCoordinatesNumber
	 * @param objMaxCoordinateNumber
	 * @param gt
	 * @param midRandom
	 * @return
	 */
	public static Collection<Geometry> decomposeGeometry(Collection<Feature> fs, boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, GeomType gt, double midRandom) {
		final Collection<Geometry> out = new ArrayList<>();
		PartitionedOperation op = new PartitionedOperation() {
			@Override
			public void run(Partition p) {
				logger.debug(p.getCode());
				out.addAll(FeatureUtil.getGeometriesSimple(p.getFeatures()));
			}
		};
		Partition.runRecursivelyApply(fs, op, parallel, maxCoordinatesNumber, objMaxCoordinateNumber, true, gt, midRandom);
		return out;
	}

	/**
	 * @param fs
	 * @param maxCoordinatesNumber
	 * @param objMaxCoordinateNumber
	 * @param gt
	 * @param midRandom
	 * @return
	 */
	public static Collection<Feature> decomposeFeature(Collection<Feature> fs, boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, GeomType gt, double midRandom) {
		final Collection<Feature> out = new ArrayList<>();
		Partition.runRecursivelyApply(fs, p -> {
			logger.debug(p.getCode());
			out.addAll(p.getFeatures());
		}, parallel, maxCoordinatesNumber, objMaxCoordinateNumber, true, gt, midRandom);
		int i=1;
		for(Feature f : out) f.setID( f.getID()+"_"+(i++) );
		return out;
	}

}
