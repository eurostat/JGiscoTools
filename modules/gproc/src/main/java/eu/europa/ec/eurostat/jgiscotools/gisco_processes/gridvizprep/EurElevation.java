package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler.Format;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler2;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler2.ColummCalculator;

public class EurElevation {
	static Logger logger = LogManager.getLogger(EurElevation.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500 /*, 200 /*, 100*/ };
	private static String basePath = "/home/juju/Bureau/gisco/geodata/elevation/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		//resampling();

		tiling(Format.PARQUET, CompressionCodecName.GZIP, 256);

		logger.info("End");
	}

	public static void resampling() {
		for (int res : resolutions) {
			logger.info("Resampling to " + res + "m");
			EurForest.resample(basePath +"EU_DEM_mosaic_1000K/eudem_dem_3035_europe.tif", basePath + res+".tif", res, "average");
		}
	}

	private static void tiling(Format format, CompressionCodecName comp, int nbp) {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + res+".tif";

			logger.info("Get envelope");
			GridCoverage2D coverage = GeoTiffUtil.getGeoTIFFCoverage(f);
			Envelope env = (Envelope) coverage.getEnvelope();

			/*
			logger.info("Load grid cells");
			ArrayList<Map<String, String>> cells = GeoTiffUtil.loadCells(coverage, new String[] {"elevation"}, (v)->{ return v[0]==0 || Double.isNaN(v[0]); } );
			logger.info(cells.size());*/

			Map<String, ColummCalculator> values = new HashMap<>();



			String outpath = basePath + "tiled_"+format+"_"+comp+"_"+nbp+"/" + res + "m";
			GridTiler2.tile("desc", values, new Coordinate(0,0),
					env,
					res, nbp, "EPSG:3035", format, comp, outpath);


			/*
			logger.info("Load geoTiff");
			GridCoverage2D coverage = GeoTiffUtil.getGeoTIFFCoverage(f);

			logger.info("Load grid cells");
			ArrayList<Map<String, String>> cells = GeoTiffUtil.loadCells(coverage, new String[] {"elevation"}, (v)->{ return v[0]==0 || Double.isNaN(v[0]); } );
			logger.info(cells.size());

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), nbp);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "tiled_"+format+"_"+comp+"_"+nbp+"/" + res + "m";
			gst.save(outpath, format, "ddb", comp, false);
			gst.saveTilingInfoJSON(outpath, format, "EU DEM Europe elevation " + res + "m");
			 */
		}
	}

}
