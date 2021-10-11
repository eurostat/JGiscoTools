/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify;

import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;

import eu.europa.ec.eurostat.jgiscotools.agent.Engine;
import eu.europa.ec.eurostat.jgiscotools.algo.base.NodingUtil;
import eu.europa.ec.eurostat.jgiscotools.algo.base.Partition;
import eu.europa.ec.eurostat.jgiscotools.algo.base.NodingUtil.NodingIssueType;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenttesselation.ATesselation;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.AUnit;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.CUnitNoding;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.CUnitOverlap;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.CUnitValidity;

/**
 * @author julien gaffuri
 *
 */
public class TesselationQuality {
	private final static Logger LOGGER = LogManager.getLogger(TesselationQuality.class.getName());


	//
	public static void checkQuality(Collection<Feature> units, double nodingResolution, String outFilePath, boolean overrideFile, boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, boolean tracePartitionning) {
		Partition.runRecursivelyApply(units, p -> {
			if(tracePartitionning) LOGGER.info(p);

			LOGGER.debug("Build spatial indexes");
			SpatialIndex index = FeatureUtil.getSTRtree(p.features);
			SpatialIndex indexLP = FeatureUtil.getSTRtreeCoordinates(p.features);
			SpatialIndex indexPP = NodingUtil.getSTRtreeCoordinatesForPP(p.features, nodingResolution);

			ATesselation t = new ATesselation(p.getFeatures());
			LOGGER.debug("Set constraints");
			for(AUnit a : t.aUnits) {
				a.clearConstraints();
				a.addConstraint(new CUnitOverlap(a, index));
				a.addConstraint(new CUnitNoding(a, indexLP, NodingIssueType.LinePoint, nodingResolution));
				a.addConstraint(new CUnitNoding(a, indexPP, NodingIssueType.PointPoint, nodingResolution));
				a.addConstraint(new CUnitValidity(a));
			}

			LOGGER.debug("Run evaluation");
			Engine<AUnit> uEng = new Engine<AUnit>(t.aUnits).sort();
			uEng.runEvaluation(outFilePath, overrideFile).clear();

			t.clear();

		}, parallel, maxCoordinatesNumber, objMaxCoordinateNumber, true, false, Partition.GeomType.ONLY_AREAS, 0);
	}



	//
	public static Collection<Feature> fixQuality(Collection<Feature> units, Envelope clipEnv, double nodingResolution, boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, boolean tracePartitionning) {
		LOGGER.info("Dissolve by id");
		FeatureUtil.dissolveById(units);

		LOGGER.info("Make valid");
		units = makeMultiPolygonValid(units);

		if(clipEnv != null) {
			LOGGER.info("Clip");
			clip(units, clipEnv);
		}

		LOGGER.info("Ensure tesselation and fix noding");
		Partition.runRecursivelyApply(units, p -> {
			if(tracePartitionning) LOGGER.info(p);
			NodingUtil.fixNoding(NodingIssueType.PointPoint, p.getFeatures(), nodingResolution);
			NodingUtil.fixNoding(NodingIssueType.LinePoint, p.getFeatures(), nodingResolution);
			ensureTesselation_(p.getFeatures());
			NodingUtil.fixNoding(NodingIssueType.PointPoint, p.getFeatures(), nodingResolution);
			NodingUtil.fixNoding(NodingIssueType.LinePoint, p.getFeatures(), nodingResolution);
		}, parallel, maxCoordinatesNumber, objMaxCoordinateNumber, true, true, Partition.GeomType.ONLY_AREAS, 0);

		LOGGER.info("Ensure units are multipolygons");
		for(Feature u : units)
			u.setGeometry(JTSGeomUtil.toMulti(u.getGeometry()));

		return units;
	}




	public static Collection<Feature> makeMultiPolygonValid(Collection<Feature> fs) {
		for(Feature f : fs)
			f.setGeometry( (MultiPolygon)JTSGeomUtil.toMulti(f.getGeometry().buffer(0)) );
		return fs;
	}

	public static void clip(Collection<Feature> fs, Envelope env) {
		Polygon extend = JTS.toGeometry(env);
		Collection<Feature> toBeRemoved = new HashSet<Feature>();

		for(Feature f : fs) {
			Geometry g = f.getGeometry();
			Envelope env_ = g.getEnvelopeInternal();

			//feature fully in the envelope
			if(env.contains(env_)) continue;
			//feature fully out of the envelope
			if(!env.intersects(env_)) {
				toBeRemoved.add(f);
				continue;
			}

			//check if feature intersects envelope
			Geometry inter = g.intersection(extend);
			inter = JTSGeomUtil.getPolygonal(inter);
			if(inter.isEmpty()) {
				toBeRemoved.add(f);
				continue;
			}

			//update geometry with intersection
			f.setGeometry((MultiPolygon)inter);
		}

		fs.removeAll(toBeRemoved);
	}



	private static void ensureTesselation_(Collection<Feature> units) {
		boolean b;

		//build spatial index
		Quadtree index = new Quadtree();
		for(Feature unit : units) index.insert(unit.getGeometry().getEnvelopeInternal(), unit);

		//int nb=0;
		//handle units one by one
		for(Feature unit : units) {
			//LOGGER.info(unit.id + " - " + 100.0*(nb++)/units.size());

			Geometry g1 = unit.getGeometry();

			//get units intersecting and correct their geometries
			@SuppressWarnings("unchecked")
			Collection<Feature> uis = index.query( g1.getEnvelopeInternal() );
			for(Feature ui : uis) {
				if(ui == unit) continue;
				Geometry g2 = ui.getGeometry();
				if(!g2.getEnvelopeInternal().intersects(g2.getEnvelopeInternal())) continue;

				//check overlap
				boolean overlap = false;
				try {
					overlap = g1.overlaps(g2);
				} catch (Exception e) {
					//overlaps.add(new Overlap(unit.id, null, -1, -1));
					continue;
				}
				if(!overlap) continue;

				Geometry inter = g2.intersection(g1);
				double interArea = inter.getArea();
				if(interArea == 0) continue;

				g2 = g2.difference(g1);
				if(g2==null || g2.isEmpty()) {
					LOGGER.trace("Unit "+ui.getID()+" disappeared when removing intersection with unit "+unit.getID()+" around "+ui.getGeometry().getCentroid().getCoordinate());
					continue;
				} else {
					//set new geometry - update index
					b = index.remove(ui.getGeometry().getEnvelopeInternal(), ui);
					if(!b) LOGGER.warn("Could not update index for "+ui.getID()+" while removing intersection of "+unit.getID()+" around "+ui.getGeometry().getCentroid().getCoordinate());
					ui.setGeometry((MultiPolygon)JTSGeomUtil.toMulti(g2));
					index.insert(ui.getGeometry().getEnvelopeInternal(), ui);
				}
			}
		}

	}




	/*
	public void ensureTesselation(String inputFile, String outputPath, String outputFile, boolean ensureMultiPolygon) {
		Collection<Feature> fs = SHPUtil.loadSHP(inputFile).fs;
		fs = ensureTesselation(fs);
		if(ensureMultiPolygon) for(Feature f : fs) f.setGeom((MultiPolygon)JTSGeomUtil.toMulti(f.getGeom()));
		SHPUtil.saveSHP(fs, outputPath, outputFile);
	}*/

	/*
	public static Collection<Feature> ensureTesselation(Collection<Feature> units) {
		Collection<Feature> out = Partition.runRecursively(units, new Operation() {
			public void run(Partition p) {
				LOGGER.info(p);
				ensureTesselation_(p.getFeatures());
			}}, 3000000, 15000, false);
		return out;
	}*/


	/*
	public static Collection<Feature> fixNoding(Collection<Feature> units, final double nodingResolution) {
		Collection<Feature> out = Partition.runRecursively(units, new Operation() {
			public void run(Partition p) {
				LOGGER.info(p);
				NodingUtil.fixNoding(NodingIssueType.PointPoint, p.getFeatures(), nodingResolution);
				NodingUtil.fixNoding(NodingIssueType.LinePoint, p.getFeatures(), nodingResolution);
			}}, 3000000, 15000, false);
		return out;
	}
	 */
}
