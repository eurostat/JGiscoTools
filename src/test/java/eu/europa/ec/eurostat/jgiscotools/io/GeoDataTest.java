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

	public static void main(String[] args) {
		junit.textui.TestRunner.run(GeoDataTest.class);
	}

	private static final String path = "src/test/resources/io/";

	public void testLoadGPKG() throws Exception { testLoad("gpkg"); }
	public void testLoadSHP() throws Exception { testLoad("shp"); }
	public void testLoadGeoJSON() throws Exception { testLoad("geojson"); }

	private void testLoad(String format) throws Exception {
		//System.out.println(format);

		GeoData gd = new GeoData(path + "test." + format);
		ArrayList<Feature> fs = gd.getFeatures();
		assertEquals("CARTO", gd.getCRSType().toString());

		//TODO ensure schemas are the same/similar ?
		//System.out.println(gd.getSchema());
		//System.out.println(gd.getSchema().isIdentified()); //all true
		//System.out.println(gd.getSchema().getAttributeDescriptors());
		//System.out.println(fs.get(0).getAttributes().keySet());

		//TODO check index by id
		//TODO load data - specify id column

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
			//System.out.println(f.getAttribute("name") + "   ***" + f.getID() + "***   " + f.getAttribute("fid"));
			//TODO ensure ids are the same
		}
	}

	public void testSaveGPKG() throws Exception { testSave("gpkg"); }
	public void testSaveSHP() throws Exception { testSave("shp"); }
	public void testSaveGeoJSON() throws Exception { testSave("geojson"); }

	private void testSave(String format) throws Exception {

		System.out.println(format);

		//load data
		GeoData gd = new GeoData(path + "test." + format);

		//save data
		String out = "target/io/testSave." + format;
		GeoData.save(gd.getFeatures(), out, gd.getCRS());

		//reload data
		GeoData gd2 = new GeoData(out);

		//check same number of features
		assertEquals(gd.getFeatures().size(), gd2.getFeatures().size());
		//check same CRS
		assertEquals(gd.getCRS(), gd2.getCRS());

		//System.out.println(gd.getSchema());
		//System.out.println(gd2.getSchema());

		//compare both datasets
		//FeatureUtil.setId(gd.getFeatures(), "fid");
		//FeatureUtil.setId(gd2.getFeatures(), "fid");
		Collection<Feature> diffs = DifferenceDetection.getDifferences(gd.getFeatures(), gd2.getFeatures(), -1);
		//System.out.println(diffs.size());
		for(Feature diff : diffs) {
			System.out.println(diff.getAttribute("GeoDiff"));
		}
		//TODO: problem. ID not preserved
		//TODO: set IDs properly when loading/saving

	}

}
