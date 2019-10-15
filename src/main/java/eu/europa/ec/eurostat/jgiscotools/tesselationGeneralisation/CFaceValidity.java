/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import eu.europa.ec.eurostat.eurogeostat.algo.graph.FaceValidity;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;

/**
 * Ensures that the face remain valid, that is its geometry is simple & valid, and it does not overlap any other face of the tesselation.
 * 
 * @author julien Gaffuri
 *
 */
public class CFaceValidity extends Constraint<AFace> {

	public CFaceValidity(AFace agent) {
		super(agent);
	}

	@Override
	public void computeSatisfaction() {
		boolean ok = FaceValidity.get(getAgent().getObject(), true, true);
		satisfaction = ok? 10 : 0;
	}

	@Override
	public boolean isHard() { return true; }

}
