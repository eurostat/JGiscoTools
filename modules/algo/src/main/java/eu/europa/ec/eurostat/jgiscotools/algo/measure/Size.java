/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.measure;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * @author julien Gaffuri
 *
 */
public class Size {

	public static boolean hasDiagonalLongerThan(Geometry geom, double val) {
		Coordinate[] cs = geom.getCoordinates();
		Coordinate ci;
		for(int i=0; i<cs.length; i++) {
			ci = cs[i];
			for(int j=i+1; j<cs.length; j++)
				if( ci.distance(cs[j]) >= val ) return true;
		}
		return false;
	}

	public static double getLongestDiagonalLength(Geometry geom){
		Coordinate[] cs = geom.getCoordinates();
		double d, max=0;
		Coordinate ci;
		for(int i=0; i<cs.length; i++) {
			ci = cs[i];
			for(int j=i+1; j<cs.length; j++) {
				d = ci.distance(cs[j]);
				if(d>max) max=d;
			}
		}
		return max;
	}

}
