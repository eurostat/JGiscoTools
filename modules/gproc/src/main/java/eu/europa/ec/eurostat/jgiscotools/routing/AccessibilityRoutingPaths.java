/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Node;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.util.Util;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;

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
	//the points of interest
	private Collection<Feature> pois = null;
	//the POI id
	private String poiIdAtt = "ID";
	//the number of closest POIs
	private int nbNearestPOIs = 3;
	//straight distance from which we are sure to find the nbNearestPOIs POIs
	double searchDistanceM = 200000; //200km
	//the linear features composing the network
	private Collection<Feature> networkSections = null;

	private Collection<Feature> paths = null;
	/**
	 * The fastest path to the nearest POIs, for each grid cell.
	 * @return
	 */
	public Collection<Feature> getRoutes() { return paths; }

	//the cost
	private String costAttribute = "cost";
	public String getCostAttribute() { return costAttribute; }
	public void setCostAttribute(String costAttribute) { this.costAttribute = costAttribute; }

	//set to true to enable multiprecessor computation
	boolean parallel = true;
	public boolean isParallel() { return parallel; }
	public void setParallel(boolean parallel) { this.parallel = parallel; }


	/**
	 * @param cells
	 * @param cellIdAtt
	 * @param resM
	 * @param pois
	 * @param poiIdAtt
	 * @param networkSections
	 * @param nbNearestPOIs
	 * @param searchDistanceM
	 */
	public AccessibilityRoutingPaths(Collection<Feature> cells, String cellIdAtt, double resM, Collection<Feature> pois, String poiIdAtt, Collection<Feature> networkSections,int nbNearestPOIs, double searchDistanceM) {
		this.cells = cells;
		this.cellIdAtt = cellIdAtt;
		this.resM = resM;
		this.pois = pois;
		this.poiIdAtt = poiIdAtt;
		this.networkSections = networkSections;
		this.nbNearestPOIs = nbNearestPOIs;
		this.searchDistanceM = searchDistanceM;
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




	/**
	 * Compute the paths.
	 */
	public void compute() {

		//create output
		paths = new ArrayList<>();

		//compute spatial indexes
		getPoisInd();
		getNetworkSectionsInd();

		logger.info("Compute accessibility routing paths...");
		Stream<Feature> st = cells.stream(); if(parallel) st = st.parallel();
		st.forEach(cell -> {
			//get cell id
			String cellId = cell.getAttribute(cellIdAtt).toString();
			if(logger.isDebugEnabled()) logger.debug(cellId);

			int nb = (int)( 1 + 1.34 * nbNearestPOIs);
			//if(logger.isDebugEnabled()) logger.debug("Get " + nb + " nearest POIs");
			Envelope env = cell.getGeometry().getEnvelopeInternal(); env.expandBy(searchDistanceM);
			Feature cellPt = new Feature(); cellPt.setGeometry(cell.getGeometry().getCentroid());
			Object[] pois_ = getPoisInd().nearestNeighbour(env, cellPt, itemDist, nb);

			//get an envelope around the cell and surrounding POIs
			env = cell.getGeometry().getEnvelopeInternal();
			for(Object poi_ : pois_)
				env.expandToInclude(((Feature) poi_).getGeometry().getEnvelopeInternal());
			env.expandBy(5000); //TODO how to choose that? Expose parameter?
			if(logger.isTraceEnabled()) logger.trace("Network search size (km): " + 0.001*Math.sqrt(env.getArea()));

			//get network sections in the envelope around the cell and surrounding POIs
			List<?> net_ = getNetworkSectionsInd().query(env);
			if(net_.size() == 0) {
				if(logger.isTraceEnabled())
					logger.trace("Could not find graph for cell: " + cellPt.getGeometry().getCoordinate());
				return;
			}
			if(logger.isTraceEnabled()) logger.trace("Local network size: " + net_.size());
			ArrayList<Feature> net__ = new ArrayList<Feature>();
			for(Object o : net_) net__.add((Feature)o);

			if(logger.isDebugEnabled()) logger.debug("Build the local network");
			Routing rt = new Routing(net__);
			rt.setEdgeWeighter(costAttribute);

			//get cell centroid as origin point
			//possible improvement: take another position depending on the network state inside the cell? Cell is supposed to be small enough?
			Coordinate oC = cellPt.getGeometry().getCoordinate();
			Node oN = rt.getNode(oC);
			if(oN == null) {
				if(logger.isTraceEnabled())
					logger.trace("Could not find graph node around cell center: " + oC);
				return;
			}
			if( ( (Point)oN.getObject() ).getCoordinate().distance(oC) > 1.3 * resM ) {
				if(logger.isTraceEnabled())
					logger.trace("Cell center "+oC+" too far from closest network node: " + oN.getObject());
				return;
			}

			if(logger.isDebugEnabled()) logger.debug("Dijkstra computation");
			DijkstraShortestPathFinder pf = rt.getDijkstraShortestPathFinder(oN);

			if(logger.isDebugEnabled()) logger.debug("Compute paths to POIs. Nb=" + pois_.length);
			ArrayList<Feature> paths_ = new ArrayList<>();
			for(Object poi_ : pois_) {
				Feature poi = (Feature) poi_;
				Coordinate dC = poi.getGeometry().getCentroid().getCoordinate();

				//include POI in path? Cell is supposed to be small enough?
				Node dN = rt.getNode(dC);

				if(dN == oN) {
					//same origin and destination
					Feature f = new Feature();
					f.setGeometry(JTSGeomUtil.toMulti( JTSGeomUtil.createLineString(oC.x, oC.y, dC.x, dC.y) ));
					String poiId = poi.getAttribute(poiIdAtt).toString();
					f.setID(cellId + "_" + poiId);
					f.setAttribute(cellIdAtt, cellId);
					f.setAttribute(poiIdAtt, poiId);
					f.setAttribute("durationMin", Util.round(60.0 * 0.001*f.getGeometry().getLength()/50, 2));
					//f.setAttribute("distanceM", Util.round(f.getGeometry().getLength(), 2));
					//f.setAttribute("avSpeedKMPerH", 50.0);
					paths_.add(f);
					continue;
				}

				Path p = pf.getPath(dN);
				if(p==null) {
					if(logger.isTraceEnabled()) logger.trace("No path found for cell: " + cellId + " to POI " + poi.getAttribute(poiIdAtt) );
					continue;
				}
				double duration = pf.getCost(dN);

				//store path
				//Feature f = Routing.toFeature(p);
				Feature f = new Feature();
				f.setGeometry(JTSGeomUtil.toMulti( JTSGeomUtil.createLineString(oC.x, oC.y, dC.x, dC.y) ));
				String poiId = poi.getAttribute(poiIdAtt).toString();
				f.setID(cellId + "_" + poiId);
				f.setAttribute(cellIdAtt, cellId);
				f.setAttribute(poiIdAtt, poiId);
				f.setAttribute("durationMin", duration);
				//f.setAttribute("distanceM", Util.round(f.getGeometry().getLength(), 2));
				//f.setAttribute("avSpeedKMPerH", Util.round(0.06 * f.getGeometry().getLength()/duration, 2));
				paths_.add(f);
			}

			int nb_ = paths_.size();
			if(nb_ < nbNearestPOIs){
				if(logger.isDebugEnabled()) logger.debug("Not enough POIs found for grid cell (nb="+nb_+"<"+nbNearestPOIs+") " + cellId + " around " + oC);
			} else if(nb_ > nbNearestPOIs) {
				//keep only the nbNearestPOIs fastest paths
				paths_.sort(pathDurationComparator);
				while(paths_.size() > nbNearestPOIs)
					paths_.remove(paths_.size()-1);
			}
			paths.addAll(paths_);
		});
		st.close();
	}



	/**
	 * 
	 */
	public static Comparator<Feature> pathDurationComparator = new Comparator<Feature>() {
		@Override
		public int compare(Feature f1, Feature f2) {
			double d1 = Double.parseDouble(f1.getAttribute("durationMin").toString());
			double d2 = Double.parseDouble(f2.getAttribute("durationMin").toString());
			return (int)(1e6*(d1-d2));
		}
	};


	/**
	 * Compute statistics on a collection of routing paths.
	 * 
	 * @param paths
	 * @param cellIdAtt
	 * @return
	 */
	public static StatsHypercube computeStats(Collection<Feature> paths, String cellIdAtt) {
		//output structure
		StatsHypercube hc = new StatsHypercube(cellIdAtt, "accInd");

		if(paths.size() == 0) return hc;

		//while there are paths
		while(paths.size() >0) {
			//get cell id of the first path
			String cellId = paths.iterator().next().getAttribute(cellIdAtt).toString();
			if(logger.isDebugEnabled()) logger.debug(cellId);

			//get all paths of the cell
			ArrayList<Feature> paths_ = new ArrayList<Feature>();
			for(Feature path : paths)
				if(path.getAttribute(cellIdAtt).toString().equals(cellId))
					paths_.add(path);

			//remove
			paths.removeAll(paths_);

			//sort paths
			paths_.sort(pathDurationComparator);

			//compute stats on grid cell id
			double val;

			//Compute indicator 1 - Shortest transport time to the nearest service
			//accInd = nearest
			val = Double.parseDouble(paths_.get(0).getAttribute("durationMin").toString());
			hc.stats.add(new Stat(val, cellIdAtt, cellId, "accInd", "nearest"));

			//Compute indicator 2- Average transport time to the X nearest services
			//accInd = ave3near
			int x = Math.min(3, paths_.size());
			val = 0;
			for(int i=0; i<x; i++)
				val += Double.parseDouble(paths_.get(i).getAttribute("durationMin").toString());
			val = val/x;
			hc.stats.add(new Stat(val, cellIdAtt, cellId, "accInd", "ave3near"));
		}
		return hc;
	}

}
