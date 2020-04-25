/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io.geo;

import java.util.ArrayList;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import junit.framework.TestCase;

/**
 * @author Julien Gaffuri
 *
 */
public class GeoDataTest extends TestCase {

	/** @param args */
	/*public static void main(String[] args) {
		junit.textui.TestRunner.run(GeoDataTest.class);
	}*/

	private static final String path = "src/test/resources/io/";

	/***/
	public void testLoadGPKG() { testLoad("gpkg"); }
	/***/
	public void testLoadSHP() { testLoad("shp"); }
	/***/
	public void testLoadGeoJSON() { testLoad("geojson"); }

	private void testLoad(String format) {
		//System.out.println(format);

		boolean gjs = format.contains("geojson");
		boolean shp = format.contains("shp");

		GeoData gd = new GeoData(path + "test." + format, "id");
		ArrayList<Feature> fs = gd.getFeatures();

		assertEquals("CARTO", gd.getCRSType().toString());
		assertEquals(3, fs.size());

		//System.out.println(gd.getSchema());
		//System.out.println(fs.get(0).getAttributes().keySet());

		for(Feature f : fs) {
			Geometry g = f.getGeometry();
			assertEquals("MultiPolygon", g.getGeometryType());
			assertFalse(g.isEmpty());
			assertTrue(g.isValid());
			assertTrue(g.getArea() > 0);

			if(gjs) assertTrue(f.getAttribute("id") instanceof String);
			else assertTrue(f.getAttribute("id") instanceof Integer);

			assertTrue(f.getAttribute("name") instanceof String);

			assertTrue(f.getAttribute("temp") instanceof Double);

			if(shp) assertTrue(f.getAttribute("allowed") instanceof Long);
			else if(gjs) assertTrue(f.getAttribute("allowed") instanceof Boolean);
			else assertTrue(f.getAttribute("allowed") instanceof Boolean);

			assertNull(f.getAttribute("sdfdsfkjsfh"));

			//System.out.println(f.getAttribute("name") + "   ***" + f.getID() + "***   ");
		}
	}

	/***/
	public void testSaveGPKG() { testSave(".gpkg"); }
	/***/
	public void testSaveSHP() { testSave(".shp"); }
	/***/
	public void testSaveGeoJSON() { testSave(".geojson"); }

	private void testSave(String format) {
		//System.out.println(format);
		boolean gjs = format.contains("geojson");
		boolean shp = format.contains("shp");

		//load data
		GeoData gd = new GeoData(path + "test" + format, "id");

		//save data
		String out = "target/io/testSave" + format;
		GeoData.save(gd.getFeatures(), out, gd.getCRS(), true);

		//reload data
		GeoData gd2 = new GeoData(out, "id");

		//check same number of features
		assertEquals(gd.getFeatures().size(), gd2.getFeatures().size());
		//check same CRS
		assertEquals(gd.getCRS(), gd2.getCRS());

		/*System.out.println(gd.getSchema());
		System.out.println(gd2.getSchema());
		System.out.println(gd.getFeatures().get(0).getAttributes().keySet());
		System.out.println(gd2.getFeatures().get(0).getAttributes().keySet());*/

		for(Feature f : gd2.getFeatures()) {
			Geometry g = f.getGeometry();
			assertEquals("MultiPolygon", g.getGeometryType());
			assertFalse(g.isEmpty());
			assertTrue(g.isValid());
			assertTrue(g.getArea() > 0);

			if(gjs) assertTrue(f.getAttribute("id") instanceof String);
			else assertTrue(f.getAttribute("id") instanceof Integer);

			assertTrue(f.getAttribute("name") instanceof String);

			assertTrue(f.getAttribute("temp") instanceof Double);

			if(shp) assertTrue(f.getAttribute("allowed") instanceof Long);
			else if(gjs) assertTrue(f.getAttribute("allowed") instanceof Boolean);
			else assertTrue(f.getAttribute("allowed") instanceof Integer); //TODO should be boolean

			assertNull(f.getAttribute("sdfdsfkjsfh"));
			//System.out.println(f.getAttribute("name") + "   ***" + f.getID() + "***   ");
		}

		//compare both datasets
		/*Collection<Feature> diffs = DifferenceDetection.getDifferences(gd.getFeatures(), gd2.getFeatures(), -1);
		System.out.println(diffs.size());
		for(Feature diff : diffs) System.out.println(diff.getAttribute("GeoDiff"));*/

	}
}
