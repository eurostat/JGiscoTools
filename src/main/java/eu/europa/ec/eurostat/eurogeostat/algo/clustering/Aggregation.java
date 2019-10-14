/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.algo.clustering;

import java.util.Collection;


/**
 * @author julien Gaffuri
 *
 */
public interface Aggregation<T> {
	public void aggregate(Collection<T> objs, T obj1, T obj2);
}
