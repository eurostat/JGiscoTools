package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import org.locationtech.jts.geom.Coordinate;

public class CPointPosition extends GAELSimpleConstraint {

	private Coordinate goalC;

	public CPointPosition(SMSinglePoint p, double imp, Coordinate goalC){
		super(p, imp);
		this.goalC = goalC;
	}

	public CPointPosition(SMSinglePoint p, double importance){
		this(p, importance, p.getPoint().getInitialPosition());
	}

	@Override
	public Coordinate getDisplacement(GAELPoint p, double alpha) {
		return new Coordinate(alpha*(this.goalC.x-p.getX()), alpha*(this.goalC.y-p.getY()));
	}
}
