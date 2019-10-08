/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
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
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.SimpleFeatureUtil;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author julien Gaffuri
 *
 */
public class Routing {
	private static Logger logger = Logger.getLogger(Routing.class.getName());

	private Graph graph;

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




	//spatial index of nodes
	private STRtree nodesIndex = null;
	public STRtree getNodesIndex() {
		if(nodesIndex == null) {
			nodesIndex = new STRtree();
			for(Object o : graph.getNodes())
				nodesIndex.insert(((Point)((Node)o).getObject()).getEnvelopeInternal(), o);
		}
		return nodesIndex;
	}


	/*/get closest node from a position
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
	}*/
	//get closest node from a position
	//TODO use spatial index
	public Node getNode(Coordinate c){
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


	private static final ItemDistance idist = new ItemDistance() {
		@Override
		public double distance(ItemBoundable i1, ItemBoundable i2) {
			Node n = (Node) i1.getItem();
			Coordinate c = (Coordinate) i2.getItem();
			return c.distance( ((Point)n.getObject()).getCoordinate() );
		}
	};


	public AStarShortestPathFinder getAStarShortestPathFinder(Node oN, Node dN){
		AStarFunctions afun = null;
		afun = new AStarFunctions(dN) {
			@Override
			public double cost(AStarNode ns0, AStarNode ns1) {
				Edge e = ns0.getNode().getEdge(ns1.getNode());
				return getEdgeWeighter().getWeight(e);
			}
			@Override
			public double h(Node n) {
				Point dP = (Point) dN.getObject();
				Point p = (Point) n.getObject();
				return p.distance(dP);
			}

		};
		AStarShortestPathFinder pf = new AStarShortestPathFinder(graph, oN, dN, afun);
		pf.calculate();
		return pf;
	}
	public AStarShortestPathFinder getAStarShortestPathFinder(Coordinate oC, Coordinate dC){
		Node oN = getNode(oC);
		if(oN == null) {
			logger.error("Could not find node around position " + oC);
			return null;
		}
		Node dN = getNode(dC);
		if(dN == null) {
			logger.error("Could not find node around position " + dC);
			return null;
		}
		return getAStarShortestPathFinder(oN, dN);
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

	public Path getShortestPathDijkstra(Coordinate oC, Coordinate dC){
		Node dN = getNode(dC);
		if(dN == null) {
			logger.error("Could not find node around position " + dC);
			return null;
		}
		return getDijkstraShortestPathFinder(oC).getPath(dN);
	}

	public static Feature toFeature(Path path) {

		//build line geometry
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
		f.setDefaultGeometry(geom);
		return f;
	}

}
