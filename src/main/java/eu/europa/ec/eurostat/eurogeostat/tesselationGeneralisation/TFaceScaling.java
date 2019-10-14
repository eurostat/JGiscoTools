package eu.europa.ec.eurostat.eurogeostat.tesselationGeneralisation;

import eu.europa.ec.eurostat.eurogeostat.algo.graph.FaceScaling;
import eu.europa.ec.eurostat.eurogeostat.transfoengine.TransformationCancellable;
import eu.europa.ec.eurostat.eurogeostat.util.Util;

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
