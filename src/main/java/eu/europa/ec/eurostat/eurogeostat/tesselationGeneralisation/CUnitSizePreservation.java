/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.tesselationGeneralisation;

import eu.europa.ec.eurostat.eurogeostat.datamodel.Feature;
import eu.europa.ec.eurostat.eurogeostat.transfoengine.Constraint;

/**
 * 
 * Constraint ensuring a unit's size is equal to a goal one, typically the initial value.
 * 
 * @author julien Gaffuri
 *
 */
public class CUnitSizePreservation extends Constraint<AUnit> {

	public CUnitSizePreservation(AUnit agent, double goalValue) {
		super(agent);
		this.goalValue = goalValue;
	}

	double currentValue, goalValue;

	@Override
	public void computeCurrentValue() {
		Feature f = getAgent().getObject();
		currentValue = f.getDefaultGeometry()==null? 0 : f.getDefaultGeometry().getArea();
	}

	@Override
	public void computeSatisfaction() {
		if(getAgent().isDeleted())
			if(goalValue == 0) satisfaction=10; else satisfaction=0;
		else
			if(goalValue == 0) satisfaction=0;
			else satisfaction = 10 - 10*Math.abs(goalValue-currentValue)/goalValue;
		if(satisfaction<0) satisfaction=0;
	}

}
