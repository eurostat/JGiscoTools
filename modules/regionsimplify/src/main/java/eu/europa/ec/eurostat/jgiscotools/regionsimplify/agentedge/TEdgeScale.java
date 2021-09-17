/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge;

import eu.europa.ec.eurostat.jgiscotools.agent.TransformationCancellable;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.EdgeScaling;

/**
 * 
 * Transformation to alter the length of a graph edge.
 * 
 * @author julien Gaffuri
 *
 */
public class TEdgeScale extends TransformationCancellable<AEdge> {

	private double factor;
	public TEdgeScale(AEdge agent, double factor) {
		super(agent);
		this.factor = factor;
	}

	@Override
	public void apply() {
		EdgeScaling.scale(getAgent().getObject(), factor);
	}



	@Override
	public boolean isCancelable() { return true; }

	@Override
	public void storeState() {}

	@Override
	public void cancel() {
		EdgeScaling.scale(getAgent().getObject(), 1/factor);
	}

}
