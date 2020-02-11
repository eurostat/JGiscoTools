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

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * Class to compute an accessibility grid to a set of points of interest, using a transport network.
 * 
 * @author julien Gaffuri
 *
 */
public class AccessibilityGrid {
	private static Logger logger = LogManager.getLogger(AccessibilityGrid.class.getName());

	//the grid cells
	private Collection<Feature> cells = null;
	//the grid cells
	private String cellIdAtt = "GRD_ID";
	//the grid resolution in m
	private double resM = -1;
	//the points of interest to measure the accessibility of
	private Collection<Feature> pois = null;
	//the linear features composing the network
	private Collection<Feature> networkSections = null;
	//population attribute
	private String populationAtt = null;

	//threshol values to compute accessibility indicators
	public double minDurAccMinT = 15;

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
	private Collection<Map<String, String>> cellData = null;
	public Collection<Map<String, String>> getCellData() { return cellData; }

	//the fastest route to one of the POIs for each grid cell
	private Collection<Feature> routes = null;
	public Collection<Feature> getRoutes() { return routes; }




	public AccessibilityGrid(Collection<Feature> cells, String cellIdAtt, double resM, Collection<Feature> pois, Collection<Feature> networkSections, String populationAtt, double minDurAccMinT) {
		this.cells = cells;
		this.cellIdAtt = cellIdAtt;
		this.resM = resM;
		this.pois = pois;
		this.networkSections = networkSections;
		this.populationAtt = populationAtt;
		this.minDurAccMinT = minDurAccMinT;
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
	public static int nbNearestPOIs = 4;
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
		cellData = new ArrayList<>();
		routes = new ArrayList<>();

		//compute spatial indexes
		getPoisInd();
		getNetworkSectionsInd();

		//make network sections feat
		SimpleFeatureType ft = SimpleFeatureUtil.getFeatureType(networkSections.iterator().next(), null);

		logger.info("Compute cell data...");
		for(Feature cell : cells) {

			//get cell id
			String cellId = cell.getAttribute(cellIdAtt).toString();
			if(logger.isDebugEnabled()) logger.debug(cellId);

			//build structure for cell data
			HashMap<String, String> d = new HashMap<String, String>();
			d.put(cellIdAtt, cellId);

			/*/when cell contains at least one POI, set the duration to 0
			if(getPoisInd().query(cell.getDefaultGeometry().getEnvelopeInternal()).size()>0) {
				if(logger.isDebugEnabled()) logger.debug("POI in cell " + cellId);
				d.put("dur_min", "0");
				cellData.add(d);
				continue;
			}*/

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

			//get cell population, if provided
			int population = 0;
			if(populationAtt != null) {
				Object pop = cell.getAttribute(populationAtt);
				if(pop != null) {
					population = (int)Double.parseDouble(pop.toString());
					//d.put("pop_ind", "" + Util.round(getPopulationIndicator(population, popAccMinT), 3));
				} else
					;//d.put("pop_ind", "" + "-999");
			}


			//get cell centroid as origin point
			//possible improvement: take another position depending on the network state inside the cell? Cell is supposed to be small enough?
			Coordinate oC = cell.getGeometry().getCentroid().getCoordinate();
			Node oN = rt.getNode(oC);
			if(oN == null) {
				logger.error("Could not find graph node around cell center: " + oC);
				d.put("dur_min", "-10");
				//d.put("dur_ind", "-10");
				if(populationAtt != null) { /*d.put("acc_ind", "-10");*/ d.put("dmp_ind", "-10"); }
				cellData.add(d);
				continue;
			}
			if( ( (Point)oN.getObject() ).getCoordinate().distance(oC) > 1.3 * resM ) {
				logger.trace("Cell center "+oC+" too far from clodest network node: " + oN.getObject());
				d.put("dur_min", "-20");
				//d.put("dur_ind", "-20");
				if(populationAtt != null) { /*d.put("acc_ind", "-20");*/ d.put("dmp_ind", "-20"); }
				cellData.add(d);
				continue;
			}

			//TODO: improve and use AStar - ask GIS_SE ?
			DijkstraShortestPathFinder pf = rt.getDijkstraShortestPathFinder(oN);

			//compute the routes to all POIs to get the best
			if(logger.isDebugEnabled()) logger.debug("Compute routes to POIs. Nb=" + pois_.length);
			Path pMin = null; double durMin = Double.MAX_VALUE;
			for(Object poi_ : pois_) {
				Feature poi = (Feature) poi_;
				Coordinate dC = poi.getGeometry().getCentroid().getCoordinate();
				//AStarShortestPathFinder pf = rt.getAStarShortestPathFinder(oC, dC);
				//pf.calculate();
				Path p = null; double cost;
				//include POI in path? Cell is supposed to be small enough?
				try {
					//p = pf.getPath();
					Node dN = rt.getNode(dC);

					if(dN == oN) {
						durMin = 0;
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
				if(pMin==null || cost<durMin) {
					pMin=p; durMin=cost;
					if(durMin == 0) break;
				}
			}


			if(durMin > 0 && pMin == null) {
				if(logger.isDebugEnabled()) logger.debug("Could not find path to POI for cell " + cellId + " around " + oC);
				d.put("dur_min", "-30");
				//d.put("dur_ind", "-30");
				if(populationAtt != null) { /*d.put("acc_ind", "-30");*/ d.put("dmp_ind", "-30"); }
				cellData.add(d);
				continue;
			}


			//store data at grid cell level
			d.put("dur_min", "" + durMin);
			//d.put("dur_ind", "" + Util.round(getAccessibilityIndicator(durMin, minDurAccMinT, maxDurAccMinT), 4));
			//d.put("acc_ind", "" + Util.round(getPopulationAccessibilityIndicator(durMin, minDurAccMinT, maxDurAccMinT, population, popAccMinT), 4));
			d.put("dmp_ind", "" + Util.round(getDelayMinPerson(durMin, minDurAccMinT, population), 4));

			cellData.add(d);


			if(pMin != null) {
				//store route
				Feature f = Routing.toFeature(pMin);
				f.setID(cellId);
				f.setAttribute(cellIdAtt, cellId);
				f.setAttribute("dur_min", durMin);
				f.setAttribute("avSpeedKMPerH", Util.round(0.06 * f.getGeometry().getLength()/durMin, 2));
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
