/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.algo.clustering;

import org.locationtech.jts.index.SpatialIndex;

/**
 * @author julien Gaffuri
 *
 */
public abstract class AggregationWithSpatialIndex<T> implements Aggregation<T> {
	public SpatialIndex index = null;
}
