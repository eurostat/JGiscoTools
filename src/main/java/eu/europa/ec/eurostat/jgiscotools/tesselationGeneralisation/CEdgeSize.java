/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.LineString;

import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Transformation;

/**
 * Ensure too short segment edges are collapsed or lengthened.
 * 
 * @author julien Gaffuri
 *
 */
public class CEdgeSize extends Constraint<AEdge> {
	double minSize, delSize;

	public CEdgeSize(AEdge agent, double minSize, double delSize) {
		super(agent);
		this.minSize = minSize;
		this.delSize = delSize;
	}

	double currentSize, goalSize;
	@Override
	public void computeCurrentValue() {
		currentSize = getAgent().getObject().getGeometry().getLength();
	}

	@Override
	public void computeGoalValue() {
		goalSize = currentSize>minSize ? currentSize : (currentSize<delSize)? 0.0001 : minSize;
	}

	@Override
	public void computeSatisfaction() {
		if(getAgent().isDeleted()) { satisfaction=10; return; }

		LineString g = getAgent().getObject().getGeometry();
		if(g.isClosed()) { satisfaction = 10; return; }

		satisfaction = 10 - 10*Math.abs(goalSize-currentSize)/goalSize;
		if(satisfaction<0) satisfaction=0;
	}

	@Override
	public List<Transformation<AEdge>> getTransformations() {
		ArrayList<Transformation<AEdge>> out = new ArrayList<Transformation<AEdge>>();
		if(!getAgent().isFrozen())
			for(double k : new double[]{1, 0.8, 0.5, 0.02})
				//out.add(new TFaceScaling(aFace, k*Math.sqrt(goalArea/currentArea)));
				out.add(new TEdgeScale(getAgent(), k*goalSize/currentSize));
		return out;
	}

}
