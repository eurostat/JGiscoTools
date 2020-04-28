package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import org.locationtech.jts.geom.Coordinate;

public class CSubmicroCenterPosition extends GAELSimpleConstraint {

	private GAELSubmicro sm;
	private Coordinate goalC;

	public CSubmicroCenterPosition(GAELSubmicro sm, double imp, Coordinate goalC) {
		super(sm, imp);
		this.sm = sm;
		this.goalC = goalC;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint pt, double alpha) {
		return new Coordinate(alpha*(this.goalC.x-sm.getX()), alpha*(this.goalC.y-sm.getY()));
	}
}
