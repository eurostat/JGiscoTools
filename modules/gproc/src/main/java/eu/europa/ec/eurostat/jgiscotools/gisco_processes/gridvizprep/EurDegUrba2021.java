package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction.Aggregator;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class EurDegUrba2021 {
	static Logger logger = LogManager.getLogger(EurDegUrba2021.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000 };
	private static String basePath = "/home/juju/Bureau/gisco/";
	private static String basePathDU = basePath + "degurba/";
	private static String outPath = basePathDU + "out/";

	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");

		prepare();
		//aggregate();

		logger.info("End");
	}

	private static void prepare() {

		logger.info("Load degurba");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePathDU + "2021_DGURBA_LV2v2_dens.csv");
		logger.info(data.size());
		logger.info(data.get(0).keySet());
		// [﻿GRD_ID, OBS_VALUE_T, gridcode]

		logger.info("Rename columns");
		CSVUtil.renameColumn(data, "OBS_VALUE_T", "TOT_P_2021");
		logger.info(data.get(0).keySet());


		logger.info("filter- remove non populated cells");
		logger.info(data.size());
		Stream<Map<String, String>> s = data.stream().filter(d -> {
			int pop = Integer.parseInt(d.get("TOT_P_2021"));
			if(pop!=0) return true;
			return false;
		});
		data = new ArrayList<>(s.collect(Collectors.toList()));
		s.close(); s = null;
		logger.info(data.size());
		logger.info(data.get(0).keySet());


		logger.info("Restructure columns as counts");
		for (Map<String, String> d : data) {
			String du = d.get("gridcode");

			//if(!du.equals("11") && !du.equals("12") && !du.equals("13") && !du.equals("21") && !du.equals("22") && !du.equals("23") && !du.equals("30") && !du.equals(""))
			//	System.out.println(du);

			d.put("uc", du.equals("30")? "1" : "0");
			d.put("du", du.equals("23")? "1" : "0");
			d.put("sdu", du.equals("22")? "1" : "0");
			d.put("sbu", du.equals("21")? "1" : "0");
			d.put("r", du.equals("13")? "1" : "0");
			d.put("lr", du.equals("12")? "1" : "0");
			d.put("vlr", du.equals("11")? "1" : "0");
			d.put("NA", du.equals("")? "1" : "0");
			d.put("TOT", "1");

			//clean
			d.remove("gridcode");
		}

		logger.info(data.get(0).keySet());


		logger.info("Load GPKG 2006-2011-2018 data");
		ArrayList<Feature> fs = GeoData.getFeatures(basePath + "grids/grid_1km_surf.gpkg");
		logger.info(fs.size() + " loaded");
		//logger.info(fs.get(0).getAttributes().keySet());
		//2022-12-15 15:48:02 INFO  EurPop:73 - [DIST_BORD, TOT_P_2018, TOT_P_2006, GRD_ID, TOT_P_2011, Y_LLC, CNTR_ID, NUTS2016_3, NUTS2016_2, NUTS2016_1, NUTS2016_0, LAND_PC, X_LLC, NUTS2021_3, NUTS2021_2, DIST_COAST, NUTS2021_1, NUTS2021_0]

		logger.info("Index CNT");
		Map<String, String> cntInd = new HashMap<String,String>();
		for(Feature f : fs) {
			String gid = f.getAttribute("GRD_ID").toString();
			String cid = f.getAttribute("CNTR_ID").toString();
			cntInd.put(gid, cid);
		}
		fs.clear(); fs = null;

		logger.info("Join CNT");
		for(Map<String, String> d:data) {
			String gid = d.get("GRD_ID");
			String cnt = cntInd.get(gid);
			if(cnt == null)
				System.err.println("No cnt id for "+gid);
			d.put("CNTR_ID", cnt);
		}

		logger.info("save");
		CSVUtil.save(data, outPath + "2021_degurba2_1km_prepared.csv");
	}

	private static void aggregate() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(outPath + "2021_degurba2_1km_prepared.csv");
		logger.info(data.size());

		//define aggregations
		Map<String, Aggregator> aggMap = new HashMap<String, Aggregator>();
		//[CNTR_ID, du, r, NA, sdu, lr, sbu, TOT, GRD_ID, vlr, uc, TOT_P]
		aggMap.put("CNTR_ID", GridMultiResolutionProduction.getCodesAggregator("-"));
		aggMap.put("du", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("r", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("NA", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("sdu", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("lr", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("sbu", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("TOT", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("vlr", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("uc", GridMultiResolutionProduction.getSumAggregator(10000, null));
		aggMap.put("TOT_P_2021", GridMultiResolutionProduction.getSumAggregator(10000, null));

		for (int res : resolutions) {
			logger.info("Aggregate " + res + "m");
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, aggMap );

			logger.info("Save " + out.size());
			CSVUtil.save(out, outPath + "degurba2_" + res + "m.csv");
		}

	}




	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = outPath + "degurba2_" + res + "m.csv";

			logger.info("Load");
			ArrayList<Map<String, String>> cells = CSVUtil.load(f);
			logger.info(cells.size());

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "out/tiled/" + res + "m";
			gst.save(outpath, GridTiler.Format.CSV, null, null, false);
			gst.saveTilingInfoJSON(outpath, GridTiler.Format.CSV, "degurba level 2 2021 resolution " + res + "m");

		}
	}

}
