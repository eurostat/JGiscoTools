package eu.europa.ec.eurostat.jgiscotools.algo.measure;

import org.locationtech.jts.geom.Geometry;

/**
 * Maesure if a shape is close to a circle or not.
 * 
 * @author julien Gaffuri
 *
 */
public class Circularity {

	/**
	 * @param lr Should be a linear ring, or a polygon, preferably without hole.
	 * @return 0 for a circle, 0.12838 for a square, and more for non-circular shapes
	 */
	public static double get(Geometry lr) {
		return lr.getLength()/(2*Math.sqrt(Math.PI*lr.getArea())) - 1;
	}
}
