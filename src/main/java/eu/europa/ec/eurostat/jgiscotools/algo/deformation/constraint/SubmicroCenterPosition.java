package eu.europa.ec.eurostat.jgiscotools.algo.deformation.constraint;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.algo.deformation.base.GPoint;
import eu.europa.ec.eurostat.jgiscotools.algo.deformation.base.GSimpleConstraint;
import eu.europa.ec.eurostat.jgiscotools.algo.deformation.base.Submicro;

public class SubmicroCenterPosition extends GSimpleConstraint {

	private Submicro sm;
	private Coordinate goalC;

	public SubmicroCenterPosition(Submicro sm, double imp, Coordinate goalC) {
		super(sm, imp);
		this.sm = sm;
		this.goalC = goalC;
	}

	@Override
	public Coordinate getDisplacement(GPoint pt, double alpha) {
		return new Coordinate(alpha*(this.goalC.x-sm.getX()), alpha*(this.goalC.y-sm.getY()));
	}
}
