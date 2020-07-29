/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.util;

import java.awt.Toolkit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Basic conversion functions for mercator projection.
 * 
 * @author julien Gaffuri
 *
 */
public class ProjectionUtil {
	public final static Logger LOGGER = LogManager.getLogger(ProjectionUtil.class.getName());

	public static final double EARTH_RADIUS_M = 6378137;
	public static final double degToRadFactor = Math.PI/180;
	public static final double ED = EARTH_RADIUS_M * degToRadFactor;
	public static final double PHI_MAX_RAD = Math.asin((Math.exp(2*Math.PI)-1)/(Math.exp(2*Math.PI)+1));
	public static final double PHI_MAX_DEG = PHI_MAX_RAD / degToRadFactor;


	// conversions between (XGeo, YGeo) and (lon, lat)

	/**
	 * @param lon The longitude.
	 * @return The X geo coordinate.
	 */
	public static double getXGeo(double lon) {
		return lon * ED;
	}

	/**
	 * @param xGeo The X geo coordinate.
	 * @return The longitude.
	 */
	public static double getLon(double xGeo) {
		return xGeo / ED;
	}

	/**
	 * @param lat The latitude.
	 * @return The Y geo coordinate.
	 */
	public static double getYGeo(double lat) {
		double s = Math.sin(lat * degToRadFactor);
		return EARTH_RADIUS_M * 0.5 * Math.log((1+s)/(1-s));
	}

	/**
	 * @param yGeo The Y geo coordinate.
	 * @return The latitude.
	 */
	public static double getLat(double yGeo) {
		return 90*( 4* Math.atan(Math.exp(yGeo/EARTH_RADIUS_M)) / Math.PI - 1 );
	}


	// conversions between (XPix, YPix) and (lon, lat)

	/**
	 * @param lon The longitude.
	 * @param zoomLevel The zoom level.
	 * @return The X pixel coordinate.
	 */
	public static double getXPixFromLon(double lon, int zoomLevel) {
		double x = (fit(lon, -180, 180) + 180) / 360; 
		int s = getTotalMapSizeInPixel(zoomLevel);
		return fit(s*x+0.5, 0, s-1);
	}

	/**
	 * @param XPix The X pixel coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The longitude.
	 */
	public static double getLonFromXPix(double XPix, int zoomLevel) {
		double s = getTotalMapSizeInPixel(zoomLevel);
		return 360 * ((fit(XPix, 0, s-1) / s) - 0.5);
	}

	/**
	 * @param lat The latitude.
	 * @param zoomLevel The zoom level.
	 * @return The Y pixel coordinate.
	 */
	public static double getYPixFromLat(double lat, int zoomLevel) {
		double sin = Math.sin(  fit(lat, -PHI_MAX_DEG, PHI_MAX_DEG) * degToRadFactor);
		double y = 0.5 - Math.log((1 + sin) / (1 - sin)) / (4 * Math.PI);
		int s = getTotalMapSizeInPixel(zoomLevel);
		return fit(s*y+0.5, 0, s-1);
	}

	/**
	 * @param YPix The Y pixel coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The latitude.
	 */
	public static double getLatFromYPix(double YPix, int zoomLevel) {
		double s = getTotalMapSizeInPixel(zoomLevel);
		double y = 0.5 - (fit(YPix, 0, s-1) / s);
		return 90 - 360 * Math.atan(Math.exp(-y*2*Math.PI)) / Math.PI;
	}


	// conversions between (XPix, YPix) and (XGeo, YGeo)

	/**
	 * @param xGeo The X geo coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The X pixel coordinate.
	 */
	public static double getXPixFromXGeo(double xGeo, int zoomLevel) {
		return getXPixFromLon(getLon(xGeo), zoomLevel);
	}

	/**
	 * @param xPix The X pixel coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The X geo coordinate.
	 */
	public static double getXGeoFromXPix(double xPix, int zoomLevel) {
		return getXGeo(getLonFromXPix(xPix, zoomLevel));
	}

	/**
	 * @param yGeo The Y geo coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The Y pixel coordinate.
	 */
	public static double getYPixFromYGeo(double yGeo, int zoomLevel) {
		return getYPixFromLat(getLat(yGeo), zoomLevel);
	}

	/**
	 * @param yPix The Y pixel coordinate.
	 * @param zoomLevel The zoom level.
	 * @return The Y geo coordinate.
	 */
	public static double getYGeoFromYPix(double yPix, int zoomLevel) {
		return getYGeo(getLatFromYPix(yPix, zoomLevel));
	}



	/**
	 * Ensure x is within min and max.
	 * 
	 * @param x
	 * @param min
	 * @param max
	 * @return
	 */
	private static double fit(double x, double min, double max) { return Math.min(Math.max(x, min), max); }

	/**
	 * @param zoomLevel The zoom level.
	 * @return The total map size in pixel.
	 */
	private static int getTotalMapSizeInPixel(int zoomLevel) { return 256 << zoomLevel; }

	/**
	 * @param zoomLevel The zoom level.
	 * @return The pixel size in meters at the equator.
	 */
	public static double getPixelSizeEqu(int zoomLevel) {
		return 2*Math.PI*EARTH_RADIUS_M / getTotalMapSizeInPixel(zoomLevel);
	}

	/**
	 * @param lat The latitude.
	 * @return Deformation factor from the projection (depends only on the latitude). To retrieve the real distance from a projected one, multiply by this factor.
	 */
	public static double getDeformationFactor(double lat) {
		return Math.abs( Math.cos( fit(degToRadFactor*lat, -PHI_MAX_RAD, PHI_MAX_RAD) ) );
	}

	/**
	 * @param lat The latitude.
	 * @param zoomLevel The zoom level.
	 * @return The pixel size in meters at a given lat.
	 */
	public static double getPixelSize(double lat, int zoomLevel) {
		return getDeformationFactor(lat) * getPixelSizeEqu(zoomLevel);
	}

	/**
	 * The screen pixel size (in m)
	 */
	public final static double METERS_PER_PIXEL = 0.02540005/Toolkit.getDefaultToolkit().getScreenResolution();

	/**
	 * @param lat The latitude.
	 * @param zoomLevel The zoom level.
	 * @return The scale (the S of 1:S).
	 */
	public static double getScale(double lat, int zoomLevel) { return getPixelSize(lat, zoomLevel) / METERS_PER_PIXEL; }



}
