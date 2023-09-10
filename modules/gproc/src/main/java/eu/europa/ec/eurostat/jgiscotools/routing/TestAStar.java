/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.routing;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Node;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility.ZZZBasicServiceAccessibility;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author julien gaffuri
 *
 */
public class TestAStar {
	private static Logger logger = LogManager.getLogger(ZZZBasicServiceAccessibility.class.getName());

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		String basePath = "/home/juju/Bureau/gisco/";

		logger.info("Loading");
		Collection<Feature> networkSections = GeoData.getFeatures(basePath + "star_network_topology_validation/ERM_LU.gpkg");
		//Collection<Feature> networkSections = GeoData.getFeatures(basePath + "geodata/euro-regional-map-gpkg/data/OpenEuroRegionalMap.gpkg", "RoadL", "id");
		//Collection<Feature> networkSections = GeoData.getFeatures("E:/workspace/basic_services_accessibility/input_data/test_tomtom_LU/luxlux_nw.gpkg", null);
		for(Feature f : networkSections) f.setAttribute("cost", f.getGeometry().getLength());
		logger.info("Loaded: " + networkSections.size());

		//logger.info("Feature type");
		//SimpleFeatureType ft = SimpleFeatureUtil.getFeatureType(networkSections, "the_geom", null);

		logger.info("Build network");
		Routing rt = new Routing(networkSections);
		rt.setEdgeWeighter("cost");

		logger.info("Prepare");
		ArrayList<Feature> paths = new ArrayList<>();
		//origin point
		Coordinate oC = new Coordinate(4041407, 2967034);
		Node oN = rt.getNode(oC);
		int aNb = 512; int rNb = 1; double rMax = 60000;

		/*
		//TODO test http://theory.stanford.edu/~amitp/GameProgramming/Heuristics.html
		logger.info("A*");

		//define default A* functions
		AStarFunctions afun = new AStarFunctions(null) {
			//NB: both cost and h should return something in the same unit.
			//h to be faster: straight line with highway...
			@Override
			public double cost(AStarNode ns0, AStarNode ns1) {
				//return the edge weighter value
				Edge e = ns0.getNode().getEdge(ns1.getNode());
				//return rt.getEdgeWeighter().getWeight(e);
				SimpleFeature sf = (SimpleFeature)e.getObject();
				return ((Geometry)sf.getDefaultGeometry()).getLength();
			}
			@Override
			public double h(Node n) {
				//return the point to point 'cost' TODO?
				Point dP = (Point) getDest().getObject();
				Point p = (Point) n.getObject();

				//rt.getEdgeWeighter().getWeight(e);

				return 1.0 * p.distance(dP);
			}
		};

		for(double r = rMax/rNb; r<=rMax; r += rMax/rNb)
			for(double angle = 0; angle<2*Math.PI; angle += 2*Math.PI/nb)
				try {
					Coordinate dC = new Coordinate(oC.x+r*Math.cos(angle), oC.y+r*Math.sin(angle));
					Node dN = rt.getNode(dC);

					afun.setDestination(dN);
					AStarShortestPathFinder pf = new AStarShortestPathFinder(rt.getGraph(), oN, dN, afun);
					pf.calculate();
					Path p = pf.getPath();
					//For A*: see https://gis.stackexchange.com/questions/337968/how-to-get-path-cost-in/337972#337972

					//store route
					//Feature f = new Feature();
					//f.setGeometry(JTSGeomUtil.toMulti( JTSGeomUtil.createLineString(oC.x, oC.y, dC.x, dC.y) ));
					Feature f = Routing.toFeature(p);
					//f.setAttribute("durationMin", duration);
					paths.add(f);
				} catch (Exception e) {
					e.printStackTrace();
				}

		logger.info("save");
		GeoData.save(paths, "E:\\workspace\\basic_services_accessibility\\routing_paths\\test\\LU_test_astar.gpkg", CRS.decode("EPSG:3035"), true);
		paths.clear();
		 */

		logger.info("Dijskra");
		DijkstraShortestPathFinder pf = rt.getDijkstraShortestPathFinder(oN);
		pf.calculate();

		for(double r = rMax/rNb; r<=rMax; r += rMax/rNb)
			for(double angle = 0; angle<2*Math.PI; angle += 2*Math.PI/aNb) {

				Coordinate dC = new Coordinate(oC.x+r*Math.cos(angle), oC.y+r*Math.sin(angle));
				Node dN = rt.getNode(dC);

				Path p = pf.getPath(dN);
				if(p==null) {
					logger.info("No path found to " + dC );
					continue;
				}

				double duration = pf.getCost(dN);

				//store route
				//Feature f = new Feature();
				//f.setGeometry(JTSGeomUtil.toMulti( JTSGeomUtil.createLineString(oC.x, oC.y, dC.x, dC.y) ));
				Feature f = Routing.toFeature(p);
				f.setAttribute("durationMin", duration);
				paths.add(f);
			}

		logger.info("save shortest paths");
		//GeoData.save(paths, "E:\\workspace\\basic_services_accessibility\\routing_paths\\test\\LU_test_dij.gpkg", CRS.decode("EPSG:3035"), true);
		GeoData.save(paths, basePath + "star_network_topology_validation/paths.gpkg", CRS.decode("EPSG:3035"), true);

		logger.info("End");
	}

}
