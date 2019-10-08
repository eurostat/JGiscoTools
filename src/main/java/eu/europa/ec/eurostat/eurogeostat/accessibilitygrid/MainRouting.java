/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

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
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.CSVUtil;
import org.opencarto.io.SHPUtil;
import org.opencarto.io.SHPUtil.SHPData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
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

		//logger.setLevel(Level.ALL);

		String basepath = "C:/Users/gaffuju/Desktop/";
		String path = basepath + "routing_test/";
		String gridpath = basepath + "grid/";
		String egpath = "E:/dissemination/shared-data/";
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");

		//TODO correct networks - snapping
		//TODO load other transport networks (ferry, etc?)
		logger.info("Load network data");
		Filter fil = CQL.toFilter("EXS=28 AND RST=1");
		//EGM
		SHPData net = SHPUtil.loadSHP(egpath+"EGM/EGM_2019_SHP_20190312_LAEA/DATA/FullEurope/RoadL.shp", fil);
		//TODO use ERM
		//SHPData net = SHPUtil.loadSHP(egpath+"ERM/ERM_2019.1_shp/Data/RoadL_RTT_14_15_16.shp", fil);
		//net.fs.addAll( SHPUtil.loadSHP(egpath+"ERM/ERM_2019.1_shp/Data/RoadL_RTT_984.shp", fil).fs );
		//net.fs.addAll( SHPUtil.loadSHP(egpath+"ERM/ERM_2019.1_shp/Data/RoadL_RTT_0.shp", fil).fs );
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
			if(logger.isDebugEnabled()) logger.debug(cellId);

			//case when cell contains at least one POI
			if(poiIndex.query(cell.getDefaultGeometry().getEnvelopeInternal()).size()>0) {
				if(logger.isDebugEnabled()) logger.debug("POI in cell " + cellId);
				HashMap<String, String> d = new HashMap<String, String>();
				d.put("cellId", cellId);
				d.put("cost", "0");
				cellData.add(d);
				continue;
			}

			if(logger.isDebugEnabled()) logger.debug("Get " + nbNearest + " nearest pois");
			Envelope netEnv = cell.getDefaultGeometry().getEnvelopeInternal(); netEnv.expandBy(1000);
			Object[] pois_ = poiIndex.nearestNeighbour(netEnv, cell, itemDist, nbNearest);

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

			//get cell centroid as origin point
			Coordinate oC = cell.getDefaultGeometry().getCentroid().getCoordinate();
			Node oN = rt.getNode(oC);
			if(oN == null) {
				logger.error("Could not find graph node around cell center: " + oC);
				HashMap<String, String> d = new HashMap<String, String>();
				d.put("cellId", cellId);
				d.put("cost", "-10");
				cellData.add(d);
				continue;
			}
			if( ( (Point)oN.getObject() ).getCoordinate().distance(oC) > 1.3 * resKM*1000 ) {
				logger.trace("Cell center "+oC+" too far from clodest network node: " + oN.getObject());
				HashMap<String, String> d = new HashMap<String, String>();
				d.put("cellId", cellId);
				d.put("cost", "-20");
				cellData.add(d);
				continue;
			}

			//TODO: improve and use AStar - ask GIS_SE ?
			DijkstraShortestPathFinder pf = rt.getDijkstraShortestPathFinder(oN);

			//compute the routes to all pois to get the best
			if(logger.isDebugEnabled()) logger.debug("Compute routes to pois. Nb="+pois_.length);
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

					if(dN == oN) {
						//TODO better handle such case
						continue;
					}

					p = pf.getPath(dN);
					cost = pf.getCost(dN);
					//For A*: see https://gis.stackexchange.com/questions/337968/how-to-get-path-cost-in/337972#337972
				} catch (Exception e) {
					//logger.warn("Could not compute path for cell " + cellId + ": " + e.getMessage());
					continue;
				}
				if(p==null) continue;
				//get best path
				if(pMin==null || cost<costMin) {
					pMin=p; costMin=cost;
				}
			}
			if(pMin==null) {
				if(logger.isDebugEnabled()) logger.debug("Could not find path to poi for cell " + cellId + " around " + oC);
				HashMap<String, String> d = new HashMap<String, String>();
				d.put("cellId", cellId);
				d.put("cost", "-999");
				cellData.add(d);
				continue;
			}

			//store data at grid cell level
			HashMap<String, String> d = new HashMap<String, String>();
			d.put("cellId", cellId);
			d.put("cost", ""+costMin);
			cellData.add(d);

			//store route
			Feature f = Routing.toFeature(pMin);
			f.setAttribute("cost", costMin);
			f.setAttribute("cellId", cellId);
			routes.add(f);
		}

		logger.info("Save data");
		CSVUtil.save(cellData, path + "cell_data_"+resKM+"km.csv");
		logger.info("Save routes. Nb="+routes.size());
		SHPUtil.saveSHP(routes, path + "routes_"+resKM+"km.shp", crs);

		logger.info("End");
	}


	//estimate speed of a transport section of ERM/EGM based on attributes
	//COR - Category of Road - 0 Unknown - 1 Motorway - 2 Road inside built-up area - 999 Other road (outside built-up area)
	//RTT - Route Intended Use - 0 Unknown - 16 National motorway - 14 Primary route - 15 Secondary route - 984 Local route
	private static double getEXMSpeedKMPerHour(SimpleFeature f) {
		String cor = f.getAttribute("COR").toString();
		if(cor==null) { logger.warn("No COR attribute for feature "+f.getID()); return 0; };
		String rtt = f.getAttribute("RTT").toString();
		if(rtt==null) { logger.warn("No RTT attribute for feature "+f.getID()); return 0; };

		//motorways
		if("1".equals(cor) || "16".equals(rtt)) return 110.0;
		//city roads
		if("2".equals(cor)) return 50.0;
		//fast roads
		if("14".equals(rtt) || "15".equals(rtt)) return 80.0;
		//local road
		if("984".equals(rtt)) return 80.0;
		return 50.0;
	}

}
