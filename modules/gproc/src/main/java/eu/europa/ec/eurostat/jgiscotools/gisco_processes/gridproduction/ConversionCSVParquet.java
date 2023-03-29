package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class ConversionCSVParquet {
	static Logger logger = LogManager.getLogger(ConversionCSVParquet.class.getName());

	public static String basePath = "/home/juju/Bureau/gisco/grid_dissemination/";
	public static String basePathPop = "/home/juju/Bureau/gisco/grid_pop/";

	//the different resolutions, in KM
	public static int[] resKMs = new int[] {100,50,20,10,5,2,1};


	//use: -Xms8g -Xmx24g
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//get crs
		CoordinateReferenceSystem crs = GeoData.getCRS(basePath+"in/grid_100km_point.gpkg");

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

			//System.out.println(pop2021Ind);

			for(String gt : new String[]{"surf", "point"}) {
				logger.info("res " + resKM + "km " + gt);

				logger.info("load intial GPKG");
				Collection<Feature> fs = GeoData.getFeatures(basePath+"in/grid_"+resKM+"km_"+gt+".gpkg");
				logger.info(fs.size());
				//System.out.println(fs.iterator().next().getAttributes().keySet());
				//[DIST_BORD, TOT_P_2018, TOT_P_2006, GRD_ID, TOT_P_2011, Y_LLC, CNTR_ID, NUTS2016_3, NUTS2016_2, NUTS2016_1, NUTS2016_0, LAND_PC, X_LLC, NUTS2021_3, NUTS2021_2, DIST_COAST, NUTS2021_1, NUTS2021_0]

				logger.info("Add 2021 population");
				for(Feature f : fs) {
					//get cell id
					String gid = (String)f.getAttribute("GRD_ID");
					if(gid == null || gid.isEmpty())
						System.err.println("No grid id");

					//get 2021 population using index
					Integer p = pop2021Ind.get(gid);

					//add new attribute
					f.setAttribute("TOT_P_2021", p==null?0 : p.intValue());
				}

				logger.info(fs.size());
				//System.out.println(fs.iterator().next().getAttributes().keySet());

				GeoData.save(fs, basePath+"out/grid_"+resKM+"km_"+gt+".gpkg", crs);
			}
		}

		logger.info("End");
	}

}
