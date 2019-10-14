/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.algo.measure;

import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Geometry;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class Convexity {
	public static final int NOT_DEFINED = -1;

	//0: not convex, 1: very convex.
	public static double get(Geometry geom){
		if (geom == null || geom.isEmpty()) return NOT_DEFINED;
		double cuArea = (new ConvexHull(geom)).getConvexHull().getArea();
		if (cuArea == 0.0) return NOT_DEFINED;
		return Math.round(1000*geom.getArea()/cuArea)/1000.0;
	}

}
