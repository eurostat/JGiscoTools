/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
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
	private double infDist;
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


	public HashMap<String,Object> getAccessibility(Feature cell) {

		//get cell sourrounding
		Envelope env = cell.getDefaultGeometry().getEnvelopeInternal();
		env.expandBy(infDist);

		//build network around the cell
		Routing localRouting = ltnb.getRoutingAround(env);

		//get interest points nearby, with spatial index
		List<?> poisCell = poiIndex.query(env);

		//compute routes
		for(Object poi_ : poisCell) {
			poi
		}

		//store data
		HashMap<String,Object> data = new HashMap<String,Object>();

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
