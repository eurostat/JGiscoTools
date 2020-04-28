package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;


import org.locationtech.jts.geom.Coordinate;

public abstract class GAELConstraint {

	public GAELConstraint(double imp) {
		setImportance(imp);
	}

	private double imp;
	public double getImportance() { return this.imp; }
	public void setImportance(double imp) { this.imp = imp; }

	protected abstract Coordinate getDisplacement(GAELPoint p, double alpha);

	public Coordinate getDisplacement(GAELPoint p){
		return getDisplacement(p, getImportance() / p.getImportanceSum());
	}

}
