/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import eu.europa.ec.eurostat.jgiscotools.graph.algo.EdgeValidity;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.FaceValidity;
import eu.europa.ec.eurostat.jgiscotools.graph.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;

/**
 * Ensure the edge is valid:
 * 1. The edge do not self intersect (it is simple)
 * 2. Both faces connected to the edge (if any) remain valid, that is:
 * - Their geometry is simple and valid
 * - They do not overlap other faces (this could happen when for example an edge is significantly simplified and a samll island becomes on the other side)
 * 
 * @author julien Gaffuri
 *
 */
public class CEdgeValidity extends Constraint<AEdge> {
	//private final static Logger LOGGER = Logger.getLogger(CEdgeValidity.class.getName());

	public CEdgeValidity(AEdge agent) { super(agent); }

	@Override
	public void computeSatisfaction() {
		Edge e = getAgent().getObject();
		boolean ok = 
				EdgeValidity.get(e, false, false)
				&&
				(e.f1 == null || FaceValidity.get(e.f1, true, true))
				&&
				(e.f2 == null || FaceValidity.get(e.f2, true, true));
		;
		satisfaction = ok ? 10 : 0;
	}

	@Override
	public boolean isHard() { return true; }

}
