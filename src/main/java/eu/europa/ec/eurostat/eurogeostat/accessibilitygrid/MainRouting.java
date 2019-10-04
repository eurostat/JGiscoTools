/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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

		String path = "C:/Users/gaffuju/Desktop/";
		String outpath = path + "routing_test/";
		String gridpath = path + "grid/";


		//String networkFile = "E:/dissemination/shared-data/ERM/ERM_2019.1_shp/Data/RoadL_RTT_14_15_16.shp";
		String networkFile = outpath + "RoadL_LAEA.shp";

		logger.info("Load data");
		FeatureCollection<?,?> fc = SHPUtil.getSimpleFeatures(networkFile);

		logger.info("Build routing network. Nb="+fc.size());
		Routing rt = new Routing(fc);

		//reference point: Luxembourg
		Coordinate oC = new Coordinate(4044373, 2952624);
		DijkstraShortestPathFinder dpf = rt.getDijkstraShortestPathFinder(oC);


		//load grid
		int resKM = 50;
		ArrayList<Feature> cells = SHPUtil.loadSHP(gridpath + resKM+"km/grid_"+resKM+"km.shp").fs;
		System.out.println(cells.size() + " cells");

		ArrayList<Feature> routes = new ArrayList<Feature>();
		for(Feature cell : cells) {
			logger.info(cell.getAttribute("cellId"));

			Coordinate dC = cell.getDefaultGeometry().getCentroid().getCoordinate();
			Node dN = rt.getNode(dC);
			Path p = dpf.getPath(dN );
			if(p==null) {
				System.err.println("Could not compute route for cell " + cell.getAttribute("cellId"));
				continue;
			}
			routes.add( Routing.toFeature(p) );
		}

		logger.info("Save");
		SHPUtil.saveSHP(routes, "C:\\Users\\gaffuju\\Desktop\\routing_test\\routing_test_LU_"+resKM+"km_EGM.shp", null);

		logger.info("End");
	}

}
