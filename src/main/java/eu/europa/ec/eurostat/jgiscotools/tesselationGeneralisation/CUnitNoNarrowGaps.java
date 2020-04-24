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

import eu.europa.ec.eurostat.jgiscotools.algo.noding.NodingUtil;
import eu.europa.ec.eurostat.jgiscotools.algo.noding.NodingUtil.NodingIssueType;
import eu.europa.ec.eurostat.jgiscotools.algo.polygon.MorphologicalAnalysis;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Transformation;

/**
 * 
 * Constraint ensuring that a unit has no narrow gap.
 * 
 * @author julien Gaffuri
 *
 */
public class CUnitNoNarrowGaps extends Constraint<AUnit> {
	private final static Logger LOGGER = LogManager.getLogger(CUnitNoNarrowGaps.class.getName());

	private double separationDistanceMeter, nodingResolution; private int quad; private boolean preserveAllUnits, preserveIfPointsInIt;
	public CUnitNoNarrowGaps(AUnit agent, double separationDistanceMeter, double nodingResolution, int quad, boolean preserveAllUnits, boolean preserveIfPointsInIt) {
		super(agent);
		this.separationDistanceMeter = separationDistanceMeter;
		this.nodingResolution = nodingResolution;
		this.quad = quad;
		this.preserveAllUnits = preserveAllUnits;
		this.preserveIfPointsInIt = preserveIfPointsInIt;
	}

	//the narrow gaps
	private Collection<Polygon> ngs;

	@Override
	public void computeCurrentValue() {
		//compute narrow gaps
		ngs = MorphologicalAnalysis.getNarrowGaps(getAgent().getObject().getGeometry(), separationDistanceMeter, quad);
	}

	@Override
	public void computeSatisfaction() {
		//depends on the size of the narrow gaps
		double a = getAgent().getObject().getGeometry().getArea();
		if(a==0) { satisfaction = 10; return; }
		double snga=0; for(Polygon ng : ngs) snga += ng.getArea();
		satisfaction = 10*(1-snga/a);
		satisfaction = satisfaction>10? 10 : satisfaction<0? 0 : satisfaction;
	}

	@Override
	public List<Transformation<AUnit>> getTransformations() {
		ArrayList<Transformation<AUnit>> out = new ArrayList<Transformation<AUnit>>();
		out.add(new TRemoveNarrowGaps(getAgent()));
		return out;
	}

	private class TRemoveNarrowGaps extends Transformation<AUnit> {

		public TRemoveNarrowGaps(AUnit agent) { super(agent); }

		@Override
		public void apply() {
			Feature unit = getAgent().getObject();

			//the collection of units to fix geometries
			Collection<Feature> unitsNoding = new ArrayList<Feature>();
			unitsNoding.add(getAgent().getObject());

			//go through the narrow gaps
			for(Polygon ng : ngs) {
				ng = (Polygon) ng.buffer(separationDistanceMeter*0.001, quad);

				//union with unit geometry
				Geometry newUnitGeom = null;
				try {
					newUnitGeom = unit.getGeometry().union(ng);
				} catch (Exception e1) {
					LOGGER.warn("Could not make union of unit "+unit.getID()+" with gap around " + ng.getCentroid().getCoordinate() + " Exception: "+e1.getClass().getName());
					continue;
				}

				//get units intersecting and try to correct their geometries
				Collection<AUnit> uis = getAgent().getAtesselation().query( ng.getEnvelopeInternal() );
				for(AUnit aui : uis) {
					Feature ui = aui.getObject();
					if(ui == unit) continue;
					if(!ui.getGeometry().getEnvelopeInternal().intersects(ng.getEnvelopeInternal())) continue;

					//store unit to fix noding in the end
					unitsNoding.add(ui);

					//compute the candidate geometry: the difference
					Geometry geomC = ui.getGeometry().difference(ng);

					//check not the whole unit has disappeared
					if(preserveAllUnits && (geomC==null || geomC.isEmpty())) {
						LOGGER.trace("Unit "+ui.getID()+" disappeared when removing gaps of unit "+unit.getID()+" around "+ng.getCentroid().getCoordinate());
						newUnitGeom = newUnitGeom.difference(ui.getGeometry());
						continue;
					}

					//store current geom, and set new one
					Geometry geomS = ui.getGeometry();
					ui.setGeometry(JTSGeomUtil.toMulti(geomC));

					//check if point has left it
					if(preserveIfPointsInIt && !getAgent().getAtesselation().getAUnit(ui).containPoints()) {
						LOGGER.trace("Unit "+ui.getID()+" has lost some point in it when removing gaps of unit "+unit.getID()+" around "+ng.getCentroid().getCoordinate());
						ui.setGeometry(JTSGeomUtil.toMulti(geomS));
						newUnitGeom = newUnitGeom.difference(ui.getGeometry());
						continue;
					}
				}

				//set new geometry
				unit.setGeometry(JTSGeomUtil.toMulti(newUnitGeom));
			}

			LOGGER.trace("Ensure noding");
			NodingUtil.fixNoding(NodingIssueType.PointPoint, unitsNoding, nodingResolution);
			NodingUtil.fixNoding(NodingIssueType.LinePoint, unitsNoding, nodingResolution);
		}

		//TODO make it cancellable - with geometry storage?
		@Override
		public boolean isCancelable() { return false; }
	}

}
