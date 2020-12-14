/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.java4eurostat.util.Util;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
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

	private EdgeWeighter edgeWeighter = null;
	/**
	 * The weighter used to estimate the cost of each network section when computing shortest paths.
	 * 
	 * @param edgeWeighter
	 */
	public void setEdgeWeighter(EdgeWeighter edgeWeighter) { this.edgeWeighter = edgeWeighter; }

	/**
	 * Set the weighter based on a speed calculator.
	 * 
	 * @param sc
	 */
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

	/**
	 * @return The edge weighter.
	 */
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


	private Collection<Feature> routes = null;
	/**
	 * The fastest route to the nearest POIs, for each grid cell.
	 * @return
	 */
	public Collection<Feature> getRoutes() { return routes; }




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
	 * Compute the routes.
	 */
	public void compute() {

		//create output
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

			int nb = (int)(1.5 * nbNearestPOIs);
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
				continue;
			}
			if(logger.isTraceEnabled()) logger.trace("Local network size: " + net_.size());
			ArrayList<Feature> net__ = new ArrayList<Feature>();
			for(Object o : net_) net__.add((Feature)o);

			//build the surrounding network
			Routing rt = new Routing(net__, ft);
			rt.setEdgeWeighter(getEdgeWeighter());

			//get cell centroid as origin point
			//possible improvement: take another position depending on the network state inside the cell? Cell is supposed to be small enough?
			Coordinate oC = cellPt.getGeometry().getCoordinate();
			Node oN = rt.getNode(oC);
			if(oN == null) {
				if(logger.isTraceEnabled())
					logger.trace("Could not find graph node around cell center: " + oC);
				continue;
			}
			if( ( (Point)oN.getObject() ).getCoordinate().distance(oC) > 1.3 * resM ) {
				if(logger.isTraceEnabled())
					logger.trace("Cell center "+oC+" too far from closest network node: " + oN.getObject());
				continue;
			}

			//TODO: improve and use AStar - ask GIS_SE ?
			DijkstraShortestPathFinder pf = rt.getDijkstraShortestPathFinder(oN);

			//compute the routes to the selected POIs
			if(logger.isDebugEnabled()) logger.debug("Compute routes to POIs. Nb=" + pois_.length);
			ArrayList<Feature> routes_ = new ArrayList<>();
			for(Object poi_ : pois_) {
				Feature poi = (Feature) poi_;
				Coordinate dC = poi.getGeometry().getCentroid().getCoordinate();
				//AStarShortestPathFinder pf = rt.getAStarShortestPathFinder(oC, dC);
				//pf.calculate();
				//p = pf.getPath();

				//include POI in path? Cell is supposed to be small enough?
				Node dN = rt.getNode(dC);

				if(dN == oN) {
					//same origin and destination					
					Feature f = new Feature();
					LineString geom = new GeometryFactory().createLineString(new Coordinate[] {oC,dC});
					f.setGeometry(JTSGeomUtil.toMulti(geom));
					String poiId = poi.getAttribute(poiIdAtt).toString();
					f.setID(cellId + "_" + poiId);
					f.setAttribute(cellIdAtt, cellId);
					f.setAttribute(poiIdAtt, poiId);
					f.setAttribute("durationMin", Util.round(60.0 * 0.001*geom.getLength()/50, 2));
					f.setAttribute("distanceM", Util.round(f.getGeometry().getLength(), 2));
					f.setAttribute("avSpeedKMPerH", 50.0);
					routes_.add(f);
					continue;
				}

				Path p = pf.getPath(dN);
				if(p==null) {
					if(logger.isTraceEnabled()) logger.trace("No path found for cell: " + cellId + " to POI " + poi.getAttribute(poiIdAtt) );
					continue;
				}
				double duration = pf.getCost(dN);
				//For A*: see https://gis.stackexchange.com/questions/337968/how-to-get-path-cost-in/337972#337972

				//store route
				Feature f = Routing.toFeature(p);
				String poiId = poi.getAttribute(poiIdAtt).toString();
				f.setID(cellId + "_" + poiId);
				f.setAttribute(cellIdAtt, cellId);
				f.setAttribute(poiIdAtt, poiId);
				f.setAttribute("durationMin", Util.round(duration, 2));
				f.setAttribute("distanceM", Util.round(f.getGeometry().getLength(), 2));
				f.setAttribute("avSpeedKMPerH", Util.round(0.06 * f.getGeometry().getLength()/duration, 2));
				routes_.add(f);
			}

			int nb_ = routes_.size();
			if(nb_ < this.nbNearestPOIs)
				logger.info("Not enough POIs found for grid cell (nb="+nb_+"<"+nbNearestPOIs+") " + cellId + " around " + oC);
			else if(nb_ > nbNearestPOIs) {
				//TODO keep only the fastest nbNearestPOIs
			}
			routes.addAll(routes_);
		}
	}

}
