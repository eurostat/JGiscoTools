package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.CommandUtil;
import eu.europa.ec.eurostat.jgiscotools.GDALResampling;
import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler.Format;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler2;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler2.ColummCalculator;

public class EurForest {
	static Logger logger = LogManager.getLogger(EurForest.class.getName());

	//Dominant Leaf Type (DLT) - 0-1-2 -mode
	//Tree Cover Density (TCD) - 0 to 100 -average

	// the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100 };
	private static String basePath = "/home/juju/Bureau/gisco/geodata/forest/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		/*
		//prepare 100m files
		//DLT
		resample(basePath + "DLT_2018_010m_eu_03035_v020/DATA/DLT_2018_010m_eu_03035_V2_0.tif", basePath +"forest_DLT_2018_100.tif", 100, "mode");
		resample(basePath + "DLT_2015_020m_eu_03035_d04_Full/DLT_2015_020m_eu_03035_d04_full.tif", basePath +"forest_DLT_2015_100.tif", 100, "mode");
		resample(basePath + "DLT_2012_020m_eu_03035_d03_Full/DLT_2012_020m_eu_03035_d03_full.tif", basePath +"forest_DLT_2012_100.tif", 100, "mode");
		//TCD
		resample(basePath + "TCD_2018_010m_eu_03035_v020/DATA/TCD_2018_010m_eu_03035_V2_0.tif", basePath +"forest_TCD_2018_100.tif", 100, "average");
		resample(basePath + "TCD_2015_100m_eu_03035_d04_Full/TCD_2015_100m_eu_03035_d04_full.tif", basePath +"forest_TCD_2015_100.tif", 100, "average");
		resample(basePath + "TCD_2012_100m_eu_03035_d04_Full/TCD_2012_100m_eu_03035_d04_full.tif", basePath +"forest_TCD_2012_100.tif", 100, "average");
		 */

		//remove255TCD();
		//resampling();

		//tiling(Format.CSV, null, 256);
		tiling(Format.PARQUET, CompressionCodecName.GZIP, 256);

		logger.info("End");
	}




	//replace 255 values by 0
	private static void remove255TCD() {
		//https://stackoverflow.com/questions/18315096/gdal-how-to-conditionally-assign-a-new-value-to-pixels-of-a-raster-image
		//gdal_calc.py -A crop.tif --outfile=level0100.tif --calc="A-A*(A=255)"     --NoDataValue=0
		//https://gdal.org/programs/gdal_edit.html
		//gdal_translate -a_nodata

		//
		String inF = basePath + "TCD_2018_010m_eu_03035_v020/DATA/TCD_2018_010m_eu_03035_V2_0.tif";
		String outF = basePath +"forest_TCD_255.tif";
		String cmd = "gdal_calc.py -A "+inF+" --outfile "+outF+" --calc=\"A-A*(A==255)\" --type UInt16";

		cmd = "export CHECK_DISK_FREE_SPACE=FALSE && " + cmd;

		logger.info(cmd);
		CommandUtil.run(cmd);

	}


	public static void resampling() {

		for (int year : new int[] { 2012, 2015, 2018 })
			for (int res : resolutions) {
				logger.info("Resampling " +year+ " to " + res + "m");

				//DLT
				GDALResampling.resample(basePath +"in/forest_DLT_"+year+"_100.tif", basePath +"forest_DLT_"+year+"_"+res+".tif", res, "mode");

				//TCD
				GDALResampling.resample(basePath +"in/forest_TCD_"+year+"_100.tif", basePath +"forest_TCD_"+year+"_"+res+".tif", res, "average");
			}
	}



	// tile all resolutions
	private static void tiling(Format format, CompressionCodecName comp, int nbp) {


		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			//make column calculators
			Map<String, ColummCalculator> values = new HashMap<>();
			for (int year : new int[] { 2012, 2015, 2018 }) {
				values.put("dlt"+year, EurElevation.geoTiffColummCalculator(basePath +"forest_DLT_"+year+"_"+res+".tif", res, v -> {
					if(v<=0 || v>=3) return null;
					return ""+v;
				}));
				values.put("tcd"+year, EurElevation.geoTiffColummCalculator(basePath +"forest_TCD_"+year+"_"+res+".tif", res, v -> {
					if(v<=0 || v>100) return null;
					return ""+v;
				}));
			}

			logger.info("Tiling...");
			String outpath = basePath + "tiled_"+format+"_"+comp+"_"+nbp+"/" + res + "m";
			GridTiler2.tile("Forest - Copernicus land monitoring - European commission",
					values,
					new Coordinate(0,0),
					GeoTiffUtil.getGeoTIFFCoverage(basePath +"forest_DLT_2018_"+res+".tif").getEnvelope(),
					res, nbp, "EPSG:3035", format, comp, outpath
					);

			/*/join country codes
			if(res >= 1000) {
				ArrayList<Map<String, String>> pop = CSVUtil.load("/home/juju/Bureau/gisco/grid_pop/pop_with_zero_"+res+"m.csv");
				logger.info("pop: " + pop.size());
				CSVUtil.removeColumn(pop, "2006", "2011", "2018");
				//CSVUtil.renameColumn(pop, "2018", "TOT_P");
				logger.info(pop.get(0).keySet());

				logger.info("Join pop");
				cells = CSVUtil.joinBothSides("GRD_ID", cells, pop, "", false);
				logger.info(cells.size());
			}//*/

		}



		/*
		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String in;

			in = "DLT";
			logger.info("Load grid cells " + in);
			List<Map<String, String>> cellsDLT = GeoTiffUtil.loadCells(
					basePath +"forest_"+in+"_"+ res + ".tif",
					new String[] {"dlt"},
					(v)->{ return v[0]<=0 || v[0]>=3; }
					);
			logger.info(cellsDLT.size());

			in = "TCD";
			logger.info("Load grid cells " + in);
			List<Map<String, String>> cellsTCD = GeoTiffUtil.loadCells(
					basePath +"forest_"+in+"_"+ res + ".tif",
					new String[] {"tcd"},
					(v)->{ return v[0]<=0 || v[0]>100; }
					);
			logger.info(cellsTCD.size());

			logger.info("Join");
			List<Map<String, String>> cells = CSVUtil.joinBothSides("GRD_ID", cellsDLT, cellsTCD, "", false);
			logger.info(cells.size());
			cellsDLT.clear(); cellsTCD.clear();

			/*join country codes
			if(res >= 1000) {
				ArrayList<Map<String, String>> pop = CSVUtil.load("/home/juju/Bureau/gisco/grid_pop/pop_with_zero_"+res+"m.csv");
				logger.info("pop: " + pop.size());
				CSVUtil.removeColumn(pop, "2006", "2011", "2018");
				logger.info(pop.get(0).keySet());

				logger.info("Join pop");
				cells = CSVUtil.joinBothSides("GRD_ID", cells, pop, "", false);
				logger.info(cells.size());
			}
			logger.info(cells.get(0).keySet());//*/

		/*/filter: cells without clc ? without CNTR ?
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

			/*if(res >= 1000) {
				//check cnt
				cells = cells.stream().filter( c -> {
					String cid = c.get("CNTR_ID");
					return cid != null && !cid.isEmpty() && !"".equals(cid);
				} ).collect(Collectors.toList());
				logger.info(cells.size());
			}//*/
		/*
			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), nbp);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "tiled_"+comp+"_"+nbp+"/" + res + "m";
			gst.save(outpath, format, "ddb", comp, true);
			gst.saveTilingInfoJSON(outpath, GridTiler.Format.CSV, "Forest - copernicus - TCD DLT " + res + "m");

		}*/
	}

}
