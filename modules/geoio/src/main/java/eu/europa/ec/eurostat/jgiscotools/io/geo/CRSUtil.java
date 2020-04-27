package eu.europa.ec.eurostat.jgiscotools.io.geo;

import java.util.Collection;

import javax.measure.Unit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.util.CRSUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.util.CRSType;

public class CRSUtil {
	public final static Logger LOGGER = LogManager.getLogger(CRSUtil.class.getName());


	//geographic: ETRS89 4937 (3D) 4258(2D)
	//# ETRS89
	//<4258> +proj=longlat +ellps=GRS80 +no_defs  <>
	//private static CoordinateReferenceSystem ETRS89_2D_CRS;
	//private static CoordinateReferenceSystem ETRS89_3D_CRS;

	//projected: ETRS89 ETRS-LAEA 3035
	//# ETRS89 / ETRS-LAEA
	//<3035> +proj=laea +lat_0=52 +lon_0=10 +x_0=4321000 +y_0=3210000 +ellps=GRS80 +units=m +no_defs  <>
	public static int WGS_84_CRS_EPSG = 4326;
	private static CoordinateReferenceSystem WGS_84_CRS;
	public static CoordinateReferenceSystem getWGS_84_CRS() {
		if(WGS_84_CRS == null) WGS_84_CRS = getCRS(WGS_84_CRS_EPSG);
		return WGS_84_CRS;
	}

	//3785->used in arcgis+"Popular Visualisation CRS / Mercator"
	//3857-> EPSG:3857 -- WGS84 Web Mercator (Auxiliary Sphere). Projection used in many popular web mapping applications (Google/Bing/OpenStreetMap/etc). Sometimes known as EPSG:900913.
	public static int WEB_MERCATOR_CRS_EPSG = 3857;
	private static CoordinateReferenceSystem WEB_MERCATOR_CRS;
	public static CoordinateReferenceSystem getWEB_MERCATOR_CRS() {
		if(WEB_MERCATOR_CRS == null) WEB_MERCATOR_CRS = getCRS(WEB_MERCATOR_CRS_EPSG);
		return WEB_MERCATOR_CRS;
	}

	public static int ETRS89_LAEA_SRS_EPSG = 3035;
	private static CoordinateReferenceSystem ETRS89_LAEA_CRS;
	public static CoordinateReferenceSystem getETRS89_LAEA_CRS() {
		if(ETRS89_LAEA_CRS == null) ETRS89_LAEA_CRS = getCRS(ETRS89_LAEA_SRS_EPSG);
		return ETRS89_LAEA_CRS;
	}

	/*public static int ETRS89_2D_SRS_EPSG = 4937;
	private static CoordinateReferenceSystem ETRS89_3D_CRS;
	public static CoordinateReferenceSystem getETRS89_3D_CRS() {
		if(ETRS89_3D_CRS == null) ETRS89_3D_CRS = getCRS(ETRS89_2D_SRS_EPSG);
		return ETRS89_3D_CRS;
	}*/

	public static int ETRS89_3D_SRS_EPSG = 4258;
	private static CoordinateReferenceSystem ETRS89_2D_CRS;
	public static CoordinateReferenceSystem getETRS89_2D_CRS() {
		if(ETRS89_2D_CRS == null) ETRS89_2D_CRS = getCRS(ETRS89_3D_SRS_EPSG);
		return ETRS89_2D_CRS;
	}



	public static CoordinateReferenceSystem getCRS(int EPSG){
		try {
			return CRS.decode("EPSG:" + EPSG);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Coordinate project(Coordinate c, CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {
		Point pt = new GeometryFactory().createPoint(c);
		pt = (Point) project(pt, sourceCRS, targetCRS);
		return pt.getCoordinate();
	}

	public static Geometry project(Geometry geom, CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {
		try {
			Geometry outGeom = JTS.transform(geom, CRS.findMathTransform(sourceCRS, targetCRS, true));
			return outGeom;
		} catch (Exception e) {
			LOGGER.error("Error while reprojecting.");
			e.printStackTrace();
		}
		return null;
	}



	public static Geometry toWebMercator(Geometry geom, CoordinateReferenceSystem sourceCRS) {
		return project(geom, sourceCRS, getWEB_MERCATOR_CRS());
	}

	public static void toWebMercator(Collection<Feature> fs, CoordinateReferenceSystem sourceCRS) {
		for(Feature f : fs)
			f.setGeometry( toWebMercator(f.getGeometry(), sourceCRS) );
	}



	public static Geometry toWGS84(Geometry geom, CoordinateReferenceSystem sourceCRS) {
		return project(geom, sourceCRS, getWGS_84_CRS());
	}

	public static void toWGS84(Collection<Feature> fs, CoordinateReferenceSystem sourceCRS) {
		for(Feature f : fs)
			f.setGeometry( toWGS84(f.getGeometry(), sourceCRS) );
	}



	public static Geometry toLAEA(Geometry geom, CoordinateReferenceSystem sourceCRS) {
		return project(geom, sourceCRS, getETRS89_LAEA_CRS());
	}

	public static void toLAEA(Collection<Feature> fs, CoordinateReferenceSystem sourceCRS) {
		for(Feature f : fs)
			f.setGeometry( toLAEA(f.getGeometry(), sourceCRS) );
	}

	

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



	//TODO try lookup thing instead (see geotools doc)
	public static int getEPSGCode(CoordinateReferenceSystem crs) {
		try {
			for(ReferenceIdentifier ri : crs.getIdentifiers()) {
				if("EPSG".equals(ri.getCodeSpace()))
					return Integer.parseInt(ri.getCode());
			}
		} catch (NumberFormatException e) {}
		//LOGGER.warn("Could not find EPSG code for CRS: "+crs.toWKT());
		return -1;
	}



	//http://epsg.io/3035.wkt
	//https://epsg.io/3035.wkt
	/*	private static CoordinateReferenceSystem getFromEPSGIOWKT(String epsgCode) {

		//get wkt from epsg.io
		//String url_ = "https://epsg.io/"+epsgCode+".wkt";
		String url_ = "https://epsg.io/"+epsgCode+".esriwkt";
		String wkt = null;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url_).openStream()));
			wkt = in.readLine();
		} catch (MalformedURLException e) {
			LOGGER.error("Could not parse URL " + url_);
			return null;
		} catch (IOException e) {
			LOGGER.error("Could not get WKT for CRS " + epsgCode + " from URL " + url_);
			return null;
		}

		//parse
		CoordinateReferenceSystem crs = null;
		try {
			crs = CRS.parseWKT(wkt);
		} catch (FactoryException e) {
			LOGGER.error("Could not parse WKT for CRS " + epsgCode);
			LOGGER.error(wkt);
			return null;
		}
		return crs;
	}
	 */

}
