package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class CSegmentSegmentJoin extends GAELRelationnalConstraint {
	private static Logger logger = Logger.getLogger(CSegmentSegmentJoin.class.getName());

	private SMSegment s1, s2;

	public CSegmentSegmentJoin(SMSegment s1, SMSegment s2, double importance) {
		super(s1,s2,importance);
		this.s1 = s1;
		this.s2 = s2;
	}

	@Override
	public Coordinate getDisplacement(GAELPoint p, double alpha) {

		//recupere le segment auquel le point n'appartient pas
		SMSegment s=null;
		if      (p==this.s1.getPt1()||p==this.s1.getPt2())
			s=this.s2;
		else if (p==this.s2.getPt1()||p==this.s2.getPt2())
			s=this.s1;
		else {
			logger.severe("Erreur dans la contrainte " + this + ". le point " + p + " n'est pas un point des segments " + this.s1 + " et " + this.s2);
			return new Coordinate(0,0);
		}

		double ps1 = (p.getX()-s.getPt1().getX())*(s.getPt2().getX()-s.getPt1().getX())+(p.getY()-s.getPt1().getY())*(s.getPt2().getY()-s.getPt1().getY());
		double ps2 = (p.getX()-s.getPt2().getX())*(s.getPt1().getX()-s.getPt2().getX())+(p.getY()-s.getPt2().getY())*(s.getPt1().getY()-s.getPt2().getY());

		double dx=0.0, dy=0.0;
		if (ps1>0.0&&ps2>0.0){
			//le minimum est atteint au niveau du projete de p sur s
			Coordinate proj = s.getProjected(p);
			dx = alpha*0.5*(proj.x-p.getX());
			dy = alpha*0.5*(proj.y-p.getY());
		}
		//le minimum est atteint au niveau de p1.
		else if (ps1<=0.0&&ps2>0.0) {
			dx = alpha*0.5*(s.getPt1().getX()-p.getX());
			dy = alpha*0.5*(s.getPt1().getY()-p.getY());
		}
		//le minimum est atteint au niveau de p2.
		else if (ps1>0.0&&ps2<=0.0){
			dx = alpha*0.5*(s.getPt2().getX()-p.getX());
			dy = alpha*0.5*(s.getPt2().getY()-p.getY());
		}
		return new Coordinate(dx, dy);
	}

}
