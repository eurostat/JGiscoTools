/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.tesselationGeneralisation;

import eu.europa.ec.eurostat.eurogeostat.datamodel.graph.Edge;
import eu.europa.ec.eurostat.eurogeostat.transfoengine.Agent;

/**
 * @author julien Gaffuri
 *
 */
public class AEdge extends Agent {
	private ATesselation aTess;
	public ATesselation getAtesselation(){ return aTess; }

	public AEdge(Edge object, ATesselation aTess) { super(object); this.aTess=aTess; }
	public Edge getObject() { return (Edge) super.getObject(); }

	public void clear() {
		aTess = null;
	}

}
