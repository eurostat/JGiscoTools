/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.CSVUtil;
import org.opencarto.io.SHPUtil;
import org.opencarto.io.SHPUtil.SHPData;
import org.opengis.feature.simple.SimpleFeature;
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
		//TODO correct networks - snapping
		SHPData net = SHPUtil.loadSHP("E:/dissemination/shared-data/EGM/EGM_2019_SHP_20190312_LAEA/DATA/FullEurope/RoadL.shp");
		//ERM TODO load other transport networks (ferry, etc?)
		//SHPData net = SHPUtil.loadSHP("E:/dissemination/shared-data/ERM/ERM_2019.1_shp/Data/RoadL_RTT_14_15_16.shp");
		//net.fs.addAll( SHPUtil.loadSHP("E:/dissemination/shared-data/ERM/ERM_2019.1_shp/Data/RoadL_RTT_984.shp").fs );
		//net.fs.addAll( SHPUtil.loadSHP("E:/dissemination/shared-data/ERM/ERM_2019.1_shp/Data/RoadL_RTT_0.shp").fs );
		logger.info(net.fs.size() + " sections loaded.");

		logger.info("Index network data");
		STRtree netIndex = new STRtree();
		for(Feature f : net.fs)
			if(f.getDefaultGeometry() != null)
				netIndex.insert(f.getDefaultGeometry().getEnvelopeInternal(), f);


		logger.info("Define edge weighter");
		EdgeWeighter edgeWeighter = new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				SimpleFeature f = (SimpleFeature) e.getObject();
				double speedMPerMinute = 1000/60 * getEXMSpeedKMPerHour(f);
				double distanceM = ((Geometry) f.getDefaultGeometry()).getLength();
				return distanceM/speedMPerMinute;
			}
		};



		logger.info("Load grid data");
		int resKM = 5;
		ArrayList<Feature> cells = SHPUtil.loadSHP(gridpath + resKM+"km/grid_"+resKM+"km.shp"/*, CQL.toFilter("CNTR_ID = 'DE'")*/).fs;
		logger.info(cells.size() + " cells");


		logger.info("Load POI data");
		//TODO test others: tomtom, osm
		ArrayList<Feature> pois = SHPUtil.loadSHP("E:/dissemination/shared-data/ERM/ERM_2019.1_shp_LAEA/Data/GovservP.shp", CQL.toFilter("GST = 'GF0703'" /*+ " AND ICC = 'DE'"*/ )).fs;
		logger.info(pois.size() + " pois");
		//- GST = GF0306: Rescue service
		//- GST = GF0703: Hospital service
		//- GST = GF090102: Primary education (ISCED-97 Level 1): Primary schools
		//- GST = GF0902: Secondary education (ISCED-97 Level 2, 3): Secondary schools
		//- GST = GF0904: Tertiary education (ISCED-97 Level 5, 6): Universities
		//- GST = GF0905: Education not definable by level

		//build poi spatial index, to quickly retrieve the X nearest (with euclidian distance) pois from cell center
		logger.info("Index POIs");
		STRtree poiIndex = new STRtree();
		for(Feature poi : pois)
			poiIndex.insert(poi.getDefaultGeometry().getEnvelopeInternal(), poi);
		int nbNearest = 5;
		ItemDistance itemDist = new ItemDistance() {
			@Override
			public double distance(ItemBoundable item1, ItemBoundable item2) {
				Feature f1 = (Feature) item1.getItem();
				Feature f2 = (Feature) item2.getItem();
				return f1.getDefaultGeometry().distance(f2.getDefaultGeometry());
			}
		};


		//prepare final data structure
		Collection<HashMap<String, String>> cellData = new ArrayList<>();
		Collection<Feature> routes = new ArrayList<>();

		//go through cells
		for(Feature cell : cells) {
			String cellId = cell.getAttribute("cellId").toString();
			logger.info(cellId);

			//logger.info("Get " + nbNearest + " nearest pois");
			Envelope netEnv = cell.getDefaultGeometry().getEnvelopeInternal(); netEnv.expandBy(1000);
			Object[] pois_ = poiIndex.nearestNeighbour(netEnv, cell, itemDist, nbNearest);

			//get cell centroid as origin point
			Coordinate oC = cell.getDefaultGeometry().getCentroid().getCoordinate();

			//get envelope to build network in
			netEnv = cell.getDefaultGeometry().getEnvelopeInternal();
			for(Object poi_ : pois_)
				netEnv.expandToInclude(((Feature)poi_).getDefaultGeometry().getEnvelopeInternal());
			netEnv.expandBy(10000);

			//get network elements
			List<?> net_ = netIndex.query(netEnv);
			ArrayList<Feature> net__ = new ArrayList<Feature>();
			for(Object o : net_) net__.add((Feature) o);

			Routing rt = new Routing(net__, net.ft);
			rt.setEdgeWeighter(edgeWeighter);
			//TODO use time: define weighter
			//TODO: improve and use AStar - ask gise ?
			DijkstraShortestPathFinder pf = rt.getDijkstraShortestPathFinder(oC);

			//compute the routes to all pois to get the best
			//logger.info("Compute routes to pois. Nb="+pois_.length);
			Path pMin = null; double costMin = Double.MAX_VALUE;
			for(Object poi_ : pois_) {
				Feature poi = (Feature) poi_;
				Coordinate dC = poi.getDefaultGeometry().getCentroid().getCoordinate();
				//AStarShortestPathFinder pf = rt.getAStarShortestPathFinder(oC, dC);
				//pf.calculate();
				Path p = null; double cost;
				//TODO include POI in path?
				try {
					//p = pf.getPath();
					Node dN = rt.getNode(dC);
					p = pf.getPath(dN);
					cost = pf.getCost(dN);
				} catch (Exception e) {
					logger.warn("Could not compute path. " + e.getMessage());
					continue;
				}
				if(p==null) continue;
				//get best path
				if(pMin==null || cost<costMin) {
					pMin=p; costMin=cost;
				}
			}
			if(pMin==null) {
				logger.warn("Could not handle grid cell " + cellId);
				costMin = -999;
			}

			//store data at grid cell level
			HashMap<String, String> d = new HashMap<String, String>();
			d.put("cellId", cellId);
			d.put("cost", ""+costMin);
			cellData.add(d);

			if(pMin==null) continue;

			//store route
			Feature f = Routing.toFeature(pMin);
			f.setAttribute("cost", costMin);
			routes.add(f);
		}

		logger.info("Save data");
		CSVUtil.save(cellData, path + "cell_data_"+resKM+"km.csv");
		logger.info("Save routes. Nb="+routes.size());
		SHPUtil.saveSHP(routes, path + "routes_"+resKM+"km.shp", crs);

		logger.info("End");
	}



	//estimate speed of a transport section of ERM/EGM based on attributes
	private static double getEXMSpeedKMPerHour(SimpleFeature f) {
		//TODO
		return 90;
	}

}
