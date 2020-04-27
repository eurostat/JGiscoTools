package eu.europa.ec.eurostat.jgiscotools.algo.clustering;

import java.util.Collection;

public interface ClusteringIndex<T> {
	Collection<T> getCandidates(T obj, double distance);
}
