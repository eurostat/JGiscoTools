package eu.europa.ec.eurostat.jgiscotools.algo.deformation.submicro;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.algo.deformation.base.GPoint;
import eu.europa.ec.eurostat.jgiscotools.algo.deformation.base.Submicro;

public class GSegment extends Submicro {
	static Logger logger = Logger.getLogger(GSegment.class.getName());

	private GPoint pt1;
	public GPoint getPt1() { return this.pt1; }

	private GPoint pt2;
	public GPoint getPt2() { return this.pt2; }

	public GSegment(GPoint pt1, GPoint pt2){
		this.pt1 = pt1;
		this.pt2 = pt2;

		getPoints().add(pt1);
		getPoints().add(pt2);

		pt1.getPointsRel().add(pt2);
		pt2.getPointsRel().add(pt1);
	}




	public double getLength() {
		return getPt1().getDistance(getPt2());
	}

	protected double iniLength = -999.0;
	public double getIniLength() {
		if (this.iniLength == -999.0) this.iniLength = getPt1().getIniDistance(getPt2());
		return this.iniLength;
	}

	//from pt1 to pt2 within [-Pi,Pi])
	public double getOrientation() { return getPt1().getOrientation(getPt2()); }

	//from pt1 to pt2 within [-Pi,Pi])
	private double iniOr = -999.9;
	public double getIniOrientation() {
		if (this.iniOr == -999.9) this.iniOr = getPt1().getIniOrientation(getPt2());
		return this.iniOr;
	}

	//within [-Pi,Pi])
	public double getOrientationGap(double orientation) {
		double diff = getPt1().getOrientation(getPt2()) - orientation;
		if (diff <- Math.PI) return diff + 2.0*Math.PI;
		else if (diff > Math.PI) return diff-2.0*Math.PI;
		else return diff;
	}

	//within [-Pi,Pi])
	public double getOrientationGap() {
		return getOrientationGap(getPt1().getIniOrientation(getPt2()));
	}

	public double getDistance(GPoint p) {
		if      ((getPt2().getX()-getPt1().getX())*(p.getX()-getPt1().getX())+(getPt2().getY()-getPt1().getY())*(p.getY()-getPt1().getY()) <=0.0) return getDistance(getPt1());
		else if ((getPt1().getX()-getPt2().getX())*(p.getX()-getPt2().getX())+(getPt1().getY()-getPt2().getY())*(p.getY()-getPt2().getY()) <=0.0) return getDistance(getPt2());
		else return Math.abs(((getPt1().getX()-p.getX())*(getPt1().getY()-getPt2().getY())+(getPt1().getY()-p.getY())*(getPt2().getX()-getPt1().getX()))/Math.sqrt(Math.pow(getPt2().getX()-getPt1().getX(),2.0)+Math.pow(getPt2().getY()-getPt1().getY(),2.0)));
	}

	public Coordinate getProjected(GPoint p) {
		double ps = (getPt2().getX()-getPt1().getX())*(p.getX()-getPt1().getX())+(getPt2().getY()-getPt1().getY())*(p.getY()-getPt1().getY());
		double dc = Math.pow(getPt2().getX()-getPt1().getX(), 2.0) + Math.pow(getPt2().getY()-getPt1().getY(), 2.0);
		return new Coordinate(getPt1().getX()+ps*(getPt2().getX()-getPt1().getX())/dc, getPt1().getY()+ps*(getPt2().getY()-getPt1().getY())/dc);
	}


	@Override
	public double getX(){
		return (getPt1().getX()+getPt2().getX())*0.5;
	}

	@Override
	public double getY(){
		return (getPt1().getY()+getPt2().getY())*0.5;
	}

	@Override
	public void clean(){
		super.clean();
		this.pt1 = null;
		this.pt2 = null;
	}


}
