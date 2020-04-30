package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CSegmentOrientation extends GAELSimpleConstraint {
	private static Logger logger = Logger.getLogger(CSegmentOrientation.class.getName());

	private SMSegment s;
	private double goal;

	public CSegmentOrientation(SMSegment s, double imp){
		this(s, imp, s.getPt1().getIniOrientation(s.getPt2()));
	}

	public CSegmentOrientation(SMSegment s, double imp, double goalOrientation){
		super(s, imp);
		this.s = s;
		this.goal = goalOrientation;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint pt, double alpha) {
		double angle = alpha*s.getOrientationGap(this.goal);
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double dx = 0.5 * (s.getPt2().getX()-s.getPt1().getX() + cos*(s.getPt1().getX()-s.getPt2().getX()) + sin*(s.getPt1().getY()-s.getPt2().getY()));
		double dy = 0.5 * (s.getPt2().getY()-s.getPt1().getY() - sin*(s.getPt1().getX()-s.getPt2().getX()) + cos*(s.getPt1().getY()-s.getPt2().getY()));
		if (pt == s.getPt1()) return new Coordinate(dx, dy);
		else if (pt == s.getPt2()) return new Coordinate(-dx, -dy);
		else {
			logger.severe("Error: point is not one of the segment's ones");
			return null;
		}
	}
}
