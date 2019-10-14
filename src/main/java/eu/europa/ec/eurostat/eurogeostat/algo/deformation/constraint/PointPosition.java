package eu.europa.ec.eurostat.eurogeostat.algo.deformation.constraint;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.GPoint;
import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.GSimpleConstraint;
import eu.europa.ec.eurostat.eurogeostat.algo.deformation.submicro.GSinglePoint;

public class PointPosition extends GSimpleConstraint {

	private Coordinate goalC;

	public PointPosition(GSinglePoint p, double imp, Coordinate goalC){
		super(p, imp);
		this.goalC = goalC;
	}

	public PointPosition(GSinglePoint p, double importance){
		this(p, importance, p.getPoint().getInitialPosition());
	}

	@Override
	public Coordinate getDisplacement(GPoint p, double alpha) {
		return new Coordinate(alpha*(this.goalC.x-p.getX()), alpha*(this.goalC.y-p.getY()));
	}
}
