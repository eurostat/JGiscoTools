package eu.europa.ec.eurostat.jgiscotools.algo.triangulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.triangulate.ConformingDelaunayTriangulator;
import org.locationtech.jts.triangulate.ConstraintVertex;
import org.locationtech.jts.triangulate.Segment;
import org.locationtech.jts.triangulate.quadedge.QuadEdge;
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision;
import org.locationtech.jts.triangulate.quadedge.Vertex;

public class Triangulation {
	private static Logger logger = Logger.getLogger(Triangulation.class.getName());

	public Collection<TPoint> getConstraintPoints() { return this.conspts; }
	private Collection<TPoint> conspts;

	public Collection<TSegment> getConstraintSegments() { return this.conssegs; }
	private Collection<TSegment> conssegs;


	public Collection<TPoint> getPoints() { return this.pts; }
	private Collection<TPoint> pts;

	public Collection<TSegment> getSegments() { return this.segs; }
	private Collection<TSegment> segs;

	public Collection<TTriangle> getTriangles() { return this.tris; }
	private Collection<TTriangle> tris;

	private TPointFactory ptF;
	private TSegmentFactory segF;
	private TTriangleFactory triF;

	public Triangulation(List<TPoint> conspts, TPointFactory ptF, TSegmentFactory segF, TTriangleFactory triF){
		this(conspts, new ArrayList<TSegment>(), ptF, segF, triF);
	}

	public Triangulation(List<TPoint> conspts, Collection<TSegment> conssegs, TPointFactory ptF, TSegmentFactory segF, TTriangleFactory triF){
		this.conspts = conspts;
		this.conssegs = conssegs;

		this.ptF = ptF;
		this.segF = segF;
		this.triF = triF;

		this.pts = new HashSet<TPoint>();
		this.segs = new HashSet<TSegment>();
		this.tris = new HashSet<TTriangle>();
	}

	public void compute() {
		compute(false);
	}

	public void compute(boolean buildTriangles) {
		compute(buildTriangles, null);
	}

	public void compute(boolean buildTriangles, Geometry geom) {
		if(logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Triangulation");

		//clear lists
		this.getPoints().clear();
		this.getTriangles().clear();
		this.getSegments().clear();

		//trivial cases
		if(this.getConstraintPoints().size() < 2) return;
		if(this.getConstraintPoints().size() == 2) {
			//build only one segment
			Iterator<TPoint> it = this.getConstraintPoints().iterator();
			this.getSegments().add( this.segF.create(it.next(), it.next()) );
			return;
		}

		//input constraint point - vertex
		ArrayList<ConstraintVertex> vertexes = new ArrayList<ConstraintVertex>();
		Hashtable<ConstraintVertex, TPoint> dictVertPt = new Hashtable<ConstraintVertex, TPoint>();
		for(TPoint pt : getConstraintPoints()) {
			ConstraintVertex cv = new ConstraintVertex( pt.getPosition() );
			dictVertPt.put(cv, pt);
			vertexes.add(cv);
		}

		//input constraint segment
		ArrayList<Segment> segments = new ArrayList<Segment>();
		for(TSegment seg : getConstraintSegments())
			segments.add( new Segment( seg.getPt1().getPosition(), seg.getPt2().getPosition() ) );

		//compute JTS triangulation
		final ConformingDelaunayTriangulator tri = new ConformingDelaunayTriangulator(vertexes, 0.000000001);
		tri.setConstraints(segments, vertexes);
		tri.formInitialDelaunay();
		tri.enforceConstraints();
		final QuadEdgeSubdivision subdiv = tri.getSubdivision();

		//the vertices
		for(Object o : subdiv.getVertices(false) ) {
			ConstraintVertex cv = (ConstraintVertex)o;
			TPoint p = dictVertPt.get(cv);
			if(p==null) p = this.ptF.create(cv.getCoordinate());
			getPoints().add(p);
		}

		GeometryFactory gf = new GeometryFactory();

		//the segments
		for(Object o : subdiv.getPrimaryEdges(false) ) {
			QuadEdge qe = (QuadEdge)o;
			TPoint p1 = dictVertPt.get(qe.orig());
			TPoint p2 = dictVertPt.get(qe.dest());
			if(geom != null && ! geom.contains(gf.createPoint(new Coordinate((p1.getPosition().x + p2.getPosition().x)*0.5, (p1.getPosition().y + p2.getPosition().y)*0.5)) ))
				continue;
			getSegments().add( this.segF.create(p1, p2) );
		}

		//the triangles
		if(buildTriangles) return;
		for(Object o : subdiv.getTriangleVertices(false) ) {
			Vertex[] t = (Vertex[])o;
			TPoint p1 = dictVertPt.get(t[0]);
			TPoint p2 = dictVertPt.get(t[1]);
			TPoint p3 = dictVertPt.get(t[2]);
			if(geom != null && ! geom.contains(gf.createPoint(new Coordinate((p1.getPosition().x + p2.getPosition().x + p3.getPosition().x)/3, (p1.getPosition().y + p2.getPosition().y + p3.getPosition().y)/3)) ))
				continue;
			getTriangles().add( this.triF.create(p1, p2, p3) );
		}
	}
}
