package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CAngleValue extends GAELSimpleConstraint {
	private static Logger logger = Logger.getLogger(CAngleValue.class.getName());

	private SMAngle ang;
	private double goal;

	public CAngleValue(SMAngle ang, double imp, double goalValue){
		super(ang, imp);
		this.ang = ang;
		this.goal = goalValue;
	}

	public CAngleValue(SMAngle ang, double imp){
		this(ang, imp, ang.getInitialValue());
	}

	@Override
	public Coordinate getDisplacement(GAELPoint pt, double alpha) {
		double val = -0.5*alpha*ang.getValueDifference(this.goal);
		double cos = Math.cos(val), sin = Math.sin(val);
		if (pt == ang.getPt()) {
			double dx1 = ang.getPt().getX()-ang.getPt1().getX() + cos*(ang.getPt1().getX()-ang.getPt().getX()) -sin*(ang.getPt1().getY()-ang.getPt().getY());
			double dy1 = ang.getPt().getY()-ang.getPt1().getY() + sin*(ang.getPt1().getX()-ang.getPt().getX()) +cos*(ang.getPt1().getY()-ang.getPt().getY());
			double dx2 = ang.getPt().getX()-ang.getPt2().getX() + cos*(ang.getPt2().getX()-ang.getPt().getX()) +sin*(ang.getPt2().getY()-ang.getPt().getY());
			double dy2 = ang.getPt().getY()-ang.getPt2().getY() - sin*(ang.getPt2().getX()-ang.getPt().getX()) +cos*(ang.getPt2().getY()-ang.getPt().getY());
			return new Coordinate(-(dx1+dx2)*0.5, -(dy1+dy2)*0.5);
		}
		else if (pt == ang.getPt1()) {
			double dx1 = ang.getPt().getX()-ang.getPt1().getX() + cos*(ang.getPt1().getX()-ang.getPt().getX()) -sin*(ang.getPt1().getY()-ang.getPt().getY());
			double dy1 = ang.getPt().getY()-ang.getPt1().getY() + sin*(ang.getPt1().getX()-ang.getPt().getX()) +cos*(ang.getPt1().getY()-ang.getPt().getY());
			return new Coordinate(dx1, dy1);
		}
		else if (pt == ang.getPt2()) {
			double dx2 = ang.getPt().getX()-ang.getPt2().getX() + cos*(ang.getPt2().getX()-ang.getPt().getX()) +sin*(ang.getPt2().getY()-ang.getPt().getY());
			double dy2 = ang.getPt().getY()-ang.getPt2().getY() - sin*(ang.getPt2().getX()-ang.getPt().getX()) +cos*(ang.getPt2().getY()-ang.getPt().getY());
			return new Coordinate(dx2, dy2);
		}
		else {
			logger.severe("Error: point is not one of the points of the angle");
			return null;
		}
	}
}
