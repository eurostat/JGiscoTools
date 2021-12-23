/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.base.distance;

/**
 * @author julien Gaffuri
 * @param <T> 
 *
 */
public interface Distance<T> {
	double get(T f1, T f2);
}
