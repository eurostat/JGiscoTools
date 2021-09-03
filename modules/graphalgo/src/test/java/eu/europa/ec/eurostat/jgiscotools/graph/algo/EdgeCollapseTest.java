package eu.europa.ec.eurostat.jgiscotools.graph.algo;

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

		assertTrue( g.getFaces().size() == 2 );
		assertTrue( g.getNodes().size() == 9 );
		assertTrue( g.getEdges().size() == 10 );

		//TODO

	}

}
