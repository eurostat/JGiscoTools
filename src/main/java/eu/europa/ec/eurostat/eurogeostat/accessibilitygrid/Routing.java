/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.graph.build.feature.FeatureGraphGenerator;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author julien Gaffuri
 *
 */
public class Routing {
	private static Logger logger = Logger.getLogger(Routing.class.getName());


	public static void main(String[] args) throws Exception {
		logger.info("Start");

		logger.setLevel(Level.ALL);

		String networkFile = "file:\\E:/dissemination/shared-data/EGM/EGM_2019_SHP_20190312/DATA/FullEurope/RoadL.shp";
		//String networkFile = "file:\\E:/dissemination/shared-data/ERM/ERM_2019.1_shp/Data/RoadL.shp";

		Map<String, Serializable> map = new HashMap<>();
		map.put( "url", new URL(networkFile)  );
		DataStore store = DataStoreFinder.getDataStore(map);
		FeatureCollection<?,?> fc =  store.getFeatureSource(store.getTypeNames()[0]).getFeatures();
		store.dispose();

		System.out.println(fc.size());

		Routing rt = new Routing(fc);

		logger.info("End");
	}


	private Graph graph;

	private EdgeWeighter edgeWeighter;
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

		System.out.println(fc);

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





	//get closest node from a position
	public Node getNode(Coordinate c){
		double dMin = Double.MAX_VALUE;
		Node nMin=null;
		for(Object o : graph.getNodes()){
			Node n = (Node)o;
			double d = getPosition(n).distance(c); //TODO fix that !
			//double d=Utils.getDistance(getPosition(n), c);
			if(d==0) return n;
			if(d<dMin) {dMin=d; nMin=n;}
		}
		return nMin;
	}

	//get the position of a graph node
	private Coordinate getPosition(Node n){
		if(n==null) return null;
		Point pt = (Point)n.getObject();
		if(pt==null) return null;
		return pt.getCoordinate();
	}

	public Path getShortestPathDijkstra(Graph g, Node oN, Node dN){
		return getShortestPathDijkstra(g, oN, dN, this.edgeWeighter);
	}
	public Path getShortestPathDijkstra(Graph g, Node oN, Node dN, EdgeWeighter edgeWeighter){
		DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(g, oN, edgeWeighter);
		pf.calculate();
		return pf.getPath(dN);
	}

}
