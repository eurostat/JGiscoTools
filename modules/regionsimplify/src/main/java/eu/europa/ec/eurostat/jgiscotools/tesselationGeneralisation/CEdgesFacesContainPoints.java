/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import eu.europa.ec.eurostat.jgiscotools.agent.Constraint;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;

/**
 * Ensures that the faces on both sides of the edge (if any) contain some specified points.
 * 
 * @author julien Gaffuri
 *
 */
public class CEdgesFacesContainPoints extends Constraint<AEdge> {

	public CEdgesFacesContainPoints(AEdge agent) {
		super(agent);
	}

	@Override
	public void computeSatisfaction() {
		Edge e = getAgent().getObject();
		AFace af1 = getAgent().getAtesselation().getAFace(e.f1);
		AFace af2 = getAgent().getAtesselation().getAFace(e.f2);

		boolean ok =
				(e.f1==null || af1.containPoints(false))
				&&
				(e.f2==null || af2.containPoints(false))
				;
		satisfaction = ok? 10 : 0;
	}

	@Override
	public boolean isHard() { return true; }

}
