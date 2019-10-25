/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import eu.europa.ec.eurostat.jgiscotools.graph.GraphUtils;
import eu.europa.ec.eurostat.jgiscotools.graph.TopologyAnalysis;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.EdgeScaling;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.NodeDisplacement;
import eu.europa.ec.eurostat.jgiscotools.graph.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.TransformationCancellable;

/**
 * Generic class for edge geometry simplifiers.
 * 
 * @author julien Gaffuri
 *
 */
public abstract class TEdgeSimplifier extends TransformationCancellable<AEdge> {

	public TEdgeSimplifier(AEdge agent) { super(agent); }

	@Override
	public boolean isCancelable() { return true; }

	private LineString geomStore= null;
	private Coordinate closedEdgeNodePosition = null;
	protected double scaleRatio = 1;

	protected void postScaleClosed(Edge e, double targetArea) {
		if(e.getGeometry().isValid()){
			scaleRatio = Math.sqrt( targetArea / GraphUtils.getArea(e) );
			scaleClosed(e);
		}
	}

	protected void scaleClosed(Edge e) {
		if(!TopologyAnalysis.isClosed(e) || scaleRatio == 1) return;
		EdgeScaling.scale(e, scaleRatio);
	}

	@Override
	public void storeState() {
		Edge e = getAgent().getObject();
		geomStore = e.getGeometry();
		if(TopologyAnalysis.isClosed(e)) closedEdgeNodePosition = new Coordinate(e.getN1().getC().x, e.getN1().getC().y);
	}

	@Override
	public void cancel() {
		Edge e = getAgent().getObject();

		if(e.getGeometry().isValid()){
			scaleRatio = 1/scaleRatio;
			scaleClosed(e);
		}

		e.setGeom(geomStore.getCoordinates());
		if(TopologyAnalysis.isClosed(e)) NodeDisplacement.moveTo(e.getN1(), closedEdgeNodePosition.x, closedEdgeNodePosition.y);;
	}

}
