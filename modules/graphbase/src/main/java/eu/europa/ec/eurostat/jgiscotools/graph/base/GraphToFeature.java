/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.graph.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.linemerge.LineMerger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Face;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.GraphElement;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Node;

/**
 * 
 * Transform graph elements in features.
 * Convenient to export them and show them in a software.
 * 
 * @author julien Gaffuri
 *
 */
public class GraphToFeature {
	private final static Logger LOGGER = LogManager.getLogger(GraphToFeature.class.getName());

	//node
	public static Feature asFeature(Node n){
		Feature f = new Feature();
		f.setGeometry(n.getGeometry());
		f.setID(n.getId());
		f.setAttribute("id", n.getId());
		f.setAttribute("value", n.value);
		f.setAttribute("edg_in_nb", n.getInEdges().size());
		f.setAttribute("edg_out_nb", n.getOutEdges().size());
		String txt=null;
		for(Edge e:n.getInEdges()) txt=(txt==null?"":txt+";")+e.getId();
		f.setAttribute("edges_in", txt);
		txt=null;
		for(Edge e:n.getOutEdges()) txt=(txt==null?"":txt+";")+e.getId();
		f.setAttribute("edges_out", txt);
		Collection<Face> faces = n.getFaces();
		f.setAttribute("face_nb", faces.size());
		txt=null;
		for(Face d:faces) txt=(txt==null?"":txt+";")+d.getId();
		f.setAttribute("faces", txt);
		f.setAttribute("type", TopologyAnalysis.getTopologicalType(n));
		return f;
	}

	//edge
	public static Feature asFeature(Edge e){
		Feature f = new Feature();
		f.setGeometry(e.getGeometry());
		f.setID(e.getId());
		f.setAttribute("id", e.getId());
		f.setAttribute("value", e.value);
		f.setAttribute("n1", e.getN1()!=null? e.getN1().getId() : "null");
		f.setAttribute("n2", e.getN2()!=null? e.getN2().getId() : "null");
		f.setAttribute("face_1", e.f1!=null?e.f1.getId():null);
		f.setAttribute("face_2", e.f2!=null?e.f2.getId():null);
		f.setAttribute("coastal", TopologyAnalysis.getCoastalType(e));
		f.setAttribute("topo", TopologyAnalysis.getTopologicalType(e));
		return f;
	}

	//face
	public static Feature asFeature(Face face) {
		Feature f = new Feature();

		f.setGeometry(face.getGeom());
		if(f.getGeometry()==null) {
			LOGGER.warn("NB: null geom for face "+face.getId());
		}
		else if(!f.getGeometry().isValid()) {
			LOGGER.warn("NB: non valide geometry for face "+face.getId());
		}

		f.setID(face.getId());
		f.setAttribute("id", face.getId());
		f.setAttribute("value", face.value);
		f.setAttribute("edge_nb", face.getEdges().size());
		String txt=null;
		for(Edge e:face.getEdges()) txt=(txt==null?"":txt+";")+e.getId();
		f.setAttribute("edge", txt);
		f.setAttribute("type", TopologyAnalysis.getTopologicalType(face));
		f.setAttribute("face_nb", face.getTouchingFaces().size());
		return f;
	}

	//generic
	public static Feature asFeature(GraphElement ge) {
		if(ge instanceof Node) return asFeature((Node)ge);
		else if(ge instanceof Edge) return asFeature((Edge)ge);
		else if(ge instanceof Face) return asFeature((Face)ge);
		else return null;
	}

	//collections
	public static <T extends GraphElement> Collection<Feature> asFeature(Collection<T> ges){
		HashSet<Feature> fs = new HashSet<Feature>();
		for(T ge : ges)
			fs.add(asFeature(ge));
		return fs;
	}





	//get objects attached to graph elements as features
	public static <T extends GraphElement> Set<Feature> getAttachedFeatures(Collection<T> ges) {
		Set<Feature> out = new HashSet<Feature>();
		for(GraphElement e : ges)
			if(e.obj != null)
				out.add((Feature) e.obj);
		return out;
	}

	//update the object geometries
	public static void updateEdgeLinearFeatureGeometry(Collection<Edge> es) {

		//index edges by feature
		HashMap<Feature, ArrayList<Edge>> ind = new HashMap<Feature, ArrayList<Edge>>();
		for(Edge e : es) {
			if(e.obj == null) continue;
			ArrayList<Edge> es_ = ind.get(e.obj);
			if(es_ == null) { es_ = new ArrayList<Edge>(); ind.put((Feature) e.obj, es_); }
			es_.add(e);
		}

		for(Feature f : ind.keySet()) {
			//build new geometry from edges
			LineMerger lm = new LineMerger();
			for(Edge e : ind.get(f)) lm.add(e.getGeometry());
			@SuppressWarnings("unchecked")
			ArrayList<LineString> ml = (ArrayList<LineString>) lm.getMergedLineStrings();

			if(ml.size() == 0)
				f.setGeometry( new GeometryFactory().createLineString() );
			else if(ml.size() == 1)
				f.setGeometry(ml.iterator().next());
			else
				f.setGeometry( new GeometryFactory().createMultiLineString( (LineString[])ml.toArray(new LineString[ml.size()])) );
		}
	}

}
