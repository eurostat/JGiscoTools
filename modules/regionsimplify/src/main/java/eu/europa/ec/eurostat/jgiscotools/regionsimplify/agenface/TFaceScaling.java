package eu.europa.ec.eurostat.jgiscotools.regionsimplify;

import eu.europa.ec.eurostat.jgiscotools.agent.TransformationCancellable;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.FaceScaling;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * @author julien Gaffuri
 *
 */
public class TFaceScaling extends TransformationCancellable<AFace> {
	double factor;

	public TFaceScaling(AFace agent, double factor) {
		super(agent);
		this.factor = factor;
	}



	@Override
	public void apply() {
		//System.out.println("Scaling "+agent.getObject().getGeometry().getCentroid());
		FaceScaling.scale(getAgent().getObject(), factor);
	}


	@Override
	public boolean isCancelable() { return true; }

	@Override
	public void storeState() {}

	@Override
	public void cancel() {
		//System.out.println("Undo scaling "+agent.getObject().getGeometry().getCentroid());
		FaceScaling.scale(getAgent().getObject(), 1/factor);
	}


	public String toString(){
		return getClass().getSimpleName() + "("+Util.round(factor, 3)+")";
	}

}
