/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Node;
import org.locationtech.jts.geom.Coordinate;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.CSVUtil;
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

		String basepath = "C:/Users/gaffuju/Desktop/";
		String path = basepath + "routing_test/";
		String gridpath = basepath + "grid/";


		logger.info("Load network data");
		//String networkFile = "E:/dissemination/shared-data/ERM/ERM_2019.1_shp/Data/RoadL_RTT_14_15_16.shp";
		String networkFile = path + "RoadL_LAEA.shp";
		FeatureCollection<?,?> fc = SHPUtil.getSimpleFeatures(networkFile, CQL.toFilter("ICC = 'DE'"));

		logger.info("Load grid data");
		int resKM = 10;
		ArrayList<Feature> cells = SHPUtil.loadSHP(gridpath + resKM+"km/grid_"+resKM+"km.shp", CQL.toFilter("CNTR_ID = 'DE'")).fs;
		logger.info(cells.size() + " cells");

		logger.info("Load POI data");
		ArrayList<Feature> pois = SHPUtil.loadSHP(path + "GovservP_LAEA.shp", CQL.toFilter("GST = 'GF0703' AND ICC = 'DE'")).fs;
		logger.info(pois.size() + " pois");
		//- GST = GF0306: Rescue service
		//- GST = GF0703: Hospital service
		//- GST = GF090102: Primary education (ISCED-97 Level 1): Primary schools
		//- GST = GF0902: Secondary education (ISCED-97 Level 2, 3): Secondary schools
		//- GST = GF0904: Tertiary education (ISCED-97 Level 5, 6): Universities
		//- GST = GF0905: Education not definable by level


		Collection<HashMap<String, String>> data = new ArrayList<>();
		for(Feature cell : cells) {
			String cellId = cell.getAttribute("cellId").toString();
			logger.info(cellId);

			Coordinate oC = cell.getDefaultGeometry().getCentroid().getCoordinate();

			//compute distance/time to the nearest poi
			//TODO
			//get X nearest pois with straight line
			//TODO
			//get maximum distance
			//TODO
			//build area where to get the network
			//TODO
			//get network elements
			FeatureCollection<?,?> fc_ = null;
			//build the network
			Routing rt = new Routing(fc_);
			DijkstraShortestPathFinder dpf = rt.getDijkstraShortestPathFinder(oC);
			//compute the routes to all pois nearby
			Coordinate dC = null;
			Node dN = rt.getNode(dC);
			Path p = dpf.getPath(dN );
			//get the shortest/fastest
			//TODO
			//store figure
			//TODO

		}

		logger.info("Save");
		CSVUtil.save(data, path + "data_DE_10km.csv");

		logger.info("End");
	}

}
