package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.GDALResampling;
import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler.Format;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler2;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler2.ColummCalculator;

public class EurCLC {
	static Logger logger = LogManager.getLogger(EurCLC.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { /*100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100*/ };
	private static String basePath = "/home/juju/Bureau/gisco/geodata/clc/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		resampling();

		//tiling(Format.CSV, null, 128);
		tiling(Format.PARQUET, CompressionCodecName.GZIP, 256);

		logger.info("End");
	}

	private static void resampling() {
		for (int res : resolutions) {
			logger.info("Resampling to " + res + "m");
			//1990
			GDALResampling.resample(basePath + "1990/DATA/U2000_CLC1990_V2020_20u1.tif", basePath + "1990_" + res+".tif", res, "mode");
			//2000
			GDALResampling.resample(basePath + "2000/DATA/U2006_CLC2000_V2020_20u1.tif", basePath + "2000_" + res+".tif", res, "mode");
			//2006
			GDALResampling.resample(basePath + "2006/DATA/U2012_CLC2006_V2020_20u1.tif", basePath + "2006_" + res+".tif", res, "mode");
			//2012
			GDALResampling.resample(basePath + "2012/DATA/U2018_CLC2012_V2020_20u1.tif", basePath + "2012_" + res+".tif", res, "mode");
			//2018
			GDALResampling.resample(basePath + "2018/DATA/U2018_CLC2018_V2020_20u1.tif", basePath + "2018_" + res+".tif", res, "mode");
		}
	}

	// tile all resolutions
	private static void tiling(Format format, CompressionCodecName comp, int nbp) {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			//make column calculators
			Map<String, ColummCalculator> values = new HashMap<>();
			for (int year : new int[] { 1990, 2000, 2006, 2012, 2018 }) {
				values.put("y"+year, EurElevation.geoTiffColummCalculator(basePath + year + "_" + res+".tif", res, v -> {
					if(v==0 || v==128 || v==44 || Double.isNaN(v)) return null;
					return ""+v;
				}));
			}

			logger.info("Tiling...");
			String outpath = basePath + "tiled_"+format+"_"+comp+"_"+nbp+"/" + res + "m";
			GridTiler2.tile("Corine Land Cover - Copernicus land monitoring - European commission",
					values,
					new Coordinate(0,0),
					GeoTiffUtil.getGeoTIFFCoverage(basePath + 2018 + "_" + res+".tif").getEnvelope(),
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

	}
}
