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
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.index.strtree.STRtree;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.CSVUtil;
import org.opencarto.io.SHPUtil;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");


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

		//build routing
		Routing rt = new Routing(fc);

		//final data
		Collection<HashMap<String, String>> data = new ArrayList<>();
		Collection<Feature> routes = new ArrayList<>();

		//build poi spatial index
		STRtree poiIndex = new STRtree();
		for(Feature poi : pois)
			//TODO envelope of point? Use another type of index? KdTree
			poiIndex.insert(poi.getDefaultGeometry().getEnvelopeInternal(), poi);

		int nbNearest = 5;

		//go through cells
		for(Feature cell : cells) {
			String cellId = cell.getAttribute("cellId").toString();
			logger.info(cellId);

			//get cell centroid as origin point
			Coordinate oC = cell.getDefaultGeometry().getCentroid().getCoordinate();
			//TODO: get and build local routing only
			DijkstraShortestPathFinder dpf = rt.getDijkstraShortestPathFinder(oC);

			//get X nearest pois with straight line
			pois_ = poiIndex.nearestNeighbour(cell.getDefaultGeometry().getEnvelopeInternal(), cell, itemDist, nbNearest);

			//compute the routes to all pois nearby
			//get the shortest/fastest
			for(Object poi_ : pois_) {
				Feature poi = (Feature) poi_;
				Coordinate dC = poi.getDefaultGeometry().getCentroid().getCoordinate();
				Node dN = rt.getNode(dC);
				Path p = dpf.getPath(dN );
				//TODO get shortest/fastest
			}
			//TODO
			//store figure
			//TODO
			//store data
			//store route
		}

		logger.info("Save data");
		CSVUtil.save(data, path + "data_DE_10km.csv");
		logger.info("Save routes");
		SHPUtil.saveSHP(routes, path + "routes_DE_10km.shp", crs);

		logger.info("End");
	}

}
