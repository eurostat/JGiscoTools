package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction.Aggregator;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class EurAccessibility {
	static Logger logger = LogManager.getLogger(EurAccessibility.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/";

	// TOT_P	avg_time_nearest_ep	avg_time_nearest_h

	// -Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		//prepareHealth();
		//prepareEducPrim();
		//join();
		//aggregate();
		tiling();
		logger.info("End");
	}

	private static void prepareEducPrim() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(
				basePath + "education/input/GRID_PRIMARY_081122.csv",
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(";".charAt(0)));
		logger.info(data.size());
		logger.info(data.get(0).keySet());
		//[Total_Trav, GRD_ID]

		logger.info("Structure");
		for (Map<String, String> d : data) {
			String ts = d.get("Total_Trav").replace(",", ".");
			double t = Double.parseDouble(ts);
			d.put("Total_Trav", (Math.ceil(t*10)/10) + "");
		}

		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "Total_Trav", "avg_time_nearest");
		//CSVUtil.renameColumn(data, "ID", "GRD_ID");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save");
		CSVUtil.save(data, basePath + "prepared_educ_prim.csv");

	}

	private static void prepareHealth() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(
				basePath + "health/input/avg_time_nearest_healthcare_1205.csv",
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(";".charAt(0)));
		logger.info(data.size());
		logger.info(data.get(0).keySet());
		// [GRD_ID;Total_Trav]

		logger.info("Structure");
		for (Map<String, String> d : data) {
			String ts = d.get("Total_Trav").replace(",", ".");
			double t = Double.parseDouble(ts);
			d.put("Total_Trav", Math.ceil(t) + "");

		}

		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "Total_Trav", "avg_time_nearest");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save");
		CSVUtil.save(data, basePath + "prepared_health.csv");

	}



	private static void preparePop(int year) {
		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil
				.load("/home/juju/Bureau/gisco/grid_pop/pop_grid_" + year + "_1km.csv");
		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save");
		CSVUtil.save(data, basePath + "pop" + year + ".csv");
	}


	private static void join() {

		List<Map<String, String>> data = CSVUtil.load("/home/juju/Bureau/gisco/grid_pop/pop_1000m.csv");
		logger.info("pop: " + data.size());
		CSVUtil.removeColumn(data, "2006", "2011");
		CSVUtil.renameColumn(data, "2018", "TOT_P");
		logger.info(data.get(0).keySet());

		ArrayList<Map<String, String>> d = CSVUtil.load(basePath + "prepared_health.csv");
		logger.info("join health: " + d.size());
		CSVUtil.renameColumn(d, "avg_time_nearest", "avg_time_nearest_h");
		logger.info(d.get(0).keySet());
		//CSVUtil.join(data, "GRD_ID", d, "GRD_ID", false);
		data = CSVUtil.joinBothSides("GRD_ID", data, d, "", true);

		d = CSVUtil.load(basePath + "prepared_educ_prim.csv");
		logger.info("join educ: " + d.size());
		CSVUtil.renameColumn(d, "avg_time_nearest", "avg_time_nearest_ep");
		logger.info(d.get(0).keySet());
		//CSVUtil.join(data, "GRD_ID", d, "GRD_ID", false);
		data = CSVUtil.joinBothSides("GRD_ID", data, d, "", true);

		logger.info("out: " + data.size());
		logger.info(data.get(0).keySet());

		logger.info("save " + data.size());
		CSVUtil.save(data, basePath + "prepared.csv");

	}




	// derive resolutions
	private static void aggregate() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "prepared.csv");
		logger.info(data.size());

		//define aggregations
		Map<String, Aggregator> aggMap = new HashMap<String, Aggregator>();
		Collection<String> vti = new ArrayList<>(); vti.add("");
		aggMap.put("TOT_P", GridMultiResolutionProduction.getSumAggregator(10000, vti));
		aggMap.put("avg_time_nearest_h", GridMultiResolutionProduction.getAverageAggregator(10000, vti));
		aggMap.put("avg_time_nearest_ep", GridMultiResolutionProduction.getAverageAggregator(10000, vti));
		aggMap.put("CNTR_ID", GridMultiResolutionProduction.getCodesAggregator("-"));

		for (int res : resolutions) {
			logger.info("Aggregate " + res + "m");

			//aggregate
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, aggMap );

			// round + NA
			for (Map<String, String> d : out) {
				String ts = d.get("avg_time_nearest_h");
				if(ts.equals("NaN")) {
					d.put("avg_time_nearest_h", "NA");
				} else {
					double t = Double.parseDouble(ts);
					d.put("avg_time_nearest_h", ((int) Math.ceil(t)) + "");
				}

				ts = d.get("avg_time_nearest_ep");
				if(ts.equals("NaN")) {
					d.put("avg_time_nearest_ep", "NA");
				} else {
					double t = Double.parseDouble(ts);
					d.put("avg_time_nearest_ep", (Math.ceil(t*10)/10) + "");
				}
			}

			logger.info("Save " + out.size());
			CSVUtil.save(out, basePath + "agg_" + res + ".csv");
		}
	}

	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + "agg_" + res + ".csv";

			logger.info("Load");
			ArrayList<Map<String, String>> cells = CSVUtil.load(f);
			logger.info(cells.size());

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "tiled/" + res + "m";
			gst.save(outpath, GridTiler.Format.CSV);
			gst.saveTilingInfoJSON(outpath, GridTiler.Format.CSV	, "Euraccess resolution " + res + "m");

		}
	}

	/*
	 * private static void check(int year) { ArrayList<Map<String, String>> dataPop
	 * = CSVUtil.load(basePath + "pop"+year+".csv"); logger.info("pop: " +
	 * dataPop.size()); ArrayList<Map<String, String>> dataAcc =
	 * CSVUtil.load(basePath + "prepared_health.csv"); logger.info("acc: " +
	 * dataAcc.size());
	 * 
	 * int nb = 0; for (Map<String, String> d : dataAcc) { String id =
	 * d.get("GRD_ID"); boolean found = false; for (Map<String, String> d2 :
	 * dataPop) { String id2 = d2.get("GRD_ID"); if(!id.equals(id2)) continue; found
	 * = true; break; } if(!found) nb++; } System.out.println(year + " " + nb); }
	 */

	/*
	 * public static void main(String[] args) {
	 * 
	 * 
	 * System.out.println("load"); CSVFormat cf =
	 * CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';');
	 * ArrayList<Map<String, String>> d = CSVUtil.load(
	 * "E:\\workspace\\basic_services_accessibility\\accessibility_output\\avg_time_primary_educ.csv",
	 * cf );
	 * 
	 * System.out.println(d.size());
	 * 
	 * CSVUtil.removeColumn(d, "TOT_P_2018");
	 * 
	 * for(Map<String, String> d_ : d) { String id = d_.get("GRD_ID"); GridCell gc =
	 * new GridCell(id); d_.put("x", gc.getLowerLeftCornerPositionX()/1000+"");
	 * d_.put("y", gc.getLowerLeftCornerPositionY()/1000+""); d_.put("avg_time",
	 * ((int)Double.parseDouble(d_.get("AVG_TIME").replace(',','.')))+""); }
	 * 
	 * CSVUtil.removeColumn(d, "GRD_ID", "AVG_TIME");
	 * 
	 * System.out.println("save"); CSVUtil.save(d,
	 * "E:\\users\\gaffuju\\eclipse_workspace\\gridviz\\assets\\csv\\Europe\\1km\\accs.csv"
	 * ); }
	 */

}
