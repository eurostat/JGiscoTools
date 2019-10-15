/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.clustering;

import org.locationtech.jts.index.SpatialIndex;

/**
 * @author julien Gaffuri
 *
 */
public abstract class AggregationWithSpatialIndex<T> implements Aggregation<T> {
	public SpatialIndex index = null;
}
