/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.distances;

/**
 * @author julien Gaffuri
 *
 */
public interface Distance<T> {
	double get(T f1, T f2);
}
