package eu.europa.ec.eurostat.eurogeostat.algo.deformation.constraint;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.GPoint;
import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.GSimpleConstraint;
import eu.europa.ec.eurostat.eurogeostat.algo.deformation.submicro.GSegment;

public class SegmentLength extends GSimpleConstraint {
	private static Logger logger = Logger.getLogger(SegmentLength.class.getName());

	private GSegment s;
	private double goalLength;

	public SegmentLength(GSegment s, double imp){
		this(s, imp, s.getIniLength());
	}

	public SegmentLength(GSegment s, double imp, double goalLength){
		super(s,imp);
		this.s = s;
		this.goalLength = goalLength;
	}

	@Override
	public Coordinate getDisplacement(GPoint pt, double alpha) {
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
