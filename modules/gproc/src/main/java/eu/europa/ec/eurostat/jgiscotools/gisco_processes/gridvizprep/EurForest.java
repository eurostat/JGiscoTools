package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.CommandUtil;
import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class EurForest {
	static Logger logger = LogManager.getLogger(EurForest.class.getName());

	//Dominant Leaf Type (DLT) - 0-1-2 -mode
	//Tree Cover Density (TCD) - 0 to 100 -average

	// the target resolutions
	//private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static int[] resolutions = new int[] { /*100000, 50000, 20000, 10000, 5000, 2000, 1000,*/ 500 /*, 200, 100*/ };
	private static String basePath = "/home/juju/Bureau/gisco/geodata/forest/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		//resampling();
		tiling();

		logger.info("End");
	}


	private static void resampling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			//https://gdal.org/programs/gdalwarp.html#gdalwarp

			//DLT
			String inF = basePath + "DLT_2018_010m_eu_03035_v020/DATA/DLT_2018_010m_eu_03035_V2_0.tif";
			String outF = basePath +"forest_DLT_"+ res + ".tif";
			String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -tap -r mode";

			logger.info(cmd);
			CommandUtil.run(cmd);

			//TCD
			inF = basePath + "TCD_2018_010m_eu_03035_v020/DATA/TCD_2018_010m_eu_03035_V2_0.tif";
			outF = basePath +"forest_TCD_"+ res + ".tif";
			cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -tap -r average";

			logger.info(cmd);
			CommandUtil.run(cmd);
		}
	}



	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String in;

			in = "DLT";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsDLT = GeoTiffUtil.loadCells(
					basePath +"forest_"+in+"_"+ res + ".tif",
					new String[] {"dlt"},
					(v)->{ return v[0]==0 || v[0]==255; }
					);
			logger.info(cellsDLT.size());

			in = "TCD";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsTCD = GeoTiffUtil.loadCells(
					basePath +"forest_"+in+"_"+ res + ".tif",
					new String[] {"tcd"},
					(v)->{ return v[0]==0 || v[0]==255; }
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

		}
	}

}
