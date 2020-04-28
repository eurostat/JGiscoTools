package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CSegmentLength extends GAELSimpleConstraint {
	private static Logger logger = Logger.getLogger(CSegmentLength.class.getName());

	private SMSegment s;
	private double goalLength;

	public CSegmentLength(SMSegment s, double imp){
		this(s, imp, s.getIniLength());
	}

	public CSegmentLength(SMSegment s, double imp, double goalLength){
		super(s,imp);
		this.s = s;
		this.goalLength = goalLength;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint pt, double alpha) {
		double dx, dy, length = s.getLength();
		if (length == 0) {
			double a = 0.5 * alpha * this.goalLength / s.getIniLength();
			dx = a * (s.getPt1().getXIni()-s.getPt2().getXIni());
			dy = a * (s.getPt1().getYIni()-s.getPt2().getYIni());
		}
		else {
			double a = 0.5 * alpha * (this.goalLength/length - 1);
			dx = a * (s.getPt1().getX()-s.getPt2().getX());
			dy = a * (s.getPt1().getY()-s.getPt2().getY());
		}
		if (pt == s.getPt1()) return new Coordinate(dx, dy);
		else if (pt == s.getPt2()) return new Coordinate(-dx, -dy);
		else {
			logger.severe("Error");
			return null;
		}
	}

}
