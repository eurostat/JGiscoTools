/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.util;

import javax.measure.Unit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.referencing.util.CRSUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Basic conversion functions for mercator projection.
 * 
 * @author julien Gaffuri
 *
 */
public class CRSTypeUtil {
	public final static Logger LOGGER = LogManager.getLogger(CRSTypeUtil.class.getName());

	public enum CRSType { GEOG, CARTO, UNKNOWN }

	private static CRSType getCRSType(Unit<?> unit) {
		if(unit == null) return CRSType.UNKNOWN;
		switch (unit.toString()) {
		case "": return CRSType.UNKNOWN;
		case "°": return CRSType.GEOG;
		case "deg": return CRSType.GEOG;
		case "dms": return CRSType.GEOG;
		case "degree": return CRSType.GEOG;
		case "m": return CRSType.CARTO;
		default:
			LOGGER.warn("Unexpected unit of measure for projection: "+unit);
			return CRSType.UNKNOWN;
		}
	}
	public static CRSType getCRSType(CoordinateReferenceSystem crs) {
		return getCRSType(CRSUtilities.getUnit(crs.getCoordinateSystem()));
	}

}
