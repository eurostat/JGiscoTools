/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.measure;

import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class Elongation {

	//compute the approximation of a polygon width
	//source: https://gis.stackexchange.com/questions/20279/calculating-average-width-of-polygon
	//quote: MISC {20279, TITLE = {Calculating average width of polygon?}, AUTHOR = {whuber (gis.stackexchange.com/users/664/whuber)}, HOWPUBLISHED = {GIS}, NOTE = {URL:gis.stackexchange.com/q/20279/664 (version: 2013-08-13)}, EPRINT = {gis.stackexchange.com/q/20279}, URL = {gis.stackexchange.com/q/20279} }
	public static class WidthApproximation {
		//the exact approximation of the dimenstions
		public double width, length;
		//the estimated elongation, indicating the pertinence of the approximaeion - the smaller, the better
		public double elongation;
		//another approximation of the width
		//public double value_;
		//an error factor indicating the pertinence of the approximaeion
		//public double appr;
	}
	public static WidthApproximation getWidthApproximation(Polygon poly) {
		WidthApproximation wa = new WidthApproximation();
		double a = poly.getArea(), p = poly.getLength();
		wa.width = (p-Math.sqrt(p*p-16*a))*0.25;
		wa.length = a / wa.width;
		wa.elongation = wa.width / wa.length;
		//wa.value_ = 2*a/p;
		//wa.appr = Math.abs(wa.value - wa.value_) / wa.value;
		return wa;
	}



	// 0: line, 1: perfect square/circle
	public static double get(Geometry geom){
		//Polygon ssr = SmallestSurroundingRectangle.get(geom);
		Polygon ssr = (Polygon) new MinimumDiameter(geom).getMinimumRectangle();
		if (ssr == null) return 0;
		Coordinate[] coords = ssr.getCoordinates();
		Coordinate c1 = coords[1];
		double lg1 = coords[0].distance(c1);
		double lg2 = c1.distance(coords[2]);
		if (lg1>lg2) return Math.round(100*lg2/lg1)/100.0;
		return Math.round(100*lg1/lg2)/100.0;
	}

}
