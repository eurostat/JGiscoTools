/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.SpatialIndex;

import eu.europa.ec.eurostat.jgiscotools.agent.Constraint;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;

/**
 * Ensures that an edge does not intersects other ones, in an unvalid manner.
 * 
 * @author julien Gaffuri
 *
 */
public class CEdgeToEdgeIntersection extends Constraint<AEdge> {
	SpatialIndex edgeSpatialIndex;

	public CEdgeToEdgeIntersection(AEdge agent, SpatialIndex edgeSpatialIndex) {
		super(agent);
		this.edgeSpatialIndex = edgeSpatialIndex;
	}

	boolean intersectsOthers = false;

	@Override
	public void computeCurrentValue() {
		Edge e = getAgent().getObject();
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
		intersectsOthers = false;
	}

	@Override
	public void computeSatisfaction() {
		satisfaction = intersectsOthers? 0 : 10;
	}

	@Override
	public boolean isHard() { return true; }

}
