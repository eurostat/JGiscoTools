/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction.Aggregator;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler.Format;
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

	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");

		//prepare();
		//aggregate();

		/*
		//tiling(Format.CSV, null, 128);
		//tiling(Format.CSV, null, 256);
		//tiling(Format.PARQUET, CompressionCodecName.UNCOMPRESSED, 128);
		//tiling(Format.PARQUET, CompressionCodecName.UNCOMPRESSED, 256);
		tiling(Format.PARQUET, CompressionCodecName.SNAPPY, 128);
		tiling(Format.PARQUET, CompressionCodecName.SNAPPY, 256);
		tiling(Format.PARQUET, CompressionCodecName.GZIP, 128);
		tiling(Format.PARQUET, CompressionCodecName.GZIP, 256);
		tiling(Format.PARQUET, CompressionCodecName.ZSTD, 128);
		tiling(Format.PARQUET, CompressionCodecName.ZSTD, 256);
		*/
		
		//tiling(Format.PARQUET, CompressionCodecName., 128);
		//tiling(Format.PARQUET, CompressionCodecName.GZIP, 128);

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
		fs.clear();
		fs = null;

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
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, aggMap );

			logger.info("Save " + out.size());
			CSVUtil.save(out, outPath + "pop_" + res + "m.csv");
		}

	}



	// tile all resolutions
	private static void tiling(Format format, CompressionCodecName comp, int nbp) {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = outPath + "pop_" + res + "m.csv";

			logger.info("Load");
			ArrayList<Map<String, String>> cells = CSVUtil.load(f);
			logger.info(cells.size());

			logger.info("Change year field names");
			for(Map<String, String> c : cells) {
				c.put("pop2006", c.get("2006"));
				c.remove("2006");
				c.put("pop2011", c.get("2011"));
				c.remove("2011");
				c.put("pop2018", c.get("2018"));
				c.remove("2018");
			}

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), nbp);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = outPath + "tiled" + format + comp + nbp + "/" + res + "m";
			/*gst.save(outpath, format, "{\"namespace\": \"ns\","
					+ "\"type\": \"record\"," //set as record
					+ "\"name\": \"na\","
					+ "\"fields\": ["
					+ "{\"name\": \"id\", \"type\": \"int\"}" //required
					+ ",{\"name\": \"x\", \"type\": \"int\"}"
					+ ",{\"name\": \"y\", \"type\": \"int\"}"
					+ ",{\"name\": \"pop2006\", \"type\": \"int\"}"
					+ ",{\"name\": \"pop2011\", \"type\": \"int\"}"
					+ ",{\"name\": \"pop2018\", \"type\": \"int\"}"
					+ ",{\"name\": \"CNTR_ID\", \"type\": \"string\"}"
					+ " ]}",
					comp,
					true
					);*/
			gst.save(outpath, format, "ddb", comp, true);
			gst.saveTilingInfoJSON(outpath, format, "Europe population resolution " + res + "m");

		}
	}

}
