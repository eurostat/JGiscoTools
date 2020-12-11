/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.java4eurostat.util.Util;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;

/**
 * Class to compute accessiblity paths from grid cells to points of interest, using a transport network.
 * 
 * @author julien Gaffuri
 *
 */
public class AccessibilityRoutingPaths {
	private static Logger logger = LogManager.getLogger(AccessibilityRoutingPaths.class.getName());

	//the grid cells
	private Collection<Feature> cells = null;
	//the grid cells id
	private String cellIdAtt = "GRD_ID";
	//the grid resolution in m
	private double resM = -1;
	//the points of interest to measure the accessibility of
	private Collection<Feature> pois = null;
	//the number of POIs to consider
	private int nbNearestPOIs = 4;
	//the linear features composing the network
	private Collection<Feature> networkSections = null;

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


	//the fastest route to one of the POIs for each grid cell
	private Collection<Feature> routes = null;
	public Collection<Feature> getRoutes() { return routes; }




	public AccessibilityRoutingPaths(Collection<Feature> cells, String cellIdAtt, double resM, Collection<Feature> pois, Collection<Feature> networkSections,int nbNearestPOIs) {
		this.cells = cells;
		this.cellIdAtt = cellIdAtt;
		this.resM = resM;
		this.pois = pois;
		this.networkSections = networkSections;
		this.nbNearestPOIs = nbNearestPOIs;

	}


	//a spatial index for network sections
	private STRtree networkSectionsInd = null;
	private STRtree getNetworkSectionsInd() {
		if(networkSectionsInd == null) {
			logger.info("Index network sections");
			networkSectionsInd = new STRtree();
			for(Feature f : networkSections)
				if(f.getGeometry() != null)
					networkSectionsInd.insert(f.getGeometry().getEnvelopeInternal(), f);
		}
		return networkSectionsInd;
	}

	//a spatial index for the POIs
	private STRtree poisInd = null;
	private STRtree getPoisInd() {
		if(poisInd == null) {
			logger.info("Index POIs");
			poisInd = new STRtree();
			for(Feature f : pois)
				poisInd.insert(f.getGeometry().getEnvelopeInternal(), f);
		}
		return poisInd;
	}

	//used to get nearest POIs from a location
	private static ItemDistance itemDist = new ItemDistance() {
		@Override
		public double distance(ItemBoundable item1, ItemBoundable item2) {
			Feature f1 = (Feature) item1.getItem();
			Feature f2 = (Feature) item2.getItem();
			return f1.getGeometry().distance(f2.getGeometry());
		}
	};





	//compute the accessibility data
	//-10: no transport node found for grid cell center
	//-20: no transport node found close enough from grid cell center
	//-30: no shortest path found to an hospital
	public void compute() throws Exception {
		//create output data structures
		routes = new ArrayList<>();

		//compute spatial indexes
		getPoisInd();
		getNetworkSectionsInd();

		//get network sections feature type
		SimpleFeatureType ft = SimpleFeatureUtil.getFeatureType(networkSections, "the_geom", null);

		logger.info("Compute accessibility routing paths...");
		for(Feature cell : cells) {

			//get cell id
			String cellId = cell.getAttribute(cellIdAtt).toString();
			if(logger.isDebugEnabled()) logger.debug(cellId);

			if(logger.isDebugEnabled()) logger.debug("Get " + nbNearestPOIs + " nearest POIs");
			Envelope netEnv = cell.getGeometry().getEnvelopeInternal(); netEnv.expandBy(1000);
			Object[] pois_ = getPoisInd().nearestNeighbour(netEnv, cell, itemDist, nbNearestPOIs);

			//get an envelope around the cell and surrounding POIs
			netEnv = cell.getGeometry().getEnvelopeInternal();
			for(Object poi_ : pois_)
				netEnv.expandToInclude(((Feature)poi_).getGeometry().getEnvelopeInternal());
			netEnv.expandBy(10000);

			//get network sections in the envelope around the cell and surrounding POIs
			List<?> net_ = getNetworkSectionsInd().query(netEnv);
			ArrayList<Feature> net__ = new ArrayList<Feature>();
			for(Object o : net_) net__.add((Feature)o);

			//build the surrounding network
			Routing rt = new Routing(net__, ft);
			rt.setEdgeWeighter(getEdgeWeighter());

			//get cell centroid as origin point
			//possible improvement: take another position depending on the network state inside the cell? Cell is supposed to be small enough?
			Coordinate oC = cell.getGeometry().getCentroid().getCoordinate();
			Node oN = rt.getNode(oC);
			if(oN == null) {
				logger.error("Could not find graph node around cell center: " + oC);
				continue;
			}
			if( ( (Point)oN.getObject() ).getCoordinate().distance(oC) > 1.3 * resM ) {
				logger.trace("Cell center "+oC+" too far from clodest network node: " + oN.getObject());
				continue;
			}

			//TODO: improve and use AStar - ask GIS_SE ?
			DijkstraShortestPathFinder pf = rt.getDijkstraShortestPathFinder(oN);

			//compute the routes to all POIs to get the best
			if(logger.isDebugEnabled()) logger.debug("Compute routes to POIs. Nb=" + pois_.length);
			for(Object poi_ : pois_) {
				Feature poi = (Feature) poi_;
				Coordinate dC = poi.getGeometry().getCentroid().getCoordinate();
				//AStarShortestPathFinder pf = rt.getAStarShortestPathFinder(oC, dC);
				//pf.calculate();
				Path p = null; double duration;
				//include POI in path? Cell is supposed to be small enough?
				try {
					//p = pf.getPath();
					Node dN = rt.getNode(dC);

					if(dN == oN) {
						break;
					}

					p = pf.getPath(dN);
					duration = pf.getCost(dN);
					//For A*: see https://gis.stackexchange.com/questions/337968/how-to-get-path-cost-in/337972#337972
				} catch (Exception e) {
					//logger.warn("Could not compute path for cell " + cellId + ": " + e.getMessage());
					continue;
				}

				//store route
				Feature f = Routing.toFeature(p);
				f.setID(cellId);
				f.setAttribute(cellIdAtt, cellId);
				//TODO save also POI id
				f.setAttribute("duration", duration);
				f.setAttribute("avSpeedKMPerH", Util.round(0.06 * f.getGeometry().getLength()/duration, 2));
				routes.add(f);

			}
		}
	}



	//from 0 (good accessibility) to infinity (bad accessibility)
	//this value is in delay minute . person
	public static double getDelayMinPerson(double durMin, double durT, double population) {
		if(durMin<0) return -999;
		double delay = durMin-durT;
		if(delay<0) return 0;
		return delay * population;
	}



	/**
	 * An indicator combining accessibility and population.
	 * 0 to 1 (well accessible)
	 * -999 is returned when the population is null
	 * 1 is returned when the duration indicator is equal to 1
	 * This is the product of two indicators on accessibility and population
	 * 
	 * @param population
	 * @param durMin
	 * @return
	 */
	/*public static double getPopulationAccessibilityIndicator(double durMin, double durT1, double durT2, double population, double popT) {
		if(population == 0) return -999;
		double accInd = getAccessibilityIndicator(durMin, durT1, durT2);
		if(accInd == 1) return 1;
		return accInd * getPopulationIndicator(population, popT);
	}*/

	/**
	 * An indicator on accessibility, within [0,1].
	 * 0 is bad (long time), 1 is good (short time)
	 * The higher the duration, the worst:
	 * ___
	 *    \
	 *     \___
	 * 
	 * @param durMin
	 * @param durT1
	 * @param durT2
	 * @return
	 */
	/*public static double getAccessibilityIndicator(double durMin, double durT1, double durT2) {
		return Util.getIndicatorValue(durMin, durT1, durT2);
	}*/


	/**
	 * An indicator on the gravity of having population taken into account, within [0,1].
	 * 0 means low population value, which does not matter.
	 * 1 means high population, which should be considered.
	 * The higher the population, the worst:
	 * 
	 * \
	 *  \___
	 * 
	 * -999 is returned when the population is null
	 * 
	 * @param population
	 * @param popT
	 * @return
	 */
	/*public static double getPopulationIndicator(double population, double popT) {
		if(population == 0) return -999;
		return Util.getIndicatorValue(population, 0, popT);
	}*/
}
