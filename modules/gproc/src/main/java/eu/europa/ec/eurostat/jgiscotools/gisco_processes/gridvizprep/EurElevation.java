package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

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
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class EurElevation {
	static Logger logger = LogManager.getLogger(EurElevation.class.getName());

	//https://docs.geotools.org/stable/userguide/library/coverage/geotiff.html

	// the target resolutions
	private static int[] resolutions = new int[] { /*100, 200, 500, 1000, 2000, 5000, 10000, 20000,*/ 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/elevation/EU_DEM_mosaic_1000K/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		//resampling
		double resIni = 25.0;
		for (int i=resolutions.length-1; i >=0; i--) {
			int res = resolutions[i];
			int ratio = (int)(res/resIni);
			logger.info("Resample to " + res + "m (ratio="+ratio+")");
			resampleTiff(basePath + "eudem_dem_3035_europe.tif", basePath + "out/resampled_"+res+".csv", ratio, "elevation");
		}

		//tiling();
		logger.info("End");
	}




	private static void resampleTiff(String inTiff, String outCSV, int ratio, String outProp) throws Throwable {

		//get coverage from tiff file
		File file = new File(inTiff);
		AbstractGridFormat format = GridFormatFinder.findFormat( file );
		GridCoverage2DReader reader = format.getReader( file );
		GridCoverage2D coverage = (GridCoverage2D) reader.read(null);

		//get envelopes
		Envelope envG = coverage.getEnvelope();
		GridEnvelope2D env = coverage.getGridGeometry().getGridRange2D();
		//System.out.println(envG);
		//System.out.println(env);

		//compute and check resolution
		double resX = (envG.getMaximum(0) - envG.getMinimum(0)) / env.getWidth();
		double resY = (envG.getMaximum(1) - envG.getMinimum(1)) / env.getHeight();
		if(resX != resY)
			throw new Error("Different X/Y resolutions: "+resX + " and "+resY);
		//System.out.println(resX);

		//
		int resT = (int) (ratio * resX);
		//logger.info("Resampling from "+resX+" to "+resT);

		//output
		Collection<Map<String, String>> data = new ArrayList<>();

		int nb = 1;
		int[] dest = new int[nb];

		IntStream.rangeClosed(0, env.width/ratio -1).parallel().forEach(i -> {
			for(int j=0; j<env.height/ratio; j++){
				
				//TODO check what takes time. Evaluate or map creation ? Then improve what can.
				//100000 7s 9s 8s
				//50000 24s 27s 23s

				coverage.evaluate(new GridCoordinates2D(i*ratio,j*ratio), dest);
				/*int v = dest[0];
				if(v==0) continue;
				//System.out.println(v);

				int x = (int)(envG.getMinimum(0) + i*resT);
				int y = (int)(envG.getMaximum(1) - (j+1)*resT);
				GridCell gc = new GridCell("3035", resT, x, y);
				//System.out.println(gc.getId());

				Map<String, String> d = new HashMap<>();
				d.put("GRD_ID", gc.getId());
				//d.put("x", x + "");
				//d.put("y", y + "");
				d.put(outProp, v + "");
				data.add(d);*/
			}
		});

		logger.info("save " + data.size());
		CSVUtil.save(data, outCSV);

		//see https://docs.geotools.org/stable/javadocs/org/geotools/coverage/processing/operation/Resample.html
		//HashMap props;
		//Hints hints;
		//Resample.doOperation(new ParameterGroup(props), hints);

		/*
		//https://www.tabnine.com/code/java/methods/org.geotools.coverage.processing.operation.Resample/doOperation
		GridGeometry2D gridGeometry;
		Interpolation interpolation;
		ParameterValueGroup param =    (ParameterValueGroup) Resample.getParameters();
		param.parameter("Source").setValue(coverage);
		param.parameter("GridGeometry").setValue(gridGeometry);
		//param.parameter("InterpolationType").setValue(interpolation);
		GridCoverage2D out = (GridCoverage2D) Resample.doOperation(param, hints);*/

	}

	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + "out/resampled_"+res+".csv";

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
			gst.saveTilingInfoJSON(outpath, "EU DEM Europe elevation " + res + "m");

		}
	}

}
