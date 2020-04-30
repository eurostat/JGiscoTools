/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.test;

import java.util.Collection;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Point;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.CRSUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.TesselationGeneralisation;
import eu.europa.ec.eurostat.jgiscotools.util.CRSType;

/**
 * @author julien Gaffuri
 *
 */
public class TestTesselationGeneralisation {
	private final static Logger LOGGER = LogManager.getLogger(TestTesselationGeneralisation.class.getName());

	//TODO deployment
	//TODO include noding as a constraint at tesselation level ???
	//TODO implement/fix narrow corridor removal
	//TODO narrow gaps/parts: make a single? compute geometry. check effect on neigbours/points
	//TODO handle geographical coordinates
	//TODO test with large scale changes - fix issues
	//TODO removal of large elongated faces/holes: face size constraint: take into account shape - use erosion? use width evaluation method?
	//TODO face collapse algorithm - for small and compact faces only
	//TODO edge size constraint: fix it!

	public static void main(String[] args) {
		LOGGER.info("Start");

		LOGGER.info("Load data");
		String in = "src/test/resources/testTesselationGeneralisation.shp";
		Collection<Feature> units = GeoData.getFeatures(in);
		for(Feature unit : units) unit.setID( unit.getAttribute("id").toString() );
		Collection<Feature> points = GeoData.getFeatures("src/test/resources/testTesselationGeneralisationPoints.shp");
		HashMap<String, Collection<Point>> pointsInd = TesselationGeneralisation.indexPoints(points, "id");

		LOGGER.info("Launch generalisation");
		double scaleDenominator = 1e6; int roundNb = 10;
		CRSType crsType = CRSUtil.getCRSType(GeoData.getCRS(in));
		units = TesselationGeneralisation.runGeneralisation(units, pointsInd, crsType, scaleDenominator, false, roundNb, 1000000, 1000);

		LOGGER.info("Save output data");
		GeoData.save(units, "target/testTesselationGeneralisation_out.shp", GeoData.getCRS(in));

		LOGGER.info("End");
	}

}
