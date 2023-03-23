/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction.Aggregator;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author julien Gaffuri
 *
 */
public class EurPopCensus2021ForJoe {
	static Logger logger = LogManager.getLogger(EurPopCensus2021ForJoe.class.getName());

	private static String basePath = "/home/juju/Bureau/gisco/";
	private static String outPath = basePath + "grid_pop/";

	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");


		logger.info("Load GPKG 2006-2011-2018 data");
		ArrayList<Feature> fs = GeoData.getFeatures(basePath + "grids/grid_1km_surf.gpkg");
		logger.info(fs.size() + " loaded");
		//logger.info(fs.get(0).getAttributes().keySet());
		//2022-12-15 15:48:02 INFO  EurPop:73 - [DIST_BORD, TOT_P_2018, TOT_P_2006, GRD_ID, TOT_P_2011, Y_LLC, CNTR_ID, NUTS2016_3, NUTS2016_2, NUTS2016_1, NUTS2016_0, LAND_PC, X_LLC, NUTS2021_3, NUTS2021_2, DIST_COAST, NUTS2021_1, NUTS2021_0]

		logger.info("Index CNT and LAND_PC");
		Map<String, String> cntInd = new HashMap<String,String>();
		Map<String, Double> landInd = new HashMap<String,Double>();
		for(Feature f : fs) {
			String gid = f.getAttribute("GRD_ID").toString();
			double lpc = Double.parseDouble(f.getAttribute("LAND_PC").toString());
			String cid = f.getAttribute("CNTR_ID").toString();
			cntInd.put(gid, cid);
			landInd.put(gid, lpc);
		}
		fs.clear(); fs = null;


		logger.info("Load 2021 GPKG data");
		fs = GeoData.getFeatures(basePath + "grids/CENSUS_2021.gpkg");
		logger.info(fs.size() + " loaded");
		logger.info(fs.get(0).getAttributes().keySet());

		ArrayList<Map<String, String>> data = new ArrayList<Map<String,String>>();
		for(Feature f : fs) {
			Map<String,String> m = new HashMap<String, String>();

			String gid = f.getAttribute("GRD_ID").toString();

			double lpc = landInd.get(gid);
			if(lpc == 0) continue;

			String cid = cntInd.get(gid);

			if (cid == "IS") continue;

			if (cid == "UK") continue;
			if (cid == "IE-UK") continue;
			if (cid == "UK-IE") continue;

			if (cid == "BA") continue;
			if (cid == "RS") continue;
			if (cid == "BA-RS") continue;
			if (cid == "RS-BA") continue;
			if (cid == "ME") continue;
			if (cid == "BA-ME") continue;
			if (cid == "ME-BA") continue;
			if (cid == "ME-RS") continue;
			if (cid == "BA-ME-RS") continue;
			if (cid == "AL") continue;
			if (cid == "AL-ME") continue;
			if (cid == "AL-RS") continue;

			if (cid == "MK") continue;
			if (cid == "MK-RS") continue;
			if (cid == "AL-MK") continue;

			if (cid == "IM") continue;
			if (cid == "SM") continue;
			if (cid == "VA") continue;
			if (cid == "MC") continue;

			int p2021 = (int) Double.parseDouble( f.getAttribute("OBS_VALUE_T").toString() );
			//if(p2021 == 0) continue;
			m.put("TOT_P_2021", p2021+"");

			m.put("GRD_ID", gid);
			data.add(m);
		}
		fs.clear(); fs = null;
		cntInd = null;
		landInd = null;


		logger.info(data.size());
		logger.info(data.get(0).keySet());



		int res = 5000;
		logger.info("Aggregate " + res + "m");

		//define aggregations
		Map<String, Aggregator> aggMap = new HashMap<String, Aggregator>();
		aggMap.put("TOT_P_2021", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("CNTR_ID", GridMultiResolutionProduction.getCodesAggregator("-"));

		//aggregate
		ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, aggMap );

		logger.info("Save " + out.size());
		CSVUtil.save(out, outPath + "X_pop_" + res + "m.csv");

		logger.info("End");
	}

}
