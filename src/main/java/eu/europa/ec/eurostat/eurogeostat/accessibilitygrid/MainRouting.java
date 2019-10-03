/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Node;
import org.locationtech.jts.geom.Coordinate;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.SHPUtil;

/**
 * @author julien Gaffuri
 *
 */
public class MainRouting {
	private static Logger logger = Logger.getLogger(MainRouting.class.getName());

	//example
	//https://krankenhausatlas.statistikportal.de/
	//show where X-border cooperation can improve accessibility



	public static void main(String[] args) throws Exception {
		logger.info("Start");

		logger.setLevel(Level.ALL);

		//TODO ERM
		String networkFile = "file:\\E:/dissemination/shared-data/ERM/ERM_2019.1_shp/Data/RoadL.shp";
		//String networkFile = "file:\\E:/dissemination/shared-data/EGM/EGM_2019_SHP_20190312/DATA/FullEurope/RoadL.shp";

		Map<String, Serializable> map = new HashMap<>();
		map.put( "url", new URL(networkFile)  );
		DataStore store = DataStoreFinder.getDataStore(map);
		FeatureCollection<?,?> fc =  store.getFeatureSource(store.getTypeNames()[0]).getFeatures();
		store.dispose();

		fc = SHPUtil.getSimpleFeatures(networkFile);


		logger.info("Build routing network");
		Routing rt = new Routing(fc);

		Coordinate oC = new Coordinate(6.16330, 49.62608);
		DijkstraShortestPathFinder dpf = rt.getDijkstraShortestPathFinder(oC);

		ArrayList<Feature> routes = new ArrayList<Feature>();
		for(double dist=1.0; dist<5.0; dist+=1.0)
			for(double angle=0; angle<2*Math.PI; angle+=Math.PI/100) {
				logger.info(dist + " " + angle);
				Coordinate dC = new Coordinate(oC.getX() + dist*Math.cos(angle), oC.getY() + dist*Math.sin(angle));
				Node dN = rt.getNode(dC);
				Path p = dpf.getPath(dN );
				if(p==null) {
					System.err.println("Could not compute route for angle="+angle);
					continue;
				}
				routes.add( Routing.toFeature(p) );
			}
		logger.info("Save");
		SHPUtil.saveSHP(routes, "C:\\Users\\gaffuju\\Desktop\\routing_test.shp", null);

		logger.info("End");
	}

}
