package eu.europa.ec.eurostat.eurogeostat.algo.deformation.base;

public abstract class GRelationnalConstraint extends GConstraint {

	public GRelationnalConstraint(Submicro sm1, Submicro sm2, double imp) {
		super(imp);

		for(GPoint p : sm1.getPoints()){
			for(GPoint p_ : sm2.getPoints()) p.getPointsRel().add(p_);
			p.getConstraints().add(this);
		}

		for(GPoint p : sm2.getPoints()){
			for(GPoint p_ : sm1.getPoints()) p.getPointsRel().add(p_);
			p.getConstraints().add(this);
		}
	}

}
