package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500 /*, 200/*, 100*/ };
	private static String basePath = "/home/juju/Bureau/gisco/geodata/forest/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		remove255();
		//resampling();
		//tiling();

		logger.info("End");
	}


	//replace 255 values by 0
	private static void remove255() {
		//https://stackoverflow.com/questions/18315096/gdal-how-to-conditionally-assign-a-new-value-to-pixels-of-a-raster-image
		//gdal_calc.py -A crop.tif --outfile=level0100.tif --calc="A-A*(A=255)"     --NoDataValue=0
		//https://gdal.org/programs/gdal_edit.html
		//gdal_translate -a_nodata

		//
		String inF = basePath + "TCD_2018_010m_eu_03035_v020/DATA/TCD_2018_010m_eu_03035_V2_0.tif";
		String outF = basePath +"forest_TCD_255.tif";
		String cmd = "gdal_calc.py -A "+inF+" --outfile "+outF+" --calc=\"A-A*(A==255)\"";

		String pre = "export CHECK_DISK_FREE_SPACE=FALSE && ";
		cmd = pre + cmd;

		logger.info(cmd);
		CommandUtil.run(cmd);

	}


	private static void resampling() {

		for (int res : resolutions) {
			logger.info("Resampling to " + res + "m");

			//https://gdal.org/programs/gdalwarp.html#gdalwarp

			//DLT
			String inF = basePath + "DLT_2018_010m_eu_03035_v020/DATA/DLT_2018_010m_eu_03035_V2_0.tif";
			String outF = basePath +"forest_DLT_"+ res + ".tif";
			String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -tap -r mode -co TILED=YES";

			logger.info(cmd);
			CommandUtil.run(cmd);

			//TCD
			inF = basePath + "TCD_2018_010m_eu_03035_v020/DATA/TCD_2018_010m_eu_03035_V2_0.tif";
			outF = basePath +"forest_TCD_"+ res + ".tif";
			cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -tap -r average -co TILED=YES";

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
			List<Map<String, String>> cellsDLT = GeoTiffUtil.loadCells(
					basePath +"forest_"+in+"_"+ res + ".tif",
					new String[] {"dlt"},
					(v)->{ return v[0]<=0 || v[0]>=3; },
					false
					);
			logger.info(cellsDLT.size());

			in = "TCD";
			logger.info("Load grid cells " + in);
			List<Map<String, String>> cellsTCD = GeoTiffUtil.loadCells(
					basePath +"forest_"+in+"_"+ res + ".tif",
					new String[] {"tcd"},
					(v)->{ return v[0]<=0 || v[0]>100; },
					false
					);
			logger.info(cellsTCD.size());

			logger.info("Join");
			List<Map<String, String>> cells = CSVUtil.joinBothSides("GRD_ID", cellsDLT, cellsTCD, "", false);
			logger.info(cells.size());
			cellsDLT.clear(); cellsTCD.clear();

			//join country codes
			if(res >= 1000) {
				ArrayList<Map<String, String>> pop = CSVUtil.load("/home/juju/Bureau/gisco/grid_pop/pop_with_zero_"+res+"m.csv");
				logger.info("pop: " + pop.size());
				CSVUtil.removeColumn(pop, "2006", "2011", "2018");
				logger.info(pop.get(0).keySet());

				logger.info("Join pop");
				cells = CSVUtil.joinBothSides("GRD_ID", cells, pop, "", false);
				logger.info(cells.size());
			}

			logger.info(cells.get(0).keySet());

			//filter: cells without clc ? without CNTR ?
			logger.info("Filter");
			logger.info(cells.size());

			//check tcd >0
			cells = cells.stream().filter( c -> {
				String tcd = c.get("tcd");
				String dlt = c.get("dlt");
				if("".equals(tcd) && "".equals(dlt)) return false;
				//double d = Integer.parseInt(tcd);
				//return d>0;
				return true;
			} ).collect(Collectors.toList());
			logger.info(cells.size());

			if(res >= 1000) {
				//check cnt
				cells = cells.stream().filter( c -> {
					String cid = c.get("CNTR_ID");
					return cid != null && !cid.isEmpty() && !"".equals(cid);
				} ).collect(Collectors.toList());
				logger.info(cells.size());
			}


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
