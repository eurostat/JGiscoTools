/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.graph.algo.stroke;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.algo.distances.SemanticDistance;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Node;

/**
 * @author julien Gaffuri
 *
 */
public class StrokeConnectionSalienceComputation {
	public final static Logger LOGGER = LogManager.getLogger(StrokeConnectionSalienceComputation.class.getName());
	private SemanticDistance sd = new SemanticDistance(true);

	//between 0 (not salient) to 1 (very salient)
	double computeSalience(Node n, Edge e1, Edge e2) {
		//compute attribute similarity indicator (within [0,1])
		double salAttributeSimilarity = getSemanticSimilarity((Feature) e1.obj, (Feature) e2.obj);
		//compute deflation angle indicator (within [0,1])
		double salDeflation = getDeflationIndicator(n, e1, e2);
		//attenuate importance of deflation
		salDeflation = Math.pow(salDeflation, 2);
		//return average
		return (salDeflation+salAttributeSimilarity)*0.5;
	};

	//between 0 (worst case: totally different semantic) to 1 (perfect: same semantic)
	double getSemanticSimilarity(Feature f1, Feature f2) {
		int nb = FeatureUtil.getAttributesSet(f1,f2).size();
		if(nb==0) return 0;
		return 1 - sd.get(f1,f2)/nb;
	}

	//between 0 (worst case) to 1 (perfect, no deflation)
	final double getDeflationIndicator(Node n, Edge e1, Edge e2) {
		Coordinate c = n.getC();
		Coordinate c1 = getCoordinateForDeflation(e1,n);
		Coordinate c2 = getCoordinateForDeflation(e2,n);
		double ang = Angle.angleBetween(c1, c, c2);
		//ang between 0 and Pi
		if(ang<0 || ang>Math.PI)
			LOGGER.warn("Unexpected deflection angle value around "+c+". Should be within [0,Pi]. "+ang);
		return ang / Math.PI;
	}

	protected final Coordinate getCoordinateForDeflation(Edge e, Node n) {
		Coordinate c = null;
		Coordinate[] cs = e.getCoords();
		if(n.getC().distance(cs[0]) == 0)
			c = cs[1];
		else if(n.getC().distance(cs[cs.length-1]) == 0)
			c = cs[cs.length-2];
		else
			LOGGER.warn("Could not getCoordinateForDeflation around "+n.getC());
		return c;
	}

}
