/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.util.ArrayList;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import junit.framework.TestCase;

/**
 * @author Julien Gaffuri
 *
 */
public class GeoDataTest extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(GeoDataTest.class);
	}

	private static final String path = "src/test/resources/io/";

	public void testLoadGPKG() throws Exception {
		testLoad("gpkg");
	}
	public void testLoadSHP() throws Exception {
		testLoad("shp");
	}
	public void testLoadGeoJSON() throws Exception {
		testLoad("geojson");
	}

	private void testLoad(String format) throws Exception {
		System.out.println(format);

		GeoData gd = new GeoData(path + "test." + format);
		ArrayList<Feature> fs = gd.getFeatures();
		assertEquals("CARTO", gd.getCRSType().toString());

		assertEquals(13, fs.size());
		for(Feature f : fs) {
			Geometry g = f.getGeometry();
			assertEquals("MultiPolygon", g.getGeometryType());
			assertFalse(g.isEmpty());
			assertTrue(g.isValid());
			assertTrue(g.getArea() > 0);
			//fid temp name
			//System.out.println(f.getAttributes().keySet());
			assertTrue(f.getAttribute("temp") instanceof Double);
			assertTrue(f.getAttribute("name") instanceof String);
			//System.out.println(f.getAttribute("name") + " " + f.getID() + " " + f.getAttribute("fid"));
		}

	}

}
