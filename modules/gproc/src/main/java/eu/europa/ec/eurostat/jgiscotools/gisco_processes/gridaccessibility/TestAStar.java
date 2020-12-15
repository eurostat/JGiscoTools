/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Node;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.java4eurostat.util.Util;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.Routing;

/**
 * @author clemoki
 *
 */
public class TestAStar {
	private static Logger logger = LogManager.getLogger(BasicServiceAccessibility.class.getName());

	public static void main(String[] args) {
		logger.info("Start");

		logger.info("Loading");
		Collection<Feature> networkSections = GeoData.getFeatures("E:/workspace/basic_services_accessibility/input_data/test_tomtom_LU/luxlux_nw.gpkg", null, null);
		logger.info("Loaded: " + networkSections.size());

		logger.info("Feature type");
		SimpleFeatureType ft = SimpleFeatureUtil.getFeatureType(networkSections, "the_geom", null);

		logger.info("Build network");
		Routing rt = new Routing(networkSections, ft);

		logger.info("Dijskra");
		Coordinate oC = new Coordinate();
		Node oN = rt.getNode(oC);
		DijkstraShortestPathFinder pf = rt.getDijkstraShortestPathFinder(oN);



		
		ArrayList<Feature> paths = new ArrayList<>();

		Coordinate dC = new Coordinate();
		Node dN = rt.getNode(dC);

		Path p = pf.getPath(dN);
		if(p==null) {
			if(logger.isTraceEnabled()) logger.trace("No path found to " + dC );
			//continue;
		}

		double duration = pf.getCost(dN);
		//For A*: see https://gis.stackexchange.com/questions/337968/how-to-get-path-cost-in/337972#337972

		//store route
		//TODO keep straight line as geometry ?
		//Feature f = Routing.toFeature(p);
		Feature f = new Feature();
		f.setGeometry(JTSGeomUtil.toMulti( JTSGeomUtil.createLineString(oC.x, oC.y, dC.x, dC.y) ));
		f.setAttribute("durationMin", Util.round(duration, 2));
		//f.setAttribute("distanceM", Util.round(f.getGeometry().getLength(), 2));
		//f.setAttribute("avSpeedKMPerH", Util.round(0.06 * f.getGeometry().getLength()/duration, 2));
		paths.add(f);
		
		logger.info("End");
	}

}
