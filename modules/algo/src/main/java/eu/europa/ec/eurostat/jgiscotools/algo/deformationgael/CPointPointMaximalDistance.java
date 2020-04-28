package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CPointPointMaximalDistance extends GAELRelationnalConstraint {
	private static Logger logger = Logger.getLogger(CPointPointMaximalDistance.class.getName());

	private double distance;
	private SMSinglePoint ps1, ps2;

	public CPointPointMaximalDistance(SMSinglePoint ps1, SMSinglePoint ps2, double imp){
		this(ps1, ps2, imp, ps1.getPoint().getIniDistance(ps2.getPoint()));
	}

	public CPointPointMaximalDistance(SMSinglePoint ps1, SMSinglePoint ps2, double imp, double distance){
		super(ps1, ps2, imp);
		this.ps1 = ps1;
		this.ps2 = ps2;
		this.distance = distance;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint p, double alpha) {
		double d,a,dx,dy;
		d = this.ps1.getPoint().getDistance(this.ps2.getPoint());
		if (d<this.distance) return new Coordinate(0,0);

		a = alpha*(this.distance-d)/d*0.5;
		dx = a*(this.ps2.getPoint().getX()-this.ps1.getPoint().getX());
		dy = a*(this.ps2.getPoint().getY()-this.ps1.getPoint().getY());
		if      (p==this.ps1.getPoint()) return new Coordinate(-dx, -dy);
		else if (p==this.ps2.getPoint()) return new Coordinate( dx,  dy);
		else {
			logger.severe("Error");
			return null;
		}
	}
}
