package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CTriangleArea extends GAELSimpleConstraint {
	private static Logger logger = Logger.getLogger(CTriangleArea.class.getName());

	private SMTriangle t;
	private double goal;

	public CTriangleArea(SMTriangle t, double imp){
		this(t, imp, t.getInitialArea());
	}

	public CTriangleArea(SMTriangle t, double imp, double goalArea){
		super(t, imp);
		this.t = t;
		this.goal = goalArea;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint pt, double alpha) {
		GAELPoint pt1 = null, pt2 = null;
		if (pt == t.getPt1()) { pt1=t.getPt2(); pt2=t.getPt3(); }
		else if (pt == t.getPt2()) { pt1=t.getPt3(); pt2=t.getPt1(); }
		else if (pt == t.getPt3()) { pt1=t.getPt1(); pt2=t.getPt2(); }
		else {
			logger.severe("Error in triangle area: the point do not belong to the triangle");
			return null;
		}
		double dx = pt2.getX()-pt1.getX();
		double dy = pt2.getY()-pt1.getY();
		double a = 2 * alpha * (this.goal - t.getArea())/(3*(dx*dx+dy*dy));
		return new Coordinate(-a*dy, a*dx);
	}
}
