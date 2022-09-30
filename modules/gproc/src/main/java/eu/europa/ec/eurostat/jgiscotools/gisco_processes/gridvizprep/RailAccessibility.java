package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;

public class RailAccessibility {
	static Logger logger = LogManager.getLogger(RailAccessibility.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_rail_perf/rail-2022-grid-data/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		prepare();
		//aggregate();
		//tiling();
		logger.info("End");
	}

	private static void prepare() {

		File file = new File(basePath + "RAIL_ACC_AV_T_WW_GR_1KM_2019.tif");
		System.out.println(file.exists());

		AbstractGridFormat format = GridFormatFinder.findFormat( file );
		GridCoverage2DReader reader = format.getReader( file );


	}

}
