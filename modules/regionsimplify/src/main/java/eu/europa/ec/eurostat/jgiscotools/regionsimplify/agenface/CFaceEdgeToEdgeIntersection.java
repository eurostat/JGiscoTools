/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.SpatialIndex;

import eu.europa.ec.eurostat.jgiscotools.agent.Constraint;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Face;

/**
 * Ensures that none of the edges of the face intersects other edges.
 * 
 * @author julien Gaffuri
 *
 */
public class CFaceEdgeToEdgeIntersection extends Constraint<AFace> {
	SpatialIndex edgeSpatialIndex;

	public CFaceEdgeToEdgeIntersection(AFace agent, SpatialIndex edgeSpatialIndex) {
		super(agent);
		this.edgeSpatialIndex = edgeSpatialIndex;
	}

	boolean intersectsOthers = false;

	@Override
	public void computeCurrentValue() {
		Face f = getAgent().getObject();
		for(Edge e : f.getEdges()){
			LineString g = e.getGeometry();

			//retrieve edges from spatial index
			List<Edge> edges = edgeSpatialIndex.query(g.getEnvelopeInternal());
			for(Edge e_ : edges){
				if(e==e_) continue;

				LineString g_ = e_.getGeometry();
				if(!g_.getEnvelopeInternal().intersects(g.getEnvelopeInternal())) continue;

				//analyse intersection
				//TODO improve speed by using right geometrical predicate. crosses?
				Geometry inter = g.intersection(g_);
				if(inter.isEmpty()) continue;
				if(inter.getLength()>0){
					//System.out.println("  length!"+e.getId()+" "+e_.getId());
					intersectsOthers = true;
					return;
				}
				for(Coordinate c : inter.getCoordinates()){
					if( c.distance(e.getN1().getC())==0 || c.distance(e.getN2().getC())==0 ) continue;
					//System.out.println("  coord!"+e.getId()+" "+e_.getId());
					intersectsOthers = true;
					return;
				}
			}
		}
		intersectsOthers = false;
	}

	@Override
	public void computeSatisfaction() {
		satisfaction = intersectsOthers? 0 : 10;
	}

	@Override
	public boolean isHard() { return true; }

}
