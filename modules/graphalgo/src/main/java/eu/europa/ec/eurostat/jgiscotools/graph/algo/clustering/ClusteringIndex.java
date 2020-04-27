package eu.europa.ec.eurostat.jgiscotools.graph.algo.clustering;

import java.util.Collection;

public interface ClusteringIndex<T> {
	Collection<T> getCandidates(T obj, double distance);
}
