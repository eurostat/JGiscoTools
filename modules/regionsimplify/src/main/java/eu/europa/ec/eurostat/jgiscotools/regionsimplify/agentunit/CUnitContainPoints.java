/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify;

import eu.europa.ec.eurostat.jgiscotools.agent.Constraint;

/**
 * Ensures that the unit contains some specified points.
 * 
 * @author julien Gaffuri
 *
 */
public class CUnitContainPoints extends Constraint<AUnit> {

	public CUnitContainPoints(AUnit agent) {
		super(agent);
	}

	@Override
	public void computeSatisfaction() {
		satisfaction = getAgent().containPoints()? 10 : 0;
	}

	@Override
	public boolean isHard() { return true; }

}
