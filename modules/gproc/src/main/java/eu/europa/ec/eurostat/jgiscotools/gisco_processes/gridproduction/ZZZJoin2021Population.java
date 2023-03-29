package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class ZZZJoin2021Population {
	static Logger logger = LogManager.getLogger(ZZZJoin2021Population.class.getName());

	public static String basePath = "/home/juju/Bureau/gisco/grid_dissemination/";

	//the different resolutions, in KM
	public static int[] resKMs = new int[] {100/*,50,20,10,5,2,1*/};


	//use: -Xms8g -Xmx24g
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		for(int resKM : resKMs) {

			logger.info("load 2021 population figures ");


			for(String gt : new String[]{"surf", "point"}) {
				logger.info("res " + resKM + " " + gt);

				logger.info("load intial GPKG ");
				Collection<Feature> fs = GeoData.getFeatures(basePath+"in/grid_"+resKM+"km_"+gt+".gpkg");
				logger.info(fs.size());

			}
		}

	}

}
