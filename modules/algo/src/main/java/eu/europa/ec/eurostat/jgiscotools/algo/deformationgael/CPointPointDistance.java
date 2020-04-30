package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CPointPointDistance extends GAELRelationnalConstraint {
	private static Logger logger = Logger.getLogger(CPointPointDistance.class.getName());

	private double distance;
	private SMSinglePoint ps1, ps2;

	public CPointPointDistance(SMSinglePoint ps1, SMSinglePoint ps2, double imp){
		this(ps1, ps2, imp, ps1.getPoint().getIniDistance(ps2.getPoint()));
	}

	public CPointPointDistance(SMSinglePoint ps1, SMSinglePoint ps2, double imp, double distance){
		super(ps1,ps2,imp);
		this.ps1 = ps1;
		this.ps2 = ps2;
		this.distance = distance;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint p, double alpha) {
		double dist = this.ps1.getPoint().getDistance(this.ps2.getPoint());
		double dx, dy;
		if (dist==0) {
			//low probability
			dx = 0.5*alpha*(this.ps1.getPoint().getXIni()-this.ps2.getPoint().getXIni());
			dy = 0.5*alpha*(this.ps1.getPoint().getYIni()-this.ps2.getPoint().getYIni());
		}
		else {
			double a = 0.5*alpha*(this.distance/dist-1);
			dx = a*(this.ps1.getPoint().getX()-this.ps2.getPoint().getX()); dy=a*(this.ps1.getPoint().getY()-this.ps2.getPoint().getY());
		}
		if (p == this.ps1.getPoint()) return new Coordinate(dx, dy);
		else if (p == this.ps2.getPoint()) return new Coordinate(-dx, -dy);
		else {
			logger.severe("Error");
			return null;
		}
	}

}
