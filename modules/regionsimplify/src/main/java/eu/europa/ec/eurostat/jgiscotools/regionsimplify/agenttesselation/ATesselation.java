/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenttesselation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.agent.Agent;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
import eu.europa.ec.eurostat.jgiscotools.graph.base.GraphBuilder;
import eu.europa.ec.eurostat.jgiscotools.graph.base.GraphToFeature;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Face;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Graph;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.AFace;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.AEdge;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.AUnit;

/**
 * A tesselation to be generalised.
 * It is a macro agent.
 * 
 * @author julien Gaffuri
 *
 */
public class ATesselation extends Agent {
	public final static Logger LOGGER = LogManager.getLogger(ATesselation.class.getName());

	//the agents
	public Collection<AUnit> aUnits;
	public Collection<AFace> aFaces;
	public Collection<AEdge> aEdges;

	//the underlying graph structure
	public Graph graph;

	//an envelope. If specified, edges along this envelope limits will be frozen.
	//this is usefull when using partitionning.
	private Envelope env = null;

	public ATesselation(Collection<Feature> units) { this(units, null, null); }
	public ATesselation(Collection<Feature> units, Envelope env, HashMap<String, Collection<Point>> points){
		super(null);

		//create unit agents
		aUnits = new HashSet<AUnit>();
		for(Feature unit : units) {
			//create unit
			AUnit au = new AUnit(unit, this);
			aUnits.add(au);
			//link points to unit
			if(points != null) {
				Collection<Point> pts = points.get(au.getId());
				if(pts != null ) {
					au.points = new ArrayList<Point>();
					au.points.addAll(pts);
				}
			}
		}

		this.env = env;
	}


	/**
	 * Build the graph structure, the agents and their relations.
	 * 
	 * @return this
	 */
	public ATesselation buildTopologicalMap() {

		//get unit's boundaries
		Collection<MultiPolygon> mps = new ArrayList<MultiPolygon>();
		for(AUnit au : aUnits)
			mps.add((MultiPolygon) au.getObject().getGeometry());

		//build graph
		graph = GraphBuilder.buildForTesselation(mps, env);
		mps.clear(); mps = null;

		//create edge and face agents
		aEdges = new HashSet<AEdge>();
		for(Edge e : graph.getEdges()) {
			AEdge ae = (AEdge) new AEdge(e,this).setId(e.getId());
			if(isToBeFrozen(ae)) ae.freeze();
			aEdges.add(ae);
		}
		aFaces = new HashSet<AFace>();
		for(Face f : graph.getFaces())
			aFaces.add((AFace) new AFace(f,this).setId(f.getId()));

		LOGGER.debug("   Build spatial index for units");
		STRtree spUnit = new STRtree();
		for(AUnit u : aUnits) spUnit.insert(u.getObject().getGeometry().getEnvelopeInternal(), u);

		LOGGER.debug("   Link face and unit agents");
		//for each face, find unit that intersects and make link
		for(AFace aFace : aFaces){
			Polygon faceGeom = aFace.getObject().getGeom();
			for(AUnit u : (List<AUnit>)spUnit.query(faceGeom.getEnvelopeInternal())) {
				Geometry uGeom = u.getObject().getGeometry();
				if(!uGeom.getEnvelopeInternal().intersects(faceGeom.getEnvelopeInternal())) continue;
				if(!uGeom.covers(faceGeom)) continue;
				//link
				if(u.aFaces == null) u.aFaces = new HashSet<AFace>();
				aFace.aUnit = u; u.aFaces.add(aFace);
				break;
			}
		}

		//link points to faces
		for(AUnit au : aUnits) {
			if(au.points == null) continue;
			au.linkPointsToFaces();
		}

		return this;
	}

	/**
	 * Destroy the topological map.
	 * 
	 * @return this
	 */
	public ATesselation destroyTopologicalMap() {
		if(graph != null) { graph.clear(); graph = null; }
		if(aEdges != null) { for(AEdge a: aEdges) a.clear(); aEdges.clear(); aEdges = null; }
		if(aFaces != null) { for(AFace a: aFaces) a.clear(); aFaces.clear(); aFaces = null; }
		return this;
	}


	public AEdge getAEdge(Edge e){
		if(e == null) return null;
		for(AEdge ae:aEdges) if(ae.getObject()==e) return ae;
		return null;
	}
	public AFace getAFace(Face f){
		if(f == null) return null;
		for(AFace af:aFaces) if(af.getObject()==f) return af;
		return null;
	}
	public AUnit getAUnit(Feature unit) {
		if(unit == null) return null;
		for(AUnit au:aUnits) if(au.getObject()==unit) return au;
		return null;
	}



	public Collection<Feature> getEdges() {
		Collection<Feature> out = new HashSet<Feature>();
		if(aEdges == null) return out;
		for(AEdge aEdg:aEdges)
			if(!aEdg.isDeleted())
				out.add(GraphToFeature.asFeature(aEdg.getObject()));
		return out;
	}

	public Collection<Feature> getFaces() {
		Collection<Feature> out = new HashSet<Feature>();
		if(aFaces ==null) return out;
		for(AFace aFace : aFaces) {
			if(aFace.isDeleted()) continue;
			Feature f = GraphToFeature.asFeature(aFace.getObject());
			if(f.getGeometry()==null){
				LOGGER.error("Null geom for face "+aFace.getId()+". Nb edges="+aFace.getObject().getEdges().size());
				continue;
			}
			if(f.getGeometry().isEmpty()){
				LOGGER.error("Empty geom for unit "+aFace.getId()+". Nb edges="+aFace.getObject().getEdges().size());
				continue;
			}
			if(!f.getGeometry().isValid()) {
				LOGGER.error("Non valid geometry for face "+aFace.getId()+". Nb edges="+aFace.getObject().getEdges().size());
			}
			//add unit's id
			f.setAttribute("unit", aFace.aUnit!=null?aFace.aUnit.getId():null);
			out.add(f);
		}
		return out;
	}

	public Collection<Feature> getUnits() {
		Collection<Feature> units = new HashSet<Feature>();
		if(aUnits ==null) return units;
		for(AUnit u : aUnits) {
			if(u.isDeleted()) continue;
			Feature f = u.getObject();
			if(f.getGeometry()==null){
				LOGGER.warn("Null geom for unit "+u.getId()+". Nb faces="+(u.aFaces!=null?u.aFaces.size():"null"));
				continue;
			}
			if(f.getGeometry().isEmpty()){
				LOGGER.warn("Empty geom for unit "+u.getId()+". Nb faces="+(u.aFaces!=null?u.aFaces.size():"null"));
				continue;
			}
			if(!f.getGeometry().isValid()) {
				f.setGeometry( (MultiPolygon)JTSGeomUtil.toMulti(f.getGeometry().buffer(0)) );
				LOGGER.warn("Non valid geometry for unit "+u.getId()+". Nb faces="+(u.aFaces!=null?u.aFaces.size():"null")+" --- "+(f.getGeometry().isValid()? " Fixed!" : " Not fixed..."));
			}
			units.add(f);
		}
		return units;
	}

	/**
	 * Check if a edge schould be frozen.
	 * This happens when the edge lays within the enveloppe. This is usefull when running the partitionning along an envelope.
	 * 
	 * @param ae
	 * @return
	 */
	private boolean isToBeFrozen(AEdge ae) {
		if(this.env == null) return false;
		Geometry g = ae.getObject().getGeometry();
		if (JTSGeomUtil.containsSFS(this.env, g.getEnvelopeInternal())) return false;
		double length = g.intersection(JTSGeomUtil.getBoundary(this.env)).getLength();
		if(length == 0) return false;
		else if(length == g.getLength()) return true;
		else {
			LOGGER.warn("*** "+length+" "+g.getLength()+" "+(length-g.getLength()));
			return true;
		}
	}

	public void clear() {
		super.clear();
		destroyTopologicalMap();
		if(aUnits != null) { for(AUnit a: aUnits) a.clear(); aUnits.clear(); aUnits=null; }
	}

	//TODO improve, with spatial index?
	public Collection<AUnit> query(Envelope env) {
		Collection<AUnit> out = new ArrayList<AUnit>();
		for(AUnit au : aUnits) {
			if(au.getObject().getGeometry() == null) continue;
			if(au.getObject().getGeometry().isEmpty()) continue;
			if(! env.intersects(au.getObject().getGeometry().getEnvelopeInternal())) continue;
			out.add(au);
		}
		return out;
	}

}
