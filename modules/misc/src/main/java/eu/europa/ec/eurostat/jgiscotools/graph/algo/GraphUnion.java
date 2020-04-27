package eu.europa.ec.eurostat.jgiscotools.graph.algo;

import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Graph;


/**
 * Build the union of various graphs
 * 
 * @author julien Gaffuri
 *
 */
public class GraphUnion {

	/**
	 * Simple union of graphs.
	 * 
	 * @param gs
	 * @return
	 */
	public static Graph get(Graph... gs) {
		Graph g = new Graph();
		for(Graph g_ : gs) {
			g.getNodes().addAll(g_.getNodes());
			g.getEdges().addAll(g_.getEdges());
			g.getFaces().addAll(g_.getFaces());
		}
		return g;
	}

}
