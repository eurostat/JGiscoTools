/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.opencarto.datamodel.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author julien Gaffuri
 *
 */
public class AccessibilityGrid {
	private static Logger logger = Logger.getLogger(AccessibilityGrid.class.getName());

	//the grid cells
	private Collection<Feature> cells = null;
	//the grid resolution in m
	private double resM = -1;
	//the points of interest to measure the accessibility of
	private Collection<Feature> pois = null;
	//the linear features compising the network
	private Collection<Feature> networkSections = null;
	private SimpleFeatureType ft = null; //TODO useless - replace and remove it

	//the weighter used to estimate the cost of each network section when computing shortest paths
	private EdgeWeighter edgeWeighter = null;
	public void setEdgeWeighter(EdgeWeighter edgeWeighter) { this.edgeWeighter = edgeWeighter; }

	public interface SpeedCalculator { double getSpeedKMPerHour(SimpleFeature sf); }
	public void setEdgeWeighter(SpeedCalculator sc) {
		this.edgeWeighter = new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				//weight is the transport duration, in minutes
				SimpleFeature sf = (SimpleFeature) e.getObject();
				double speedMPerMinute = 1000/60 * sc.getSpeedKMPerHour(sf);
				double distanceM = ((Geometry) sf.getDefaultGeometry()).getLength();
				return distanceM/speedMPerMinute;
			}
		};
	}

	public EdgeWeighter getEdgeWeighter() {
		if(this.edgeWeighter == null) {
			//set default weighter: All sections are walked at the same speed, 70km/h
			setEdgeWeighter(new SpeedCalculator() {
				@Override
				public double getSpeedKMPerHour(SimpleFeature sf) { return 70.0; }
			});
		}
		return this.edgeWeighter;
	}




	//the transport duration by grid cell
	private Collection<HashMap<String, String>> cellData = null;
	public Collection<HashMap<String, String>> getCellData() { return cellData; }

	//the fastest route to one of the POIs for each grid cell
	private Collection<Feature> routes = null;
	public Collection<Feature> getRoutes() { return routes; }




	public AccessibilityGrid(Collection<Feature> cells, double resM, Collection<Feature> pois, Collection<Feature> networkSections, SimpleFeatureType ft) {
		this.cells = cells;
		this.resM = resM;
		this.pois = pois;
		this.networkSections = networkSections;
		this.ft = ft; //TODO useless - replace and remove it
	}


	//a spatial index for network sections
	private STRtree networkSectionsInd = null;
	private STRtree getNetworkSectionsInd() {
		if(networkSectionsInd == null) {
			logger.info("Index network sections");
			networkSectionsInd = new STRtree();
			for(Feature f : networkSections)
				if(f.getDefaultGeometry() != null)
					networkSectionsInd.insert(f.getDefaultGeometry().getEnvelopeInternal(), f);
		}
		return networkSectionsInd;
	}


	//a spatial index for POIs
	private STRtree poisInd = null;
	private STRtree getPoisInd() {
		if(poisInd == null) {
			logger.info("Index POIs");
			poisInd = new STRtree();
			for(Feature f : pois)
				poisInd.insert(f.getDefaultGeometry().getEnvelopeInternal(), f);
		}
		return poisInd;
	}

	//used to get nearest POIs from a location
	public static int nbNearestPOIs = 4;
	private static ItemDistance itemDist = new ItemDistance() {
		@Override
		public double distance(ItemBoundable item1, ItemBoundable item2) {
			Feature f1 = (Feature) item1.getItem();
			Feature f2 = (Feature) item2.getItem();
			return f1.getDefaultGeometry().distance(f2.getDefaultGeometry());
		}
	};





	//compute the accessibility data
	public void compute() throws Exception {
		//create output data structures
		cellData = new ArrayList<>();
		routes = new ArrayList<>();

		//compute spatial indexes
		getPoisInd();
		getNetworkSectionsInd();

		logger.info("Compute cell figure");
		for(Feature cell : cells) {
			String cellId = cell.getAttribute("cellId").toString();
			//logger.info(cellId);
			if(logger.isDebugEnabled()) logger.debug(cellId);

			//when cell contains at least one POI, set the duration to 0
			if(getPoisInd().query(cell.getDefaultGeometry().getEnvelopeInternal()).size()>0) {
				if(logger.isDebugEnabled()) logger.debug("POI in cell " + cellId);
				HashMap<String, String> d = new HashMap<String, String>();
				d.put("cellId", cellId);
				d.put("durMin", "0");
				cellData.add(d);
				continue;
			}

			if(logger.isDebugEnabled()) logger.debug("Get " + nbNearestPOIs + " nearest POIs");
			Envelope netEnv = cell.getDefaultGeometry().getEnvelopeInternal(); netEnv.expandBy(1000);
			Object[] pois_ = getPoisInd().nearestNeighbour(netEnv, cell, itemDist, nbNearestPOIs);

			//get an envelope around the cell and surrounding POIs
			netEnv = cell.getDefaultGeometry().getEnvelopeInternal();
			for(Object poi_ : pois_)
				netEnv.expandToInclude(((Feature)poi_).getDefaultGeometry().getEnvelopeInternal());
			netEnv.expandBy(10000);

			//get network sections in the envelope around the cell and surrounding POIs
			List<?> net_ = getNetworkSectionsInd().query(netEnv);
			ArrayList<Feature> net__ = new ArrayList<Feature>();
			for(Object o : net_) net__.add((Feature)o);

			//build the surrounding network
			Routing rt = new Routing(net__, ft);
			rt.setEdgeWeighter(getEdgeWeighter());

			//get cell centroid as origin point
			//take another position depending on the network state inside the cell? Cell is supposed to be small enough?
			Coordinate oC = cell.getDefaultGeometry().getCentroid().getCoordinate();
			Node oN = rt.getNode(oC);
			if(oN == null) {
				logger.error("Could not find graph node around cell center: " + oC);
				HashMap<String, String> d = new HashMap<String, String>();
				d.put("cellId", cellId);
				d.put("durMin", "-10");
				cellData.add(d);
				continue;
			}
			if( ( (Point)oN.getObject() ).getCoordinate().distance(oC) > 1.3 * resM ) {
				logger.trace("Cell center "+oC+" too far from clodest network node: " + oN.getObject());
				HashMap<String, String> d = new HashMap<String, String>();
				d.put("cellId", cellId);
				d.put("durMin", "-20");
				cellData.add(d);
				continue;
			}

			//TODO: improve and use AStar - ask GIS_SE ?
			DijkstraShortestPathFinder pf = rt.getDijkstraShortestPathFinder(oN);

			//compute the routes to all POIs to get the best
			if(logger.isDebugEnabled()) logger.debug("Compute routes to POIs. Nb=" + pois_.length);
			Path pMin = null; double costMin = Double.MAX_VALUE;
			for(Object poi_ : pois_) {
				Feature poi = (Feature) poi_;
				Coordinate dC = poi.getDefaultGeometry().getCentroid().getCoordinate();
				//AStarShortestPathFinder pf = rt.getAStarShortestPathFinder(oC, dC);
				//pf.calculate();
				Path p = null; double cost;
				//include POI in path? Cell is supposed to be small enough?
				try {
					//p = pf.getPath();
					Node dN = rt.getNode(dC);

					if(dN == oN) {
						costMin = 0;
						pMin = null;
						break;
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
					if(costMin == 0) break;
				}
			}

			if(costMin > 0 && pMin == null) {
				if(logger.isDebugEnabled()) logger.debug("Could not find path to POI for cell " + cellId + " around " + oC);
				HashMap<String, String> d = new HashMap<String, String>();
				d.put("cellId", cellId);
				d.put("durMin", "-30");
				cellData.add(d);
				continue;
			}

			//store data at grid cell level
			HashMap<String, String> d = new HashMap<String, String>();
			d.put("cellId", cellId);
			d.put("durMin", ""+costMin);
			cellData.add(d);

			if(pMin != null) {
				//store route
				Feature f = Routing.toFeature(pMin);
				f.setAttribute("durMin", costMin);
				f.setAttribute("cellId", cellId);
				routes.add(f);
			}
		}
	}




	/**
	 * An indicator measuring how bad a cell is considered, based on its accessibility AND population.
	 * 
	 * @param population
	 * @param durMin
	 * @return
	 */
	private double getPopulationAccessibilityIndicator(double population, double durMin) {
		//the higher the duration, the worst.
		//the higher the population, the worst
		//TODO test others ? To give more weight to low population cells, increase p
		//TODO use population density instead, with an average to average density?
		double p = 1;
		return durMin*Math.pow(population, 1/p);
	}

	//TODO function which produce indicator for all cells, based on previous function, cell data and cell population data


}
