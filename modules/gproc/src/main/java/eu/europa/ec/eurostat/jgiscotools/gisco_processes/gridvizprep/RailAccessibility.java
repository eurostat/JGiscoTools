package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.awt.image.RenderedImage;
import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.opengis.geometry.Envelope;

public class RailAccessibility {
	static Logger logger = LogManager.getLogger(RailAccessibility.class.getName());

	//https://docs.geotools.org/stable/userguide/library/coverage/geotiff.html

	// the target resolutions
	private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_rail_perf/rail-2022-grid-data/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");
		prepare();
		//aggregate();
		//tiling();
		logger.info("End");
	}

	private static void prepare() throws Throwable {

		File file = new File(basePath + "RAIL_ACC_AV_T_WW_GR_1KM_2019.tif");

		AbstractGridFormat format = GridFormatFinder.findFormat( file );
		GridCoverage2DReader reader = format.getReader( file );

		//GeoTiffReader reader = new GeoTiffReader(file, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));


		GridCoverage2D coverage = (GridCoverage2D) reader.read(null);
		Envelope env = coverage.getEnvelope();
		GridEnvelope2D bn = coverage.getGridGeometry().getGridRange2D();

		double[] dest = new double[1];
		for(int i=0; i<bn.width; i++)
			for(int j=0; j<bn.height; j++){
				coverage.evaluate(new GridCoordinates2D(i,j), dest);
				double d = dest[0];
				if(d==2.147483647E9) continue;
				if(d==0.0) continue;
				//System.out.println(i+" "+j+" --- "+d);
			}		

	}

}
