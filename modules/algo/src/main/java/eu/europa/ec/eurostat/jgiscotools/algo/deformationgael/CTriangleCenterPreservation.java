package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import org.locationtech.jts.geom.Coordinate;

public class CTriangleCenterPreservation extends GAELSimpleConstraint {
	private SMTriangle t;

	public CTriangleCenterPreservation(SMTriangle t, double imp){
		super(t, imp);
		this.t = t;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint pt, double alpha) {

		if (t.istReverted()) {
			return new Coordinate(0,0);
		}

		double xG = (t.getPt1().getX()+t.getPt2().getX()+t.getPt3().getX())/3;
		double yG = (t.getPt1().getY()+t.getPt2().getY()+t.getPt3().getY())/3.0;
		double length = Math.sqrt((xG-pt.getX())*(xG-pt.getX())+(yG-pt.getY())*(yG-pt.getY()));

		if (length == 0.0) return new Coordinate(alpha*(pt.getX()-xG), alpha*(pt.getY()-yG));

		double xGIni = (t.getPt1().getXIni()+t.getPt2().getXIni()+t.getPt3().getXIni())/3.0;
		double yGIni = (t.getPt1().getYIni()+t.getPt2().getYIni()+t.getPt3().getYIni())/3.0;
		double lengthIni = Math.hypot((xGIni-pt.getXIni()), (yGIni-pt.getYIni()));

		return new Coordinate(alpha*(1-lengthIni/length)*(xG-pt.getX()), alpha*(1-lengthIni/length)*(yG-pt.getY()));
	}
}
