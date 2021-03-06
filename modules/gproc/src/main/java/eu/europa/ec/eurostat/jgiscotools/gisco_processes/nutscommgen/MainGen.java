/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.nutscommgen;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.CRSUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.TesselationGeneralisation;
import eu.europa.ec.eurostat.jgiscotools.util.CRSType;

/**
 * @author julien Gaffuri
 *
 */
public class MainGen {
	private final static Logger LOGGER = LogManager.getLogger(MainGen.class.getName());

	public static void main(String[] args) {
		LOGGER.info("Start");

		String basePath = "/home/juju/Bureau/GLOBAL_ADMIN_AREAS/";
		String in = basePath+"GLOBAL_ADMIN_AREAS_clean.shp";
		String out = basePath+"GLOBAL_ADMIN_AREAS_1M.shp";
		String idCol = "ID_";
		boolean tracePartitionning = true;

		int maxCoordinatesNumber = 1000000;
		int objMaxCoordinateNumber = 1000;




		//load data

		LOGGER.info("Load data from "+in);
		Collection<Feature> units = GeoData.getFeatures(in);
		LOGGER.info("Set ID");
		for(Feature f : units) f.setID( ""+f.getAttribute(idCol) );


		//quality check

		//LOGGER.info("Check identifier");
		//FeatureUtil.checkIdentfier(units, idCol);
		//LOGGER.info("Check quality");
		//TesselationQuality.checkQuality(units, 1e-6, basePath + "qc.csv", true, maxCoordinatesNumber, objMaxCoordinateNumber, tracePartitionning);


		//quality correction

		/*
		LOGGER.info("Fix quality");
		double eps = 1e-9; Envelope env = new Envelope(-180+eps, 180-eps, -90+eps, 90-eps);
		units = TesselationQuality.fixQuality(units, env, 1e-7, maxCoordinatesNumber, objMaxCoordinateNumber, tracePartitionning);

		LOGGER.info("Save output data in "+out);
		SHPUtil.saveSHP(units, basePath+"GLOBAL_ADMIN_AREAS_clean.shp", SHPUtil.getCRS(in));
		 */

		//generalisation


		LOGGER.info("Launch generalisation");
		double scaleDenominator = 1.0*1e6;
		int roundNb = 5;
		CRSType crsType = CRSUtil.getCRSType(GeoData.getCRS(in));
		units = TesselationGeneralisation.runGeneralisation(units, null, crsType, scaleDenominator, false, roundNb, maxCoordinatesNumber, objMaxCoordinateNumber);

		LOGGER.info("Save output data in "+out);
		GeoData.save(units, out, GeoData.getCRS(in));



		LOGGER.info("End");
	}

}
