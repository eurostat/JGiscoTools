/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify;

import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.AFace;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.CFaceContainPoints;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.CFaceNoTriangle;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.CFaceSize;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.CFaceValidity;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.AEdge;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgeFaceSize;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgeGranularity;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgeNoTriangle;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgeValidity;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgesFacesContainPoints;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenttesselation.ATesselation;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.AUnit;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.CUnitContainPoints;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.CUnitNoNarrowGaps;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.CUnitNoNarrowParts;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.CUnitNoTriangle;
import eu.europa.ec.eurostat.jgiscotools.util.CRSType;

/**
 * @author julien Gaffuri
 *
 */
public class TesselationGeneralisationSpecification {
	protected CartographicResolution res;

	protected boolean removeNarrowGaps, removeNarrowParts, preserveAllUnits, preserveIfPointsInIt, noTriangle;
	protected int quad;

	private double nodingResolution;
	public double getNodingResolution() { return nodingResolution; }

	public TesselationGeneralisationSpecification(double scaleDenominator, CRSType crsType) { this(new CartographicResolution(scaleDenominator, crsType), crsType, true, false, true, true, true, 5); }
	public TesselationGeneralisationSpecification(CartographicResolution res, CRSType crsType) { this(res, crsType, true, false, true, true, true, 5); }
	public TesselationGeneralisationSpecification(CartographicResolution res, CRSType crsType, boolean removeNarrowGaps, boolean removeNarrowParts, boolean preserveAllUnits, boolean preserveIfPointsInIt, boolean noTriangle, int quad) {
		this.res=res;
		this.removeNarrowGaps = removeNarrowGaps;
		this.removeNarrowParts = removeNarrowParts;
		this.preserveAllUnits = preserveAllUnits;
		this.preserveIfPointsInIt = preserveIfPointsInIt;
		this.noTriangle = noTriangle;
		nodingResolution = crsType==CRSType.CARTO?1e-5:1e-8;
		this.quad = quad;
	}

	public void setUnitConstraints(ATesselation t) {
		for(AUnit a : t.aUnits) {
			if(removeNarrowGaps) a.addConstraint(new CUnitNoNarrowGaps(a, res.getSeparationDistanceMeter(), getNodingResolution(), quad, preserveAllUnits, preserveIfPointsInIt).setPriority(10));
			if(removeNarrowParts) a.addConstraint(new CUnitNoNarrowParts(a, res.getSeparationDistanceMeter(), getNodingResolution(), quad, preserveAllUnits, preserveIfPointsInIt).setPriority(9));
			if(preserveIfPointsInIt) a.addConstraint(new CUnitContainPoints(a));
			if(noTriangle) a.addConstraint(new CUnitNoTriangle(a));
		}
	}
	public void setTopologicalConstraints(ATesselation t) {
		for(AFace a : t.aFaces) {
			a.addConstraint(new CFaceSize(a, 0.1*res.getPerceptionSizeSqMeter(), 3*res.getPerceptionSizeSqMeter(), res.getPerceptionSizeSqMeter(), preserveAllUnits, preserveIfPointsInIt).setPriority(2));
			a.addConstraint(new CFaceValidity(a));
			if(preserveIfPointsInIt) a.addConstraint(new CFaceContainPoints(a));
			if(noTriangle) a.addConstraint(new CFaceNoTriangle(a));
		}
		for(AEdge a : t.aEdges) {
			a.addConstraint(new CEdgeGranularity(a, 2*res.getResolutionM()));
			a.addConstraint(new CEdgeValidity(a));
			if(noTriangle) a.addConstraint(new CEdgeNoTriangle(a));
			a.addConstraint(new CEdgeFaceSize(a).setImportance(6));
			if(preserveIfPointsInIt) a.addConstraint(new CEdgesFacesContainPoints(a));
		}
	}
}
