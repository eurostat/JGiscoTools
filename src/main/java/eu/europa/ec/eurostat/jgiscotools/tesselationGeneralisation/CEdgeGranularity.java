/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import java.util.ArrayList;
import java.util.List;

import eu.europa.ec.eurostat.eurogeostat.algo.measure.Granularity;
import eu.europa.ec.eurostat.eurogeostat.algo.measure.Granularity.GranularityMeasurement;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Transformation;

/**
 * Ensure the granularity of an edge is below a target resolution value.
 * 
 * @author julien Gaffuri
 *
 */
public class CEdgeGranularity extends Constraint<AEdge> {
	double goalGranularity, currentGranularity;

	public CEdgeGranularity(AEdge agent, double goalResolution) {
		super(agent);
		this.goalGranularity = goalResolution;
	}

	@Override
	public void computeCurrentValue() {
		GranularityMeasurement m = Granularity.get(getAgent().getObject().getGeometry(), goalGranularity);
		if(Double.isNaN(m.averageBelow)) currentGranularity = m.average;
		else currentGranularity = m.averageBelow;
	}

	@Override
	public void computeSatisfaction() {
		//case of segment
		if(getAgent().getObject().getGeometry().getNumPoints()==2) { satisfaction=10; return; }
		//case when granularity is large enough
		if(currentGranularity >= goalGranularity) { satisfaction=10; return; }
		//case when granularity too low
		satisfaction = 10-10*Math.abs(goalGranularity-currentGranularity)/goalGranularity;
	}

	@Override
	public List<Transformation<AEdge>> getTransformations() {
		ArrayList<Transformation<AEdge>> out = new ArrayList<Transformation<AEdge>>();

		//Edge e = getAgent().getObject();
		//double length = e.getGeometry()==null? 0 : e.getGeometry().getLength();
		//if(length<=goalResolution){
		//tr.add(new TEdgeCollapse(getAgent())); //TODO ensure faces remain valid after edge collapse
		//} else {

		double[] ks = new double[]{ 1, 0.8, 0.6, 0.4, 0.2 };
		for(double k : ks)
			out.add(new TEdgeSimplifierVisvalingamWhyatt(getAgent(), k*goalGranularity));
		/*for(double k : ks){
			//tr.add(new TEdgeRamerDouglasPeuckerSimplifier(getAgent(), k*goalGranularity, false));
			out.add(new TEdgeSimplifierRamerDouglasPeucker(getAgent(), k*goalGranularity, true));
		}*/

		return out;
	}

}
