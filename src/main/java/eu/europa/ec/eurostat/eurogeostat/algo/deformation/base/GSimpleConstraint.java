package eu.europa.ec.eurostat.eurogeostat.algo.deformation.base;

public abstract class GSimpleConstraint extends GConstraint {

	public GSimpleConstraint(Submicro sm, double imp) {
		super(imp);
		for(GPoint p : sm.getPoints())
			p.getConstraints().add(this);
	}

}
