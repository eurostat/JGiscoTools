package eu.europa.ec.eurostat.jgiscotools.gisco_processes.test;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.TesselationQuality;

/**
 * @author julien Gaffuri
 *
 */
public class TestQualityCheck {
	private final static Logger LOGGER = LogManager.getLogger(TestQualityCheck.class.getName());

	public static void main(String[] args) {
		System.out.println("Start");

		LOGGER.info("Load data");
		Collection<Feature> units = SHPUtil.getFeatures("src/test/resources/testTesselationGeneralisation.shp");
		for(Feature unit : units) unit.setID( unit.getAttribute("id").toString() );

		LOGGER.info("Run quality check");
		final double nodingResolution = 1e-7;
		TesselationQuality.checkQuality(units, nodingResolution, "target/eval_units.csv", true, 3000000, 15000, true);

		/*
		LOGGER.info("Check identifier");
		HashMap<String, Integer> ids = FeatureUtil.checkIdentfier(units, "id");
		System.out.println(ids);
		 */

		System.out.println("End");
	}

}
