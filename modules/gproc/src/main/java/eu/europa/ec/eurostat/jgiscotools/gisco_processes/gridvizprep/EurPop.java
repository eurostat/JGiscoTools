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
public class EurPop {
	static Logger logger = LogManager.getLogger(EurPop.class.getName());


	//the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000 };
	private static String basePath = "/home/juju/Bureau/gisco/";
	private static String outPath = basePath + "grid_pop/";

	//2006-2001-2018


	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");

		//prepare();
		aggregate();

		logger.info("End");
	}


	private static void prepare() {

		ArrayList<Feature> fs = GeoData.getFeatures(basePath + "grids/grid_1km_surf.gpkg");
		logger.info(fs.size() + " loaded");
		logger.info(fs.get(0).getAttributes().keySet());
		//2022-12-15 15:48:02 INFO  EurPop:73 - [DIST_BORD, TOT_P_2018, TOT_P_2006, GRD_ID, TOT_P_2011, Y_LLC, CNTR_ID, NUTS2016_3, NUTS2016_2, NUTS2016_1, NUTS2016_0, LAND_PC, X_LLC, NUTS2021_3, NUTS2021_2, DIST_COAST, NUTS2021_1, NUTS2021_0]

		ArrayList<Map<String, String>> data = new ArrayList<Map<String,String>>();
		for(Feature f : fs) {
			Map<String,String> m = new HashMap<String, String>();

			double lpc = Double.parseDouble(f.getAttribute("LAND_PC").toString());
			if(lpc == 0) continue;
			//String cid = f.getAttribute("CNTR_ID").toString();
			//if(cid.isEmpty()) System.out.println("aaa");

			int p2006 = (int) Double.parseDouble( f.getAttribute("TOT_P_2006").toString() );
			int p2011 = (int) Double.parseDouble( f.getAttribute("TOT_P_2011").toString() );
			int p2018 = (int) Double.parseDouble( f.getAttribute("TOT_P_2018").toString() );
			if(p2006 == 0 && p2011 == 0 && p2018 == 0) continue;

			m.put("GRD_ID", f.getAttribute("GRD_ID").toString());
			m.put("CNTR_ID", f.getAttribute("CNTR_ID").toString());
			m.put("2006", p2006+"");
			m.put("2011", p2011+"");
			m.put("2018", p2018+"");
			data.add(m);
		}

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save");
		CSVUtil.save(data, outPath + "prepared.csv");
	}



	private static void aggregate() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(outPath + "prepared.csv");
		logger.info(data.size());

		//define aggregations
		Map<String, Aggregator> aggMap = new HashMap<String, Aggregator>();
		aggMap.put("2006", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("2011", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("2018", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("CNTR_ID", GridMultiResolutionProduction.getCodesAggregator("-"));

		for (int res : resolutions) {
			logger.info("Aggregate " + res + "m");

			//aggregate
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregationA(data, "GRD_ID", res, aggMap );

			logger.info("Save " + out.size());
			CSVUtil.save(out, outPath + "pop_" + res + "m.csv");
		}

	}




	// tile all resolutions
	private static void tiling() {

		/*
		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String in;

			in = "DLT";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsDLT = GeoTiffUtil.loadCells(
					basePath +"forest_"+in+"_"+ res + ".tif",
					new String[] {"dlt"},
					(v)->{ return v[0]<=0 || v[0]>=3; }
					);
			logger.info(cellsDLT.size());

			in = "TCD";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsTCD = GeoTiffUtil.loadCells(
					basePath +"forest_"+in+"_"+ res + ".tif",
					new String[] {"tcd"},
					(v)->{ return v[0]<=0 || v[0]>100; }
					);
			logger.info(cellsTCD.size());

			logger.info("Join");
			ArrayList<Map<String, String>> cells = CSVUtil.joinBothSides("GRD_ID", cellsDLT, cellsTCD, "", false);
			logger.info(cells.size());

			logger.info(cells.get(0).keySet());


			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "out/" + res + "m";
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "Forest - copernicus - TCD DLT " + res + "m");

		}*/
	}


}
