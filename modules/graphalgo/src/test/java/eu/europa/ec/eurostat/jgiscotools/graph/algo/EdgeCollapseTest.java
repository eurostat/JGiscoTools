package eu.europa.ec.eurostat.jgiscotools.graph.algo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Face;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Graph;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Node;

public class EdgeCollapseTest {

	@Test
	public void test1() {

		Graph g = new Graph();
		Node n1 = g.buildNode(0,0);
		Node n2 = g.buildNode(2,0);
		Node n3 = g.buildNode(1,1);
		Node n4 = g.buildNode(1,-1);
		Node n11 = g.buildNode(-1,1);
		Node n12 = g.buildNode(-1,0);
		Node n13 = g.buildNode(-1,-1);
		Node n21 = g.buildNode(3,1);
		Node n22 = g.buildNode(3,0);
		Node n23 = g.buildNode(3,-1);

		Edge e12 = g.buildEdge(n1, n2);
		Edge e13 = g.buildEdge(n1, n3);
		Edge e23 = g.buildEdge(n2, n3);
		Edge e41 = g.buildEdge(n4, n1);
		Edge e42 = g.buildEdge(n4, n2);

		Edge e111 = g.buildEdge(n11, n1);
		Edge e112 = g.buildEdge(n1, n12);
		Edge e131 = g.buildEdge(n13, n1);
		Edge e221 = g.buildEdge(n2, n21);
		Edge e222 = g.buildEdge(n22, n2);
		Edge e223 = g.buildEdge(n2, n23);

		Face f1 = g.buildFace(e12, e23, e13);
		Face f2 = g.buildFace(e12, e41, e42);


		//collapse
		Coordinate c = EdgeCollapse.collapseEdge(e12);

		assertTrue( c.distance(new Coordinate(1.0,0)) == 0 );

		//g
		assertTrue( g.getFaces().size() == 2 );
		assertTrue( g.getNodes().size() == 9 );
		assertTrue( g.getEdges().size() == 10 );

		//faces
		assertTrue( f1.getEdges().size() == 2 );
		assertTrue( f1.getEdges().contains(e13) );
		assertTrue( f1.getEdges().contains(e23) );
		assertTrue( f2.getEdges().size() == 2 );
		assertTrue( f2.getEdges().contains(e41) );
		assertTrue( f2.getEdges().contains(e42) );

		//e12 - deleted?
		assertFalse( g.getEdges().contains(e12) );
		assertTrue( e12.f1 == null );
		assertTrue( e12.f2 == null );
		assertTrue( e12.getN1() == null );
		assertTrue( e12.getN2() == null );

		//other edges - nodes
		assertTrue( e13.getN1() == n1 );
		assertTrue( e13.getN2() == n3 );
		assertTrue( e23.getN1() == n1 );
		assertTrue( e23.getN2() == n3 );
		assertTrue( e41.getN1() == n4 );
		assertTrue( e41.getN2() == n1 );
		assertTrue( e42.getN1() == n4 );
		assertTrue( e42.getN2() == n1 );

		assertTrue( e111.getN1() == n11 );
		assertTrue( e111.getN2() == n1 );
		assertTrue( e112.getN1() == n1 );
		assertTrue( e112.getN2() == n12 );
		assertTrue( e131.getN1() == n13 );
		assertTrue( e131.getN2() == n1 );
		assertTrue( e221.getN1() == n1 );
		assertTrue( e221.getN2() == n21 );
		assertTrue( e222.getN1() == n22 );
		assertTrue( e222.getN2() == n1 );
		assertTrue( e223.getN1() == n1 );
		assertTrue( e223.getN2() == n23 );

		//other edges - faces
		assertTrue( e12.getFaces().size() == 0 );
		assertTrue( e13.f1 == f1 );
		assertTrue( e13.f2 == null );
		assertTrue( e23.f1 == f1 );
		assertTrue( e23.f2 == null );
		assertTrue( e41.f1 == f2 );
		assertTrue( e41.f2 == null );
		assertTrue( e42.f1 == f2 );
		assertTrue( e42.f2 == null );
		assertTrue( e111.getFaces().size() == 0 );
		assertTrue( e112.getFaces().size() == 0 );
		assertTrue( e131.getFaces().size() == 0 );
		assertTrue( e221.getFaces().size() == 0 );
		assertTrue( e222.getFaces().size() == 0 );
		assertTrue( e223.getFaces().size() == 0 );


		//n1
		assertTrue( n1.getFaces().size() == 2 );
		assertTrue( n1.getEdges().size() == 10 );
		assertTrue( n1.getOutEdges().size() == 5 );
		assertTrue( n1.getInEdges().size() == 5 );

		//n2 - deleted ?
		assertFalse( g.getNodes().contains(n2) );
		assertTrue( n2.getFaces().size() == 0 );
		assertTrue( n2.getEdges().size() == 0 );
		assertTrue( n2.getOutEdges().size() == 0 );
		assertTrue( n2.getInEdges().size() == 0 );

		//other nodes
		assertTrue( n3.getInEdges().size() == 2 );
		assertTrue( n3.getOutEdges().size() == 0 );
		assertTrue( n4.getInEdges().size() == 0 );
		assertTrue( n4.getOutEdges().size() == 2 );
		assertTrue( n11.getInEdges().size() == 0 );
		assertTrue( n11.getOutEdges().size() == 1 );
		assertTrue( n12.getInEdges().size() == 1 );
		assertTrue( n12.getOutEdges().size() == 0 );
		assertTrue( n13.getInEdges().size() == 0 );
		assertTrue( n13.getOutEdges().size() == 1 );
		assertTrue( n21.getInEdges().size() == 1 );
		assertTrue( n21.getOutEdges().size() == 0 );
		assertTrue( n22.getInEdges().size() == 0 );
		assertTrue( n22.getOutEdges().size() == 1 );
		assertTrue( n23.getInEdges().size() == 1 );
		assertTrue( n23.getOutEdges().size() == 0 );

	}

}
