/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.regionsimplify;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import eu.europa.ec.eurostat.jgiscotools.graph.algo.GraphUtils;
import eu.europa.ec.eurostat.jgiscotools.graph.base.TopologyAnalysis;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * @author julien Gaffuri
 *
 */
public class TEdgeSimplifierRamerDouglasPeucker extends TEdgeSimplifier {

	private double resolution;
	private boolean preserveTopology = false;

	public TEdgeSimplifierRamerDouglasPeucker(AEdge agent, double resolution, boolean preserveTopology) {
		super(agent);
		this.resolution = resolution;
		this.preserveTopology = preserveTopology;
	}

	@Override
	public void apply() {
		Edge e = getAgent().getObject();
		double area = GraphUtils.getArea(e);
		LineString lsIni = e.getGeometry(), lsFin;

		if(preserveTopology){
			TopologyPreservingSimplifier tr = new TopologyPreservingSimplifier(lsIni);
			tr.setDistanceTolerance(resolution);
			lsFin = (LineString) tr.getResultGeometry();
		} else {
			//LineString lsFin = (LineString) DouglasPeuckerSimplifier.simplify(lsIni, resolution);
			DouglasPeuckerSimplifier rdps = new DouglasPeuckerSimplifier(lsIni);
			rdps.setDistanceTolerance(resolution);
			rdps.setEnsureValid(true);
			lsFin = (LineString) rdps.getResultGeometry();
		}

		if(TopologyAnalysis.isClosed(e)){
			//TODO apply scaling
		}

		e.setGeom(lsFin.getCoordinates());

		//scale closed lines
		postScaleClosed(e, area);
	}


	public String toString(){
		return getClass().getSimpleName() + "(res="+Util.round(resolution, 3)+";topo="+preserveTopology+")";
	}
}
