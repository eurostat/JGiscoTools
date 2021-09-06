/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.graph.base;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Face;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Graph;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Node;

/**
 * @author julien Gaffuri
 *
 */
public class GraphValidity {
	private final static Logger LOGGER = LogManager.getLogger(GraphValidity.class.getName());

	/**
	 * Check a graph is valid.
	 * 
	 * @param g
	 * @return
	 * @throws Exception 
	 */
	public static boolean isValid(Graph g) {

		//nodes -> edges
		for(Node n : g.getNodes()) {
			for(Edge e : n.getInEdges()) {
				if(e.getN2() == n) continue;
				LOGGER.error("Unvalid graph: unexpected node-edge connection.");
				return false;
			}
			for(Edge e : n.getOutEdges()) {
				if(e.getN1() == n) continue;
				LOGGER.error("Unvalid graph: unexpected node-edge connection.");
				return false;
			}
		}

		//edge -> node
		for(Edge e : g.getEdges()) {
			Node n1 = e.getN1();
			if(! n1.getOutEdges().contains(e)) {
				LOGGER.error("Unvalid graph: unexpected edge-node connection.");
				return false;
			}

			Node n2 = e.getN2();
			if(! n2.getInEdges().contains(e)) {
				LOGGER.error("Unvalid graph: unexpected edge-node connection.");
				return false;
			}
		}

		//edge -> face
		for(Edge e : g.getEdges()) {
			if(e.f1 != null && !e.f1.getEdges().contains(e)) {
				LOGGER.error("Unvalid graph: unexpected edge-face connection.");
				return false;
			}
			if(e.f2 != null && !e.f2.getEdges().contains(e)) {
				LOGGER.error("Unvalid graph: unexpected edge-face connection.");
				return false;
			}
		}

		//face -> edge
		for(Face f : g.getFaces()) {
			for(Edge e : f.getEdges()) {
				if(e.f1 == f || e.f2 == f) continue;
				LOGGER.error("Unvalid graph: unexpected face-edge connection.");
			}
		}

		//TODO check also consistency between geometries ?

		return true;
	}

}
