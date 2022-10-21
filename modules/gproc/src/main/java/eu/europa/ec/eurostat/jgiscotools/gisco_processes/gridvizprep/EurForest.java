package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.CommandUtil;

public class EurForest {
	static Logger logger = LogManager.getLogger(EurForest.class.getName());

	// the target resolutions
	//private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000 };
	private static String basePath = "/home/juju/Bureau/gisco/geodata/forest/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		resampling();
		//tiling();

		logger.info("End");
	}


	private static void resampling() {

		String inF = basePath + "DLT_2018_010m_lu_03035_v020/DATA/DLT_2018_010m_E40N30_03035_v020.tif";

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String outF = basePath +"forest_"+ res + ".tif";
			//https://gdal.org/programs/gdalwarp.html#gdalwarp
			String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -tap -r average";

			logger.info(cmd);
			CommandUtil.run(cmd);
		}


	}



	/*/ tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String in;

			in = "ROAD_ACC_1H30";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsRA = GeoTiffUtil.loadCells(
					basePath +in+"_"+ res + ".tif",
					new String[] {"ra"},
					(v)->{ return v[0]==-1; }
					);
			logger.info(cellsRA.size());

			in = "POPL_PROX_120KM";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsPP = GeoTiffUtil.loadCells(
					basePath +in+"_"+ res + ".tif",
					new String[] {"pp"},
					(v)->{ return v[0]==-1; }
					);
			logger.info(cellsPP.size());

			in = "ROAD_PERF_1H30";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsRP = GeoTiffUtil.loadCells(
					basePath +in+"_"+ res + ".tif",
					new String[] {"rp"},
					(v)->{ return v[0]==-1; }
					);
			logger.info(cellsRP.size());


			logger.info("Join 1");
			ArrayList<Map<String, String>> cells = CSVUtil.joinBothSides("GRD_ID", cellsRA, cellsPP, "", false);
			logger.info(cells.size());

			logger.info("Join 2");
			cells = CSVUtil.joinBothSides("GRD_ID", cells, cellsRP, "", false);
			logger.info(cells.size());

			logger.info(cells.get(0).keySet());


			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "out/" + res + "m";
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "Road transport performance " + res + "m");

		}
	}*/

}
