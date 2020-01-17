package eu.europa.ec.eurostat.jgiscotools.changedetection;

import java.util.ArrayList;
import java.util.Collection;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import eu.europa.ec.eurostat.jgiscotools.util.JTSGeomUtil;
import junit.framework.TestCase;

public class ChangeDetectionTest extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(ChangeDetectionTest.class);
	}


	public void test1() throws Exception {
		String path = "src/test/resources/change_detection/";

		//load datasets
		ArrayList<Feature> fsIni = GeoPackageUtil.getFeatures(path+"ini.gpkg");
		assertEquals(13, fsIni.size());
		ArrayList<Feature> fsFin = GeoPackageUtil.getFeatures(path+"fin.gpkg");
		assertEquals(14, fsFin.size());

		//set identifiers
		FeatureUtil.setId(fsIni, "id");
		FeatureUtil.setId(fsFin, "id");

		//check geometries
		assertTrue(JTSGeomUtil.checkGeometry(fsIni, true, MultiPolygon.class));
		assertTrue(JTSGeomUtil.checkGeometry(fsFin, true, MultiPolygon.class));

		//check ids
		assertEquals(0, FeatureUtil.checkIdentfier(fsIni, "id").size());
		assertEquals(0, FeatureUtil.checkIdentfier(fsFin, "id").size());

		//build change detection object
		double resolution = 1;
		ChangeDetection cd = new ChangeDetection(fsIni, fsFin, resolution);
		//cd.setAttributesToIgnore("id","name");

		//check unchanged
		Collection<Feature> unchanged = cd.getUnchanged();
		assertEquals(6, unchanged.size());
		assertTrue(JTSGeomUtil.checkGeometry(unchanged, true, MultiPolygon.class));
		assertEquals(0, FeatureUtil.checkIdentfier(unchanged, null).size());

		//check changes
		Collection<Feature> changes = cd.getChanges();
		assertEquals(12, changes.size());
		assertTrue(JTSGeomUtil.checkGeometry(changes, true, MultiPolygon.class));
		assertEquals(0, FeatureUtil.checkIdentfier(changes, null).size());

		//check hausdorf geom changes
		Collection<Feature> hfgeoms = cd.getHausdorffGeomChanges();
		assertEquals(2, hfgeoms.size());
		assertTrue(JTSGeomUtil.checkGeometry(hfgeoms, true, LineString.class));
		assertEquals(0, FeatureUtil.checkIdentfier(hfgeoms, null).size());

		//check  geometry changes
		Collection<Feature> geomch = cd.getGeomChanges();
		assertEquals(4, geomch.size());
		assertTrue(JTSGeomUtil.checkGeometry(changes, true, MultiPolygon.class));
		assertEquals(0, FeatureUtil.checkIdentfier(geomch, null).size());

		//check id stability issues
		Collection<Feature> sus = ChangeDetection.findIdStabilityIssues(changes, 50);
		assertEquals(4, sus.size());
		assertEquals(0, FeatureUtil.checkIdentfier(sus, null).size());

		/*
		String outpath = "target/";
		CoordinateReferenceSystem crs = GeoPackageUtil.getCRS(path+"ini.gpkg");
		GeoPackageUtil.save(changes, outpath+"changes.gpkg", crs, true);
		GeoPackageUtil.save(unchanged, outpath+"unchanged.gpkg", crs, true);
		GeoPackageUtil.save(hfgeoms, outpath+"hfgeoms.gpkg", crs, true);
		GeoPackageUtil.save(geomch, outpath+"geomch.gpkg", crs, true);
		GeoPackageUtil.save(sus, outpath+"suspects.gpkg", crs, true);
		 */

		//test equals function
		assertFalse( ChangeDetection.equals(fsIni, fsFin, resolution) );
		assertFalse( ChangeDetection.equals(fsFin, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsIni, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsFin, fsFin, resolution) );

		//test change application
		ChangeDetection.applyChanges(fsIni, changes);
		assertTrue( ChangeDetection.equals(fsIni, fsFin, resolution) );
		assertTrue(JTSGeomUtil.checkGeometry(fsIni, true, MultiPolygon.class));
		assertEquals(0, FeatureUtil.checkIdentfier(fsIni, null).size());
	}

}
