/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify;

import java.util.Collection;

import org.locationtech.jts.geom.Point;

import eu.europa.ec.eurostat.jgiscotools.agent.Agent;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Face;

/**
 * A face, as an agent.
 * 
 * @author julien Gaffuri
 *
 */
public class AFace extends Agent {
	private ATesselation aTess;
	public ATesselation getAtesselation(){ return aTess; }

	public AFace(Face object, ATesselation aTess) { super(object); this.aTess=aTess; }
	public Face getObject() { return (Face) super.getObject(); }

	//the points that are supposed to be inside the face, and might be used for a constraint
	public Collection<Point> points = null;


	/**
	 * The unit agent the face may belong to
	 */
	public AUnit aUnit = null;

	public boolean lastUnitFace(){
		if(aUnit == null) return false;
		return aUnit.getNumberOfNonDeletedFaces() == 1;
	}

	/**
	 * @return True if the face relate to a unit hole.
	 */
	public boolean isHole() {
		return aUnit == null;
	}

	public boolean hasFrozenEdge() {
		for(Edge e : getObject().getEdges())
			if (aTess.getAEdge(e).isFrozen()) return true;
		return false;
	}

	//check if the face contains its points
	public boolean containPoints(boolean checkAlsoNeigbours) {
		if(points == null || points.size()==0) return true;
		for(Point pt : points)
			if(! getObject().getGeom().contains(pt)) return false;
		if(checkAlsoNeigbours)
			for(Face f : getObject().getTouchingFaces())
				if(!aTess.getAFace(f).containPoints(false)) return false;
		return true;
	}

	public void clear() {
		super.clear();
		aTess = null;
		aUnit = null;
		if(points != null) points.clear(); points = null;
	}

}
