package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class ZZZJoin2021Population {
	static Logger logger = LogManager.getLogger(ZZZJoin2021Population.class.getName());

	public static String basePath = "/home/juju/Bureau/gisco/grid_dissemination/";
	public static String basePathPop = "/home/juju/Bureau/gisco/grid_pop/";

	//the different resolutions, in KM
	public static int[] resKMs = new int[] {100/*,50,20,10,5,2,1*/};


	//use: -Xms8g -Xmx24g
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		for(int resKM : resKMs) {

			logger.info("Load 2021 population");
			ArrayList<Map<String, String>> pop2021 = CSVUtil.load(basePathPop + "pop_"+resKM+"000m.csv");
			logger.info(pop2021.size());
			//logger.info(pop2021.get(0).keySet());
			//CNTR_ID,TOT_P_2018,TOT_P_2006,GRD_ID,TOT_P_2011,TOT_P_2021

			logger.info("Index");
			Map<String, Integer> pop2021Ind = new HashMap<>();
			for(Map<String, String> d:pop2021) {
				int p = Integer.parseInt(d.get("TOT_P_2021"));
				pop2021Ind.put(d.get("GRD_ID"), p);
			}


			for(String gt : new String[]{"surf", "point"}) {
				logger.info("res " + resKM + " " + gt);

				logger.info("load intial GPKG ");
				Collection<Feature> fs = GeoData.getFeatures(basePath+"in/grid_"+resKM+"km_"+gt+".gpkg");
				logger.info(fs.size());

			}
		}

	}

}
