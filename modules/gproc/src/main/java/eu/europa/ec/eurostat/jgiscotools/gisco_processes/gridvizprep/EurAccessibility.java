package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

public class EurAccessibility {
	static Logger logger = LogManager.getLogger(EurAccessibility.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		// preparePop(2018);
		// prepareHealth();
		// prepareEduc();
		// check(2006);check(2011);check(2018);
		join(2018);
		// aggregate();
		// tiling();
		logger.info("End");
	}

	private static void join(int year) {

		ArrayList<Map<String, String>> data;

		data = CSVUtil.load(basePath + "pop" + year + ".csv");
		logger.info("pop: " + data.size());
		HashMap<String, Map<String, String>> iPop = Util.index(data, "GRD_ID");
		logger.info("popI: " + iPop.keySet().size());

		data = CSVUtil.load(basePath + "prepared_health.csv");
		logger.info("acc: " + data.size());
		HashMap<String, Map<String, String>> iHealth = Util.index(data, "GRD_ID");
		logger.info("accI: " + iHealth.keySet().size());

		data = null;

		// get all ids
		Set<String> ids = new HashSet<String>();
		ids.addAll(iPop.keySet());
		ids.addAll(iHealth.keySet());
		logger.info("Ids: " + ids.size());

		// go through ids
		ArrayList<Map<String, String>> out = new ArrayList<Map<String, String>>();
		for (String id : ids) {
			Map<String, String> d = new HashMap<String, String>();
			d.put("GRD_ID", id);

			String pop = iPop.get(id) == null ? "NA" : iPop.get(id).get("TOT_P");
			d.put("TOT_P", pop);

			String atnh = iHealth.get(id) == null ? "NA" : iHealth.get(id).get("avg_time_nearest");
			d.put("avg_time_nearest_h", atnh);

			out.add(d);
		}

		logger.info("save " + out.size());
		CSVUtil.save(out, basePath + "prepared.csv");
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

	// derive resolutions
	private static void aggregate() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "prepared.csv");
		logger.info(data.size());

		for (int res : resolutions) {
			logger.info("Aggregate " + res + "m");
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res,
					10000, Set.of("avg_time_nearest"));
			logger.info(out.size());

			// round
			for (Map<String, String> d : out) {
				String ts = d.get("avg_time_nearest");
				double t = Double.parseDouble(ts);
				d.put("avg_time_nearest", ((int) Math.ceil(t)) + "");
			}

			logger.info("Save");
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
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "Euraccess resolution " + res + "m");

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
