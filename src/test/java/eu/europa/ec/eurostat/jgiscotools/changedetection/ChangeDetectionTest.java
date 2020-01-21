package eu.europa.ec.eurostat.jgiscotools.changedetection;

import java.util.ArrayList;
import java.util.Collection;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import eu.europa.ec.eurostat.jgiscotools.util.JTSGeomUtil;
import junit.framework.TestCase;

public class ChangeDetectionTest extends TestCase {

	/*public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(ChangeDetectionTest.class);
		//new ChangeDetectionTest().testSurf();
		//new ChangeDetectionTest().testPt();
	}*/


	public void testSurf() throws Exception {
		String path = "src/test/resources/change_detection/";

		//load datasets
		ArrayList<Feature> fsIni = GeoPackageUtil.getFeatures(path+"ini_surf.gpkg");
		assertEquals(13, fsIni.size());
		ArrayList<Feature> fsFin = GeoPackageUtil.getFeatures(path+"fin_surf.gpkg");
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
		assertEquals(6, cd.getUnchanged().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getUnchanged(), true, MultiPolygon.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getUnchanged(), null).size());

		//check changes
		assertEquals(12, cd.getChanges().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getChanges(), true, MultiPolygon.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getChanges(), null).size());

		//check hausdorf geom changes
		assertEquals(2, cd.getHausdorffGeomChanges().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getHausdorffGeomChanges(), true, LineString.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getHausdorffGeomChanges(), null).size());

		//check  geometry changes
		assertEquals(4, cd.getGeomChanges().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getGeomChanges(), true, MultiPolygon.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getGeomChanges(), null).size());

		//check id stability issues
		Collection<Feature> sus = ChangeDetection.findIdStabilityIssues(cd.getChanges(), 50);
		assertEquals(4, sus.size());
		assertTrue(JTSGeomUtil.checkGeometry(sus, true, MultiPolygon.class));
		assertEquals(0, FeatureUtil.checkIdentfier(sus, null).size());

		/*
		String outpath = "target/";
		CoordinateReferenceSystem crs = GeoPackageUtil.getCRS(path+"ini_surf.gpkg");
		GeoPackageUtil.save(cd.getChanges(), outpath+"changes_surf.gpkg", crs, true);
		GeoPackageUtil.save(cd.getUnchanged(), outpath+"unchanged_surf.gpkg", crs, true);
		GeoPackageUtil.save(cd.getHausdorffGeomChanges(), outpath+"hfgeoms_surf.gpkg", crs, true);
		GeoPackageUtil.save(cd.getGeomChanges(), outpath+"geomch_surf.gpkg", crs, true);
		GeoPackageUtil.save(sus, outpath+"suspects_surf.gpkg", crs, true);
		 */

		//test equals function
		assertFalse( ChangeDetection.equals(fsIni, fsFin, resolution) );
		assertFalse( ChangeDetection.equals(fsFin, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsIni, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsFin, fsFin, resolution) );

		//test change application
		ChangeDetection.applyChanges(fsIni, cd.getChanges());
		assertTrue( ChangeDetection.equals(fsIni, fsFin, resolution) );
		assertTrue(JTSGeomUtil.checkGeometry(fsIni, true, MultiPolygon.class));
		assertEquals(0, FeatureUtil.checkIdentfier(fsIni, null).size());
	}




	public void testPt() throws Exception {
		String path = "src/test/resources/change_detection/";

		//load datasets
		ArrayList<Feature> fsIni = GeoPackageUtil.getFeatures(path+"ini_pt.gpkg");
		assertEquals(10, fsIni.size());
		ArrayList<Feature> fsFin = GeoPackageUtil.getFeatures(path+"fin_pt.gpkg");
		assertEquals(10, fsFin.size());

		//set identifiers
		FeatureUtil.setId(fsIni, "id");
		FeatureUtil.setId(fsFin, "id");

		//check geometries
		assertTrue(JTSGeomUtil.checkGeometry(fsIni, true, MultiPoint.class));
		assertTrue(JTSGeomUtil.checkGeometry(fsFin, true, MultiPoint.class));

		//check ids
		assertEquals(0, FeatureUtil.checkIdentfier(fsIni, "id").size());
		assertEquals(0, FeatureUtil.checkIdentfier(fsFin, "id").size());



		//build change detection object
		double resolution = 1;
		ChangeDetection cd = new ChangeDetection(fsIni, fsFin, resolution);
		//cd.setAttributesToIgnore("id","name");

		//check unchanged
		assertEquals(2, cd.getUnchanged().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getUnchanged(), true, MultiPoint.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getUnchanged(), null).size());

		//check changes
		assertEquals(11, cd.getChanges().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getChanges(), true, MultiPoint.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getChanges(), null).size());

		//check hausdorf geom changes
		assertEquals(4, cd.getHausdorffGeomChanges().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getHausdorffGeomChanges(), true, LineString.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getHausdorffGeomChanges(), null).size());

		//check  geometry changes
		assertEquals(8, cd.getGeomChanges().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getGeomChanges(), true, MultiPoint.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getGeomChanges(), null).size());

		//check id stability issues
		Collection<Feature> sus = ChangeDetection.findIdStabilityIssues(cd.getChanges(), 50);
		assertEquals(2, sus.size());
		assertTrue(JTSGeomUtil.checkGeometry(sus, true, MultiPoint.class));
		assertEquals(0, FeatureUtil.checkIdentfier(sus, null).size());

		/*
		String outpath = "target/";
		CoordinateReferenceSystem crs = GeoPackageUtil.getCRS(path+"ini_pt.gpkg");
		GeoPackageUtil.save(cd.getChanges(), outpath+"changes_pt.gpkg", crs, true);
		GeoPackageUtil.save(cd.getUnchanged(), outpath+"unchanged_pt.gpkg", crs, true);
		GeoPackageUtil.save(cd.getHausdorffGeomChanges(), outpath+"hfgeoms_pt.gpkg", crs, true);
		GeoPackageUtil.save(cd.getGeomChanges(), outpath+"geomch_pt.gpkg", crs, true);
		GeoPackageUtil.save(ChangeDetection.findIdStabilityIssues(cd.getChanges(), 50), outpath+"suspects_pt.gpkg", crs, true);
		 */

		//test equals function
		assertFalse( ChangeDetection.equals(fsIni, fsFin, resolution) );
		assertFalse( ChangeDetection.equals(fsFin, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsIni, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsFin, fsFin, resolution) );

		//test change application
		ChangeDetection.applyChanges(fsIni, cd.getChanges());
		assertTrue( ChangeDetection.equals(fsIni, fsFin, resolution) );
		assertTrue(JTSGeomUtil.checkGeometry(fsIni, true, MultiPoint.class));
		assertEquals(0, FeatureUtil.checkIdentfier(fsIni, null).size());

	}


	public void testLin() throws Exception {
		String path = "src/test/resources/change_detection/";

		//load datasets
		ArrayList<Feature> fsIni = GeoPackageUtil.getFeatures(path+"ini_lin.gpkg");
		assertEquals(9, fsIni.size());
		ArrayList<Feature> fsFin = GeoPackageUtil.getFeatures(path+"fin_lin.gpkg");
		assertEquals(10, fsFin.size());

		//set identifiers
		FeatureUtil.setId(fsIni, "id");
		FeatureUtil.setId(fsFin, "id");

		//check geometries
		assertTrue(JTSGeomUtil.checkGeometry(fsIni, true, MultiLineString.class));
		assertTrue(JTSGeomUtil.checkGeometry(fsFin, true, MultiLineString.class));

		//check ids
		assertEquals(0, FeatureUtil.checkIdentfier(fsIni, "id").size());
		assertEquals(0, FeatureUtil.checkIdentfier(fsFin, "id").size());

		//build change detection object
		double resolution = 1;
		ChangeDetection cd = new ChangeDetection(fsIni, fsFin, resolution);
		//cd.setAttributesToIgnore("id","name");

		//check unchanged
		assertEquals(3, cd.getUnchanged().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getUnchanged(), true, MultiLineString.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getUnchanged(), null).size());

		//check changes
		assertEquals(10, cd.getChanges().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getChanges(), true, MultiLineString.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getChanges(), null).size());

		//check hausdorf geom changes
		assertEquals(2, cd.getHausdorffGeomChanges().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getHausdorffGeomChanges(), true, LineString.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getHausdorffGeomChanges(), null).size());

		//check  geometry changes
		assertEquals(4, cd.getGeomChanges().size());
		assertTrue(JTSGeomUtil.checkGeometry(cd.getGeomChanges(), true, MultiLineString.class));
		assertEquals(0, FeatureUtil.checkIdentfier(cd.getGeomChanges(), null).size());

		//check id stability issues
		Collection<Feature> sus = ChangeDetection.findIdStabilityIssues(cd.getChanges(), 50);
		assertEquals(2, sus.size());
		assertTrue(JTSGeomUtil.checkGeometry(sus, true, MultiLineString.class));
		assertEquals(0, FeatureUtil.checkIdentfier(sus, null).size());

		/*
		String outpath = "target/";
		CoordinateReferenceSystem crs = GeoPackageUtil.getCRS(path+"ini_lin.gpkg");
		GeoPackageUtil.save(cd.getChanges(), outpath+"changes_lin.gpkg", crs, true);
		GeoPackageUtil.save(cd.getUnchanged(), outpath+"unchanged_lin.gpkg", crs, true);
		GeoPackageUtil.save(cd.getHausdorffGeomChanges(), outpath+"hfgeoms_lin.gpkg", crs, true);
		GeoPackageUtil.save(cd.getGeomChanges(), outpath+"geomch_lin.gpkg", crs, true);
		GeoPackageUtil.save(sus, outpath+"suspects_lin.gpkg", crs, true);
		 */

		//test equals function
		assertFalse( ChangeDetection.equals(fsIni, fsFin, resolution) );
		assertFalse( ChangeDetection.equals(fsFin, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsIni, fsIni, resolution) );
		assertTrue( ChangeDetection.equals(fsFin, fsFin, resolution) );

		//test change application
		ChangeDetection.applyChanges(fsIni, cd.getChanges());
		assertTrue( ChangeDetection.equals(fsIni, fsFin, resolution) );
		assertTrue(JTSGeomUtil.checkGeometry(fsIni, true, MultiLineString.class));
		assertEquals(0, FeatureUtil.checkIdentfier(fsIni, null).size());
	}

}
