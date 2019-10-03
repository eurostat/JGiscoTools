/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.Collection;
import java.util.HashMap;

import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.opencarto.datamodel.Feature;

/**
 * @author julien Gaffuri
 *
 */
public class AccessibilityGrid {

	private Collection<Feature> pois;
	private Collection<Feature> cells;
	private LocalTransportNetworkBuilder ltnb;
	private STRtree poiIndex;


	public AccessibilityGrid(Collection<Feature> gridCells, Collection<Feature> pois) {
		this.cells = gridCells;
		this.pois = pois;

		//build poi spatial index
		poiIndex = new STRtree();
		for(Feature poi : pois)
			//TODO envelope of point?
			poiIndex.insert(poi.getDefaultGeometry().getEnvelopeInternal(), poi);
	}


	public HashMap<String,Object> getAccessibility(Feature cell, int poiNb) {
		//TODO do something special when pois are already within the cell?

		//get cell envelope
		Envelope env = cell.getDefaultGeometry().getEnvelopeInternal();

		//get X pois nearby the cell, with spatial index
		//List<?> poisCell = poiIndex.query(env);
		ItemDistance itemDist = new ItemDistance() {
			@Override
			public double distance(ItemBoundable cellIB, ItemBoundable poiIB) {
				Feature cell = (Feature) cellIB.getItem();
				Feature poi = (Feature) poiIB.getItem();
				return 0;
			}
		};
		Object[] poisCell = poiIndex.nearestNeighbour(env, cell, itemDist , poiNb);

		//build network around the cell
		Envelope envNetwork = null; //TODO build envelope which includes everyone
		Routing localRouting = ltnb.getRoutingAround(envNetwork);

		//get representative point within the cell
		//TODO take another position depending on the network state inside the cell? Cell is supposed to be small enough?
		Coordinate gridC = cell.getDefaultGeometry().getCentroid().getCoordinate();

		//get pathfinder for the grid cell
		DijkstraShortestPathFinder pf = localRouting.getDijkstraShortestPathFinder(gridC);

		//compute routes and store data
		HashMap<String,Object> data = new HashMap<String,Object>();
		for(Object poi_ : poisCell) {
			Feature poi = (Feature) poi_;

			//route from grid cell to poi
			Path p = pf.getPath(localRouting.getNode(poi.getDefaultGeometry().getCentroid().getCoordinate()));

			//TODO: start with something simple: the distance/duration
		}

		//store data

		return data;
	}



	public interface LocalTransportNetworkBuilder {

		//build a routing object
		Routing getRoutingAround(Envelope env);

	}


	public static void main(String[] args) {

		//for each grid cell, compute accessibility
		//for each grid cell, get the list of interestPoints 'nearby' and assess their accessiblity
		//OR
		//for each interest point, get cells 'nearby' and assess their accessibility

	}

}
