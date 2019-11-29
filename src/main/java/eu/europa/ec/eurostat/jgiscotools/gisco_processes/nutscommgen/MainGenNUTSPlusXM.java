/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.nutscommgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Point;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.AEdge;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.AFace;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.ATesselation;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.AUnit;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CEdgeFaceSize;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CEdgeGranularity;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CEdgeNoTriangle;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CEdgeValidity;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CEdgesFacesContainPoints;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CFaceContainPoints;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CFaceNoTriangle;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CFaceSize;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CFaceValidity;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CUnitContainPoints;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CUnitNoNarrowGaps;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.CUnitNoTriangle;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.TesselationGeneralisation;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.TesselationGeneralisationSpecification;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil.CRSType;

/**
 * @author julien Gaffuri
 *
 */
public class MainGenNUTSPlusXM {
	private final static Logger LOGGER = Logger.getLogger(MainGenNUTSPlusXM.class.getName());

	public static void main(String[] args) {
		LOGGER.info("Start");

		TesselationGeneralisation.tracePartitioning = false;
		String basePath = "/home/juju/Bureau/nuts_gene_data/nutsplus/";
		String inFile = basePath+"NUTS_PLUS_01M_1904_fixed.shp";

		/*
		LOGGER.info("Run quality check");
		double nodingResolution = 1e-8;
		TesselationQuality.checkQuality(SHPUtil.loadSHP(inFile).fs, nodingResolution, basePath+"eval_units.csv", true, 3000000, 15000, false);
		 */
		/*
		LOGGER.info("Fix quality");
		double nodingResolution = 1e-8;
		Collection<Feature> units = SHPUtil.loadSHP(inFile).fs;
		for(Feature f : units) if(f.getProperties().get("NUTS_P_ID") != null) f.id = ""+f.getProperties().get("NUTS_P_ID");
		units = TesselationQuality.fixQuality(units, null, nodingResolution, 3000000, 15000);
		LOGGER.info("Save");
		SHPUtil.saveSHP(units, basePath+"NUTS_PLUS_01M_1904_fixed.shp", SHPUtil.getCRS(inFile));
		if (true) return;
		 */

		LOGGER.info("Load pts data");
		final HashMap<String, Collection<Point>> ptsData = loadPoints(basePath);

		for(double s : new double[]{3,10,20,60,1}) {
			double scaleDenominator = s*1e6;

			//define specifications
			TesselationGeneralisationSpecification specs = new TesselationGeneralisationSpecification(scaleDenominator, CRSType.GEOG) {
				public void setUnitConstraints(ATesselation t) {
					for(AUnit a : t.aUnits) {
						a.addConstraint(new CUnitNoNarrowGaps(a, res.getSeparationDistanceMeter(), getNodingResolution(), quad, preserveAllUnits, preserveIfPointsInIt).setPriority(10));
						//a.addConstraint(new CUnitNoNarrowParts(a, res.getSeparationDistanceMeter(), getNodingResolution(), quad, preserveAllUnits, preserveIfPointsInIt).setPriority(9));
						a.addConstraint(new CUnitContainPoints(a));
						a.addConstraint(new CUnitNoTriangle(a));
					}
				}

				public void setTopologicalConstraints(ATesselation t) {
					for(AFace a : t.aFaces) {
						a.addConstraint(new CFaceSize(a, 0.1*res.getPerceptionSizeSqMeter(), 3*res.getPerceptionSizeSqMeter(), res.getPerceptionSizeSqMeter(), preserveAllUnits, preserveIfPointsInIt).setPriority(2));
						a.addConstraint(new CFaceValidity(a));
						a.addConstraint(new CFaceContainPoints(a));
						a.addConstraint(new CFaceNoTriangle(a));
						//difference here
						a.addConstraint(new CFaceEEZInLand(a).setPriority(10));
					}
					for(AEdge a : t.aEdges) {
						a.addConstraint(new CEdgeGranularity(a, 2*res.getResolutionM()));
						a.addConstraint(new CEdgeValidity(a));
						a.addConstraint(new CEdgeNoTriangle(a));
						a.addConstraint(new CEdgeFaceSize(a).setImportance(6));
						a.addConstraint(new CEdgesFacesContainPoints(a));
					}
				}
			};

			LOGGER.info("Load data for "+((int)s)+"M generalisation");
			Collection<Feature> units = SHPUtil.getFeatures(inFile);
			for(Feature f : units) if(f.getAttribute("NUTS_P_ID") != null) f.setID( ""+f.getAttribute("NUTS_P_ID") );

			LOGGER.info("Launch generalisation for "+((int)s)+"M");
			int roundNb = 8;
			units = TesselationGeneralisation.runGeneralisation(units, ptsData, specs, roundNb, 1000000, 1000);

			LOGGER.info("Save output data");
			SHPUtil.saveSHP(units, basePath + "out/NUTS_PLUS_"+((int)s)+"M.shp", SHPUtil.getCRS(inFile));
		}
		LOGGER.info("End");
	}

	private static HashMap<String,Collection<Point>> loadPoints(String basePath) {
		HashMap<String,Collection<Point>> index = new HashMap<String,Collection<Point>>();
		for(String file : new String[] {"GISCO.CNTR_CAPT_PT_2013","NUTS_PLUS_01M_1904_Points"})
			for(Feature f : SHPUtil.getFeatures(basePath+file+".shp")) {
				String id = (String)f.getAttribute("CNTR_ID");
				if(id == null) id = (String)f.getAttribute("NUTS_P_ID");
				if("".equals(id)) continue;
				Collection<Point> data = index.get(id);
				if(data == null) { data=new ArrayList<Point>(); index.put(id, data); }
				data.add((Point) f.getDefaultGeometry());
			}
		return index;
	}

}
