/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.graph.algo.clustering;

import java.util.Collection;


/**
 * @author julien Gaffuri
 * @param <T> 
 *
 */
public interface Aggregation<T> {
	public void aggregate(Collection<T> objs, T obj1, T obj2);
}
