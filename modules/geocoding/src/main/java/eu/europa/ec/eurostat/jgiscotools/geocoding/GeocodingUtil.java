package eu.europa.ec.eurostat.jgiscotools.geocoding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

public class GeocodingUtil {
	private final static Logger LOGGER = LogManager.getLogger(GeocodingUtil.class.getName());

	public static ArrayList<Feature> geocodingComparison(Collection<Map<String,String>> hospitals, String lon1, String lat1, String lon2, String lat2) {
		CoordinateReferenceSystem crs = null;
		try { crs = CRS.decode("EPSG:4326"); } catch (Exception e) { e.printStackTrace(); }

		//build linear features
		ArrayList<Feature> fs = new ArrayList<>();
		for(Map<String, String> h : hospitals) {
			Coordinate c1 = new Coordinate(Double.parseDouble(h.get(lon1)), Double.parseDouble(h.get(lat1)));
			Coordinate c2 = new Coordinate(Double.parseDouble(h.get(lon2)), Double.parseDouble(h.get(lat2)));
			LineString line = new GeometryFactory().createLineString( new Coordinate[] {c2, c1} );

			Feature f = new Feature();
			f.setGeometry(line);
			//TODO add all attributes of input features?

			//compute and store orthodromic distance
			f.setAttribute("dist", -1);
			try {
				double dist = JTS.orthodromicDistance( c1, c2, crs);
				f.setAttribute("dist", dist);
			} catch (TransformException e) {
				LOGGER.warn("Could not compute orthodromic distance in geocoding comparision.");
				e.printStackTrace();
			}

			fs.add(f);
		}
		return fs;
	}


}
