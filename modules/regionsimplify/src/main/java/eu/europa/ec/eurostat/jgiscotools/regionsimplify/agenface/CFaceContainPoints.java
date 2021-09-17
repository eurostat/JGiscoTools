/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify;

import eu.europa.ec.eurostat.jgiscotools.agent.Constraint;

/**
 * Ensures that the face contains some specified points.
 * 
 * @author julien Gaffuri
 *
 */
public class CFaceContainPoints extends Constraint<AFace> {

	public CFaceContainPoints(AFace agent) {
		super(agent);
	}

	@Override
	public void computeSatisfaction() {
		satisfaction = getAgent().containPoints(true)? 10 : 0;
	}

	@Override
	public boolean isHard() { return true; }

}
