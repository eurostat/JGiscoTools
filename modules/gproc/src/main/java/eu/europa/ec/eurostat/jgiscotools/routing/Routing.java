/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.routing;

import java.io.IOException;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.build.GraphBuilder;
import org.geotools.graph.build.GraphGenerator;
import org.geotools.graph.build.basic.BasicGraphGenerator;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Graphable;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.linemerge.LineMerger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

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
	/**
	 * @return
	 */
	public Graph getGraph() { return graph; }

	/**
	 * The weighter used to determine the cost of traveling along a section.
	 * By default, the weight is set as the length of the section.
	 */
	private EdgeWeighter edgeWeighter;
	/**
	 * @param edgeWeighter
	 */
	public void setEdgeWeighter(EdgeWeighter edgeWeighter) { this.edgeWeighter = edgeWeighter; }

	/**
	 * Set the weighter based on a speed calculator.
	 * 
	 * @param sc
	 */
	public void setEdgeWeighter(SpeedCalculator sc) {
		this.edgeWeighter = new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				//weight is the transport duration, in minutes
				Feature sf = (Feature) e.getObject();
				double speedMPerMinute = 1000/60 * sc.getSpeedKMPerHour(sf);
				double distanceM = ((Geometry) sf.getGeometry()).getLength();
				return distanceM / speedMPerMinute;
			}
		};
	}

	/**
	 * Set the weighter based on a attribute.
	 * 
	 * @param costAttribute
	 */
	public void setEdgeWeighter(String costAttribute) {
		this.edgeWeighter = new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				Feature sf = (Feature) e.getObject();
				String costS = sf.getAttribute(costAttribute).toString();
				double cost = Double.parseDouble(costS);
				return cost;
			}
		};
	}

	/**
	 * @return
	 */
	public EdgeWeighter getEdgeWeighter() {
		if(edgeWeighter == null) {
			edgeWeighter = new DijkstraIterator.EdgeWeighter() {
				public double getWeight(Edge e) {
					Feature f = (Feature) e.getObject();
					Geometry g = (Geometry) f.getGeometry();
					return g.getLength();
				}
			};
		}
		return edgeWeighter;
	}




	/**
	 * @param networkFileURL
	 * @param edgeWeighter
	 * @throws IOException
	 */
	/*public Routing(URL networkFileURL, EdgeWeighter edgeWeighter) throws IOException {
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
	public Routing(URL networkFileURL) throws IOException { this(networkFileURL, null); }
	 */

	public Routing(Collection<Feature> fs) { buildGraph(fs); }

	/**
	 * Build the graph from the input linear features.
	 * 
	 * @param fs
	 */
	private void buildGraph(Collection<Feature> fs) {
		if(logger.isDebugEnabled()) logger.debug("Build graph from "+fs.size()+" lines.");
		this.graph = null;
		FeatureGraphGenerator2 gGen = new FeatureGraphGenerator2(new LineStringGraphGenerator());
		for(Feature f : fs) gGen.add(f);
		this.graph = gGen.getGraph();
	}



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



	/*
	public AStarShortestPathFinder getAStarShortestPathFinder(Node oN, Node dN){
		//define default A* functions
		AStarFunctions afun = new AStarFunctions(dN) {
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
		return pf;
	}
	 */

	/**
	 * Get the shortest path from a origin to a destination position using A* algorithm.
	 * 
	 * @param oC
	 * @param dC
	 * @return
	 */
	/*public Path getAStarShortestPathFinder(Coordinate oC, Coordinate dC){

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
	 */


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
			Feature f = (Feature) e.getObject();
			if(f==null) continue;
			Geometry mls = (Geometry)f.getGeometry();
			lm.add(mls);
		}
		Collection<?> lss = lm.getMergedLineStrings();
		Geometry geom = new GeometryFactory().createMultiLineString( lss.toArray(new LineString[lss.size()]) );

		//build feature
		Feature f = new Feature();
		f.setGeometry(geom);

		return f;
	}














	private static class FeatureGraphGenerator2 extends BasicGraphGenerator {
		GraphGenerator decorated;
		public FeatureGraphGenerator2(GraphGenerator decorated) { this.decorated = decorated; }
		public Graph getGraph() { return decorated.getGraph(); }
		public GraphBuilder getGraphBuilder() { return decorated.getGraphBuilder(); }
		public GraphGenerator getDecorated() { return decorated; }

		public Graphable add(Object obj) {
			Feature feature = (Feature)obj;
			Graphable g = decorated.add(feature.getGeometry());
			Geometry geom = (Geometry) g.getObject();
			feature.setGeometry(geom);
			g.setObject(feature);
			return g;
		}

		public Graphable remove(Object obj) {
			Feature feature = (Feature)obj;
			return decorated.remove(feature.getGeometry());
		}

		public Graphable get(Object obj) {
			Feature feature = (Feature) obj;
			return decorated.get(feature.getGeometry());
		}
	}

}
