/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.noding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.datamodel.Feature;
import eu.europa.ec.eurostat.jgiscotools.util.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.util.JTSGeomUtil;

/**
 * @author julien Gaffuri
 *
 */
public class NodingUtil {
	public final static Logger LOGGER = Logger.getLogger(NodingUtil.class.getName());


	public enum NodingIssueType { PointPoint, LinePoint }

	public static class NodingIssue{
		public NodingIssueType type;
		public Coordinate c;
		public double distance;
		public NodingIssue(NodingIssueType type, Coordinate c, double distance) { this.type=type; this.c=c; this.distance=distance; }
		public String toString() { return "NodingIssue "+type+" c="+c+" d="+distance; }
	}




	private static boolean checkLPNodingIssue(Coordinate c, Coordinate c1, Coordinate c2, double nodingResolution) {
		return
				c.distance(c1) > nodingResolution
				&& c.distance(c2) > nodingResolution
				&& new LineSegment(c1,c2).distance(c) <= nodingResolution
				;
	}

	private static boolean checkPPNodingIssue(Coordinate c, Coordinate c_, double nodingResolution) {
		double d = c.distance(c_);
		return(d!=0 && d <= nodingResolution);
	}






	//get noding issues for multi-polygonal features
	public static Collection<NodingIssue> getNodingIssues(NodingIssueType type, Collection<Feature> mpfs, double nodingResolution) {
		STRtree index = type==NodingIssueType.LinePoint? FeatureUtil.getSTRtreeCoordinates(mpfs) : getSTRtreeCoordinatesForPP(mpfs, nodingResolution);
		Collection<NodingIssue> nis = new HashSet<NodingIssue>();
		for(Feature mpf : mpfs)
			nis.addAll(getNodingIssues(type, mpf, index, nodingResolution));
		return nis;
	}

	public static Collection<NodingIssue> getNodingIssues(NodingIssueType type, Feature mpf, SpatialIndex index, double nodingResolution) {
		return getNodingIssues(type, (MultiPolygon)mpf.getDefaultGeometry(), index, nodingResolution);
	}

	public static Collection<NodingIssue> getNodingIssues(NodingIssueType type, MultiPolygon mp, SpatialIndex index, double nodingResolution) {
		Collection<NodingIssue> out = new HashSet<NodingIssue>();
		for(int i=0; i<mp.getNumGeometries(); i++)
			out.addAll( getNodingIssues(type,(Polygon) mp.getGeometryN(i),index,nodingResolution) );
		return out;
	}

	public static Collection<NodingIssue> getNodingIssues(NodingIssueType type, Polygon p, SpatialIndex index, double nodingResolution) {
		Collection<NodingIssue> out = new HashSet<NodingIssue>();
		for(LineString lr : JTSGeomUtil.getRings(p))
			out.addAll( getNodingIssues(type,lr,index,nodingResolution) );
		return out;
	}



	private static Collection<NodingIssue> getNodingIssues(NodingIssueType type, LineString ls, SpatialIndex index, double nodingResolution) {

		Collection<NodingIssue> out = new HashSet<NodingIssue>();

		if(type == NodingIssueType.LinePoint ) {
			//go through segments of l1
			Coordinate[] c1s = ls.getCoordinates();
			Coordinate c1 = c1s[0], c2;
			for(int i=1; i<c1s.length; i++) {
				c2 = c1s[i];

				//get points close to segment and check noding of it
				for(Coordinate c : (List<Coordinate>)index.query(new Envelope(c1,c2))) {
					NodingIssue ni = getLinePointNodingIssues(c,c1,c2,nodingResolution);
					if(ni != null) out.add(ni);
				}
				c1 = c2;
			}
		} else if(type == NodingIssueType.PointPoint ) {
			//go through coordinates of l1
			Coordinate[] c1s = ls.getCoordinates();
			for(int i=0; i<c1s.length+(ls.isClosed()?-1:0); i++) {
				Coordinate c_ = c1s[i];
				//get points close to it and check noding
				Envelope env = new Envelope(c_); env.expandBy(nodingResolution*1.01);
				for(Coordinate c : (List<Coordinate>)index.query(env)) {
					NodingIssue ni = getPointPointNodingIssues(c,c_,nodingResolution);
					if(ni != null) out.add(ni);
				}
			}
		}
		return out;
	}





	public static NodingIssue getLinePointNodingIssues(Coordinate c, Coordinate c1, Coordinate c2, double nodingResolution) {
		if( checkLPNodingIssue(c,c1,c2,nodingResolution) )
			return new NodingIssue(NodingIssueType.LinePoint,c,new LineSegment(c1,c2).distance(c));
		else
			return null;
	}

	public static NodingIssue getPointPointNodingIssues(Coordinate c, Coordinate c_, double nodingResolution) {
		if( checkPPNodingIssue(c,c_,nodingResolution) )
			return new NodingIssue(NodingIssueType.PointPoint,c,c.distance(c_));
		else
			return null;
	}








	public static void fixNoding(NodingIssueType type, Collection<Feature> mpfs, double nodingResolution) {
		STRtree index = type==NodingIssueType.LinePoint? FeatureUtil.getSTRtreeCoordinates(mpfs) : getSTRtreeCoordinatesForPP(mpfs, nodingResolution);
		for(Feature mpf : mpfs)
			fixNoding(type, mpf, index, nodingResolution);
	}



	private static void fixNoding(NodingIssueType type, Feature mpf, SpatialIndex index, double nodingResolution) {
		MultiPolygon mp = fixNoding(type, (MultiPolygon) mpf.getDefaultGeometry(), index, nodingResolution);
		mpf.setDefaultGeometry(mp);
	}

	public static MultiPolygon fixNoding(NodingIssueType type, MultiPolygon mp, SpatialIndex index, double nodingResolution) {
		Polygon[] ps = new Polygon[mp.getNumGeometries()];
		for(int i=0; i<mp.getNumGeometries(); i++)
			ps[i] = fixNoding(type, (Polygon) mp.getGeometryN(i), index, nodingResolution);
		return mp.getFactory().createMultiPolygon(ps);
	}

	public static Polygon fixNoding(NodingIssueType type, Polygon p, SpatialIndex index, double nodingResolution) {
		LinearRing shell = (LinearRing) fixNoding(type,p.getExteriorRing(), index, nodingResolution);
		LinearRing[] holes = new LinearRing[p.getNumInteriorRing()];
		for(int i=0; i<p.getNumInteriorRing(); i++)
			holes[i] = (LinearRing) fixNoding(type,p.getInteriorRingN(i), index, nodingResolution);
		return p.getFactory().createPolygon(shell, holes);
	}

	public static LineString fixNoding(NodingIssueType type, LineString ls, SpatialIndex index, double nodingResolution) {
		LineString out = ls;
		//for(NodingIssue ni : nis) out = fixNoding(ni.type, out, ni.c, nodingResolution);
		//fix the noding issues until it is all solved
		Collection<NodingIssue> nis = getNodingIssues(type, ls, index, nodingResolution);
		while(nis.size() != 0) {
			NodingIssue ni = nis.iterator().next();
			out = fixNoding(ni.type, out, ni.c, nodingResolution);
			nis = getNodingIssues(type, out, index, nodingResolution);
		}
		return out;
	}


	public static LineString fixNoding(NodingIssueType type, LineString ls, Coordinate c, double nodingResolution) {
		LineString out = null;
		if(type == NodingIssueType.PointPoint)
			out = fixPPNoding(ls, c, nodingResolution);
		if(type == NodingIssueType.LinePoint)
			out = fixLPNoding(ls, c, nodingResolution);
		return out;
	}

	//fix a noding issue by moving a coordinate (or several for closed lines) to a target position
	public static LineString fixPPNoding(LineString ls, Coordinate c, double nodingResolution) {
		Coordinate[] cs = ls.getCoordinates();
		Coordinate[] csOut = new Coordinate[cs.length];
		boolean found = false;
		for(int i=0; i<cs.length; i++) {
			Coordinate c_ = cs[i];
			boolean issue = checkPPNodingIssue(c, c_, nodingResolution);
			csOut[i] = issue? c : c_;
			if(issue) found = true;
		}

		if(!found) {
			LOGGER.warn("Could not fix line-point noding issue around "+c);
			return ls;
		}

		if(ls.isClosed())
			return ls.getFactory().createLinearRing(csOut);
		else
			return ls.getFactory().createLineString(csOut);
	}

	//fix a noding issue by including a coordinate located on a segment into the line geometry
	public static LineString fixLPNoding(LineString ls, Coordinate c, double nodingResolution) {
		Coordinate[] cs = ls.getCoordinates();
		Coordinate[] csOut = new Coordinate[cs.length+1];
		csOut[0] = cs[0];
		Coordinate c1 = cs[0], c2;
		boolean found = false;
		for(int i=1; i<cs.length; i++) {
			c2 = cs[i];

			if(!found && checkLPNodingIssue(c,c1,c2,nodingResolution)) {
				//insert c
				csOut[i] = c;
				found = true;
			}
			csOut[i+(found?1:0)] = cs[i];

			c1 = c2;
		}

		if(!found) {
			LOGGER.warn("Could not fix line-point noding issue around "+c);
			return ls;
		}

		if(ls.isClosed())
			return ls.getFactory().createLinearRing(csOut);
		else
			return ls.getFactory().createLineString(csOut);
	}









	public static STRtree getSTRtreeCoordinatesForPP(Collection<Feature> fs, double nodingResolution) {
		Collection<Geometry> geoms = new HashSet<Geometry>();
		for(Feature f : fs) geoms.add(f.getDefaultGeometry());
		return getSTRtreeCoordinatesForPPG(geoms, nodingResolution);
	}
	/*private static SpatialIndex getSTRtreeCoordinatesForPP(double nodingResolution, Geometry... geoms) {
		Collection<Geometry> gs = new HashSet<Geometry>();
		for(Geometry g : geoms) gs.add(g);
		return getSTRtreeCoordinatesForPPG(gs, nodingResolution);
	}*/

	private static STRtree getSTRtreeCoordinatesForPPG(Collection<Geometry> gs, double nodingResolution) {
		//build index of all coordinates, ensuring newly added coordinates are not within a radius of nodingResolution of other ones.
		Quadtree index = new Quadtree();
		boolean found;
		for(Geometry g : gs) {
			for(Coordinate c : g.getCoordinates()) {
				//try to find a coordinate of the index within nodingResolution radius of c
				found = false;
				Envelope env = new Envelope(c); env.expandBy(nodingResolution*1.01);
				for(Coordinate c2 : (List<Coordinate>)index.query(env )) {
					if(c.distance(c2) > nodingResolution) continue;
					found = true;
					break;
				}
				if(found) continue;
				index.insert(new Envelope(c), c);
			}
		}
		STRtree index_ = new STRtree();
		for(Coordinate c : (List<Coordinate>)index.queryAll()) index_.insert(new Envelope(c), c);
		return index_;
	}



	//public static void main(String[] args) {
	//LOGGER.info("Start");

	/*
		double nodingResolution = 1e-3;

		Polygon p1 = JTSGeomUtil.createPolygon(0,0, 1,0, 0,1, 0,0);
		Polygon p2 = JTSGeomUtil.createPolygon(1,0, 0.5,0.5, 1,1, 1,0);
		//SpatialIndex index = getCoordinatesSpatialIndex(p1, p2);
		SpatialIndex index = getSTRtreeCoordinatesForPP(nodingResolution, p1, p2);

		System.out.println(p1);
		System.out.println(p2);
		for(NodingIssue ni : getNodingIssues(NodingIssueType.LinePoint, p1, index, nodingResolution)) System.out.println(ni);

		p1 = fixNoding(NodingIssueType.LinePoint, p1, index, nodingResolution);
		p2 = fixNoding(NodingIssueType.LinePoint, p2, index, nodingResolution);
		System.out.println(p1);
		System.out.println(p2);
		for(NodingIssue ni : getNodingIssues(NodingIssueType.LinePoint, p1, index, nodingResolution)) System.out.println(ni);
	 */

	/*
		Polygon p1 = JTSGeomUtil.createPolygon(0,1, 0,0, 1.00001,0, 0,1);
		Polygon p2 = JTSGeomUtil.createPolygon(1,0, 0,1, 1,1, 1,0);
		SpatialIndex index = getSTRtreeCoordinatesForPP(nodingResolution, p1, p2);

		System.out.println(p1);
		System.out.println(p2);
		for(NodingIssue ni : getNodingIssues(NodingIssueType.PointPoint, p1, index, nodingResolution)) System.out.println(ni);

		p1 = fixNoding(NodingIssueType.PointPoint, p1, index, nodingResolution);
		p2 = fixNoding(NodingIssueType.PointPoint, p2, index, nodingResolution);
		System.out.println(p1);
		System.out.println(p2);
		for(NodingIssue ni : getNodingIssues(NodingIssueType.PointPoint, p1, index, nodingResolution)) System.out.println(ni);
	 */
	//LOGGER.info("End");
	//}











	//node features with linear geoemtries intersecting
	public static void fixLineStringsIntersectionNoding(Collection<Feature> fs) {
		//make spatial index
		Quadtree si = FeatureUtil.getQuadtree(fs);
		boolean b;

		//go through pairs of features to check their intersection
		for(Feature f1 : fs) {
			Geometry g1 = f1.getDefaultGeometry();
			for(Object f2_ : si.query(g1.getEnvelopeInternal())) {
				if(f1==f2_) continue;
				Feature f2 = (Feature) f2_;
				if(f1.getID().compareTo(f2.getID()) < 0) continue;
				Geometry g2 = f2.getDefaultGeometry();
				if(! g1.getEnvelopeInternal().intersects(g2.getEnvelopeInternal())) continue;

				Geometry inter = null;
				try {
					inter = g1.intersection(g2);
				} catch (Exception e) {
					try {
						inter = g2.intersection(g1);
					} catch (TopologyException e1) {
						LOGGER.error("Could not compute intersection");
						LOGGER.error(e1.getMessage());
						continue;
					}
				}

				if(inter.isEmpty()) continue;

				//case when intersection has line component
				if(inter.getLength() != 0) {
					g1 = g1.difference(g2);
					inter = g1.intersection(g2);
				}
				if(inter.isEmpty()) continue;

				//intersection can only be with points
				Coordinate[] cs = inter.getCoordinates();
				for(Coordinate c : cs) {
					g1 = insertCoordinate(g1, c);
					g2 = insertCoordinate(g2, c);
				}

				//update both features
				b = si.remove(f1.getDefaultGeometry().getEnvelopeInternal(), f1);
				if(!b) LOGGER.warn("Error when removing feature in spatial index - fixLineStringsIntersectionNoding (1)");
				b = si.remove(f2.getDefaultGeometry().getEnvelopeInternal(), f2);
				if(!b) LOGGER.warn("Error when removing feature in spatial index - fixLineStringsIntersectionNoding (2)");
				f1.setDefaultGeometry(g1);
				f2.setDefaultGeometry(g2);
				si.insert(f1.getDefaultGeometry().getEnvelopeInternal(), f1);
				si.insert(f2.getDefaultGeometry().getEnvelopeInternal(), f2);
			}
		}
	}



	public static Geometry insertCoordinate(Geometry g, Coordinate c) {
		if(g instanceof Point)
			return insertCoordinate((Point)g, c);
		if(g instanceof MultiPoint)
			return insertCoordinate((MultiPoint)g, c);
		if(g instanceof LineString)
			return insertCoordinate((LineString)g, c);
		if(g instanceof MultiLineString)
			return insertCoordinate((MultiLineString)g, c);
		LOGGER.warn("Method insertCoordinate not supported for geometry type "+g.getClass().getSimpleName());
		return g;
	}

	public static Geometry insertCoordinate(Point p, Coordinate c) {
		return p.union(p.getFactory().createPoint(c));
	}

	public static Geometry insertCoordinate(MultiPoint mp, Coordinate c) {
		return mp.union(mp.getFactory().createPoint(c));
	}

	public static LineString insertCoordinate(LineString ls, Coordinate c) {
		double d = ls.distance(ls.getFactory().createPoint(c));
		if(d == 0) return ls;
		//System.out.println(d + " " + c);
		return fixLPNoding(ls, c, d * 1.1);
	}

	public static MultiLineString insertCoordinate(MultiLineString mls, Coordinate c) {
		if(mls == null || mls.isEmpty()) return mls;

		//get component closest to p
		ArrayList<Geometry> lss = new ArrayList<>( JTSGeomUtil.getGeometries(mls) );
		Geometry lsMin = null;
		double dMin = Double.MAX_VALUE;
		Point pt = mls.getFactory().createPoint(c);
		for(Geometry ls : lss) {
			double d = ls.distance(pt);
			if(d<dMin) { lsMin=ls; dMin=d; }
		}

		//build new geometry
		LineString[] lss_ = new LineString[lss.size()];
		int i=0;
		for(Geometry ls : lss) {
			if(ls == lsMin) lss_[i] = insertCoordinate((LineString)ls, c);
			else lss_[i] = (LineString) ls;
			i++;
		}
		return mls.getFactory().createMultiLineString(lss_);
	}

}
