/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.graph.algo;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import eu.europa.ec.eurostat.jgiscotools.algo.base.DouglasPeuckerRamerFilter;
import eu.europa.ec.eurostat.jgiscotools.algo.base.Resolutionise;
import eu.europa.ec.eurostat.jgiscotools.graph.base.GraphBuilder;

/**
 * Some functions to simplify linear meshes.
 * The input is a collection of lines, possibly intersecting, which form a mesh to be simplified.
 * 
 * @author julien Gaffuri
 *
 */
public class GraphSimplify {
	private final static Logger LOGGER = LogManager.getLogger(GraphSimplify.class.getName());


	/*
	public static Collection deleteFlatTriangles(Collection lines, double d) {
		Graph g = GraphBuilder.buildFromLinearFeaturesPlanar( linesToFeatures(lines), true );
		deleteFlatTriangles(g, d);
		return getEdgeGeometries(g.getEdges());
	}

	public static void deleteFlatTriangles(Graph g, double d) {
		Edge e = findEdgeToDeleteForFlatTriangle(g, d);
		while(e != null) {
			if(e.f1!=null) g.remove(e.f1);
			if(e.f2!=null) g.remove(e.f2);
			g.remove(e);
			e = findEdgeToDeleteForFlatTriangle(g, d);
		}
	}

	public static Edge findEdgeToDeleteForFlatTriangle(Graph g, double d) {

		//TODO
		/*for(Face f : g.getFaces()) {
			if(f.getNodes().size() > 3) continue;
			Edge e = f.getLongestEdge();
			//TODO measure minimum heigth and compare to d
			double h = Math.abs(()*()-()*()) / e.getGeometry().getLength();
			if(h>d) continue;
			return e;
		}*/
	//return null;
	//}




	/**
	 * @param <T>
	 * @param lines
	 * @param res
	 * @param startWithShortestEdge
	 * @param planarGraph
	 * @return
	 */
	public static <T extends Geometry> Collection<LineString> collapseTooShortEdgesAndPlanifyLines(Collection<LineString> lines, double res, boolean startWithShortestEdge, boolean planarGraph) {
		int sI=lines.size(), sF=0;
		while(sF < sI) {
			LOGGER.debug(" dtsePlanifyLines loop " + sI);
			lines = EdgeCollapse.collapseTooShortEdges(lines, res, startWithShortestEdge, planarGraph);
			lines = GraphBuilder.planifyLines(lines);
			lines = GraphBuilder.lineMerge(lines);
			sI=sF;
			sF=lines.size();
		}
		return lines;
	}


	/**
	 * @param lines
	 * @param res
	 * @param withRDPFiltering
	 * @return
	 */
	public static Collection<LineString> resPlanifyLines(Collection<LineString> lines, double res, boolean withRDPFiltering) {

		//***
		lines = GraphBuilder.lineMerge(lines);
		if(withRDPFiltering) lines = DouglasPeuckerRamerFilter.get(lines, res);
		lines = Resolutionise.getLine(lines, res);
		lines = GraphBuilder.planifyLines(lines);

		int sI=1,sF=0;
		while(sF<sI) {
			LOGGER.debug(" resPlanifyLines loop " + lines.size());
			sI = lines.size();

			//***
			lines = GraphBuilder.lineMerge(lines);
			if(withRDPFiltering) lines = DouglasPeuckerRamerFilter.get(lines, res);
			lines = Resolutionise.getLine(lines, res);
			lines = GraphBuilder.planifyLines(lines);

			sF = lines.size();
		}
		return lines;
	}

}
