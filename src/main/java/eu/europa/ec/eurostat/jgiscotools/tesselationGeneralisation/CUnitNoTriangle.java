/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import org.locationtech.jts.geom.MultiPolygon;

import eu.europa.ec.eurostat.eurogeostat.algo.polygon.Triangle;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;

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
		nbTriangleIni = Triangle.nb((MultiPolygon) getAgent().getObject().getDefaultGeometry());
	}

	@Override
	public void computeSatisfaction() {
		int nbTriangle = Triangle.nb((MultiPolygon) getAgent().getObject().getDefaultGeometry());
		satisfaction = nbTriangle<=nbTriangleIni? 10 : 0;
	}

	@Override
	public boolean isHard() { return true; }

}
