package eu.europa.ec.eurostat.jgiscotools.algo.distances;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import eu.europa.ec.eurostat.jgiscotools.algo.distances.HausdorffDistance;
import junit.framework.TestCase;

/**
 * @author Julien Gaffuri
 *
 */
public class HausdorffDistanceTest extends TestCase {
	private final WKTReader wr = new WKTReader();

	public HausdorffDistanceTest(String name) { super(name); }

	/*
	public static void main(String[] args) {
		junit.textui.TestRunner.run(HausdorffDistanceTest.class);
	}
	 */


	public void testNull() throws Exception {
		Logger.getLogger(HausdorffDistance.class.getName()).setLevel(Level.OFF);
		HausdorffDistance hd = new HausdorffDistance(null, wr.read("LINESTRING(0 0, 100 0)"));
		assertNull(hd.getC0());
		assertNull(hd.getC1());
		assertTrue(Double.isNaN(hd.getDistance()));
	}
	public void testEmpty() throws Exception {
		Logger.getLogger(HausdorffDistance.class.getName()).setLevel(Level.OFF);
		HausdorffDistance hd = new HausdorffDistance(wr.read("LINESTRING EMPTY"), wr.read("LINESTRING(0 0, 100 0)"));
		assertNull(hd.getC0());
		assertNull(hd.getC1());
		assertTrue(Double.isNaN(hd.getDistance()));
	}

	public void test1() throws Exception {
		HausdorffDistance hd = new HausdorffDistance(
				wr.read("LINESTRING(0 0, 100 0, 200 20)"),
				wr.read("LINESTRING(0 0, 100 0, 200 20)")
				);
		assertEquals(hd.getDistance(), 0.0);
	}


	private void runTest(Geometry g0, Geometry g1, double expectedDistance, Coordinate expectedC0, Coordinate expectedC1) {
		HausdorffDistance hd = new HausdorffDistance(g0, g1);
		assertEquals(hd.getDistance(), expectedDistance);
		assertEquals(hd.getC0().distance(expectedC0), 0.0);
		assertEquals(hd.getC1().distance(expectedC1), 0.0);
	}

	public void test2() throws Exception {
		runTest(wr.read("LINESTRING(0 10, 100 10, 200 30)"), wr.read("LINESTRING(0 0, 100 0, 200 20)"), 10.0, new Coordinate(0, 10), new Coordinate(0, 0));
	}
	public void test3() throws Exception {
		runTest(wr.read("LINESTRING(0 0, 100 0, 200 20)"), wr.read("LINESTRING(0 20, 100 20)"), 100.0, new Coordinate(200, 20), new Coordinate(100, 20));
	}
	public void test4() throws Exception {
		runTest(wr.read("LINESTRING(0 20, 100 20)"), wr.read("LINESTRING(0 0, 100 0, 200 20)"), 100.0, new Coordinate(100, 20), new Coordinate(200, 20));
	}
	public void test5() throws Exception {
		runTest(wr.read("LINESTRING (0 0, 2 1)"), wr.read("LINESTRING (0 0, 2 0)"), 1.0, new Coordinate(2, 1), new Coordinate(2, 0));
	}
	public void test6() throws Exception {
		runTest(wr.read("LINESTRING (0 0, 2 0)"), wr.read("LINESTRING (0 1, 1 2, 2 1)"), 2.0, new Coordinate(1, 0), new Coordinate(1, 2));
	}
	public void test7() throws Exception {
		runTest(wr.read("LINESTRING (0 0, 2 0)"), wr.read("MULTIPOINT (0 1, 1 0, 2 1)"), 1.0, new Coordinate(0, 0), new Coordinate(0, 1));
	}
	public void test8() throws Exception {
		runTest(wr.read("LINESTRING (130 0, 0 0, 0 150)"), wr.read("LINESTRING (10 10, 10 150, 130 10)"), 14.142135623730951, new Coordinate(0, 0), new Coordinate(10, 10));
	}

}
