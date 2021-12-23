/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.graph.algo.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import eu.europa.ec.eurostat.jgiscotools.algo.base.distance.Distance;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.ConnexComponents;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.MinimumSpanningTree;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Graph;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Node;

/**
 * 
 * Detect spatial clusters
 * 
 * @author julien Gaffuri
 * @param <T> 
 *
 */
public class Clustering<T> {

	/**
	 * Build clusters. Clusters are composed of objects that are at dMax distance of all others objects in the cluster.
	 * 
	 * @param objs The objects list - the clusters have to be added there
	 * @param d The distance measure
	 * @param dMax The distance maximum value
	 * @param agg The aggregation method to use to define the cluster representations
	 * @param closest 
	 * @param index 
	 */
	public void perform(ArrayList<T> objs, Distance<T> d, double dMax, Aggregation<T> agg, boolean closest, ClusteringIndex<T> index) {
		//while it is possible to find a couple of object to close, aggregate them
		T[] toAgg = findToAggregate(objs, d, dMax, closest, index);
		while(toAgg != null){
			agg.aggregate(objs, toAgg[0], toAgg[1]);
			toAgg = findToAggregate(objs, d, dMax, closest, index);
		}
	}

	private T[] findToAggregate(ArrayList<T> objs, Distance<T> d, double dMax, boolean closest, ClusteringIndex<T> index) {
		if(closest) return getClosest(objs, d, dMax, index);
		else return getCloser(objs, d, dMax, index);
	}

	//get couple of closest
	@SuppressWarnings("unchecked")
	private T[] getClosest(ArrayList<T> objs, Distance<T> d, double dMax, ClusteringIndex<T> index) {
		if(objs.size()<2) return null;

		T ciMin=null, cjMin=null;
		double dist, distMin = Double.MAX_VALUE;
		if(index != null){
			Collection<T> objsj;
			for(T ci:objs){
				objsj = index.getCandidates(ci, dMax);
				for(T cj:objsj){
					if(ci==cj) continue;
					dist = d.get(ci, cj);
					if(dist > distMin) continue;
					distMin = dist;
					ciMin=ci;
					cjMin=cj;
				}
			}
		} else
			for(int i=0; i<objs.size(); i++){
				T ci = objs.get(i);
				for(int j=i+1; j<objs.size(); j++){
					T cj = objs.get(j);
					dist = d.get(ci, cj);
					if(dist > distMin) continue;
					distMin = dist;
					ciMin=ci;
					cjMin=cj;
				}
			}
		if(distMin > dMax) return null;
		return (T[]) new Object[]{ciMin, cjMin};
	}

	//get one couple closer than threshold
	@SuppressWarnings("unchecked")
	private T[] getCloser(ArrayList<T> objs, Distance<T> d, double dMax, ClusteringIndex<T> index) {
		if(objs.size()<2) return null;
		Collections.shuffle(objs);

		double dist;
		if(index != null){
			Collection<T> objsj;
			for(T ci:objs){
				objsj = index.getCandidates(ci, dMax);
				if(objsj == null) continue;
				for(T cj:objsj){
					if(ci==cj) continue;
					dist = d.get(ci, cj);
					if(dist < dMax)
						return (T[]) new Object[]{ci, cj};
				}
			}
		} else
			for(int i=0; i<objs.size(); i++){
				T ci = objs.get(i);
				for(int j=i+1; j<objs.size(); j++){
					T cj = objs.get(j);
					dist = d.get(ci, cj);
					if(dist < dMax)
						return (T[]) new Object[]{ci, cj};
				}
			}

		return null;
	}








	//cluster based on MST: clusters are composed of objects that are at dMax distance of at least one other
	public Collection<Collection<T>> performMST(Collection<Object> objs, Distance<Object> d, double dMax) {
		//build MST
		Graph mst = new MinimumSpanningTree().perform(objs, d);

		//cut MST
		mst.removeAll( getHighValueEdges(mst, dMax) );

		//get connex components
		Collection<Graph> clusterGraphs = ConnexComponents.get(mst);

		//build clusters from connex components
		Collection<Collection<T>> clusters = new HashSet<Collection<T>>();
		for(Graph clusterGraph : clusterGraphs){
			Collection<T> cluster = new HashSet<T>();
			for(Node n:clusterGraph.getNodes()) cluster.add((T)n.obj);
			clusters.add(cluster);
		}
		return clusters;
	}

	private Collection<Edge> getHighValueEdges(Graph mst, double minValue) {
		Collection<Edge> tse = new HashSet<Edge>();
		for(Edge e:mst.getEdges()) if(e.value>minValue) tse.add(e);
		return tse;
	}

}
