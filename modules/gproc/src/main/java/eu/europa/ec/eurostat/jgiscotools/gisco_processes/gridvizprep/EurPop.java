/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author julien Gaffuri
 *
 */
public class EurPop {
	static Logger logger = LogManager.getLogger(EurPop.class.getName());


	//the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100 };
	private static String basePath = "/home/juju/Bureau/gisco/";
	private static String outPath = basePath + "grid_pop/";

	//2006-2001-2018


	//-Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");

		prepare();

		/*
		int year = 2018;

		for(int resKm : new int[] {100, 50, 20, 10, 5}) {

			logger.info("Load");
			ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "pop_grid_" + year + "_"+ resKm +"km.csv");
			logger.info(data.size());
			logger.info(data.get(0).keySet());

			for(Map<String, String> cell : data) {

				String id = cell.get("GRD_ID");
				GridCell gc = new GridCell(id);
				cell.put("x", gc.getLowerLeftCornerPositionX()+"");
				cell.put("y", gc.getLowerLeftCornerPositionY()+"");
				cell.remove("GRD_ID");

				cell.put("population", cell.get("TOT_P"));
				cell.remove("TOT_P");
			}

			logger.info("save");
			CSVUtil.save(data, basePath + "xy/" + "pop_grid_xy_" + year + "_"+ resKm +"km.csv");

		}*/

		logger.info("End");
	}


	private static void prepare() {

		ArrayList<Feature> fs = GeoData.getFeatures(basePath + "grids/grid_1km_surf.gpkg");
		logger.info(fs.size() + " loaded");
		

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
