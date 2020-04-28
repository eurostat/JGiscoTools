package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CPointSegmentMinimalDistance extends GAELRelationnalConstraint {
	private static Logger logger = Logger.getLogger(CPointSegmentMinimalDistance.class.getName());

	public double distance;
	private SMSinglePoint ps;
	private SMSegment s;

	public CPointSegmentMinimalDistance(SMSinglePoint ps, SMSegment s, double imp, double distance){
		super(ps, s, imp);
		this.ps = ps;
		this.s = s;
		this.distance = distance;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint p, double alpha) {
		//calculs des produits scalaires pour connaitre la configuration
		double ps1 = (this.s.getPt2().getX()-this.s.getPt1().getX())*(this.ps.getPoint().getX()-this.s.getPt1().getX())+(this.s.getPt2().getY()-this.s.getPt1().getY())*(this.ps.getPoint().getY()-this.s.getPt1().getY());
		double ps2 = (this.s.getPt1().getX()-this.s.getPt2().getX())*(this.ps.getPoint().getX()-this.s.getPt2().getX())+(this.s.getPt1().getY()-this.s.getPt2().getY())*(this.ps.getPoint().getY()-this.s.getPt2().getY());

		if (p==this.ps.getPoint()){
			if (ps1>0.0&&ps2>0.0){
				//le point p est entre s.p1 et s.p2
				Coordinate proj = this.s.getProjected(p);
				double d = p.getDistance(proj.x, proj.y);
				if (d>this.distance) return new Coordinate(0,0);
				if (d==0){
					double a = this.distance*alpha*0.5/this.s.getLength();
					return new Coordinate(a*(this.s.getPt2().getY()-this.s.getPt1().getY()), a*(this.s.getPt1().getX()-this.s.getPt2().getX()));
				}
				double a = alpha*0.5*(this.distance/d-1);
				return new Coordinate(a*(p.getX()-proj.x), a*(p.getY()-proj.y));
			}
			else if (ps1<=0.0&&ps2>0.0){
				//le point p est du cote de s.p1
				double d = p.getDistance(this.s.getPt1());
				if (d>this.distance) return new Coordinate(0,0);
				if (d==0){
					double a = this.distance*alpha*0.5/this.s.getLength();
					return new Coordinate(a*(this.s.getPt1().getX()-this.s.getPt2().getX()), a*(this.s.getPt1().getY()-this.s.getPt2().getY()));
				}
				double a = alpha*0.5*(this.distance/d-1);
				return new Coordinate(a*(p.getX()-this.s.getPt1().getX()), a*(p.getY()-this.s.getPt1().getY()));
			}
			else if (ps2<=0.0&&ps1>0.0){
				//le point p est du cote de s.p2
				double d = p.getDistance(this.s.getPt2());
				if (d>this.distance) return new Coordinate(0,0);
				if (d==0){
					double a = this.distance*alpha*0.5/this.s.getLength();
					return new Coordinate(a*(this.s.getPt2().getX()-this.s.getPt1().getX()), a*(this.s.getPt2().getY()-this.s.getPt1().getY()));
				}
				double a=  alpha*0.5*(this.distance/d-1);
				return new Coordinate(a*(p.getX()-this.s.getPt2().getX()), a*(p.getY()-this.s.getPt2().getY()));
			}
		}
		else if (p==this.s.getPt1()||p==this.s.getPt2()){
			if (ps1>0.0&&ps2>0.0){
				//le point p est entre s.p1 et s.p2
				Coordinate proj = this.s.getProjected(this.ps.getPoint());
				double d = this.ps.getPoint().getDistance(proj.x, proj.y);
				if (d>this.distance) return new Coordinate(0,0);
				if (d==0){
					double d_ = p.getDistance(this.ps.getPoint());
					double dd = this.s.getLength();
					double a =( 1-d_/dd)*alpha*0.5*this.distance;
					return new Coordinate(a*(this.s.getPt1().getY()-this.s.getPt2().getY()), a*(this.s.getPt2().getX()-this.s.getPt1().getX()));
				}
				double d_ = p.getDistance(proj.x, proj.y);
				double dd = this.s.getLength();
				double a = (1-d_/dd)*alpha*0.5*(this.distance/d-1);
				return new Coordinate(a*(proj.x-this.ps.getPoint().getX()), a*(proj.y-this.ps.getPoint().getY()));
			}
			//le point p n'est pas entre s.p1 et s.p2
			double d = this.ps.getPoint().getDistance(p.getX(),p.getY());
			if (d>this.distance) return new Coordinate(0,0);
			if (d==0){
				GAELPoint p_ = null;
				if (p==this.s.getPt1()) p_=this.s.getPt2(); else p_=this.s.getPt1();
				double a = alpha*0.5*this.distance;
				return new Coordinate(a*(p_.getX()-p.getX()), a*(p_.getY()-p.getY()));
			}
			double a = alpha*0.5*(this.distance/d-1);
			return new Coordinate(a*(p.getX()-this.ps.getPoint().getX()), a*(p.getY()-this.ps.getPoint().getY()));
		}
		logger.severe("Error");
		return null;
	}

}
