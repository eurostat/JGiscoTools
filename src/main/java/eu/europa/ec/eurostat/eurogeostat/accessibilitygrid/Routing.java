/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
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

	private Graph graph;
	private EdgeWeighter edgeWeighter;

	public Routing(URL networkFileURL) {
		this(networkFileURL, new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				SimpleFeature f = (SimpleFeature) e.getObject();
				Geometry g = (Geometry) f.getDefaultGeometry();
				return g.getLength();
			}
		});
	}
	public Routing(URL networkFileURL, EdgeWeighter edgeWeighter) {
		try {
			Map<String, Serializable> map = new HashMap<>();
			map.put( "url", networkFileURL );
			DataStore store = DataStoreFinder.getDataStore(map);
			FeatureCollection<?,?> fc =  store.getFeatureSource(store.getTypeNames()[0]).getFeatures();
			store.dispose();

			//build graph
			FeatureIterator<?> it = fc.features();
			FeatureGraphGenerator gGen = new FeatureGraphGenerator(new LineStringGraphGenerator());
			while(it.hasNext()) gGen.add(it.next());
			graph = gGen.getGraph();
			it.close();

			//define weighter
			this.edgeWeighter = edgeWeighter;
		} catch (Exception e) { e.printStackTrace(); }

	}

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

	private Coordinate getPosition(Node n){
		if(n==null) return null;
		Point pt = (Point)n.getObject();
		if(pt==null) return null;
		return pt.getCoordinate();
	}

	public Path getShortestPathDijkstra(Graph g, Node sN, Node dN, EdgeWeighter weighter){
		DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(g, sN, weighter);
		pf.calculate();
		return pf.getPath(dN);
	}

}
