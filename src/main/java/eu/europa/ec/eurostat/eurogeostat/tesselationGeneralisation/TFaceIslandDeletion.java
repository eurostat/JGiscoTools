/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.tesselationGeneralisation;

import java.util.Collection;
import java.util.HashSet;

import eu.europa.ec.eurostat.eurogeostat.datamodel.graph.Edge;
import eu.europa.ec.eurostat.eurogeostat.datamodel.graph.Face;
import eu.europa.ec.eurostat.eurogeostat.datamodel.graph.Graph;
import eu.europa.ec.eurostat.eurogeostat.datamodel.graph.Node;
import eu.europa.ec.eurostat.eurogeostat.transfoengine.TransformationNonCancellable;

/**
 * 
 * Delete a graph face. It should be used only to remove island faces.
 * Otherwise, this operation may result in a hole in the graph tesselation.
 * The edges and nodes which are not linked anymore to any other graph element are also deleted
 * 
 * @author julien Gaffuri
 * 
 */
public class TFaceIslandDeletion extends TransformationNonCancellable<AFace> {

	public TFaceIslandDeletion(AFace agent) { super(agent); }

	@Override
	public void apply() {
		boolean b;

		Face f = getAgent().getObject();
		Graph g = f.getGraph();

		//remove agent
		getAgent().setDeleted(true);
		//if(getAgent().lastUnitFace()) getAgent().aUnit.setDeleted(true);

		//store face edges and nodes
		Collection<Edge> es = new HashSet<Edge>(); es.addAll(f.getEdges());
		Collection<Node> ns = new HashSet<Node>(); ns.addAll(f.getNodes());

		//remove face from graph
		g.remove(f);

		//break link with unit
		if(getAgent().aUnit != null){
			b = getAgent().aUnit.aFaces.remove(getAgent());
			if(!b) System.err.println("Could not remove face agent "+getAgent().getId()+" from tesselation");
		}

		//remove useless edges
		for(Edge e:es){
			if(e.getFaces().size()>0) continue;
			g.remove(e);
			getAgent().getAtesselation().getAEdge(e).setDeleted(true);
		}

		//remove useless nodes
		for(Node n:ns)
			if(n.getFaces().size() == 0) g.remove(n);
	}

}
