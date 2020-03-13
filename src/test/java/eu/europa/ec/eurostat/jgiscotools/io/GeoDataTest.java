/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.util.ArrayList;
import java.util.Collection;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geodiff.DifferenceDetection;
import junit.framework.TestCase;

/**
 * @author Julien Gaffuri
 *
 */
public class GeoDataTest extends TestCase {

	/** @param args */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(GeoDataTest.class);
	}

	private static final String path = "src/test/resources/io/";

	/***/
	//public void testLoadGPKG() { testLoad(".gpkg", null); }
	/***/
	//public void testLoadSHP() { testLoad(".shp", null); }
	/***/
	//public void testLoadGeoJSON() { testLoad(".geojson", null); }
	/***/
	//public void testLoadGeoJSONID() { testLoad("_with_id.geojson", "id"); }

	private void testLoad(String format, String idAtt) {
		//System.out.println(format);

		GeoData gd = new GeoData(path + "test" + format, idAtt);
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
			assertTrue(f.getAttribute("temp") instanceof Double);
			assertTrue(f.getAttribute("name") instanceof String);
			assertNotNull(f.getAttribute("allowed"));
			assertNull(f.getAttribute("sdfdsfkjsfh"));
			//System.out.println(f.getAttribute("name") + "   ***" + f.getID() + "***   ");
		}
	}

	/***/
	public void testSaveGPKG() { testSave(".gpkg", null); }
	/***/
	public void testSaveSHP() { testSave(".shp", null); }
	/***/
	public void testSaveGeoJSON() { testSave(".geojson", null); }
	/***/
	public void testSaveGeoJSONID() { testSave("_with_id.geojson", "id"); }

	private void testSave(String format, String idAtt) {
		System.out.println(format);

		//load data
		GeoData gd = new GeoData(path + "test" + format, idAtt);

		//save data
		String out = "target/io/testSave" + format;
		GeoData.save(gd.getFeatures(), out, gd.getCRS(), true);

		//reload data
		GeoData gd2 = new GeoData(out, idAtt);

		//check same number of features
		assertEquals(gd.getFeatures().size(), gd2.getFeatures().size());
		//check same CRS
		assertEquals(gd.getCRS(), gd2.getCRS());

		//System.out.println(gd.getSchema());
		//System.out.println(gd2.getSchema());
		//System.out.println(gd.getFeatures().get(0).getAttributes().keySet());
		//System.out.println(gd2.getFeatures().get(0).getAttributes().keySet());

		//TODO fix attribute datatype when saving
		for(Feature f : gd2.getFeatures()) {
			Geometry g = f.getGeometry();
			assertEquals("MultiPolygon", g.getGeometryType());
			assertFalse(g.isEmpty());
			assertTrue(g.isValid());
			assertTrue(g.getArea() > 0);
			//TODO assertTrue(f.getAttribute("temp") instanceof Double);
			//TODO assertTrue(f.getAttribute("name") instanceof String);
			assertNotNull(f.getAttribute("allowed"));
			assertNull(f.getAttribute("sdfdsfkjsfh"));
			//System.out.println(f.getAttribute("name") + "   ***" + f.getID() + "***   ");
		}

		//TODO
		//compare both datasets
		//FeatureUtil.setId(gd.getFeatures(), "fid");
		//FeatureUtil.setId(gd2.getFeatures(), "fid");
		Collection<Feature> diffs = DifferenceDetection.getDifferences(gd.getFeatures(), gd2.getFeatures(), -1);
		//TODO add id column for gpkg and shp ...
		//System.out.println(diffs.size());
		//for(Feature diff : diffs) System.out.println(diff.getAttribute("GeoDiff"));

	}
}
