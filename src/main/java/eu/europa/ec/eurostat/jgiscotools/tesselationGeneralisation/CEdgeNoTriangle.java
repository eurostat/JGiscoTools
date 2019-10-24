/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import eu.europa.ec.eurostat.jgiscotools.algo.polygon.Triangle;
import eu.europa.ec.eurostat.jgiscotools.graph.Edge;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;

/**
 * Ensure the faces on both sides of the edge do not become triangles.
 * 
 * @author julien Gaffuri
 *
 */
public class CEdgeNoTriangle extends Constraint<AEdge> {
	public CEdgeNoTriangle(AEdge agent) { super(agent); }

	boolean isTriangleIni1 = false, isTriangleIni2 = false;
	@Override
	public void computeInitialValue() {
		Edge e = getAgent().getObject();
		if(e.f1 != null) isTriangleIni1 = Triangle.is(e.f1.getGeom());
		if(e.f2 != null) isTriangleIni2 = Triangle.is(e.f2.getGeom());
	}

	@Override
	public void computeSatisfaction() {
		Edge e = getAgent().getObject();
		if(e.f1 != null && !isTriangleIni1 && Triangle.is(e.f1.getGeom())) satisfaction = 0;
		else if(e.f2 != null && !isTriangleIni2 && Triangle.is(e.f2.getGeom())) satisfaction = 0;
		else satisfaction = 10;
	}

	@Override
	public boolean isHard() { return true; }
}
