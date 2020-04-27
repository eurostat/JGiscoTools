package eu.europa.ec.eurostat.jgiscotools.algo.deformation.constraint;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.algo.deformation.base.GPoint;
import eu.europa.ec.eurostat.jgiscotools.algo.deformation.base.GRelationnalConstraint;
import eu.europa.ec.eurostat.jgiscotools.algo.deformation.submicro.GSegment;

public class SegmentSegmentMinimalDistance extends GRelationnalConstraint{
	private static Logger logger=Logger.getLogger(SegmentSegmentMinimalDistance.class.getName());

	public double distance;
	private GSegment s1, s2;

	public SegmentSegmentMinimalDistance(GSegment s1, GSegment s2, double importance, double distance){
		super(s1,s2,importance);
		this.s1 = s1;
		this.s2 = s2;
		this.distance = distance;
	}

	@Override
	public Coordinate getDisplacement(GPoint p, double alpha) {
		GSegment s=null;
		if      (p==this.s1.getPt1() || p==this.s1.getPt2())
			s = this.s2;
		else if (p==this.s2.getPt1() || p==this.s2.getPt2())
			s = this.s1;
		else {
			logger.severe("Erreur dans la contrainte " + this + ". le point " + p + " n'est pas un point des segments " + this.s1 + " et " +this.s2);
			return new Coordinate(0,0);
		}

		double ps1 = (p.getX()-s.getPt1().getX())*(s.getPt2().getX()-s.getPt1().getX())+(p.getY()-s.getPt1().getY())*(s.getPt2().getY()-s.getPt1().getY());
		double ps2 = (p.getX()-s.getPt2().getX())*(s.getPt1().getX()-s.getPt2().getX())+(p.getY()-s.getPt2().getY())*(s.getPt1().getY()-s.getPt2().getY());

		//traiter le cas ou les deux se coupent

		double dx=0.0, dy=0.0;
		if (ps1>0.0 && ps2>0.0){
			//le minimum est atteint au niveau du projete de p sur s
			Coordinate proj = s.getProjected(p);
			double d = p.getDistance(proj.x,proj.y);
			if (d>this.distance) return new Coordinate(0,0);
			if (d==0.0){
				double a=this.distance*alpha*0.5/s.getLength();
				return new Coordinate(a*(s.getPt2().getY()-s.getPt1().getY()), a*(s.getPt1().getX()-s.getPt2().getX()));
			}
			double a = alpha*0.5*(this.distance/d-1);
			dx = a*(p.getX()-proj.x);
			dy = a*(p.getY()-proj.y);
		}
		else if (ps1<=0.0 && ps2>0.0){
			//le minimum est atteint au niveau de p1.
			double d = p.getDistance(s.getPt1());
			if (d>this.distance) return new Coordinate(0,0);
			if (d==0.0){
				double a = this.distance*alpha*0.5/s.getLength();
				return new Coordinate(a*(s.getPt1().getX()-s.getPt2().getX()), a*(s.getPt1().getY()-s.getPt2().getY()));
			}
			double a = alpha*0.5*(this.distance/d-1);
			dx = a*(p.getX()-s.getPt1().getX());
			dy = a*(p.getY()-s.getPt1().getY());
		}
		else if (ps1>0.0&&ps2<=0.0){
			//le minimum est atteint au niveau de p2.
			double d = p.getDistance(s.getPt2());
			if (d>this.distance) return new Coordinate(0,0);
			if (d==0.0){
				double a = this.distance*alpha*0.5/s.getLength();
				return new Coordinate(a*(s.getPt2().getX()-s.getPt1().getX()), a*(s.getPt2().getY()-s.getPt1().getY()));
			}
			double a = alpha*0.5*(this.distance/d-1);
			dx = a*(p.getX()-s.getPt2().getX());
			dy = a*(p.getY()-s.getPt2().getY());
		}
		return new Coordinate(dx, dy);
	}
}
