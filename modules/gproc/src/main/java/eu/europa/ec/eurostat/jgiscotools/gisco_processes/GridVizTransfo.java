package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class GridVizTransfo {
	static Logger logger = LogManager.getLogger(GridVizTransfo.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 1, 2, 5, 10, 20, 50, 100 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/health/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		prepare();
		// aggregate();
		// tiling();
		logger.info("End");
	}

	// remove attributes
	private static void prepare() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "input/avg_time_nearest_healthcare_1205.csv",
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(";".charAt(0)));
		logger.info(data.size());
		logger.info(data.get(0).keySet());
		// [GRD_ID;Total_Trav]

		logger.info("Structure");
		for (Map<String, String> d : data) {
			String gid = d.get("GRD_ID");
			gid = gid.replace("CRS3035RES1000mN", "");
			String[] s = gid.split("E");
			int y = Integer.parseInt(s[0]);
			int x = Integer.parseInt(s[1]);
			y /= 1000;
			x /= 1000;
			d.put("x", x + "");
			d.put("y", y + "");
			d.remove("GRD_ID");

			String ts = d.get("Total_Trav").replace(",", ".");
			double t = Double.parseDouble(ts);
			d.put("Total_Trav", Math.ceil(t) + "");

		}

		logger.info("Rename colums");
		CSVUtil.renameColumn(data, "Total_Trav", "avg_time_nearest");

		logger.info(data.size());
		logger.info(data.get(0).keySet());

		logger.info("save");
		CSVUtil.save(data, basePath + "prepared.csv");

	}

	// derive resolutions
	private static void aggregate() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "Filosofi2015_prepared.csv");
		logger.info(data.size());

		for (int res : resolutions) {
			logger.info("Aggregate " + res + "m");
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res,
					10000);
			logger.info(out.size());

			logger.info("Save");
			CSVUtil.save(out, basePath + "Filosofi2015_" + res + ".csv");
		}
	}

	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + "Filosofi2015_" + res + ".csv";

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
			gst.saveTilingInfoJSON(outpath, "Filosofi 2015 resolution " + res + "m");

		}
	}

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
