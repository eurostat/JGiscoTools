/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.tesselationGeneralisation;

import eu.europa.ec.eurostat.eurogeostat.transfoengine.Constraint;

/**
 * Ensures an edge does not intersect itself (it should remain "simple").
 * 
 * @author julien Gaffuri
 *
 */
public class CEdgeSelfIntersection extends Constraint<AEdge> {

	public CEdgeSelfIntersection(AEdge agent) {
		super(agent);
	}

	boolean selfIntersects = false;

	@Override
	public void computeCurrentValue() {
		selfIntersects = !getAgent().getObject().getGeometry().isSimple();
	}

	@Override
	public void computeSatisfaction() {
		satisfaction = selfIntersects? 0 : 10;
	}

	@Override
	public boolean isHard() { return true; }

}
