/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.zzz.nutscommgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.europa.ec.eurostat.jgiscotools.algo.graph.TopologyAnalysis;
import eu.europa.ec.eurostat.jgiscotools.graph.structure.Face;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.AFace;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.AUnit;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.TFaceAggregation;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Transformation;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class CFaceEEZInLand extends Constraint<AFace> {

	public CFaceEEZInLand(AFace agent) { super(agent); }

	boolean toBeDeleted;

	@Override
	public void computeCurrentValue() {
		toBeDeleted = false;
		AFace aFace = getAgent();
		if(aFace.isDeleted()) return;
		if(aFace.aUnit == null) return;
		if(!aFace.aUnit.getObject().getID().contains("EEZ")) return;
		if(!isEEZEnclave(aFace)) return;
		if(aFace.lastUnitFace()) return;
		toBeDeleted = true;
	}

	private boolean isEEZEnclave(AFace af) {
		//f is a EZZ enclave if:
		// - it is not coastal.
		Face f = af.getObject();
		if(TopologyAnalysis.isCoastal(f)) return false;
		// - it is not surrounded by another EEZ face.
		// - all surrounded faces are linked to the same unit
		Collection<Face> tfs = f.getTouchingFaces();
		String unitId = null;
		for(Face tf : tfs) {
			AUnit aunit = af.getAtesselation().getAFace(tf).aUnit;
			if(aunit == null) return false;
			String id = aunit.getObject().getID();
			if(id.contains("EEZ")) return false;
			if(unitId == null)
				unitId = id;
			else
				if(!unitId.equals(id)) return false;
		}
		return true;
	}

	@Override
	public void computeSatisfaction() {
		satisfaction = toBeDeleted && !getAgent().isDeleted() && !getAgent().lastUnitFace() ? 0 : 10;
	}

	@Override
	public List<Transformation<AFace>> getTransformations() {
		ArrayList<Transformation<AFace>> out = new ArrayList<Transformation<AFace>>();
		if(toBeDeleted)
			out.add(new TFaceAggregation(getAgent()));
		return out;
	}

}
