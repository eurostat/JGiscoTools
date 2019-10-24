/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.edgematching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.operation.linemerge.LineMerger;

import eu.europa.ec.eurostat.jgiscotools.algo.graph.ConnexComponents;
import eu.europa.ec.eurostat.jgiscotools.algo.graph.GraphBuilder;
import eu.europa.ec.eurostat.jgiscotools.algo.graph.GraphUtils;
import eu.europa.ec.eurostat.jgiscotools.algo.graph.ConnexComponents.EdgeFilter;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.graph.Edge;
import eu.europa.ec.eurostat.jgiscotools.graph.Graph;
import eu.europa.ec.eurostat.jgiscotools.graph.Node;

/**
 * Compute edgematching of network data collected on different regions.
 * 
 * @author julien Gaffuri
 *
 */
public class NetworkEdgeMatching {
	public final static Logger LOGGER = Logger.getLogger(NetworkEdgeMatching.class.getName());

	/**
	 * The network sections to be edge matched. The geometries must be LineString (or MultiLineString with a single element).
	 */
	private ArrayList<Feature> secs;
	public Collection<? extends Feature> getSections() { return secs; }

	/**
	 * The attribute of the sections giving region they belong to.
	 */
	private String rgAtt = "RG";

	/**
	 * The resolutions of the different regions. The key is the region code as specified in 'rgAtt' above, and the value is the resolution value, in meter.
	 * The algorithm does not handle all sections in the same manner depending on their resolution.
	 * If no resolution is specified, the default value is set to 1.0 m.
	 */
	private HashMap<String,Double> resolutions = null;

	/**
	 * A multiplication parameter to increase the snapping distance.
	 */
	private double mult = 1.5;

	/**
	 * If set, the output sections will be added an attribute 'outAtt' describing the role they had in the process.
	 * If set to null, no attribute will be added.
	 */
	private String outAtt = null;

	/**
	 * The matching edges
	 * Those are network edges created during the process, which link two tips of sections that do not belong to the same region, are close to each other, and not already connected (by chance).
	 */
	private ArrayList<Edge> mes;
	public ArrayList<Edge> getMatchingEdges() { return mes; }

	/**
	 * The graph structure used during the process.
	 */
	private Graph g;


	public NetworkEdgeMatching(ArrayList<Feature> sections) { this(sections, null, 1.5, "RG", null); }
	public NetworkEdgeMatching(ArrayList<Feature> sections, HashMap<String,Double> resolutions, double mult, String rgAtt, String outAtt) {
		this.secs = sections;
		this.resolutions = resolutions;
		this.mult = mult;
		this.rgAtt = rgAtt;
		this.outAtt = outAtt;
	}


	/**
	 * Compute the edge matching based on matching edges.
	 */
	public void makeEdgeMatching() {

		LOGGER.info("Ensure input geometries are not collections");
		FeatureUtil.ensureGeometryNotAGeometryCollection(secs);

		if (outAtt != null) {
			LOGGER.info("Initialise outAtt attribute values");
			for(Feature s : secs) s.setAttribute(outAtt, "");
		}

		LOGGER.info("Clip with buffer of all sections, depending on region resolution");
		makeEdgeMatchingBufferClipping();

		LOGGER.info("Build graph structure");
		g = GraphBuilder.buildFromLinearFeaturesNonPlanar(secs);

		LOGGER.info("Build matching edges");
		buildMatchingEdges();

		LOGGER.info("Filter matching edges");
		filterMatchingEdges();

		LOGGER.info("Connect sections");
		extendSectionswithMatchingEdges();
	}


	/**
	 * @param rg The section region code.
	 * @return The resolution value as provided in 'resolutions' dictionnary. 1.0 if not.
	 */
	private double getResolution(String rg) {
		if(resolutions == null) return 1.0;
		Double res = resolutions.get(rg);
		if(res == null) {
			LOGGER.warn("Could not find resolution value for region " + rg);
			return 1.0;
		}
		return res.doubleValue();
	}


	/**
	 * Clip network section geometries with buffer of other network sections having a better resolution.
	 */
	private void makeEdgeMatchingBufferClipping() {

		//build spatial index
		Quadtree si = FeatureUtil.getQuadtree(secs);

		//get maximum resolution
		double resMax = resolutions==null? 1.0 : Collections.max(resolutions.values());

		//copy the sections list
		ArrayList<Feature> secsToCheck = new ArrayList<Feature>();
		secsToCheck.addAll(secs);

		while(secsToCheck.size() > 0) {
			Feature s = secsToCheck.get(0);
			secsToCheck.remove(s);

			if(s.getDefaultGeometry().isEmpty()) {
				secs.remove(s);
				continue;
			}

			String rg = s.getAttribute(rgAtt).toString();
			double res = getResolution(rg);
			Geometry g = (LineString) s.getDefaultGeometry();

			//s to be 'cut' by nearby sections from other regions with better resolution
			Envelope env = g.getEnvelopeInternal(); env.expandBy(resMax*1.01);
			boolean changed = false;
			for(Object s2 : si.query(env)) {
				Feature s_ = (Feature) s2;
				LineString ls_ = (LineString) s_.getDefaultGeometry();

				//filter
				if(s == s_) continue;
				if(ls_.isEmpty()) continue;
				if(! ls_.getEnvelopeInternal().intersects(env)) continue;
				String rg_ = s_.getAttribute(rgAtt).toString();
				if(rg_.equals(rg)) continue;
				if(getResolution(rg_) > res) continue; //s to be cut by those with better resolution only

				//compute buffer
				Geometry buff = ls_.buffer(res);
				if(! g.intersects(buff)) continue;

				//compute difference
				g = g.difference(buff);

				changed = true;
				if (outAtt != null) s_.setAttribute(outAtt, "bufferInvolved");

				//if the entire geometry has been remved, no need to continue
				if(g.isEmpty()) break;
			}

			if(!changed) continue;

			//update the spatial index
			boolean b = si.remove(s.getDefaultGeometry().getEnvelopeInternal(), s);
			if(!b) LOGGER.warn("Failed removing object from spatial index");

			if(g.isEmpty()) {
				secs.remove(s);
				continue;
			}

			if(g instanceof LineString) {
				//update section geometry
				s.setDefaultGeometry(g);
				si.insert(s.getDefaultGeometry().getEnvelopeInternal(), s);
				if (outAtt != null) s.setAttribute(outAtt, "bufferClipped");
			} else {
				//if output geometry has several components, create one object per component
				//TODO should we really do that? Consider case when 2 sections of different regions cross...
				MultiLineString mls = (MultiLineString)g;
				for(int i=0; i<mls.getNumGeometries(); i++) {
					Feature f = new Feature();
					f.setDefaultGeometry( (LineString)mls.getGeometryN(i) );
					f.getAttributes().putAll(s.getAttributes());
					if (outAtt != null) f.setAttribute(outAtt, "bufferClipped"); //TODO use another code? Check result?
					secs.add(f);
					secsToCheck.add(f); //TODO maybe not necessary?
					si.insert(f.getDefaultGeometry().getEnvelopeInternal(), f);
				}
			}
		}
	}


	/**
	 * Build the matching edges.
	 */
	private void buildMatchingEdges() {

		//label each node with a region code
		for(Node n : g.getNodes()) {
			String rg = getEdgesRegion(n);
			if(rg==null) LOGGER.warn("Could not determine region for node around " + n.getC());
			n.obj = rg;
		}

		//initialise collection of matching edges
		if(mes == null) mes = new ArrayList<>(); else mes.clear();

		//Connect node pairs with a matching edge: From different regions and not already connected.
		//The connection distance depends on the the resolution of both regions.
		for(Node n : g.getNodes()) {
			String rg = n.obj.toString();
			double res = mult * getResolution(rg);

			for(Node n_ : g.getNodesAt(n.getGeometry().buffer(res*1.01).getEnvelopeInternal()) ) {
				if(n==n_) continue;
				if(n.getC().distance(n_.getC()) > res) continue;
				if(rg.equals(n_.obj.toString())) continue;

				//exclude already connected nodes
				if( g.getEdge(n, n_) != null || g.getEdge(n_, n) != null ) continue;

				//build matching edge
				Edge e = g.buildEdge(n, n_);
				mes.add(e);
			}
		}
	}

	/**
	 * Check that all edges of a node have the same region code and return it.
	 * 
	 * @param n The node.
	 * @return Return the unique region code of the eges. If there is no edge or some edges have different regions, return null.
	 */
	private String getEdgesRegion(Node n) {
		String rg = null;
		for(Edge e : n.getEdges()) {
			if(e.obj == null) continue;
			if(rg == null) {
				rg = ((Feature)e.obj).getAttribute(rgAtt).toString();
				continue;
			}
			if( !((Feature)e.obj).getAttribute(rgAtt).toString().equals(rg))
				return null;
		}
		return rg;
	}


	/**
	 * Filter matching edges based on several criteria and configurations.
	 * This operation remove the unnecessary matching edges, that can have a negative impact on the result.
	 */
	private void filterMatchingEdges() {

		//get connex components of matching edges
		Collection<Graph> gcc = ConnexComponents.get(g, new EdgeFilter() {
			@Override
			public boolean keep(Edge e) { return e.obj == null; } //keep only the matching edges
		}, true);

		//go through the connex components
		for(Graph cc : gcc) {

			//count the number of edges
			double eNb = cc.getEdges().size();

			if(eNb == 1) continue;

			//TODO do something for cases with more than 3 edges. break connex components (by detecting isthmus?) ? remove the longest(s)? check intersections?
			//TODO maybe it is general to size=3 also...

			if(eNb == 3) {

				//get the three edges and their length
				Iterator<Edge> it = cc.getEdges().iterator();
				Edge me1=it.next(), me2=it.next(), me3=it.next();
				double d1=me1.getGeometry().getLength(), d2=me2.getGeometry().getLength(), d3=me3.getGeometry().getLength();

				if(cc.getNodes().size() == 3) {
					//triangle case: remove the longest edge
					Edge meToRemove = (d1>d2&&d1>d3)? me1 : (d2>d1&&d2>d3) ? me2 : me3;
					mes.remove(meToRemove); g.remove(meToRemove);
				} else if(cc.getNodes().size() == 4) {
					Node n1 = GraphUtils.areConnected(me2, me3), n2 = GraphUtils.areConnected(me3, me1), n3 = GraphUtils.areConnected(me1, me2);
					if( n1==null || n2==null || n3==null ) {
						//line structure: remove the edge in the middle
						Edge meToRemove = n1==null? me1 : n2==null? me2 : me3;
						mes.remove(meToRemove); g.remove(meToRemove);
					} else if(n1==n2 && n2==n3) {
						//star structure: do nothing
					}
				}
			}

			if(eNb == 2) {
				//handle special case with triangular structure with 2 matching edges, that arrive to the same node.
				//in such case, the longest matching edge is removed
				Iterator<Edge> it = cc.getEdges().iterator();
				Edge me1 = it.next(), me2 = it.next();
				Node n = me1.getN1()==me2.getN1()||me1.getN1()==me2.getN2()?me1.getN1() : me1.getN2()==me2.getN1()||me1.getN2()==me2.getN2()?me1.getN2() : null;
				Node n1 = me1.getN1()==n?me1.getN2():me1.getN1();
				Node n2 = me2.getN1()==n?me2.getN2():me2.getN1();
				//is there an edge between n1 and n2?
				if( g.getEdge(n1, n2) == null && g.getEdge(n2, n1) == null ) continue;
				//remove longest matching edge
				Edge meToRemove = me1.getGeometry().getLength() > me2.getGeometry().getLength() ? me1 : me2;
				mes.remove(meToRemove); g.remove(meToRemove);
			}
		}

	}




	/**
	 * Connect sections based on the matching edges by either extending them, or (when not possible) creating a new feature based on matching edges.
	 */
	private void extendSectionswithMatchingEdges() {

		for(Edge me : mes) {

			//get candidate section to extend: It depends on the number of eges connected to the nodes.
			Node n1 = me.getN1(), n2 = me.getN2();

			//no way to extend an existing section: Create a new section from matching edge.
			if(n1.getEdges().size()>2 && n2.getEdges().size()>2) {
				//create a new section from matching edge
				Feature f = new Feature();
				f.setDefaultGeometry(me.getGeometry());
				//set properties: choose the ones of the first feature found TODO do better - attribute in common to all edges?
				for(Edge e : me.getEdges()) {
					if(e.obj == null) continue;
					f.getAttributes().putAll( ((Feature)e.obj).getAttributes() );
					break;
				}
				if (outAtt != null) f.setAttribute(outAtt, "created");
				me.obj = f;
				secs.add(f);
				continue;
			}

			//get section to extend
			Feature sectionToExtend = null;
			if(n2.getEdges().size()>2)
				sectionToExtend = getNonMatchingEdgeFromPair(n1.getEdges());
			else if(n1.getEdges().size()>2)
				sectionToExtend = getNonMatchingEdgeFromPair(n2.getEdges());
			else {
				//choose the one of the two sectionw with worst resolution
				Feature s1 = getNonMatchingEdgeFromPair(n1.getEdges()),
						s2 = getNonMatchingEdgeFromPair(n2.getEdges());
				double res1 = getResolution(s1.getAttribute(rgAtt).toString()),
						res2 = getResolution(s2.getAttribute(rgAtt).toString());
				sectionToExtend = res1<res2? s2 : s1;
			}

			//extend selected section
			LineString g = null;
			try {
				g = extendLineString((LineString)sectionToExtend.getDefaultGeometry(), me);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			sectionToExtend.setDefaultGeometry(g);
			if (outAtt != null) sectionToExtend.setAttribute(outAtt, "extended");
		}
	}

	/**
	 * Among a pair of edges, get the feature of the one which is not a matching edge
	 * 
	 * @param edgePair
	 * @return
	 */
	private static Feature getNonMatchingEdgeFromPair(Set<Edge> edgePair) {
		if(edgePair.size() != 2) {
			LOGGER.error("Unexpected number of edges when getSectionToExtend: "+edgePair.size()+". Should be 2.");
			return null;
		}
		Iterator<Edge> it = edgePair.iterator();
		Edge e1 = it.next(), e2 = it.next();
		if(e1.obj != null && e2.obj == null) return (Feature) e1.obj;
		if(e1.obj == null && e2.obj != null) return (Feature) e2.obj;
		LOGGER.warn("Problem in getNonMatchingEdgeFromPair");
		return null;
	}


	/**
	 * Extend line from a segment. The segment is supposed to be an extention of the line.
	 * 
	 * @param ls
	 * @param me
	 * @return
	 * @throws Exception
	 */
	private static LineString extendLineString(LineString ls, Edge me) throws Exception {

		LineMerger lm = new LineMerger();
		lm.add(ls); lm.add(me.getGeometry());
		Collection<?> lss = lm.getMergedLineStrings();
		if(lss.size() != 1) {
			LOGGER.error("Unexpected number of merged lines: "+lss.size()+" (expected value: 1).");
			for(Object l : lss) LOGGER.error(l);
			return null;
		}
		Object out = lss.iterator().next();

		if(out instanceof LineString) return (LineString) out;
		if(out instanceof MultiLineString) {
			MultiLineString out_ = (MultiLineString) out;
			if(out_.getNumGeometries() != 1)
				throw new Exception("Unexpected number of geometries ("+out_.getNumGeometries()+" (expected value: 1).");
			return (LineString) out_.getGeometryN(0);
		}
		throw new Exception("Unexpected geometry type ("+out.getClass().getSimpleName()+". Linear geometry expected.");
	}

}
