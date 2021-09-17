/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge;

import eu.europa.ec.eurostat.jgiscotools.agent.Agent;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenttesselation.ATesselation;

/**
 * An edge, as an agent.
 * 
 * @author julien Gaffuri
 *
 */
public class AEdge extends Agent {
	private ATesselation aTess;
	public ATesselation getAtesselation(){ return aTess; }

	public AEdge(Edge object, ATesselation aTess) { super(object); this.aTess=aTess; }
	public Edge getObject() { return (Edge) super.getObject(); }

	public void clear() {
		super.clear();
		aTess = null;
	}

}
