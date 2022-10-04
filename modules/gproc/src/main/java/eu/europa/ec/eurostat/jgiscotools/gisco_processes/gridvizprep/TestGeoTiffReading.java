package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.opengis.geometry.Envelope;

import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;

public class TestGeoTiffReading {

	public static void main(String[] args) {
		System.out.println("start");

		GridCoverage2D c = GeoTiffUtil.getGeoTIFFCoverage("/home/juju/Bureau/test/tt.tif");

		//get envelopes
		Envelope envG = c.getEnvelope();
		GridEnvelope2D env = c.getGridGeometry().getGridRange2D();
		System.out.println(envG);
		System.out.println(env);

		//compute and check resolution
		double resX = (envG.getMaximum(0) - envG.getMinimum(0)) / env.getWidth();
		double resY = (envG.getMaximum(1) - envG.getMinimum(1)) / env.getHeight();
		if(resX != resY)
			throw new Error("Different X/Y resolutions: "+resX + " and "+resY);
		System.out.println(resX);


		float[] v = new float[1];

		c.evaluate(new GridCoordinates2D(0,0), v);
		//250.26807
		System.out.println(v[0]);

		c.evaluate(new GridCoordinates2D(1,0), v);
		//249.40909
		System.out.println(v[0]);

		c.evaluate(new GridCoordinates2D(0,1), v);
		//247.82031
		System.out.println(v[0]);

		c.evaluate(new GridCoordinates2D(1,1), v);
		//259.77542
		System.out.println(v[0]);

		System.out.println("end");
	}

}
