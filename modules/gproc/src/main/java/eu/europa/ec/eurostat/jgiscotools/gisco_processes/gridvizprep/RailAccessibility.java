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
import org.opengis.geometry.Envelope;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class RailAccessibility {
	static Logger logger = LogManager.getLogger(RailAccessibility.class.getName());

	//https://docs.geotools.org/stable/userguide/library/coverage/geotiff.html

	// the target resolutions
	private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_rail_perf/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");
		prepare();
		//aggregate();
		//tiling();
		logger.info("End");
	}

	private static void prepare() throws Throwable {

		//get coverage from tiff file
		File file = new File(basePath + "rail-2022-grid-data/RAIL_ACC_AV_T_WW_GR_1KM_2019.tif");
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

				Map<String, String> d = new HashMap<>();
				d.put("x", i + "");
				d.put("y", j + "");
				d.put("value", v + "");
				data.add(d);
			}

		logger.info("save " + data.size());
		CSVUtil.save(data, basePath + "out/test.csv");
	}

}
