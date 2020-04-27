package eu.europa.ec.eurostat.jgiscotools.algo.deformation.base;


import org.locationtech.jts.geom.Coordinate;

public abstract class GConstraint {

	public GConstraint(double imp) {
		setImportance(imp);
	}

	private double imp;
	public double getImportance() { return this.imp; }
	public void setImportance(double imp) { this.imp = imp; }

	protected abstract Coordinate getDisplacement(GPoint p, double alpha);

	public Coordinate getDisplacement(GPoint p){
		return getDisplacement(p, getImportance() / p.getImportanceSum());
	}

}
