/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify;

import org.locationtech.jts.geom.MultiPolygon;

import eu.europa.ec.eurostat.jgiscotools.agent.Constraint;
import eu.europa.ec.eurostat.jgiscotools.algo.polygon.Triangle;

/**
 * Ensure an edge does not become a triangle.
 * 
 * @author julien Gaffuri
 *
 */
public class CUnitNoTriangle extends Constraint<AUnit> {

	public CUnitNoTriangle(AUnit agent) { super(agent); }

	int nbTriangleIni = 0;
	@Override
	public void computeInitialValue() {
		nbTriangleIni = Triangle.nb((MultiPolygon) getAgent().getObject().getGeometry());
	}

	@Override
	public void computeSatisfaction() {
		int nbTriangle = Triangle.nb((MultiPolygon) getAgent().getObject().getGeometry());
		satisfaction = nbTriangle<=nbTriangleIni? 10 : 0;
	}

	@Override
	public boolean isHard() { return true; }

}
