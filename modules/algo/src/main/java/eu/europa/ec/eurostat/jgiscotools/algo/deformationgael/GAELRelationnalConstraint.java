package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

public abstract class GAELRelationnalConstraint extends GAELConstraint {

	public GAELRelationnalConstraint(GAELSubmicro sm1, GAELSubmicro sm2, double imp) {
		super(imp);

		for(GAELPoint p : sm1.getPoints()){
			for(GAELPoint p_ : sm2.getPoints()) p.getPointsRel().add(p_);
			p.getConstraints().add(this);
		}

		for(GAELPoint p : sm2.getPoints()){
			for(GAELPoint p_ : sm1.getPoints()) p.getPointsRel().add(p_);
			p.getConstraints().add(this);
		}
	}

}
