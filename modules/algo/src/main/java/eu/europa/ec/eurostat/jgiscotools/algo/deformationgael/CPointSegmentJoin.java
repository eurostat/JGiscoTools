package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CPointSegmentJoin extends GAELRelationnalConstraint {
	private static Logger logger = Logger.getLogger(CPointSegmentJoin.class.getName());

	private SMSinglePoint ps;
	private SMSegment s;

	public CPointSegmentJoin(SMSinglePoint ps, SMSegment s, double importance){
		super(ps,s,importance);
		this.ps = ps;
		this.s = s;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint p, double alpha) {
		double ps1, ps2;
		//calculs des produits scalaires pour connaitre la configuration
		ps1 = (this.s.getPt2().getX()-this.s.getPt1().getX())*(this.ps.getPoint().getX()-this.s.getPt1().getX())+(this.s.getPt2().getY()-this.s.getPt1().getY())*(this.ps.getPoint().getY()-this.s.getPt1().getY());
		ps2 = (this.s.getPt1().getX()-this.s.getPt2().getX())*(this.ps.getPoint().getX()-this.s.getPt2().getX())+(this.s.getPt1().getY()-this.s.getPt2().getY())*(this.ps.getPoint().getY()-this.s.getPt2().getY());

		if (p==this.ps.getPoint()){
			if (ps1>0.0&&ps2>0.0){
				//le point p est entre s.p1 et s.p2
				Coordinate proj = this.s.getProjected(this.ps.getPoint());
				return new Coordinate(alpha*0.5*(proj.x-p.getX()), alpha*0.5*(proj.y-p.getY()));
			}
			//le point p est du cote de s.p1
			else if (ps1<=0.0&&ps2>0.0)
				return new Coordinate(alpha*0.5*(this.s.getPt1().getX()-p.getX()), alpha*0.5*(this.s.getPt1().getY()-p.getY()));
			else if (ps1>0.0&&ps2<=0.0)
				return new Coordinate(alpha*0.5*(this.s.getPt2().getX()-p.getX()), alpha*0.5*(this.s.getPt2().getY()-p.getY()));
		}
		else if (p==this.s.getPt1()||p==this.s.getPt2()){
			if (ps1>0.0&&ps2>0.0){
				//le minimum est atteint au niveau du projete du point sur le segment.
				Coordinate proj = this.s.getProjected(this.ps.getPoint());
				double d = p.getDistance(proj.x, proj.y);
				double dd = this.s.getLength();
				return new Coordinate((dd-d)/dd*alpha*0.5*(this.ps.getPoint().getX()-proj.x), (dd-d)/dd*alpha*0.5*(this.ps.getPoint().getY()-proj.y));
			}	
			return new Coordinate(alpha*0.5*(this.ps.getPoint().getX()-p.getX()), alpha*0.5*(this.ps.getPoint().getY()-p.getY()));
		}
		logger.severe("Error");
		return null;
	}
}
