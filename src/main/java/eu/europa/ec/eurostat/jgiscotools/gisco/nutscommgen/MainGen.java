/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco.nutscommgen;

import java.util.Collection;

import org.apache.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.datamodel.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.TesselationGeneralisation;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil.CRSType;

/**
 * @author julien Gaffuri
 *
 */
public class MainGen {
	private final static Logger LOGGER = Logger.getLogger(MainGen.class.getName());

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
		Collection<Feature> units = SHPUtil.loadSHP(in).fs;
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
		CRSType crsType = SHPUtil.getCRSType(in);
		units = TesselationGeneralisation.runGeneralisation(units, null, crsType, scaleDenominator, roundNb, maxCoordinatesNumber, objMaxCoordinateNumber);

		LOGGER.info("Save output data in "+out);
		SHPUtil.saveSHP(units, out, SHPUtil.getCRS(in));
		 


		LOGGER.info("End");
	}

}
