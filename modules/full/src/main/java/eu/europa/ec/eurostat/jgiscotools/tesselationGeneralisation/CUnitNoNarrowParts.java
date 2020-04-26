/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import eu.europa.ec.eurostat.jgiscotools.agent.Constraint;
import eu.europa.ec.eurostat.jgiscotools.agent.Transformation;
import eu.europa.ec.eurostat.jgiscotools.algo.noding.NodingUtil;
import eu.europa.ec.eurostat.jgiscotools.algo.noding.NodingUtil.NodingIssueType;
import eu.europa.ec.eurostat.jgiscotools.algo.polygon.MorphologicalAnalysis;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;

/**
 * 
 * Constraint ensuring that a unit has no narrow part.
 * 
 * @author julien Gaffuri
 *
 */
public class CUnitNoNarrowParts extends Constraint<AUnit> {
	private final static Logger LOGGER = LogManager.getLogger(CUnitNoNarrowParts.class.getName());

	private double widthMeter, nodingResolution; private int quad; private boolean preserveAllUnits, preserveIfPointsInIt;
	public CUnitNoNarrowParts(AUnit agent, double widthMeter, double nodingResolution, int quad, boolean preserveAllUnits, boolean preserveIfPointsInIt) {
		super(agent);
		this.widthMeter = widthMeter;
		this.nodingResolution = nodingResolution;
		this.quad = quad;
		this.preserveAllUnits = preserveAllUnits;
		this.preserveIfPointsInIt = preserveIfPointsInIt;
	}

	//the narrow parts
	private Collection<Polygon> nps;

	@Override
	public void computeCurrentValue() {
		//compute narrow parts
		nps = MorphologicalAnalysis.getNarrowParts(getAgent().getObject().getGeometry(), widthMeter, quad);
	}

	@Override
	public void computeSatisfaction() {
		if(nps.size()==0) { satisfaction=10; return; }
		//depends on the size of the narrow parts
		double a = getAgent().getObject().getGeometry().getArea();
		if(a==0) { satisfaction = 10; return; }
		double snpa=0; for(Polygon np : nps) snpa += np.getArea();
		satisfaction = 10*(1-snpa/a);
		satisfaction = satisfaction>10? 10 : satisfaction<0? 0 : satisfaction;
	}

	@Override
	public List<Transformation<AUnit>> getTransformations() {
		ArrayList<Transformation<AUnit>> out = new ArrayList<Transformation<AUnit>>();
		out.add(new TRemoveNarrowParts(getAgent()));
		return out;
	}

	private class TRemoveNarrowParts extends Transformation<AUnit> {

		public TRemoveNarrowParts(AUnit agent) { super(agent); }

		@Override
		public void apply() {
			Feature unit = getAgent().getObject();
			for(Polygon np : nps) {
				np = (Polygon) np.buffer(widthMeter*0.001, quad);

				//check point thing
				if(preserveIfPointsInIt && getAgent().containAtLeastOnePoint(np))
					continue;

				Geometry newUnitGeom = null;
				try {
					newUnitGeom = unit.getGeometry().difference(np);
				} catch (Exception e1) {
					LOGGER.warn("Could not make difference of unit "+unit.getID()+" with narrow part around " + np.getCentroid().getCoordinate() + " Exception: "+e1.getClass().getName());
					continue;
				}

				if(newUnitGeom==null || newUnitGeom.isEmpty()) {
					LOGGER.trace("Unit "+unit.getID()+" disappeared when removing parts of unit "+unit.getID()+" around "+np.getCentroid().getCoordinate());
					if(preserveAllUnits) continue;
					else getAgent().setDeleted(true);
				}

				//set new geometry
				unit.setGeometry(JTSGeomUtil.toMulti(newUnitGeom));
			}

			LOGGER.trace("Ensure noding");
			Collection<Feature> unitsNoding = new ArrayList<Feature>();
			for(AUnit au : getAgent().getAtesselation().query( unit.getGeometry().getEnvelopeInternal() )) unitsNoding.add(au.getObject());
			NodingUtil.fixNoding(NodingIssueType.PointPoint, unitsNoding, nodingResolution);
			NodingUtil.fixNoding(NodingIssueType.LinePoint, unitsNoding, nodingResolution);
		}

		//TODO make it cancellable - with geometry storage?
		@Override
		public boolean isCancelable() { return false; }
	}

}
