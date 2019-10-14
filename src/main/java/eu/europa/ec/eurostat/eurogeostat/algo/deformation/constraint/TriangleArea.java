package eu.europa.ec.eurostat.eurogeostat.algo.deformation.constraint;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.GPoint;
import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.GSimpleConstraint;
import eu.europa.ec.eurostat.eurogeostat.algo.deformation.submicro.GTriangle;

public class TriangleArea extends GSimpleConstraint {
	private static Logger logger = Logger.getLogger(TriangleArea.class.getName());

	private GTriangle t;
	private double goal;

	public TriangleArea(GTriangle t, double imp){
		this(t, imp, t.getInitialArea());
	}

	public TriangleArea(GTriangle t, double imp, double goalArea){
		super(t, imp);
		this.t = t;
		this.goal = goalArea;
	}

	@Override
	public Coordinate getDisplacement(GPoint pt, double alpha) {
		GPoint pt1 = null, pt2 = null;
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
