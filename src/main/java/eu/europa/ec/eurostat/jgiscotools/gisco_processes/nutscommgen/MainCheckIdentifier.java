package eu.europa.ec.eurostat.jgiscotools.gisco_processes.nutscommgen;

import java.util.Collection;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;

public class MainCheckIdentifier {
	private final static Logger LOGGER = LogManager.getLogger(MainCheckIdentifier.class.getName());

	public static void main(String[] args) {
		LOGGER.info("Start");

		LOGGER.info("Load data");
		Collection<Feature> units = GeoData.getFeatures("/home/juju/Bureau/nuts_gene_data/nutsplus/NUTS_PLUS_01M_1904.shp");

		LOGGER.info("Check id");
		HashMap<String, Integer> ids = FeatureUtil.checkIdentfier(units, "NUTS_P_ID");
		System.out.println(ids);

		LOGGER.info("End");
	}

}
