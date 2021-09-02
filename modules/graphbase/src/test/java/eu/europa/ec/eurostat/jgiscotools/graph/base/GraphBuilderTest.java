package eu.europa.ec.eurostat.jgiscotools.graph.base;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.locationtech.jts.geom.LineString;

import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;

public class GraphBuilderTest {


	//line merger


	@Test
	public void testLineMergerSingle() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 1,0) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 1,0) ) );
	}
	@Test
	public void testLineMergerBasic() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 1,0) );
		in.add( JTSGeomUtil.createLineString(1,0 , 2,0) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 1,0, 2,0) ));
	}
	@Test
	public void testLineMergerBasic2() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 50,0) );
		in.add( JTSGeomUtil.createLineString(50,0 , 2,0) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 50,0, 2,0) ));
	}

	@Test
	public void testLineMergerCross() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,1 , 2,1) );
		in.add( JTSGeomUtil.createLineString(1,0 , 1,2) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(2, out.size());
	}

	@Test
	public void testLineMergerJunction() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 1,0) );
		in.add( JTSGeomUtil.createLineString(1,0 , 2,0) );
		in.add( JTSGeomUtil.createLineString(1,0 , 1,1) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(3, out.size());
	}

	@Test
	public void testLineMergerSelfIntersectsPoint() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 1,1, 0,1, 1,0) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 1,1, 0,1, 1,0) ));
	}

	@Test
	public void testLineMergerSelfIntersectsLine() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 2,0, 1,0, 3,0) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 2,0, 1,0, 3,0) ));
	}

	@Test
	public void testLineMergerSelfIntersectsLine2() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 2,0) );
		in.add( JTSGeomUtil.createLineString(2,0, 1,0) );
		in.add( JTSGeomUtil.createLineString(1,0, 3,0) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 2,0, 1,0, 3,0) ));
	}



	//planify lines


	@Test
	public void testPlanifyLines1() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 1,0) );

		Collection<LineString> out = GraphBuilder.planifyLines(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 1,0) ));
	}

	@Test
	public void testPlanifyLinesCross() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,1 , 2,1) );
		in.add( JTSGeomUtil.createLineString(1,0 , 1,2) );

		Collection<LineString> out = GraphBuilder.planifyLines(in);
		assertNotNull(out);
		assertEquals(4, out.size());
	}

	@Test
	public void testPlanifyLinesCross2() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,1 , 2,1) );
		in.add( JTSGeomUtil.createLineString(1,0 , 1,2) );
		in.add( JTSGeomUtil.createLineString(1,0 , 1,2) );
		in.add( JTSGeomUtil.createLineString(0,1.5 , 1.5,0) );

		Collection<LineString> out = GraphBuilder.planifyLines(in);
		assertNotNull(out);
		assertEquals(9, out.size());
	}
	@Test
	public void testPlanifyLinesCross3() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 3, 2.0/3.0) );
		in.add( JTSGeomUtil.createLineString(0,0.5 , 3,0.5) );

		Collection<LineString> out = GraphBuilder.planifyLines(in);
		assertNotNull(out);
		assertEquals(4, out.size());
	}

	@Test
	public void testPlanifyLinesOverlap() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,1 , 2,1) );
		in.add( JTSGeomUtil.createLineString(0,1 , 2,1) );

		Collection<LineString> out = GraphBuilder.planifyLines(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,1 , 2,1) ));
	}

	@Test
	public void testPlanifyLinesOverlap2() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 2,0) );
		in.add( JTSGeomUtil.createLineString(1,0 , 3,0) );

		Collection<LineString> out = GraphBuilder.planifyLines(in);
		assertNotNull(out);
		assertEquals(3, out.size());

		out = GraphBuilder.lineMerge(out);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 1,0, 2,0, 3,0) ));
		assertEquals(4, g.getNumPoints());
	}

	@Test
	public void testPlanifyLinesSelfOverlap() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 2,0, 1,0, 3,0) );

		Collection<LineString> out = GraphBuilder.planifyLines(in);
		assertNotNull(out);
		assertEquals(3, out.size());
	}

	@Test
	public void testPlanifyLinesSelfIntersect() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 1,1, 0,1, 1,0) );

		Collection<LineString> out = GraphBuilder.planifyLines(in);
		assertNotNull(out);
		assertEquals(3, out.size());
	}

	@Test
	public void testPlanifyT() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(1,0 , 1,2) );
		in.add( JTSGeomUtil.createLineString(0,2 , 2,2) );

		Collection<LineString> out = GraphBuilder.planifyLines(in);
		assertNotNull(out);
		assertEquals(3, out.size());
	}
}
