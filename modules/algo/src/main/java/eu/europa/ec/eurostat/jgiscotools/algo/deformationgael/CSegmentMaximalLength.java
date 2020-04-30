package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CSegmentMaximalLength extends GAELSimpleConstraint {
	private static Logger logger = Logger.getLogger(CSegmentMaximalLength.class.getName());

	private SMSegment s;
	private double goalLength;

	public CSegmentMaximalLength(SMSegment s, double imp){
		this(s, imp, s.getIniLength());
	}

	public CSegmentMaximalLength(SMSegment s, double imp, double goalLength){
		super(s,imp);
		this.s = s;
		this.goalLength = goalLength;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint p, double alpha) {
		double d,a,dx,dy;
		d = s.getPt1().getDistance(s.getPt2());
		if (d<this.goalLength) return new Coordinate(0,0);

		a = alpha*(this.goalLength-d)/d*0.5;
		dx = a*(s.getPt2().getX()-s.getPt1().getX());
		dy = a*(s.getPt2().getY()-s.getPt1().getY());
		if      (p==s.getPt1()) return new Coordinate(-dx, -dy);
		else if (p==s.getPt2()) return new Coordinate( dx,  dy);
		else {
			logger.severe("Error");
			return null;
		}
	}
}
