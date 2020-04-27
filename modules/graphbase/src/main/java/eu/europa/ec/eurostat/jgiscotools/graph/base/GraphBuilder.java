/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.graph.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.operation.union.UnaryUnionOp;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Graph;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Node;

/**
 * @author julien Gaffuri
 *
 */
public class GraphBuilder {
	public final static Logger LOGGER = LogManager.getLogger(GraphBuilder.class.getName());

	/**
	 * Build graph (not necessary planar) from lines.
	 * 
	 * @param lines The input lines.
	 * @param buildFaces Set to false if the faces are not needed.
	 * @return The graph
	 */
	private static Graph build(Collection<LineString> lines, boolean buildFaces) {
		Graph g = new Graph();

		if(LOGGER.isDebugEnabled()) LOGGER.debug("   Create nodes and edges");
		SpatialIndex siNodes = new Quadtree();
		for(LineString ls : lines){
			if(ls.isClosed()) {
				Coordinate c = ls.getCoordinateN(0);
				Node n = g.getNodeAt(c);
				if(n==null) {
					n=g.buildNode(c);
					siNodes.insert(new Envelope(n.getC()), n);
				}
				Coordinate[] coords = ls.getCoordinates();
				coords[0]=n.getC(); coords[coords.length-1]=n.getC();
				g.buildEdge(n, n, coords);
			} else {
				Coordinate c;
				c = ls.getCoordinateN(0);
				Node n0 = g.getNodeAt(c);
				if(n0==null) {
					n0 = g.buildNode(c);
					siNodes.insert(new Envelope(n0.getC()), n0);
				}
				c = ls.getCoordinateN(ls.getNumPoints()-1);
				Node n1 = g.getNodeAt(c);
				if(n1==null) {
					n1 = g.buildNode(c);
					siNodes.insert(new Envelope(n1.getC()), n1);
				}
				Coordinate[] coords = ls.getCoordinates();
				coords[0]=n0.getC(); coords[coords.length-1]=n1.getC();
				g.buildEdge(n0, n1, coords);
			}
		}
		siNodes = null;

		if( !buildFaces ) {
			if(LOGGER.isDebugEnabled()) LOGGER.debug("Graph built ("+g.getNodes().size()+" nodes, "+g.getEdges().size()+" edges)");
			return g;
		}

		if(LOGGER.isDebugEnabled()) LOGGER.debug("   Build face geometries with polygonisation");
		Polygonizer pg = new Polygonizer();
		pg.add(lines);
		lines = null;
		@SuppressWarnings("unchecked")
		Collection<Polygon> polys = pg.getPolygons();
		pg = null;

		if(LOGGER.isDebugEnabled()) LOGGER.debug("   Create faces and link them to edges");
		for(Polygon poly : polys){
			//get candidate edges
			Set<Edge> edges = new HashSet<Edge>();
			Collection<Edge> es = g.getEdgesAt(poly.getEnvelopeInternal());
			for(Edge e : es){
				Geometry edgeGeom = e.getGeometry();
				if(!edgeGeom.getEnvelopeInternal().intersects(poly.getEnvelopeInternal())) continue;

				//Geometry inter = poly.getBoundary().intersection(edgeGeom);
				//if(inter.getLength()==0) continue;

				if(!poly.covers(edgeGeom)) continue;

				edges.add(e);
			}
			//create face
			g.buildFace(edges);
		}

		if(LOGGER.isDebugEnabled()) LOGGER.debug("Graph built ("+g.getNodes().size()+" nodes, "+g.getEdges().size()+" edges, "+g.getFaces().size()+" faces)");

		return g;
	}

	/**
	 * Build graph from sections, by connecting them directly at their tips.
	 * This graph is not necessary planar. No faces are built.
	 * 
	 * @param sections
	 * @return
	 */
	public static Graph buildFromLinearFeaturesNonPlanar(Collection<Feature> sections) {
		Graph g = new Graph();
		for(Feature f : sections) {
			MultiLineString mls = (MultiLineString) JTSGeomUtil.toMulti(f.getGeometry());
			for(int i=0; i<mls.getNumGeometries(); i++) {
				//for each section, create edge and link it to nodes (if it exists) or create new
				Coordinate[] cs = ((LineString) mls.getGeometryN(i)).getCoordinates();
				Node n1 = g.getCreateNodeAt(cs[0]), n2 = g.getCreateNodeAt(cs[cs.length-1]);
				Edge e = g.buildEdge(n1, n2, cs);
				e.obj = f;
			}
		}
		return g;
	}

	/**
	 * Build graph from lines, by connecting them directly at their tips.
	 * This graph is not necessary planar. No faces are built.
	 * 
	 * @param lines
	 * @return
	 */
	public static Graph buildFromLinearGeometriesNonPlanar(Collection<LineString> lines) {
		Graph g = new Graph();
		for(LineString ls : lines) {
			//for each, create edge and link it to nodes (if it exists) or create new
			Coordinate[] cs = ls.getCoordinates();
			Node n1 = g.getCreateNodeAt(cs[0]), n2 = g.getCreateNodeAt(cs[cs.length-1]);
			g.buildEdge(n1, n2, cs);
		}
		return g;
	}

	/**
	 * Build planar graph from sections.
	 * 
	 * @param sections
	 * @return
	 */
	public static Graph buildFromLinearFeaturesPlanar(Collection<Feature> sections, boolean buildFaces) {

		//get feature geometries
		Collection<LineString> geoms = JTSGeomUtil.getLineStrings( FeatureUtil.getGeometries(sections) );

		//build planar graph from geometries
		Graph g = buildFromLinearGeometriesPlanar(geoms, buildFaces);
		geoms.clear(); geoms = null;

		//link sections and edges

		//TODO: use hausdorf distance instead ?
		//build spatial index for features
		//STRtree si = FeatureUtil.getSTRtreeSpatialIndex(sections);

		for(Feature f : sections) {
			for(Edge e : g.getEdgesAt(f.getGeometry().getEnvelopeInternal())) {
				//for(Edge e : g.getEdges()) {
				LineString eg = e.getGeometry();
				//	for(Feature f : (Collection<Feature>)si.query(eg.getEnvelopeInternal())) {
				if(!f.getGeometry().getEnvelopeInternal().intersects(eg.getEnvelopeInternal())) continue;
				//retrieve feature the edge is the closest to
				Geometry inter = f.getGeometry().intersection(eg);
				if(inter.getLength() == 0) continue;
				//if(!f.getGeom().contains(eg) && !f.getGeom().overlaps(eg)) continue;
				if(e.obj != null) {
					LOGGER.warn("Problem when building network: Ambiguous assignement of edge "+e.getId()+" around "+e.getC()+" to feature "+f.getID()+" or "+((Feature)e.obj).getID());
					LOGGER.warn("   Lenghts: diff=" + ( e.getGeometry().getLength() - inter.getLength() ) + " Edge="+e.getGeometry().getLength() + " Inter="+inter.getLength());
					LOGGER.warn("   Intersection: " + inter);
				}
				e.obj = f;
			}
		}

		//check all edges have been assigned to a section
		for(Edge e : g.getEdges())
			if(e.obj==null) LOGGER.warn("Problem when building network: Edge "+e.getId()+" has not been assigned to a feature. Around "+e.getC());

		return g;
	}

	public static Graph buildFromLinearGeometriesPlanar(Collection<LineString> geoms, boolean buildFaces) {
		if(LOGGER.isDebugEnabled()) LOGGER.debug("Build graph from "+geoms.size()+" geometries.");

		if(LOGGER.isDebugEnabled()) LOGGER.debug("     compute union of " + geoms.size() + " lines...");
		Geometry union = new GeometryFactory().buildGeometry(geoms).union();

		if(LOGGER.isDebugEnabled()) LOGGER.debug("     run linemerger...");
		LineMerger lm = new LineMerger();
		lm.add(union); union = null;
		@SuppressWarnings("unchecked")
		Collection<LineString> lines = lm.getMergedLineStrings(); lm = null;
		if(LOGGER.isDebugEnabled()) LOGGER.debug("     done. " + lines.size() + " lines obtained");

		return build(lines, buildFaces);
	}






	public static Graph buildForTesselation(Collection<MultiPolygon> geoms) { return buildForTesselation(geoms, null); }
	public static Graph buildForTesselation(Collection<MultiPolygon> geoms, Envelope env) {
		if(LOGGER.isDebugEnabled()) LOGGER.debug("Build graph from "+geoms.size()+" geometries.");

		if(LOGGER.isDebugEnabled()) LOGGER.debug("   Run linemerger on lines");
		Collection<Geometry> lineCol = new ArrayList<Geometry>();
		for(Geometry g : geoms) lineCol.add(g.getBoundary());

		if(LOGGER.isDebugEnabled()) LOGGER.debug("     compute union of " + lineCol.size() + " lines...");
		Geometry union = null;
		GeometryFactory gf = new GeometryFactory();
		while(union == null)
			try {
				//union = new GeometryFactory().buildGeometry(lineCol);
				//union = union.union();
				union = UnaryUnionOp.union(lineCol, gf);
			} catch (TopologyException e) {
				Coordinate c = e.getCoordinate();
				LOGGER.warn("     Geometry.union failed. Topology exception (found non-noded intersection) around: " + c.x +", "+c.y);
				//LOGGER.warn("     "+e.getMessage());

				Collection<Geometry> close = JTSGeomUtil.getGeometriesCloseTo(c, lineCol, 0.001);
				Geometry unionClose = UnaryUnionOp.union(close, gf);
				lineCol.removeAll(close);
				lineCol.add(unionClose);
				union = null;
			}

		lineCol.clear(); lineCol = null;

		if(LOGGER.isDebugEnabled()) LOGGER.debug("     run linemerger...");
		LineMerger lm = new LineMerger();
		lm.add(union); union = null;
		@SuppressWarnings("unchecked")
		Collection<LineString> lines = lm.getMergedLineStrings(); lm = null;
		if(LOGGER.isDebugEnabled()) LOGGER.debug("     done. " + lines.size() + " lines obtained");


		//decompose lines along the envelope (if provided)
		if(env != null) {
			Collection<LineString> lines_ = new HashSet<LineString>();
			LineString envL = JTSGeomUtil.getBoundary(env);
			for(LineString line : lines) {
				if(JTSGeomUtil.containsSFS(env, line.getEnvelopeInternal())) { lines_.add(line); continue; }
				MultiLineString inter = JTSGeomUtil.getLinear(envL.intersection(line));
				if(inter==null || inter.isEmpty()) { lines_.add(line); continue; }
				lines_.addAll(JTSGeomUtil.getLineStrings(inter));
				lines_.addAll(JTSGeomUtil.getLineStrings(line.difference(inter)));
			}
			//replace collection
			lines.clear(); lines = lines_;
		}

		return build(lines, true);
	}

}
