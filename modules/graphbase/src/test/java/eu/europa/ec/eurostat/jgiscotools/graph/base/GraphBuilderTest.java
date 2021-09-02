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
	public void testLineMerger1() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 1,0) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 1,0) ) );
	}
	@Test
	public void testLineMerger2() {
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
	public void testLineMerger3() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 50,0) );
		in.add( JTSGeomUtil.createLineString(50,0 , 2,0) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 50,0, 2,0) ));
	}

	//cross
	@Test
	public void testLineMerger4() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,1 , 2,1) );
		in.add( JTSGeomUtil.createLineString(1,0 , 1,2) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(2, out.size());
	}

	//junction
	@Test
	public void testLineMerger5() {
		Collection<LineString> in = new ArrayList<LineString>();
		in.add( JTSGeomUtil.createLineString(0,0 , 1,0) );
		in.add( JTSGeomUtil.createLineString(1,0 , 2,0) );
		in.add( JTSGeomUtil.createLineString(1,0 , 1,1) );

		Collection<LineString> out = GraphBuilder.lineMerge(in);
		assertNotNull(out);
		assertEquals(3, out.size());
	}




	//planify lines


	@Test
	public void testPlanifyLines() {
		/*Collection<LineString> out = GraphBuilder.lineMerge(getExample1());
		assertNotNull(out);
		assertEquals(1, out.size());
		LineString g = out.iterator().next();
		assertTrue( g.equalsTopo( JTSGeomUtil.createLineString(0,0 , 1,0) ) );*/
	}


}
