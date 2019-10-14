package eu.europa.ec.eurostat.eurogeostat.algo.polygon;

import java.util.ArrayList;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.valid.IsValidOp;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class ShortEdgesDeletion {

	/**
	 * Reference to a line edge
	 * 
	 * @author julien Gaffuri
	 *
	 */
	private static class LineEdge {
		public int id = -1;
		public double length = -1.0;

		public LineEdge(double l, int id) {
			this.length = l;
			this.id = id;
		}
	}

	/**
	 * Reference to a polygon edge
	 * 
	 * @author julien Gaffuri
	 *
	 */
	private static class PolyEdge {
		public int ringId = -999;
		public LineEdge edge = null;

		public PolyEdge(int ringId, LineEdge seg) {
			this.ringId = ringId;
			this.edge = seg;
		}

	}

	/**
	 * Get the edges of a line shorter than a tolerence value.
	 * 
	 * @param line
	 * @param tol
	 * @return
	 */
	private static ArrayList<LineEdge> getShort(LineString line, double tol){
		ArrayList<LineEdge> out = new ArrayList<LineEdge>();
		Coordinate[] cs = line.getCoordinates();
		for(int i=0; i<cs.length-1; i++){
			double len = cs[i].distance(cs[i+1]);
			if (len < tol) out.add( new LineEdge(len, i) );
		}
		return out;
	}

	/**
	 * Get the edges of a polygon shorter than a tolerence value.
	 * 
	 * @param poly
	 * @param tol
	 * @return
	 */
	private static ArrayList<PolyEdge> getShort(Polygon poly, double tol){
		ArrayList<PolyEdge> out = new ArrayList<PolyEdge>();
		ArrayList<LineEdge> lss = getShort(poly.getExteriorRing(), tol);
		for(LineEdge cls : lss) out.add(new PolyEdge(-1, cls));
		for(int i=0; i<poly.getNumInteriorRing(); i++) {
			lss = getShort(poly.getInteriorRingN(i), tol);
			for(LineEdge cls : lss) out.add(new PolyEdge(i, cls));
		}
		return out;
	}

	/**
	 * The outcome of an edge deletion on a ring.
	 * 
	 * @author julien Gaffuri
	 *
	 */
	private static class RingEdgeDeletionOut {
		public LinearRing ring = null;
		public boolean success = false;

		public RingEdgeDeletionOut(LinearRing ring, boolean success) {
			this.ring = ring;
			this.success = success;
		}
	}

	/**
	 * The outcome of an edge deletion on a polygon.
	 * 
	 * @author julien Gaffuri
	 *
	 */
	private static class PolyEdgeDeletionRes {
		public Polygon poly = null;
		public boolean success = false;

		public PolyEdgeDeletionRes(Polygon poly, boolean success) {
			this.poly = poly;
			this.success = success;
		}
	}


	/**
	 * Delete a polygon short edges.
	 * 
	 * @param poly
	 * @param tolmm the tolerance value in map mm.
	 * @param scale the visualisation scale.
	 * @return
	 */
	public static Geometry get(Polygon poly, double tolmm, double scale) {
		return get(poly, tolmm * scale * 0.001);
	}


	/**
	 * Delete a polygon short edges.
	 * 
	 * @param poly
	 * @param tol the tolerance value in ground meters.
	 * @return
	 */
	public static Geometry get(Polygon poly, double tol) {
		Polygon p_ = (Polygon) poly.clone();
		ArrayList<PolyEdge> shSegs = getShort(p_, tol);

		while(shSegs.size()>0) {
			PolyEdge shst = shSegs.get(0);
			for(PolyEdge seg : shSegs) if(seg.edge.length < shst.edge.length) shst = seg;
			shSegs.remove(shst);

			PolyEdgeDeletionRes out = deleteEdge(p_, shst);

			if( !out.success ) continue;

			p_ = out.poly;
			shSegs = getShort(p_, tol);
		}
		return p_;
	}


	private static PolyEdgeDeletionRes deleteEdge(Polygon p, PolyEdge seg) {
		LineString r;
		if( seg.ringId == -1 ) r = p.getExteriorRing();
		else r = p.getInteriorRingN(seg.ringId);

		RingEdgeDeletionOut out = deleteEdge(r, seg.edge);

		if (!out.success) return new PolyEdgeDeletionRes(null, false);

		r = out.ring;

		LinearRing shell;
		LinearRing[] holes = new LinearRing[p.getNumInteriorRing()];
		if(seg.ringId == -1) {
			shell = (LinearRing) r;
			for(int i=0; i<p.getNumInteriorRing(); i++) holes[i] = (LinearRing) p.getInteriorRingN(i).clone();
		}
		else {
			shell = (LinearRing) p.getExteriorRing();
			for(int i=0; i<p.getNumInteriorRing(); i++) holes[i] = (LinearRing) p.getInteriorRingN(i).clone();
			holes[seg.ringId] = (LinearRing) r;
		}
		Polygon p_ = new GeometryFactory().createPolygon(shell, holes);

		if ( ! IsValidOp.isValid(p_) || p_.isEmpty() ) return new PolyEdgeDeletionRes(p_, false);

		return new PolyEdgeDeletionRes(p_, true);
	}



	private static double TOL_ANGLE = 20*Math.PI/180;

	private static RingEdgeDeletionOut deleteEdge(LineString lr, LineEdge seg){

		Coordinate[] cs = lr.getCoordinates();
		if(cs.length <= 4 ) return new RingEdgeDeletionOut(null, false);

		//get usefull points
		Coordinate p1 = cs[seg.id];
		Coordinate p2 = cs[seg.id+1];
		Coordinate p0 = seg.id==0? cs[cs.length-2] : cs[seg.id-1];
		Coordinate p3 = seg.id+2==cs.length? cs[1] : cs[seg.id+2];

		//get angle of the previous and next edges
		double a = Math.atan2(p3.y-p2.y, p3.x-p2.x) - Math.atan2(p0.y-p1.y, p0.x-p1.x);
		if (a<=-Math.PI) a += 2*Math.PI;
		else if (a>Math.PI) a -= 2*Math.PI;

		//ortho
		if ( Math.abs(a) <= Math.PI/2+TOL_ANGLE && Math.abs(a) >= Math.PI/2-TOL_ANGLE ) {

			//get intersection
			double x1 = p0.x-p1.x, ya=p0.y-p1.y;
			double x2 = p3.x-p2.x, yb=p3.y-p2.y;
			double t = ( x2*(p1.y-p2.y) - yb*(p1.x-p2.x) ) / ( x1*yb-ya*x2 );
			Coordinate c = new Coordinate(p1.x+t*x1, p1.y+t*ya);

			Coordinate[] cs_ = new Coordinate[cs.length-1];

			if (seg.id != 0){
				for(int i=0; i<seg.id; i++) cs_[i]=cs[i];
				cs_[seg.id]=c;
				for(int i=seg.id+1; i<cs.length-1; i++) cs_[i]=cs[i+1];
				if(seg.id==cs.length-2) cs_[0]=c;
			}
			else {
				cs_[0]=c;
				for(int i=1; i<cs.length-2; i++) cs_[i]=cs[i-1];
				cs_[cs.length-2]=c;
			}

			if( cs_[0].x != cs_[cs_.length-1].x || cs_[0].y != cs_[cs_.length-1].y  ) return new RingEdgeDeletionOut(null, false);
			else if (cs_.length <= 3) return new RingEdgeDeletionOut(null, false);
			else return new RingEdgeDeletionOut(new GeometryFactory().createLinearRing(cs_), true);
		}

		//parallel case 1
		else if( Math.abs(a) >= Math.PI-TOL_ANGLE ) {
			double dx = p1.x-p0.x + p3.x-p2.x;
			double dy = p1.y-p0.y + p3.y-p2.y;
			double length = Math.sqrt(dx*dx+dy*dy);
			dx = dx/length;
			dy = dy/length;

			double xMid = (p0.x+p3.x)*0.5, yMid = (p0.y+p3.y)*0.5;

			double t1 = (p0.x-xMid)*dx + (p0.y-yMid)*dy;
			double t2 = (p3.x-xMid)*dx + (p3.y-yMid)*dy;
			Coordinate c1 = new Coordinate(xMid+t1*dx, yMid+t1*dy);
			Coordinate c2 = new Coordinate(xMid+t2*dx, yMid+t2*dy);

			Coordinate[] cs_ = new Coordinate[cs.length-2];
			cs_[0] = c1; cs_[1] = c2;
			if (seg.id != 0) {
				for(int i=seg.id+3; i<cs.length; i++) cs_[i-seg.id-1] = cs[i];
				for(int i=1; i<seg.id-1; i++) cs_[cs.length-seg.id-2+i] = cs[i];
				cs_[cs.length-3] = c1;
			}
			else {
				for(int i=2; i<cs.length-3; i++) cs_[i] = cs[i+1];
				cs_[cs.length-3] = c1;
			}

			if( cs_[0].x != cs_[cs_.length-1].x || cs_[0].y != cs_[cs_.length-1].y  ) return new RingEdgeDeletionOut(null, false);
			else if (cs_.length<=3) return new RingEdgeDeletionOut(null, false);
			else return new RingEdgeDeletionOut(new GeometryFactory().createLinearRing(cs_), true);
		}

		//parallel case 2
		else if( Math.abs(a) <= TOL_ANGLE ) {
			if(p0==p3) return new RingEdgeDeletionOut(null, false);

			double t1 = ( (p2.x-p3.x)*(p0.x-p3.x) + (p2.y-p3.y)*(p0.y-p3.y) ) / ( (p2.x-p3.x)*(p2.x-p3.x) + (p2.y-p3.y)*(p2.y-p3.y) );
			double t2 = ( (p1.x-p0.x)*(p3.x-p0.x) + (p1.y-p0.y)*(p3.y-p0.y) ) / ( (p1.x-p0.x)*(p1.x-p0.x) + (p1.y-p0.y)*(p1.y-p0.y) );
			Coordinate c1_ = new Coordinate( p3.x + t1*(p2.x-p3.x) , p3.y + t1*(p2.y-p3.y) );
			Coordinate c2_ = new Coordinate( p0.x + t2*(p1.x-p0.x) , p0.y + t2*(p1.y-p0.y) );
			boolean v1 = (p3.x-c1_.x)*(p2.x-c1_.x) + (p3.y-c1_.y)*(p2.y-c1_.y) < 0;
			boolean v2 = (p0.x-c2_.x)*(p1.x-c2_.x) + (p0.y-c2_.y)*(p1.y-c2_.y) < 0;

			Coordinate c1,c2;

			if (!v1 && !v2) return new RingEdgeDeletionOut(null, false);
			else if (!v1 && v2) { c1=p0; c2=c2_; }
			else if (v1 && !v2) { c1=c1_; c2=p3; }
			else {
				double d1 = p0.distance(p1);
				double d2 = p3.distance(p2);
				if (d1<d2) { c1=c1_; c2=p3; }
				else { c1=p0; c2=c2_; }
			}

			Coordinate[] cs_ = new Coordinate[cs.length-2];
			cs_[0] = c1;
			cs_[1] = c2;
			if (seg.id!=0) {
				for(int i=seg.id+3; i<cs.length; i++)
					cs_[i-seg.id-1]=cs[i];
				for(int i=1; i<seg.id-1; i++) cs_[cs.length-seg.id-2+i]=cs[i];
				cs_[cs.length-3]=c1;
			}
			else {
				for(int i=2; i<cs.length-3; i++) cs_[i]=cs[i+1];
				cs_[cs.length-3]=c1;
			}

			if( cs_[0].x != cs_[cs_.length-1].x || cs_[0].y != cs_[cs_.length-1].y  ) return new RingEdgeDeletionOut(null, false);
			else if (cs_.length<=3) return new RingEdgeDeletionOut(null, false);
			else return new RingEdgeDeletionOut(new GeometryFactory().createLinearRing(cs_), true);
		}

		else {
			return new RingEdgeDeletionOut(null, false);
		}
	}

}
