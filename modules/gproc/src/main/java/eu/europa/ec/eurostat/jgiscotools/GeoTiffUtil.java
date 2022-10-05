package eu.europa.ec.eurostat.jgiscotools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.geometry.Envelope;

public class GeoTiffUtil {


	//get coverage from tiff file
	public static GridCoverage2D getGeoTIFFCoverage(String inTiff) {
		/*
		File file = new File(inTiff);
		AbstractGridFormat format = GridFormatFinder.findFormat( file );
		GridCoverage2DReader reader = format.getReader( file );
		GridCoverage2D coverage = null;
		try {
			coverage = (GridCoverage2D) reader.read(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return coverage;
		 */

		try {
			GeoTiffReader reader = new GeoTiffReader(new File(inTiff)/*, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE)*/);
			return reader.read(null);
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}

	public interface SkipFunction { boolean skip(int[] v); }

	public static ArrayList<Map<String, String>> loadCells(GridCoverage2D coverage, String[] outProps, SkipFunction skip) {

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

		double minX = envG.getMinimum(0);
		double maxY = envG.getMaximum(1);

		int nb = outProps.length;

		//output
		ArrayList<Map<String, String>> out = new ArrayList<>();

		//for(int i=0; i<env.width; i++){
		IntStream.rangeClosed(0, env.width -1).parallel().forEach(i -> {
			int[] v = new int[nb];
			for(int j=0; j<env.height; j++){

				//get cell values
				coverage.evaluate(new GridCoordinates2D(i,j), v);

				//check if to keep
				if(skip.skip(v)) continue;

				//prepare cell
				Map<String, String> d = new HashMap<>();

				//set cell code
				int x = (int)(minX + i*resX);
				int y = (int)(maxY - (j+1)*resX);
				d.put("GRD_ID", "CRS3035RES"+((int)resX)+"m"+"N"+y+"E"+x);
				//d.put("x", x + "");
				//d.put("y", y + "");

				//set cell values
				for(int p=0; p<nb; p++) {
					d.put(outProps[p], v[p] + "");
				}

				out.add(d);
			}
		});
		return out;
	}

}
