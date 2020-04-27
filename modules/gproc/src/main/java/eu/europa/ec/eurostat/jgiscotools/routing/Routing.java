/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.routing;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.graph.build.feature.FeatureGraphGenerator;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.AStarIterator.AStarFunctions;
import org.geotools.graph.traverse.standard.AStarIterator.AStarNode;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;

/**
 * A class to compute 'shortest' pathes from a network composed of linear features.
 * 
 * @author julien Gaffuri
 *
 */
public class Routing {
	private static Logger logger = LogManager.getLogger(Routing.class.getName());

	/**
	 * The graph structure used to compute routes.
	 */
	private Graph graph;
	public Graph getGraph() { return graph; }

	/**
	 * The weighter used to determine the cost of traveling along a section.
	 * By default, the weight is set as the length of the section.
	 */
	private EdgeWeighter edgeWeighter;
	public void setEdgeWeighter(EdgeWeighter edgeWeighter) { this.edgeWeighter = edgeWeighter; }
	public EdgeWeighter getEdgeWeighter() {
		if(edgeWeighter == null) {
			edgeWeighter = new DijkstraIterator.EdgeWeighter() {
				public double getWeight(Edge e) {
					SimpleFeature f = (SimpleFeature) e.getObject();
					Geometry g = (Geometry) f.getDefaultGeometry();
					return g.getLength();
				}
			};
		}
		return edgeWeighter;
	}




	public Routing(URL networkFileURL, EdgeWeighter edgeWeighter) throws IOException {
		//load features
		if(logger.isDebugEnabled()) logger.debug("Get line features");
		Map<String, Serializable> map = new HashMap<>();
		map.put( "url", networkFileURL );
		DataStore store = DataStoreFinder.getDataStore(map);
		FeatureCollection<?,?> fc =  store.getFeatureSource(store.getTypeNames()[0]).getFeatures();
		store.dispose();

		buildGraph(fc);
		this.edgeWeighter = edgeWeighter;
	}
	public Routing(FeatureCollection<?,?> fc, EdgeWeighter edgeWeighter) throws IOException {
		buildGraph(fc);
		this.edgeWeighter = edgeWeighter;
	}

	/**
	 * Build the graph from the input linear features.
	 * 
	 * @param fc
	 */
	private void buildGraph(FeatureCollection<?,?> fc) {
		if(logger.isDebugEnabled()) logger.debug("Build graph from "+fc.size()+" lines.");
		this.graph = null;
		FeatureIterator<?> it = fc.features();
		FeatureGraphGenerator gGen = new FeatureGraphGenerator(new LineStringGraphGenerator());
		while(it.hasNext()) gGen.add(it.next());
		this.graph = gGen.getGraph();
		it.close();
	}

	public Routing(URL networkFileURL) throws IOException { this(networkFileURL, null); }
	public Routing(FeatureCollection<?,?> fc) throws IOException { this(fc, null); }
	public Routing(ArrayList<Feature> fs, SimpleFeatureType ft) throws IOException { this(SimpleFeatureUtil.get(fs, ft)); }




	/*/spatial index of nodes
	private STRtree nodesIndex = null;
	public STRtree getNodesIndex() {
		if(nodesIndex == null) {
			nodesIndex = new STRtree();
			for(Object o : graph.getNodes())
				nodesIndex.insert(((Point)((Node)o).getObject()).getEnvelopeInternal(), o);
		}
		return nodesIndex;
	}

	//get closest node from a position
	public Node getNode(Coordinate c) {
		try {
			Envelope env = new Envelope(); env.expandToInclude(c);
			Object o = getNodesIndex().nearestNeighbour(env, c, idist);
			Node n = (Node)o;
			return n;
		} catch (Exception e) {
			//logger.warn("Could not find graph node around position "+c);
			return null;
		}
	}
	private static final ItemDistance idist = new ItemDistance() {
		@Override
		public double distance(ItemBoundable i1, ItemBoundable i2) {
			Node n = (Node) i1.getItem();
			Coordinate c = (Coordinate) i2.getItem();
			return c.distance( ((Point)n.getObject()).getCoordinate() );
		}
	};
	 */

	/**
	 * Get closest node from a position.
	 * 
	 * @param c A position.
	 * @return The closest node from the position.
	 */
	public Node getNode(Coordinate c){
		//TODO use spatial index
		double d, dMin = Double.MAX_VALUE;
		Node n, nMin=null;
		for(Object o : graph.getNodes()){
			n = (Node)o;
			d = ((Point)n.getObject()).getCoordinate().distance(c);
			if(d==0) return n;
			if(d<dMin) {dMin=d; nMin=n;}
		}
		return nMin;
	}




	public AStarShortestPathFinder getAStarShortestPathFinder(Node oN, Node dN){
		//define default A* functions
		AStarFunctions afun = null;
		afun = new AStarFunctions(dN) {
			@Override
			public double cost(AStarNode ns0, AStarNode ns1) {
				//return the edge weighter value
				Edge e = ns0.getNode().getEdge(ns1.getNode());
				return getEdgeWeighter().getWeight(e);
			}
			@Override
			public double h(Node n) {
				//return the point to point 'cost' TODO !!!
				Point dP = (Point) dN.getObject();
				Point p = (Point) n.getObject();
				return p.distance(dP);
			}
		};
		AStarShortestPathFinder pf = new AStarShortestPathFinder(graph, oN, dN, afun);
		pf.calculate();
		return pf;
	}


	/**
	 * Get the shortest path from a origin to a destination position using A* algorithm.
	 * 
	 * @param oC
	 * @param dC
	 * @return
	 */
	public Path getAStarShortestPathFinder(Coordinate oC, Coordinate dC){

		//get origin node
		Node oN = getNode(oC);
		if(oN == null) {
			logger.error("Could not find node around position " + oC);
			return null;
		}

		//get destination node
		Node dN = getNode(dC);
		if(dN == null) {
			logger.error("Could not find node around position " + dC);
			return null;
		}

		//compute shortest path
		Path path = null;
		try { path = getAStarShortestPathFinder(oN, dN).getPath();
		} catch (Exception e) { e.printStackTrace(); }
		return path;
	}



	public DijkstraShortestPathFinder getDijkstraShortestPathFinder(Node oN){
		DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(graph, oN, getEdgeWeighter());
		pf.calculate();
		return pf;
	}
	public DijkstraShortestPathFinder getDijkstraShortestPathFinder(Coordinate oC) {
		Node oN = getNode(oC);
		if(oN == null) {
			logger.error("Could not find node around position " + oC);
			return null;
		}
		return getDijkstraShortestPathFinder(oN);
	}

	/**
	 * Get the shortest path from a origin to a destination position using Dijkstra algorithm.
	 * 
	 * @param oC
	 * @param dC
	 * @return
	 */
	public Path getDijkstraShortestPath(Coordinate oC, Coordinate dC){
		Node dN = getNode(dC);
		if(dN == null) {
			logger.error("Could not find node around position " + dC);
			return null;
		}
		return getDijkstraShortestPathFinder(oC).getPath(dN);
	}


	/**
	 * Transorm a path into a feature.
	 * 
	 * @param path
	 * @return
	 */
	public static Feature toFeature(Path path) {

		//build line geometry as a merge of the path edges.
		LineMerger lm = new LineMerger();
		for(Object o : path.getEdges()){
			Edge e = (Edge)o;
			SimpleFeature f = (SimpleFeature) e.getObject();
			if(f==null) continue;
			Geometry mls = (Geometry)f.getDefaultGeometry();
			lm.add(mls);
		}
		Collection<?> lss = lm.getMergedLineStrings();
		Geometry geom = new GeometryFactory().createMultiLineString( lss.toArray(new LineString[lss.size()]) );

		//build feature
		Feature f = new Feature();
		f.setGeometry(geom);

		return f;
	}

}
