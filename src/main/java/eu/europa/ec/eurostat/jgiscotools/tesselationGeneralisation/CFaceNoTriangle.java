/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import eu.europa.ec.eurostat.eurogeostat.algo.polygon.Triangle;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;

/**
 * Ensure a face does not become a triangle.
 * 
 * @author julien Gaffuri
 *
 */
public class CFaceNoTriangle extends Constraint<AFace> {

	public CFaceNoTriangle(AFace agent) { super(agent); }

	boolean isTriangleIni = false;
	@Override
	public void computeInitialValue() {
		isTriangleIni = Triangle.is(getAgent().getObject().getGeom());
	}

	@Override
	public void computeSatisfaction() {
		satisfaction = isTriangleIni? 10 : Triangle.is(getAgent().getObject().getGeom())? 0 : 10;
	}

	@Override
	public boolean isHard() { return true; }

}
