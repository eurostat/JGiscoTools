/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

import eu.europa.ec.eurostat.jgiscotools.agent.Agent;
import eu.europa.ec.eurostat.jgiscotools.algo.base.Union;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;

/**
 * A tesselation unit, which consists of one or several AFaces.
 * It is an agent representing a multipolygon statistical unit.
 * 
 * @author julien Gaffuri
 *
 */
public class AUnit extends Agent {
	private final static Logger LOGGER = LogManager.getLogger(AUnit.class.getName());

	private ATesselation aTess;
	public ATesselation getAtesselation(){ return aTess; }

	public AUnit(Feature f, ATesselation aTess) {
		super(f);
		this.aTess=aTess;
		this.setId(f.getID());
	}

	//the points that are supposed to be inside the unit, and might be used for a constraint
	public Collection<Point> points = null;

	public Feature getObject() { return (Feature)super.getObject(); }

	//the patches composing the units
	public Collection<AFace> aFaces = null;

	//update unit geometry from face geometries
	public void updateGeomFromFaceGeoms(){
		if(aFaces == null) return;
		//if(aFaces == null || aFaces.size()==0 || isDeleted()) { getObject().setGeom(null); return; }

		Collection<Geometry> geoms = new HashSet<Geometry>();
		for(AFace aFace : aFaces) {
			if(aFace.isDeleted()) continue;
			Geometry aFaceGeom = aFace.getObject().getGeom();
			if(aFaceGeom==null || aFaceGeom.isEmpty()){
				LOGGER.error("Error when building unit's geometry for unit "+this.getId()+": Face as null/empty geometry "+aFace.getId());
				continue;
			}
			geoms.add(aFaceGeom);
		}

		Geometry union;
		try {
			union = CascadedPolygonUnion.union(geoms);
		} catch (Exception e) {
			LOGGER.warn("CascadedPolygonUnion failed for unit "+getId()+". Trying another union method. Message: "+e.getMessage());
			try {
				union = new GeometryFactory().buildGeometry(geoms).union();
			} catch (Exception e1) {
				LOGGER.warn("Collection<Geometry>.union failed for unit "+getId()+". Trying another union method. Message: "+e1.getMessage());
				try {
					union = Union.getPolygonUnion(geoms);
				} catch (Exception e2) {
					LOGGER.warn("Union.get failed for unit "+getId()+". Trying another union method. Message: "+e1.getMessage());
					union = null;
				}
			}
		}

		if(union==null || union.isEmpty()){
			LOGGER.warn("Null union found when updating geometry of unit "+getId()+". Nb polygons="+geoms.size());
		} else
			union = (MultiPolygon) JTSGeomUtil.toMulti(union);

		getObject().setGeometry(union);
	}

	public int getNumberOfNonDeletedFaces() {
		int n=0;
		for(AFace aFace : aFaces) if(!aFace.isDeleted()) n++;
		return n;
	}

	public void clear() {
		super.clear();
		if(aFaces != null) aFaces.clear(); aFaces = null;
		if(points != null) points.clear(); points = null;
	}




	void linkPointsToFaces() {
		if(points == null) return;
		for(Point pt : points) {
			AFace af = getAFace(pt);
			if(af==null) {
				LOGGER.warn("Could not find any face for point "+pt.getCoordinate()+" belonging to unit "+getId());
				continue;
			}
			if(af.points == null) af.points = new ArrayList<Point>();
			af.points.add(pt);
		}
	}

	private AFace getAFace(Point pt) {
		if(isDeleted()) return null;
		if(aFaces == null) return null;
		for(AFace af : aFaces) {
			if(af.isDeleted()) continue;
			if(af.getObject().getGeom()==null) continue;
			if(af.getObject().getGeom().contains(pt)) return af;
		}
		return null;
	}

	//check if the unit contains its points
	public boolean containPoints() {
		if(points == null) return true;
		for(Point pt : points)
			if(! getObject().getGeometry().contains(pt)) return false;
		return true;
	}

	//check if one of the points is within a polygon
	public boolean containAtLeastOnePoint(Polygon p) {
		if(points == null) return false;
		for(Point pt : points)
			if(! p.contains(pt)) return true;
		return false;
	}

}
