package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

public abstract class GAELSimpleConstraint extends GAELConstraint {

	public GAELSimpleConstraint(GAELSubmicro sm, double imp) {
		super(imp);
		for(GAELPoint p : sm.getPoints())
			p.getConstraints().add(this);
	}

}
