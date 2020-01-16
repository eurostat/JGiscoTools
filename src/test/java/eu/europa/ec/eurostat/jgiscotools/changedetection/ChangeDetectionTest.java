package eu.europa.ec.eurostat.jgiscotools.changedetection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import junit.framework.TestCase;

public class ChangeDetectionTest extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(ChangeDetectionTest.class);
	}


	public void test1() throws Exception {
		String path = "src/test/resources/change_detection/";

		ArrayList<Feature> fsIni = GeoPackageUtil.getFeatures(path+"ini.gpkg");
		assertEquals(13, fsIni.size());
		ArrayList<Feature> fsFin = GeoPackageUtil.getFeatures(path+"fin.gpkg");
		assertEquals(14, fsFin.size());

		FeatureUtil.setId(fsIni, "id");
		FeatureUtil.setId(fsFin, "id");

		HashMap<String, Integer> idc = FeatureUtil.checkIdentfier(fsIni, "id");
		assertEquals(0, idc.size());
		idc = FeatureUtil.checkIdentfier(fsFin, "id");
		assertEquals(0, idc.size());

		double resolution = 1;
		ChangeDetection cd = new ChangeDetection(fsIni, fsFin, resolution);
		//cd.setAttributesToIgnore("id","name");

		Collection<Feature> unchanged = cd.getUnchanged();
		assertEquals(6, unchanged.size());
		idc = FeatureUtil.checkIdentfier(unchanged, null);
		assertEquals(0, idc.size());
		Collection<Feature> changes = cd.getChanges();
		assertEquals(12, changes.size());
		idc = FeatureUtil.checkIdentfier(changes, null);
		assertEquals(0, idc.size());
		Collection<Feature> hfgeoms = cd.getHausdorffGeomChanges();
		assertEquals(2, hfgeoms.size());
		idc = FeatureUtil.checkIdentfier(hfgeoms, null);
		assertEquals(0, idc.size());
		Collection<Feature> geomch = cd.getGeomChanges();
		assertEquals(4, geomch.size());
		idc = FeatureUtil.checkIdentfier(geomch, null);
		assertEquals(0, idc.size());
		Collection<Feature> sus = ChangeDetection.findIdStabilityIssues(changes, 50);
		assertEquals(4, sus.size());
		idc = FeatureUtil.checkIdentfier(sus, null);
		assertEquals(0, idc.size());

		/*
		String outpath = "target/";
		CoordinateReferenceSystem crs = GeoPackageUtil.getCRS(path+"ini.gpkg");
		GeoPackageUtil.save(changes, outpath+"changes.gpkg", crs, true);
		GeoPackageUtil.save(unchanged, outpath+"unchanged.gpkg", crs, true);
		GeoPackageUtil.save(hfgeoms, outpath+"hfgeoms.gpkg", crs, true);
		GeoPackageUtil.save(geomch, outpath+"geomch.gpkg", crs, true);
		GeoPackageUtil.save(sus, outpath+"suspects.gpkg", crs, true);
		 */

		assertFalse( ChangeDetection.equals(fsIni, fsFin, resolution) );
		assertFalse( ChangeDetection.equals(fsFin, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsIni, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsFin, fsFin, resolution) );

		ChangeDetection.applyChanges(fsIni, changes);
		assertTrue( ChangeDetection.equals(fsIni, fsFin, resolution) );
	}

}
