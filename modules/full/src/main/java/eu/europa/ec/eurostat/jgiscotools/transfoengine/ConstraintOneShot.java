/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.transfoengine;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * A constraint to force a transformation to be applied.
 * The moment when the transformation is to be applied can be adjusted with the constraint priority.
 * 
 * @author Julien Gaffuri
 *
 */
public class ConstraintOneShot<T extends Agent> extends Constraint<T> {

	//the transformation to apply
	Transformation<T> transformation = null;

	public ConstraintOneShot(T agent, Transformation<T> transformation) {
		super(agent);
		this.transformation = transformation;
	}

	//the flag to show the transformation has been applied
	boolean applied = false;

	@Override
	public void computeCurrentValue() {}

	@Override
	public void computeSatisfaction() { satisfaction = applied? 10 : 0; }

	@Override
	public List<Transformation<T>> getTransformations() {
		ArrayList<Transformation<T>> tr = new ArrayList<Transformation<T>>();
		tr.add(transformation);
		applied = true;
		return tr;
	}

}
