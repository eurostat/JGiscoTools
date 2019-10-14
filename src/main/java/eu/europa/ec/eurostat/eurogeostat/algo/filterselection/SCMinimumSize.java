/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.algo.filterselection;

import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author julien Gaffuri
 *
 */
public class SCMinimumSize implements SelectionCriteria {
	private String geomAtt;
	private double resolution;

	public SCMinimumSize(String geomAtt, double resolution){
		this.geomAtt=geomAtt;
		this.resolution=resolution;
	}

	public boolean keep(Object f) {
		Geometry geom = ((Geometry)((SimpleFeature)f).getAttribute(geomAtt));
		Geometry cu = geom.convexHull();

		//non area
		if(geom.getArea()==0){
			//keep if long enough or CU big enough
			if(cu.getLength()>resolution) return true;
			if(cu.getArea()>resolution*resolution) return true;
			return false;
		}

		//area
		//keep if big enough or CU big enough
		if(cu.getArea()>resolution*resolution) return true;
		if(cu.getArea()>resolution*resolution) return true;
		return false;
	}
}
