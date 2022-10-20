package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.geometry.Envelope;

import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;
import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class RoadTransportPerformance {
	static Logger logger = LogManager.getLogger(RoadTransportPerformance.class.getName());

	//https://docs.geotools.org/stable/userguide/library/coverage/geotiff.html

	// the target resolutions
	private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_rail_perf/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");
		tiffToCSV();
		aggregate();
		tiling();
		logger.info("End");
	}

	private static void tiffToCSV() throws Throwable {

		/*
Accessibility: population within a 1.5-h travel by rail
RAIL_ACC_xxxx_yy_GR_1KM_2019

Performance: accessibility / proximity x 100
RAIL_PERF_xxxx_yy_GR_1KM_2019

xxxx = AV_T = average travel time
xxxx = OPTI = optimal travel time

yy = WW = walk + rail + walk
yy = BW = bike + rail + walk
yy = BB = bike + rail + bike		
		*/

		String zz = "ACC";
		String xx = "AV_T";
		String yy = "WW";

		//get coverage from tiff file
		File file = new File(basePath + "rail-2022-grid-data/RAIL_"+zz+"_"+xx+"_"+yy+"_GR_1KM_2019.tif");
		AbstractGridFormat format = GridFormatFinder.findFormat( file );
		GridCoverage2DReader reader = format.getReader( file );
		GridCoverage2D coverage = (GridCoverage2D) reader.read(null);

		//get envelopes
		Envelope envG = coverage.getEnvelope();
		GridEnvelope2D env = coverage.getGridGeometry().getGridRange2D();

		//compute and check resolution
		double resX = (envG.getMaximum(0) - envG.getMinimum(0)) / env.getWidth();
		double resY = (envG.getMaximum(1) - envG.getMinimum(1)) / env.getHeight();
		if(resX != resY)
			throw new Error("Different X/Y resolutions: "+resX + " and "+resY);

		//output
		Collection<Map<String, String>> data = new ArrayList<>();

		int nb = 1;
		int naValue = (int) 2.147483647E9;

		int[] dest = new int[nb];
		for(int i=0; i<env.width; i++)
			for(int j=0; j<env.height; j++){
				coverage.evaluate(new GridCoordinates2D(i,j), dest);
				int v = dest[0];
				if(v==naValue) continue;
				if(v==0) continue;

				int x = (int)(envG.getMinimum(0) + i*resX);
				int y = (int)(envG.getMaximum(1) - (j+1)*resY);
				GridCell gc = new GridCell("3035", 1000, x, y);

				Map<String, String> d = new HashMap<>();
				d.put("GRD_ID", gc.getId());
				//d.put("x", x + "");
				//d.put("y", y + "");
				d.put(zz+"_"+xx+"_"+yy, v + "");
				data.add(d);
			}

		logger.info("save " + data.size());
		CSVUtil.save(data, basePath + "out/out_prepared.csv");
	}



	private static void aggregate() {

		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "out/out_prepared.csv");
		logger.info(data.size());

		for (int res : resolutions) {
			logger.info("Aggregate " + res + "m");
			ArrayList<Map<String, String>> out = GridMultiResolutionProduction.gridAggregation(data, "GRD_ID", res, 10000, null, null);

			logger.info("Save " + out.size());
			CSVUtil.save(out, basePath + "out/out_" + res + "m.csv");
		}

	}


	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + "out/out_" + res + "m.csv";

			logger.info("Load");
			ArrayList<Map<String, String>> cells = CSVUtil.load(f);
			logger.info(cells.size());

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "out/tiled/" + res + "m";
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "rail accessibility resolution " + res + "m");

		}
	}

}
